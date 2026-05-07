//
//  NotificationManager.swift

import SwiftUI
import FirebaseMessaging
import UserNotifications
import ComposeApp

class NotificationManager: NSObject, ObservableObject {

    static let shared = NotificationManager()
    private var isConfigured = false
    private var hasApnsToken = false
    private var latestFcmToken: String?
    private var lastForwardedFcmToken: String?

    func configure() {
        guard !isConfigured else { return }
        isConfigured = true

        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self

        let options: UNAuthorizationOptions = [.alert, .badge, .sound]
        UNUserNotificationCenter.current().requestAuthorization(options: options) { granted, error in
            if let error = error {
                print("PUSH_SERVICE: Push(iOS) permission request failed: \(error.localizedDescription)")
            } else {
                print("PUSH_SERVICE: Push(iOS) permission granted=\(granted)")
            }
            DispatchQueue.main.async {
                UIApplication.shared.registerForRemoteNotifications()
                print("PUSH_SERVICE: Push(iOS) registerForRemoteNotifications requested")
            }
        }
    }

    func didRegisterForRemoteNotifications(deviceToken: Data) {
        hasApnsToken = true
        let apnsTokenHex = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        print("PUSH_SERVICE: Push(iOS) APNs token=\(apnsTokenHex)")

        Messaging.messaging().apnsToken = deviceToken
        print("PUSH_SERVICE: Push(iOS) APNs token received and mapped to Firebase (\(deviceToken.count) bytes)")
        PushBridgeKt.setIosApnsReady(ready: true)

        if let cachedToken = latestFcmToken, !cachedToken.isEmpty {
            print("PUSH_SERVICE: Push(iOS) using cached FCM token after APNs")
            forwardFcmTokenToShared(token: cachedToken, source: "cached_pre_apns")
        }

        fetchFreshFcmTokenAfterApns()
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
            print("PUSH_SERVICE: Push(iOS) FCM token received before APNs, saving temporarily")
            return
        }

        forwardFcmTokenToShared(token: token, source: "messaging_delegate")
    }
}
