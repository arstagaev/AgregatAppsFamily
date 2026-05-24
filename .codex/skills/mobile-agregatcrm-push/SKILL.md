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

## CoreService Contract (Must Keep in Mobile Integration)

- `mute` is **push-delivery only**:
  - muted device/document type can suppress push send
  - but notification is still persisted to inbox and must be visible in `/notifications/feed`
- Do not treat missing device push as missing notification history.
- For UX:
  - unread badge and in-app inbox must rely on inbox endpoints, not on push-received callbacks.

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
- When debugging mute scenarios, always split checks:
  1. push send eligibility (token/device mute/session),
  2. inbox persistence (`/notifications/feed`, unread count),
  3. client rendering/read-state transitions.
