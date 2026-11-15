import SwiftUI
import UIKit
import ComposeApp

/// EdgeCOS iOS 界面由 Compose Multiplatform 共享 UI（`:composeApp`）渲染。
///
/// 与 Windows 本地 `:composeApp:run` 预览使用的是同一份 Kotlin 代码，
/// Swift 这一层只负责把 Compose 的 UIViewController 塞进 SwiftUI 窗口。
struct ContentView: View {
    var body: some View {
        ComposeHostView()
            .ignoresSafeArea(.keyboard)
    }
}

private struct ComposeHostView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
