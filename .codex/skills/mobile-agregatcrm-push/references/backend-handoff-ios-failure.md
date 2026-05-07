# Backend Handoff: iOS Still Not Receiving Push

Last updated: 2026-05-07
Incident status: Android receives pushes; iOS app currently does not receive pushes.

Use this checklist when coordinating with backend/infrastructure.

Note:
- Current app baseline uses strict APNs gate for iOS token registration.
- If backend sees no early iOS register call before APNs-ready, this is expected behavior.

## 1) App-Side Facts Already Confirmed

- iOS app requests notification permission and calls APNs registration.
- APNs token is mapped to Firebase Messaging (`apnsToken` set).
- FCM token is forwarded into shared registration coordinator.
- Registration is attempted with `platform=ios` once both token and user are available.
- Client uses core endpoint first (`/devices/register`) with fallback (`/users/register-device`).

If backend needs proof, request runtime logs from device using the expected chain in `push-flow-ios-android.md`.

## 2) Backend Checks (Most Important)

1. Device registration storage
- Verify iOS device records are persisted from `/devices/register`.
- Confirm fields are not overwritten by Android record for same user (multi-device case).
- Confirm `platform=ios` records are included in recipient selection.

2. Provider path and platform routing
- Verify delivery pipeline does not route iOS tokens through Android-specific sender logic.
- Confirm backend sends via Firebase/APNs path compatible with iOS FCM tokens.

3. APNs environment consistency
- App debug entitlement uses `aps-environment=development`.
- Ensure Firebase/APNs credentials and backend sending mode are valid for development tokens.
- Confirm no forced production-only path for debug iOS tokens.

4. Payload shape and APNs compatibility
- Confirm payload is valid for APNs delivery (notification/data usage, headers, collapse/priority rules).
- If only data payload is used, confirm iOS background delivery expectations are realistic.

5. Token freshness and invalidation
- Check backend handling for invalid/expired iOS tokens.
- Ensure token updates from repeated register calls replace old values correctly.

## 3) Minimal Evidence Backend Should Return

Ask backend for these artifacts per one failed iOS user attempt:
1. Raw register request body received from app (`/devices/register` or fallback).
2. Stored device row after register (masked token is fine).
3. Outbound push provider request metadata (timestamp, platform, token hash, message id).
4. Provider response/error for that exact send attempt.
5. Final delivery status classification (queued/sent/rejected/throttled/invalid token).

## 4) Quick Decision Matrix

- If register request never reaches backend:
  - app/network issue.
- If register reaches backend but iOS token not stored/selected:
  - backend recipient or persistence issue.
- If token stored and selected but provider rejects:
  - APNs/Firebase credential/env mismatch or token invalid.
- If provider accepts but no device alert:
  - payload/notification presentation mismatch or iOS device notification settings.

## 5) Suggested Message to Backend

"Android delivery works; iOS does not. Please validate iOS registration and send path end-to-end for platform=ios: ingestion (`/devices/register`), storage/selection, provider request, and provider response. We need one correlated trace for a single iOS test user (register request -> stored device -> outbound send -> provider result)."
