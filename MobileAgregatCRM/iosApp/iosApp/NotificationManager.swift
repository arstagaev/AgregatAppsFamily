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
                print("Push(iOS): permission request failed: \(error.localizedDescription)")
            } else {
                print("Push(iOS): permission granted=\(granted)")
            }
            DispatchQueue.main.async {
                UIApplication.shared.registerForRemoteNotifications()
                print("Push(iOS): registerForRemoteNotifications requested")
            }
        }
    }

    func didRegisterForRemoteNotifications(deviceToken: Data) {
        hasApnsToken = true
        let apnsTokenHex = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        print("Push(iOS): APNs token=\(apnsTokenHex)")

        Messaging.messaging().apnsToken = deviceToken
        print("Push(iOS): APNs token received and mapped to Firebase (\(deviceToken.count) bytes)")
        PushBridgeKt.setIosApnsReady(ready: true)

        if let cachedToken = latestFcmToken, !cachedToken.isEmpty {
            print("Push(iOS): using cached FCM token after APNs")
            forwardFcmTokenToShared(token: cachedToken, source: "cached_pre_apns")
        }

        fetchFreshFcmTokenAfterApns()
    }

    func didFailToRegisterForRemoteNotifications(error: Error) {
        hasApnsToken = false
        PushBridgeKt.setIosApnsReady(ready: false)
        print("Push(iOS): APNs registration failed: \(error.localizedDescription)")
    }

    private func fetchFreshFcmTokenAfterApns() {
        Messaging.messaging().token { [weak self] token, error in
            if let error = error {
                print("Push(iOS): FCM token after APNs error=\(error.localizedDescription)")
                return
            }
            guard let token = token, !token.isEmpty else {
                print("Push(iOS): FCM token after APNs is nil/empty")
                return
            }
            print("Push(iOS): FCM token after APNs=\(token)")
            self?.latestFcmToken = token
            self?.forwardFcmTokenToShared(token: token, source: "post_apns_refresh")
        }
    }

    private func forwardFcmTokenToShared(token: String, source: String) {
        let safeToken = token.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !safeToken.isEmpty else {
            print("Push(iOS): token from \(source) is blank, skipping")
            return
        }

        guard lastForwardedFcmToken != safeToken else {
            print("Push(iOS): FCM token already forwarded, skipping source=\(source)")
            return
        }

        lastForwardedFcmToken = safeToken
        print("Push(iOS): forwarding FCM token from \(source), length=\(safeToken.count)")
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
        let userInfo = response.notification.request.content.userInfo

        if let docId = userInfo["docId"] as? String {
            print("Tapped notification for docId: \(docId)")
            // TODO: pass into your KMP root to navigate to this thread
        }

        completionHandler()
    }
}

extension NotificationManager: MessagingDelegate {

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken, !token.isEmpty else {
            print("Push(iOS): messaging delegate token missing")
            return
        }

        latestFcmToken = token
        if !hasApnsToken {
            print("Push(iOS): FCM token received before APNs, saving temporarily")
            return
        }

        forwardFcmTokenToShared(token: token, source: "messaging_delegate")
    }
}
