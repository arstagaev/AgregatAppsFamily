# CMP Web Target (Wasm) Guide

## What is implemented

- Separate web target (`wasmJs`) with real app screens (login/documents).
- QR scanner disabled for web target.
- Web API base is `/api`.
- `/api` is proxied to `https://agrapp.agregatka.ru` to bypass browser CORS limits.
- Web target uses in-memory cache stores (no SQLDelight persistence in browser).

## Local web testing

### 1) Development mode (recommended)

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

- Open printed URL (usually `http://localhost:8081`).
- This starts webpack dev server with `/api` proxy already configured.

### 2) Build production bundle

```bash
./gradlew :composeApp:wasmJsBrowserDistribution
```

Artifacts are generated in:

- `composeApp/build/dist/wasmJs/productionExecutable/`

## Remote server launch (Docker Compose, port 8444)

This is the simplest standalone setup:

- One container.
- No host-level nginx reverse proxy.
- One direct endpoint: `https://10.0.4.180:8444/`.

### One-command packaging (recommended)

Use:

```bash
./deploy/scripts/prepare-web-remote-bundle.sh
```

This creates one upload-ready folder:

- `deploy/out/web-remote-package/`

Upload that folder to server and run:

```bash
cd ~/trrapp
bash run-on-server.sh
```

`run-on-server.sh` will:

1. Generate self-signed TLS cert in `./certs` (if not found).
2. Run `docker compose up -d --force-recreate`.
3. Run health check on `https://127.0.0.1:8444/healthz`.

### Files required on server

You need these in one folder (example: `~/trrapp`):

- `docker-compose.yml`
- `mobileagregatcrm-web-docker.conf`
- `run-on-server.sh`
- `web-dist/`
- `certs/` (can be empty)

### Example folder layout

```text
~/trrapp/
  docker-compose.yml
  mobileagregatcrm-web-docker.conf
  run-on-server.sh
  certs/
  web-dist/
    index.html
    composeApp.js
    *.wasm
    composeResources/...
```

### docker-compose.yml

```yaml
services:
  mobileagregatcrm-web:
    image: nginx:1.27-alpine
    container_name: mobileagregatcrm-web
    restart: unless-stopped
    ports:
      - "8444:8444"
    volumes:
      - ./web-dist:/usr/share/nginx/html:ro
      - ./mobileagregatcrm-web-docker.conf:/etc/nginx/conf.d/default.conf:ro
      - ./certs:/etc/nginx/certs:ro
```

### Manual launch commands

```bash
cd ~/trrapp
chmod +x run-on-server.sh
./run-on-server.sh
```

Check status:

```bash
sudo docker compose ps
curl -kI https://127.0.0.1:8444/healthz
```

Open in browser:

- `https://10.0.4.180:8444/`
- Browser warning is expected for self-signed cert.

### Update after new build

```bash
# on build machine
./gradlew :composeApp:wasmJsBrowserDistribution

# copy new build to server ~/trrapp/web-dist/

# on server
cd ~/trrapp
bash run-on-server.sh
```

## Troubleshooting

### `403 Forbidden`

Usually means `web-dist` is empty/missing files or unreadable.

Check:

```bash
ls -la ~/trrapp/web-dist
sudo docker exec -it mobileagregatcrm-web sh -c "ls -la /usr/share/nginx/html"
```

### `curl -k https://127.0.0.1:8444/healthz` works, but server IP is unreachable

- Container is healthy; problem is outside app container:
  - cloud security group/firewall
  - host firewall / provider ACL
  - blocked port `8444`
  - service bound on another host IP (wrong VM/node)

### Container fails to start with mount error

File/folder mismatch in volume mappings.  
For one-folder setup, compose paths must be exactly:

- `./web-dist` (directory)
- `./mobileagregatcrm-web-docker.conf` (file)
- `./certs` (directory)

### Login/API request fails on `https://10.0.4.180:8444`

Check API from server first:

```bash
curl -k -i "https://127.0.0.1:8444/api/?task=gettoken&user=test&pass=test"
```

If this fails, collect for sysadmin:

1. `sudo docker compose logs --tail=200`
2. `sudo docker compose ps`
3. `sudo ss -ltnp | grep 8444`
4. Output of the curl command above
5. Confirmation that outbound HTTPS from server to `agrapp.agregatka.ru:443` is allowed

## Security notes

- API URLs are always discoverable in browser apps (DevTools/network/bundle analysis).
- Do not rely on hidden URLs for security.
- Use server-side auth/authorization, HTTPS, and rate limiting.
- Current backend login in query params is risky (can leak via logs/history); prefer POST body for credentials.
