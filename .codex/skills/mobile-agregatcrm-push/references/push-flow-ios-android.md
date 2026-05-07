# MobileAgregatCRM Push Flow (Android + iOS)

Last updated: 2026-05-07 (post-fix baseline)

## 1) Shared Registration Contract (KMP)

Primary shared coordinator:
- `composeApp/src/commonMain/kotlin/com/tagaev/trrcrm/push/PushRegistration.kt`

Registration gate (`PushRegistrationCoordinator.registerIfReady`):
- Requires both values in settings:
  - `AppSettingsKeys.FCM_TOKEN`
  - `AppSettingsKeys.PERSONAL_DATA`
- For iOS also requires:
  - `AppSettingsKeys.IOS_APNS_READY == true`
- If one is missing, registration is skipped with reason:
  - `missing_token`
  - `missing_user`
  - `missing_token_and_user`
- iOS strict gate skip reason:
  - `missing_apns_ready`

Register endpoints (same contract, no schema changes expected):
- Primary: `POST {GLOBAL_CORE_URL}/devices/register`
  - payload: `full_name`, `platform`, `device_id`, `fcm_token`, `device_name`
- Fallback: `POST {GLOBAL_CORE_URL}/users/register-device`
  - payload: `full_name`, `platform`, `fcm_token`

## 2) Android Flow

Entry points:
- `composeApp/src/androidMain/kotlin/com/tagaev/trrcrm/App.kt`
- `composeApp/src/androidMain/kotlin/com/tagaev/trrcrm/push/AppFirebaseMessagingService.kt`

Flow:
1. `FirebaseApp.initializeApp(...)` in `Application.onCreate`.
2. Koin starts before token forwarding.
3. Proactive token fetch from `FirebaseMessaging.getInstance().token`.
4. Token also refreshed in `onNewToken`.
5. Both paths call `PushRegistrationCoordinator.onTokenReceived(..., preferredPlatform="android")`.
6. Shared coordinator stores token and attempts backend register if user info exists.

## 3) iOS Flow (strict APNs-first pattern)

Entry points:
- `iosApp/iosApp/iOSApp.swift`
- `iosApp/iosApp/AppDelegate.swift`
- `iosApp/iosApp/NotificationManager.swift`
- `composeApp/src/iosMain/kotlin/com/tagaev/trrcrm/push/PushBridge.kt`

Flow:
1. `iOSApp.init` calls `PushBridgeKt.ensureIosDependenciesReady()` (starts Koin early).
2. `iOSApp.init` resets shared APNs readiness flag: `PushBridgeKt.setIosApnsReady(false)`.
3. `AppDelegate` configures Firebase once and starts notification setup.
4. Permission request executes, then app calls `registerForRemoteNotifications()`.
5. APNs callback arrives in `didRegisterForRemoteNotificationsWithDeviceToken`.
6. APNs token is mapped to Firebase: `Messaging.messaging().apnsToken = deviceToken`.
7. Native marks APNs ready in shared: `PushBridgeKt.setIosApnsReady(true)`.
8. Native fetches a fresh FCM token **after APNs binding**.
9. iOS token is forwarded to KMP bridge:
  - `PushBridgeKt.onIosFcmTokenReceived(...)`
10. Bridge ensures dependencies, retries forwarding up to 3 attempts.
11. Shared coordinator registers device with platform `ios` only when APNs-ready gate is open.

If `didReceiveRegistrationToken` arrives before APNs:
- token is cached on iOS side,
- registration is not triggered yet,
- token is forwarded only after APNs-ready.

## 4) Expected Runtime Logs (Healthy Path)

iOS expected chain:
1. `Push(iOS): Firebase configured` (or already configured)
2. `Push(iOS): permission granted=...`
3. `Push(iOS): registerForRemoteNotifications requested`
4. `Push(iOS): APNs token=...`
5. `Push(iOS): APNs token received and mapped to Firebase (...)`
6. `Push(iOS): FCM token after APNs=...`
7. `Push(iOS): forwarding FCM token from ...`
8. `PushRegistrationCoordinator: token_received(platform=ios, ...)`
9. `PushRegistrationCoordinator: register_attempt(platform=ios, ...)`
10. `PushRegistration: register_core_status(code=...)` or fallback status

Android expected chain:
1. `FCM(Android): proactive token fetch success`
2. `PushRegistrationCoordinator: token_received(platform=android, ...)`
3. `PushRegistrationCoordinator: register_attempt(platform=android, ...)`
4. `PushRegistration: register_core_status(code=...)` or fallback status

## 5) Known Caveats To Remember

- iOS push delivery can still fail even when app-side registration is correct if APNs/Firebase backend configuration is mismatched.
- `aps-environment` in `iosApp/iosApp/iosApp.entitlements` is `development` (debug context).
- Seeing `register_skipped(missing_apns_ready)` before APNs callback is expected and healthy for iOS.
- Logout path currently uses `platform = getPlatform().deviceSpecificInfo` in settings component; this is not registration path, but note it for backend/device lifecycle discussions.
