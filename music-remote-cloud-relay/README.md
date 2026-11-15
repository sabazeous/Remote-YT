# Music Remote — Standalone Cloud Relay

Deploy this tiny Node.js WebSocket relay to **Render**, **Replit**, or **Railway** so your Windows laptop and Android phone can connect over TLS (`wss://<your-domain>/ws`) from any network without authentication walls.

## Deploy on Render (Free Tier, ~1 minute)
1. Push this folder to a GitHub repository.
2. On [render.com](https://render.com), click **New → Web Service** and select the repo.
3. Build Command: `npm install`
4. Start Command: `npm start`
5. Once live (e.g. `https://my-music-relay.onrender.com`), your **Cloud Relay WSS URL** is:
   `wss://my-music-relay.onrender.com/ws`

## Deploy on Replit
1. Create a **Node.js** Repl and upload `package.json` and `server.mjs`.
2. Click **Run**.
3. Copy your Repl HTTPS URL and replace `https://` with `wss://` + `/ws` at the end.
