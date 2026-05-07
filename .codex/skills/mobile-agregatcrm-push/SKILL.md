---
name: mobile-agregatcrm-push
description: Diagnose and coordinate Android/iOS push notification flows for MobileAgregatCRM. Use when iOS does not receive pushes, backend/device token registration is unclear, or when preparing a backend handoff with exact app-side context.
---

# MobileAgregatCRM Push Router

Use this skill when prompts mention `push`, `notifications`, `FCM`, `APNs`, `iOS not receiving`, or backend delivery debugging.

## Current Known-Good Pattern

- iOS uses **Strict APNs Gate**:
  - no push registration for `platform=ios` until APNs readiness is set from native callback
  - FCM token is fetched/refreshed only after APNs token is bound
  - duplicate token forwarding is skipped
- Keep existing shared flow:
  - `PushBridge -> PushRegistrationCoordinator -> PushRegistration`
- Keep Firebase swizzling enabled unless explicitly reworking full manual integration.

## Token-Efficient Loading Map

Load only what is needed:

- Push architecture and app-side signal flow:
  - `references/push-flow-ios-android.md`
- Backend handoff and incident checklist:
  - `references/backend-handoff-ios-failure.md`

Do not load both references unless the task needs both.

## Cross-Repo Safety

- If `MobileAgregatCRM/docs/ai-agents-work-guide.md` exists, treat it as primary reliability policy.
- If absent, use this skill as fallback and explicitly mark assumptions.

## Reliability Defaults

- Prefer observability + smallest safe fix over broad refactor.
- Keep backend API contracts unchanged unless explicitly requested.
- Separate failures by stage: permission -> APNs token -> FCM token -> backend register -> provider delivery.
