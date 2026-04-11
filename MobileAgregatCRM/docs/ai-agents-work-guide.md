# MobileAgregatCRM: AI Agent Working Guide

This document is a practical guide for AI agents working in this repository.

## 1) Primary Rule: Reliability First

- Prioritize stable, low-risk changes over fast but brittle edits.
- If reliability cannot be guaranteed, explicitly say so and propose a safer alternative.
- Do not silently change API contracts, routing assumptions, or auth flow behavior.

## 2) Project Snapshot

- Kotlin Multiplatform (Compose Multiplatform) CRM app.
- Targets:
  - Android
  - iOS
  - Web (`wasmJs`)
- Main shared code:
  - `composeApp/src/commonMain/kotlin/com/tagaev/trrcrm`

## 3) Main Product Functionality

- Authentication:
  - Login via `gettoken` and token persistence in settings.
  - Role fetch (`getroles`) after successful login.
- Cold-start startup gate:
  - App probes server reachability before normal login flow.
  - If blocked (no internet or 3xx..5xx), shows blocked screen with `Refresh` and `Re-login`.
- Session splash image behavior:
  - One random TRR image per app session, reused during session.
  - Source: `SessionTrrImage`.
- Document sections (master/detail UX with messaging):
  - `События` (Events)
  - `Заказ-Наряды` (Work Orders)
  - `Комплектация` (Complectation)
  - `Рекламации` (Complaints)
  - `Внутр. заказы` (Inner Orders)
  - `Груз` (Cargo / deliveries)
- Document details:
  - Shared message UI and send-message flow (`setmessage`).
  - Local optimistic append for just-sent comments.

## 4) Key Architecture Map

### Navigation and root flow

- Root component and deep-link routing:
  - `composeApp/src/commonMain/kotlin/com/tagaev/trrcrm/ui/root/RootComponent.kt`
- Root UI + bottom navigation:
  - `composeApp/src/commonMain/kotlin/com/tagaev/trrcrm/ui/root/AppRoot.kt`

### Feature pattern (for document sections)

Each section follows this shape:

1. `*Component.kt`:
   - list loading
   - paging
   - refine/filter state
   - send message
2. `*Screen.kt`:
   - list + details panel behavior
3. `*DetailsSheet.kt`:
   - fields rendering and message UI hookup

Examples:

- Work orders:
  - `ui/work_order/WorkOrderComponent.kt`
  - `ui/work_order/WorkOrdersScreen.kt`
  - `ui/work_order/WorkOrderDetailsSheet.kt`
- Complectation:
  - `ui/complectation/ComplectationComponent.kt`
  - `ui/complectation/ComplectationsScreen.kt`
  - `ui/complectation/ComplectationDetailsSheet.kt`

### Shared master/detail wrappers

- `ui/master_screen/MasterScreen.kt`
- `ui/master_screen/LiveListWrapper.kt`
- `ui/master_screen/DetailsSheetWrapper.kt`

## 5) Data/API Layer

- Repository:
  - `composeApp/src/commonMain/kotlin/com/tagaev/trrcrm/data/MainRepository.kt`
- Network API client:
  - `composeApp/src/commonMain/kotlin/com/tagaev/trrcrm/data/remote/CommonApi.kt`
- Document type mapping for `setmessage`:
  - `composeApp/src/commonMain/kotlin/com/tagaev/trrcrm/domain/Orders.kt`

### Important API notes

- `task=getitemslist` is used for list screens.
- `task=setmessage` is used for comments.
- Document names are Russian strings and must match backend exactly (example: `Комплектация`).

## 6) Startup/Login Behavior

- Login logic:
  - `composeApp/src/commonMain/kotlin/com/tagaev/trrcrm/ui/login/LoginComponent.kt`
  - `composeApp/src/commonMain/kotlin/com/tagaev/trrcrm/ui/login/LoginScreen.kt`
- Startup probe result can block login form and show connection card.
- Session image pool:
  - `composeApp/src/commonMain/kotlin/com/tagaev/trrcrm/ui/custom/SessionTrrImage.kt`

## 7) Web Target Rules (Critical)

- Web Koin module uses relative API base URL:
  - `composeApp/src/wasmJsMain/kotlin/com/tagaev/trrcrm/di/WasmModule.kt`
  - `ApiConfig(baseUrl = "/api", ...)`
- Keep web API as relative `/api` to avoid mixed-content/CORS problems behind HTTPS proxies.
- SQLDelight driver is intentionally unavailable on web simplified mode:
  - `composeApp/src/wasmJsMain/kotlin/com/tagaev/trrcrm/data/db/DriverFactory.wasm.kt`
- Web uses memory fallback for cache stores when DB driver is absent.

## 8) Deployment and Healthcheck Utilities

- Web deployment bundle preparation:
  - `deploy/scripts/prepare-web-remote-bundle.sh`
- Remote-ready package output:
  - `deploy/out/web-remote-package/`
- Docker compose files:
  - `deploy/docker/docker-compose.yml`
  - `deploy/docker/docker-compose.web.yml`
- Nginx examples:
  - `deploy/nginx/*`
- Healthcheck (authoritative):
  - `deploy/tools/server-healthcheck.sh`
  - doc: `docs/server-healthcheck-cli.md`
- HTML checker is debug-only due browser CORS policy:
  - `deploy/tools/server-healthcheck.html`
  - doc: `docs/server-healthcheck-html.md`

## 9) Safe Change Playbook for AI Agents

When adding a new document flow:

1. Add document enum mapping in `Orders.kt`.
2. Add list loader in `CommonApi.kt`.
3. Add repository methods in `MainRepository.kt`.
4. Reuse compatible DTO or add minimal fields in model.
5. Add feature package (`component/screen/details`) using existing pattern.
6. Add dedicated refine-state key in `AppSettingsKeys`.
7. Wire new config/child/open method in `RootComponent`.
8. Add bottom nav chip in `AppRoot`.
9. Add deep-link aliases in `mapScreenToConfig`.
10. Compile at least wasm + android targets.

## 10) Build and Verification Commands

Use these before finalizing changes:

```bash
./gradlew :composeApp:compileKotlinWasmJs
./gradlew :composeApp:compileDebugKotlinAndroid
```

For web dev:

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

For web distribution:

```bash
./gradlew :composeApp:wasmJsBrowserDistribution
```

## 11) Known Failure Patterns

- Mixed content:
  - App is opened via `https://...` but API URL is `http://...` -> browser blocks requests.
- CORS false failures from browser-based tools:
  - Endpoint may be healthy but browser reports `Failed to fetch`.
  - Use CLI healthcheck script for trusted status.
- Front proxy misrouting:
  - `/api` accidentally routed to HTML/redirect endpoint instead of backend API.
- Certbot/nginx issues:
  - broken TLS config can block nginx reload and all proxy checks.

## 12) Definition of Done for AI Agent Tasks

- Feature works in intended target(s).
- Existing sections/flows remain functional (no regressions in navigation/auth/list load).
- Build checks pass for impacted targets.
- Deployment-sensitive changes are documented when needed.
- If uncertainty remains, explicitly list risk and a safer follow-up path.

