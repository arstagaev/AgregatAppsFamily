# Server Healthcheck CLI

Standalone endpoint checker for server API.  
No browser and no `wasmJsBrowserDevelopmentRun`.

This is the authoritative health checker for operations.

## Files

- `deploy/tools/server-healthcheck.sh`
- `deploy/tools/server-healthcheck.example.env`

## Quick Start

```bash
cd /Users/arsenx/Dev/AgregatAppsFamily/MobileAgregatCRM
bash deploy/tools/server-healthcheck.sh \
  --base-url https://agrapp.agregatka.ru/ \
  --user you@my.agregatka.ru \
  --pass <hash-or-pass>
```

Token-only mode:

```bash
bash deploy/tools/server-healthcheck.sh \
  --base-url https://agrapp.agregatka.ru/ \
  --token <TOKEN> \
  --show-json
```

Using env file:

```bash
set -a
source deploy/tools/server-healthcheck.example.env
set +a
bash deploy/tools/server-healthcheck.sh
```

## Behavior

- Checks:
  - `gettoken` (unless token-only mode)
  - `getroles`
  - `getitemslist` for `Событие`, `ЗаказНаряд`, `Рекламация`, `ЗаказВнутренний`, `Груз`
  - `getqrcomlectinfo`
- Verdict rules:
  - `PASS`: HTTP 200 and no `"error"` field
  - `WARN`: HTTP 200 with `"error"` field
  - `FAIL`: non-200 HTTP or curl/network error
- Exit code:
  - `0`: all checks `PASS`
  - `1`: any `WARN` or `FAIL`

## Reliability Notes

- Prefer running this checker from terminal on the same server/network where app/backend is hosted.
- Browser-based checker is debug-only because browser CORS policy can produce false negatives (`Failed to fetch`) even when API is healthy.
