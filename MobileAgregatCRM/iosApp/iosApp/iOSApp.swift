import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    init() {
        // Start shared DI before any push token forwarding to avoid Koin/token race.
        PushBridgeKt.ensureIosDependenciesReady()
        // Reset APNs readiness at launch; it becomes true only after current APNs callback.
        PushBridgeKt.setIosApnsReady(ready: false)
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
            // ComposeView { // your compose hosting
            //     let root = OrgAgregatcrmNavigationDefaultRootComponent(
            //         componentContext: OrgArkivanovDecomposeDefaultComponentContext(lifecycle: OrgArkivanovEssentyLifecycleLifecycleRegistry())
            //     )
            //     OrgAgregatcrmUiAppRoot(root: root)
            // }
        }
    }
}
