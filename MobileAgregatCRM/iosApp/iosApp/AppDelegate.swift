import UIKit
import FirebaseCore
import FirebaseMessaging
import ComposeApp

final class AppDelegate: NSObject, UIApplicationDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // Safety: ensure shared dependencies are available before push bridge callbacks.
        PushBridgeKt.ensureIosDependenciesReady()

        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
            print("PUSH_SERVICE: Push(iOS) Firebase configured")
        } else {
            print("PUSH_SERVICE: Push(iOS) Firebase already configured")
        }

        NotificationManager.shared.configure()
        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        print("PUSH_SERVICE: AppDelegate didRegisterForRemoteNotificationsWithDeviceToken")
        NotificationManager.shared.didRegisterForRemoteNotifications(deviceToken: deviceToken)
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("PUSH_SERVICE: AppDelegate didFailToRegisterForRemoteNotificationsWithError=\(error.localizedDescription)")
        NotificationManager.shared.didFailToRegisterForRemoteNotifications(error: error)
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        UnreadCountSyncBridgeKt.onAppForegroundForUnreadCount()
    }
}
