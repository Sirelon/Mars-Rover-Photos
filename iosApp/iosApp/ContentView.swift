import SwiftUI
import shared

struct ContentView: View {
    var body: some View {
        ComposeView()
            // Extend behind ALL safe areas (status bar, home indicator, keyboard).
            // Compose Multiplatform receives the system WindowInsets and applies its own
            // padding via windowInsetsPadding(WindowInsets.statusBars/navigationBars/ime),
            // so SwiftUI must not add its own safe-area inset on top.
            .ignoresSafeArea(.all)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // Create the main view controller from the shared module
        // The shared module should export a function that creates the main Compose UI
        return Main_iosKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // No updates needed for static content
    }
}

#Preview {
    ContentView()
}
