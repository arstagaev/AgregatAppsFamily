//
//  NotificationManager.swift

import SwiftUI
import FirebaseCore
import FirebaseMessaging
import UserNotifications
import ComposeApp

class NotificationManager: NSObject, ObservableObject {

    static let shared = NotificationManager()
    private var isConfigured = false

    func configure() {
        guard !isConfigured else { return }
        isConfigured = true
        FirebaseApp.configure()

        UNUserNotificationCenter.current().delegate = self

        let options: UNAuthorizationOptions = [.alert, .badge, .sound]
        UNUserNotificationCenter.current().requestAuthorization(options: options) { granted, error in
            DispatchQueue.main.async {
                UIApplication.shared.registerForRemoteNotifications()
            }
        }

        Messaging.messaging().delegate = self
        Messaging.messaging().token { token, error in
            if let error = error {
                print("FCM(iOS): proactive token fetch failed: \(error.localizedDescription)")
                return
            }
            guard let token = token, !token.isEmpty else {
                print("FCM(iOS): proactive token fetch returned empty token")
                return
            }
            print("FCM(iOS): proactive token fetch success")
            PushBridgeKt.onIosFcmTokenReceived(token: token)
        }
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
            print("FCM(iOS): token missing")
            return
        }
        print("FCM token (iOS): \(token)")
        PushBridgeKt.onIosFcmTokenReceived(token: token)
    }
}

