//
//  NotificationManager.swift

import SwiftUI
import UIKit
import FirebaseMessaging
import UserNotifications
import ComposeApp

class NotificationManager: NSObject, ObservableObject {

    static let shared = NotificationManager()
    private var isConfigured = false
    private var didRegisterObserver = false
    private var hasShownSettingsPromptThisLaunch = false
    private var hasApnsToken = false
    private var latestFcmToken: String?
    private var lastForwardedFcmToken: String?

    func configure() {
        guard !isConfigured else { return }
        isConfigured = true

        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self
        registerPostLoginPermissionObserverIfNeeded()
        print("PUSH_SERVICE: Push(iOS) configured (startup permission prompt disabled)")
        // APNs registration does not require alert permission; request it early.
        registerForRemoteNotificationsOnMain()
    }

    private func registerPostLoginPermissionObserverIfNeeded() {
        guard !didRegisterObserver else { return }
        didRegisterObserver = true
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handlePostLoginPermissionCheck(_:)),
            name: .trrcrmPostLoginPushPermissionCheck,
            object: nil
        )
    }

    @objc private func handlePostLoginPermissionCheck(_ notification: Notification) {
        print("PUSH_SERVICE: Push(iOS) post-login permission check received")
        checkNotificationPermissionAfterLogin()
    }

    private func checkNotificationPermissionAfterLogin() {
        // Keep APNs registration hot even before/without alert permission.
        registerForRemoteNotificationsOnMain()
        UNUserNotificationCenter.current().getNotificationSettings { [weak self] settings in
            guard let self else { return }
            switch settings.authorizationStatus {
            case .authorized, .provisional, .ephemeral:
                print("PUSH_SERVICE: Push(iOS) permission status=\(settings.authorizationStatus.rawValue), requesting APNs register")
                self.registerForRemoteNotificationsOnMain()
                self.requestFcmTokenProactively(reason: "post_login_authorized")
            case .notDetermined:
                print("PUSH_SERVICE: Push(iOS) permission status=not_determined, requesting")
                self.requestPermissionAndRegister()
            case .denied:
                print("PUSH_SERVICE: Push(iOS) permission status=denied")
                self.showNotificationSettingsPromptIfNeeded()
            @unknown default:
                print("PUSH_SERVICE: Push(iOS) permission status=unknown(\(settings.authorizationStatus.rawValue))")
            }
        }
    }

    private func requestPermissionAndRegister() {
        let options: UNAuthorizationOptions = [.alert, .badge, .sound]
        UNUserNotificationCenter.current().requestAuthorization(options: options) { [weak self] granted, error in
            if let error = error {
                print("PUSH_SERVICE: Push(iOS) permission request failed: \(error.localizedDescription)")
                return
            }
            print("PUSH_SERVICE: Push(iOS) permission granted=\(granted)")
            if granted {
                self?.registerForRemoteNotificationsOnMain()
            } else {
                self?.showNotificationSettingsPromptIfNeeded()
            }
        }
    }

    private func registerForRemoteNotificationsOnMain() {
        DispatchQueue.main.async {
            UIApplication.shared.registerForRemoteNotifications()
            print("PUSH_SERVICE: Push(iOS) registerForRemoteNotifications requested")
        }
    }

    private func showNotificationSettingsPromptIfNeeded() {
        DispatchQueue.main.async {
            guard !self.hasShownSettingsPromptThisLaunch else { return }
            guard let topController = Self.topViewController() else {
                print("PUSH_SERVICE: Push(iOS) settings prompt skipped(no_top_controller)")
                return
            }

            self.hasShownSettingsPromptThisLaunch = true

            let alert = UIAlertController(
                title: "Уведомления отключены",
                message: "Чтобы получать уведомления, разрешите их в настройках iOS.",
                preferredStyle: .alert
            )
            alert.addAction(UIAlertAction(title: "Не сейчас", style: .cancel, handler: nil))
            alert.addAction(UIAlertAction(title: "Открыть настройки", style: .default) { _ in
                guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
                UIApplication.shared.open(url, options: [:], completionHandler: nil)
            })
            topController.present(alert, animated: true)
        }
    }

    private static func topViewController(
        from root: UIViewController? = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?
            .rootViewController
    ) -> UIViewController? {
        if let nav = root as? UINavigationController {
            return topViewController(from: nav.visibleViewController)
        }
        if let tab = root as? UITabBarController, let selected = tab.selectedViewController {
            return topViewController(from: selected)
        }
        if let presented = root?.presentedViewController {
            return topViewController(from: presented)
        }
        return root
    }

    func didRegisterForRemoteNotifications(deviceToken: Data) {
        hasApnsToken = true
        let apnsTokenHex = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        print("PUSH_SERVICE: Push(iOS) APNs token=\(apnsTokenHex)")

        Messaging.messaging().setAPNSToken(deviceToken, type: .unknown)
        print("PUSH_SERVICE: Push(iOS) APNs token received and mapped to Firebase via setAPNSToken (\(deviceToken.count) bytes)")
        PushBridgeKt.setIosApnsReady(ready: true)

        if let cachedToken = latestFcmToken, !cachedToken.isEmpty {
            print("PUSH_SERVICE: Push(iOS) using cached FCM token after APNs")
            forwardFcmTokenToShared(token: cachedToken, source: "cached_pre_apns")
        }

        fetchFreshFcmTokenAfterApns()
        requestFcmTokenProactively(reason: "apns_ready")
    }

    func didFailToRegisterForRemoteNotifications(error: Error) {
        hasApnsToken = false
        PushBridgeKt.setIosApnsReady(ready: false)
        print("PUSH_SERVICE: Push(iOS) APNs registration failed: \(error.localizedDescription)")
    }

    private func fetchFreshFcmTokenAfterApns() {
        Messaging.messaging().token { [weak self] token, error in
            if let error = error {
                print("PUSH_SERVICE: Push(iOS) FCM token after APNs error=\(error.localizedDescription)")
                return
            }
            guard let token = token, !token.isEmpty else {
                print("PUSH_SERVICE: Push(iOS) FCM token after APNs is nil/empty")
                return
            }
            print("PUSH_SERVICE: Push(iOS) FCM token after APNs=\(token)")
            self?.latestFcmToken = token
            self?.forwardFcmTokenToShared(token: token, source: "post_apns_refresh")
        }
    }

    func requestFcmTokenProactively(reason: String) {
        guard hasApnsToken else {
            print("PUSH_SERVICE: Push(iOS) proactive token fetch skipped reason=\(reason) apns_missing")
            return
        }
        Messaging.messaging().token { [weak self] token, error in
            if let error = error {
                print("PUSH_SERVICE: Push(iOS) proactive token fetch failed reason=\(reason) error=\(error.localizedDescription)")
                return
            }
            guard let token = token, !token.isEmpty else {
                print("PUSH_SERVICE: Push(iOS) proactive token fetch empty reason=\(reason)")
                return
            }
            print("PUSH_SERVICE: Push(iOS) proactive token fetch success reason=\(reason)")
            self?.latestFcmToken = token
            self?.forwardFcmTokenToShared(token: token, source: "proactive_\(reason)")
        }
    }

    private func forwardFcmTokenToShared(token: String, source: String) {
        let safeToken = token.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !safeToken.isEmpty else {
            print("PUSH_SERVICE: Push(iOS) token from \(source) is blank, skipping")
            return
        }

        guard lastForwardedFcmToken != safeToken else {
            print("PUSH_SERVICE: Push(iOS) FCM token already forwarded, skipping source=\(source)")
            return
        }

        lastForwardedFcmToken = safeToken
        print("PUSH_SERVICE: Push(iOS) forwarding FCM token from \(source), length=\(safeToken.count)")
        PushBridgeKt.onIosFcmTokenReceived(token: safeToken)
    }
}

extension NotificationManager: UNUserNotificationCenterDelegate {

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .badge])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let content = response.notification.request.content
        let userInfo = content.userInfo
        let screen = firstNonBlank(userInfo, keys: ["screen", "Screen", "target", "docType", "doc_type"])
        let docId = firstNonBlank(userInfo, keys: ["docId", "doc_id", "docID", "docGuid", "doc_guid", "guid"])
        let canonicalTitle = firstNonBlank(userInfo, keys: ["docTitle", "title", "notification_title"])
        let hasCanonicalPayload = (screen != nil) && (docId != nil || canonicalTitle != nil)
        let messageText = firstNonBlank(userInfo, keys: ["message_text", "messageText", "body", "text", "comment"]) ?? content.body
        let keys = userInfo.keys.map { String(describing: $0) }.sorted()
        print("PUSH_SERVICE DEEPLINK: Push(iOS) tap payload keys=\(keys), screen='\(screen ?? "")', docId='\(docId ?? "")', title='\(content.title)', bodyLen=\(content.body.count), hasCanonicalPayload=\(hasCanonicalPayload)")
        if !hasCanonicalPayload {
            print("PUSH_SERVICE DEEPLINK: Push(iOS) missing_canonical_push_payload keys=\(keys)")
        }
        PushBridgeKt.onIosNotificationTap(
            title: content.title,
            screen: screen,
            docId: docId,
            messageText: messageText
        )

        completionHandler()
    }

    private func firstNonBlank(_ userInfo: [AnyHashable: Any], keys: [String]) -> String? {
        for key in keys {
            if let value = userInfo[key] as? String {
                let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
                if !trimmed.isEmpty { return trimmed }
            }
        }
        return nil
    }
}

extension NotificationManager: MessagingDelegate {

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken, !token.isEmpty else {
            print("PUSH_SERVICE: Push(iOS) messaging delegate token missing")
            return
        }

        latestFcmToken = token
        if !hasApnsToken {
            print("PUSH_SERVICE: Push(iOS) FCM token received before APNs, saving and forwarding to shared")
            forwardFcmTokenToShared(token: token, source: "messaging_delegate_pre_apns")
        } else {
            forwardFcmTokenToShared(token: token, source: "messaging_delegate")
        }
    }
}

private extension Notification.Name {
    static let trrcrmPostLoginPushPermissionCheck =
        Notification.Name("TRRCRM_POST_LOGIN_PUSH_PERMISSION_CHECK")
}
