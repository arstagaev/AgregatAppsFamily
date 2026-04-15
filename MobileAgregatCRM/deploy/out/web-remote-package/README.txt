Upload this whole folder to your remote server (for example: ~/trrapp).

Required files here:
- docker-compose.yml
- mobileagregatcrm-web-docker.conf
- web-dist/
- certs/ (can be empty; cert is auto-generated on first launch)

Launch web container:
  bash run-on-server.sh

Open in browser:
  https://10.0.4.180:8444/

Notes:
- This bundle is standalone (no host nginx config required).
- First run generates a self-signed TLS cert in ./certs.
- Browser will show certificate warning until you trust this cert.

Health check:
  curl -kI https://127.0.0.1:8444/healthz
