import SwiftUI

@main
struct iOSApp: App {
    // init() {
    //     _ = IosModuleKt.initKoinIos()
    // }
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