//
//  NotificationManager.swift

import SwiftUI
import FirebaseCore
import FirebaseMessaging
import UserNotifications
import ComposeApp

class NotificationManager: NSObject, ObservableObject {

    static let shared = NotificationManager()

    func configure() {
        FirebaseApp.configure()

        UNUserNotificationCenter.current().delegate = self

        let options: UNAuthorizationOptions = [.alert, .badge, .sound]
        UNUserNotificationCenter.current().requestAuthorization(options: options) { granted, error in
            DispatchQueue.main.async {
                UIApplication.shared.registerForRemoteNotifications()
            }
        }

        Messaging.messaging().delegate = self
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
//        guard
//            let token = fcmToken,
//            let fullName = CurrentUser.shared.fullName, // adapt this to your shared user store
//            !fullName.isEmpty
//        else {
//            print("FCM(iOS): missing fullName or token")
//            return
//        }
//
//        print("FCM token (iOS): \(token)")
//        
//        PushRegistration.shared.registerCurrentUserToken(
//            fullName: fullName,
//            platform: "ios",
//            token: token
//        )
        
    }
}

