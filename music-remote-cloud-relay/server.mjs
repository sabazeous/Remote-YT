import http from 'node:http';
import crypto from 'node:crypto';
import { WebSocketServer, WebSocket } from 'ws';

const PORT = Number(process.env.PORT || 3000);
const sessions = new Map();

function getOrCreateSession(code) {
  const normalized = String(code || '').trim().toUpperCase();
  if (!normalized) return null;
  let s = sessions.get(normalized);
  if (!s) {
    s = {
      pairingCode: normalized,
      companionName: 'Windows Laptop',
      companionSocket: null,
      remoteSockets: new Set(),
      chromeConnected: false,
      lastHeartbeatAt: null,
      playbackState: {
        title: '',
        artist: '',
        album: '',
        artworkUrl: '',
        isPlaying: false,
        positionSec: 0,
        durationSec: 0,
        volume: 80,
        isMuted: false,
        hasActiveTab: false,
        tabTitle: '',
        updatedAt: 0,
      },
    };
    sessions.set(normalized, s);
  }
  return s;
}

function buildStatus(session) {
  const companionOnline = Boolean(
    session.companionSocket && session.companionSocket.readyState === WebSocket.OPEN
  );
  return {
    pairingCode: session.pairingCode,
    companionOnline,
    companionName: session.companionName,
    chromeConnected: companionOnline && (session.chromeConnected || session.playbackState.hasActiveTab),
    remoteCount: session.remoteSockets.size,
    lastHeartbeatAt: session.lastHeartbeatAt,
  };
}

function sendJson(ws, payload) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(payload));
  }
}

function broadcastRemotes(session, payload) {
  const raw = JSON.stringify(payload);
  for (const sock of session.remoteSockets) {
    if (sock.readyState === WebSocket.OPEN) {
      sock.send(raw);
    }
  }
}

const server = http.createServer((req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');

  if (req.url === '/api/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ ok: true, activeSessions: sessions.size }));
    return;
  }

  res.writeHead(200, { 'Content-Type': 'text/plain; charset=utf-8' });
  res.end('Music Remote Cloud Relay is running. Connect via wss://<your-host>/ws');
});

const wss = new WebSocketServer({ server, path: '/ws' });
const metaMap = new WeakMap();

wss.on('connection', (ws) => {
  metaMap.set(ws, { role: null, pairingCode: null, isAlive: true });

  ws.on('pong', () => {
    const m = metaMap.get(ws);
    if (m) m.isAlive = true;
  });

  ws.on('message', (raw) => {
    const m = metaMap.get(ws);
    if (m) m.isAlive = true;

    let msg;
    try {
      msg = JSON.parse(raw.toString());
    } catch {
      return;
    }

    if (msg.type === 'ping') {
      sendJson(ws, { type: 'pong', timestamp: Date.now() });
      return;
    }

    if (msg.type === 'register_companion') {
      const code = String(msg.pairingCode || '').trim().toUpperCase();
      if (!code) {
        sendJson(ws, { type: 'error', message: 'Pairing code is required.' });
        return;
      }
      const session = getOrCreateSession(code);
      if (session.companionSocket && session.companionSocket !== ws) {
        try {
          session.companionSocket.close(1000, 'Replaced by newer companion');
        } catch {}
      }
      session.companionSocket = ws;
      session.companionName = msg.deviceName?.trim() || session.companionName;
      session.lastHeartbeatAt = Date.now();
      m.role = 'companion';
      m.pairingCode = code;

      const status = buildStatus(session);
      sendJson(ws, { type: 'registered', pairingCode: code, status });
      broadcastRemotes(session, { type: 'session_status', status });
      return;
    }

    if (msg.type === 'register_remote') {
      const code = String(msg.pairingCode || '').trim().toUpperCase();
      if (!code) {
        sendJson(ws, { type: 'error', message: 'Pairing code is required.' });
        return;
      }
      const session = getOrCreateSession(code);
      session.remoteSockets.add(ws);
      m.role = 'remote';
      m.pairingCode = code;

      const status = buildStatus(session);
      sendJson(ws, { type: 'registered', pairingCode: code, status });
      sendJson(ws, { type: 'session_status', status });
      sendJson(ws, { type: 'playback_state', state: session.playbackState });
      broadcastRemotes(session, { type: 'session_status', status });

      if (session.companionSocket && session.companionSocket.readyState === WebSocket.OPEN) {
        sendJson(session.companionSocket, {
          type: 'command',
          command: { commandId: crypto.randomUUID(), type: 'requestSync', issuedAt: Date.now() },
        });
      }
      return;
    }

    const code = m?.pairingCode || String(msg.pairingCode || '').trim().toUpperCase();
    const session = sessions.get(code);
    if (!session) return;

    if (msg.type === 'playback_state' && msg.state && m?.role === 'companion') {
      session.playbackState = { ...session.playbackState, ...msg.state, updatedAt: Date.now() };
      session.chromeConnected = Boolean(msg.state.hasActiveTab || msg.state.chromeConnected);
      session.lastHeartbeatAt = Date.now();
      broadcastRemotes(session, { type: 'playback_state', state: session.playbackState });
      broadcastRemotes(session, { type: 'session_status', status: buildStatus(session) });
      return;
    }

    if (msg.type === 'session_status' && msg.status && m?.role === 'companion') {
      session.chromeConnected = Boolean(msg.status.chromeConnected);
      session.companionName = msg.status.companionName || session.companionName;
      session.lastHeartbeatAt = Date.now();
      broadcastRemotes(session, { type: 'session_status', status: buildStatus(session) });
      return;
    }

    if (msg.type === 'command' && msg.command && m?.role === 'remote') {
      if (!session.companionSocket || session.companionSocket.readyState !== WebSocket.OPEN) {
        sendJson(ws, {
          type: 'command_ack',
          ack: { commandId: msg.command.commandId, success: false, error: 'Windows laptop is offline.' },
        });
        return;
      }
      sendJson(session.companionSocket, { type: 'command', command: msg.command });
      return;
    }

    if (msg.type === 'command_ack' && msg.ack && m?.role === 'companion') {
      if (msg.ack.state) {
        session.playbackState = { ...session.playbackState, ...msg.ack.state, updatedAt: Date.now() };
        broadcastRemotes(session, { type: 'playback_state', state: session.playbackState });
      }
      broadcastRemotes(session, { type: 'command_ack', ack: msg.ack });
    }
  });

  ws.on('close', () => {
    const m = metaMap.get(ws);
    if (!m?.pairingCode) return;
    const session = sessions.get(m.pairingCode);
    if (!session) return;
    if (m.role === 'companion' && session.companionSocket === ws) {
      session.companionSocket = null;
      session.chromeConnected = false;
      broadcastRemotes(session, { type: 'session_status', status: buildStatus(session) });
    } else if (m.role === 'remote') {
      session.remoteSockets.delete(ws);
      broadcastRemotes(session, { type: 'session_status', status: buildStatus(session) });
    }
  });
});

setInterval(() => {
  for (const client of wss.clients) {
    const m = metaMap.get(client);
    if (m && !m.isAlive) {
      client.terminate();
      continue;
    }
    if (m) m.isAlive = false;
    client.ping();
  }
}, 25000);

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Music Remote Cloud Relay listening on port ${PORT}`);
});
