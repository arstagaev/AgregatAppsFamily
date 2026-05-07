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
            print("Push(iOS): Firebase configured")
        } else {
            print("Push(iOS): Firebase already configured")
        }

        NotificationManager.shared.configure()
        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        NotificationManager.shared.didRegisterForRemoteNotifications(deviceToken: deviceToken)
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        NotificationManager.shared.didFailToRegisterForRemoteNotifications(error: error)
    }
}
