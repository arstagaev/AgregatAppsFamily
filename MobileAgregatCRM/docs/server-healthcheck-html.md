# Server Healthcheck HTML Tool

Standalone browser tool for checking API endpoints.
Use this only for quick visual debug.
For reliable PASS/FAIL health verdicts use CLI tool: `deploy/tools/server-healthcheck.sh`.

## File

- `deploy/tools/server-healthcheck.html`

## Local launch

Option 1 (open directly):

```bash
open deploy/tools/server-healthcheck.html
```

Option 2 (simple local static server):

```bash
python3 -m http.server 8099 --directory deploy/tools
```

Then open:

- `http://localhost:8099/server-healthcheck.html`

## Usage

1. Set host address (default: `https://agrapp.agregatka.ru/`).
2. Fill either:
   - `token`, or
   - `user` + `pass` (tool will call `gettoken` first and auto-fill token).
3. Click `Check All Endpoints`.
4. Use `View JSON` on any row for full response body.
5. URL column is shown in readable form (Russian letters, no `%D0...` encoding).

## Notes

- If browser blocks cross-origin requests, row will show `FAIL` with fetch/CORS error.
- Because of CORS, HTML results can be false-negative even when API is reachable.
- Default document checks use:
  - `count=30`
  - `ncount=0`
  - `orderby=ДатаМод`
  - `orderdir=desc`
  - `viewtype=onlymy1`
