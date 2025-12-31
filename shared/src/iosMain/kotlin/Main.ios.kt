import androidx.compose.ui.window.ComposeUIViewController
import com.sirelon.marsroverphotos.presentation.App
import platform.UIKit.UIViewController

/**
 * Main entry point for iOS app.
 * Creates a UIViewController hosting the Compose UI.
 */
fun MainViewController(): UIViewController {
    return ComposeUIViewController {
        App()
    }
}
