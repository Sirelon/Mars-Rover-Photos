import SwiftUI
import shared

@main
struct MarsRoverApp: App {
    init() {
        // Initialize Koin dependency injection from shared module
        KoinInitKt.initKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
