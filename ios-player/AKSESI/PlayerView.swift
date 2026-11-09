import SwiftUI
import WebKit
import UIKit

struct PlayerView: View {
    @State private var failed = false
    @State private var reloadID = UUID()

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            PlayerWebView(reloadID: reloadID, onFailure: { failed = true })
                .ignoresSafeArea()
            if failed {
                VStack(spacing: 16) {
                    Image(systemName: "wifi.slash")
                        .font(.system(size: 42))
                    Text("Nuk ka lidhje me internetin")
                        .font(.title3.bold())
                    Text("Kontrollo rrjetin dhe provo përsëri.")
                        .foregroundStyle(.white.opacity(0.75))
                    Button("Provo përsëri") {
                        failed = false
                        reloadID = UUID()
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(Color(red: 0.85, green: 0.0, blue: 0.29))
                }
                .multilineTextAlignment(.center)
                .padding(28)
                .foregroundStyle(.white)
            }
        }
        .statusBarHidden(true)
        .persistentSystemOverlays(.hidden)
        .onAppear {
            UIApplication.shared.isIdleTimerDisabled = true
        }
        .onDisappear {
            UIApplication.shared.isIdleTimerDisabled = false
        }
    }
}

private struct PlayerWebView: UIViewRepresentable {
    let reloadID: UUID
    let onFailure: () -> Void

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = .default()
        configuration.allowsInlineMediaPlayback = true
        configuration.mediaTypesRequiringUserActionForPlayback = []
        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.backgroundColor = .black
        webView.isOpaque = false
        webView.scrollView.isScrollEnabled = false
        webView.scrollView.bounces = false
        webView.navigationDelegate = context.coordinator
        webView.load(URLRequest(url: URL(string: "https://aksesi.space/player.html")!,
                                cachePolicy: .reloadIgnoringLocalCacheData,
                                timeoutInterval: 30))
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        if context.coordinator.lastReloadID != reloadID {
            context.coordinator.lastReloadID = reloadID
            webView.load(URLRequest(url: URL(string: "https://aksesi.space/player.html")!,
                                    cachePolicy: .reloadIgnoringLocalCacheData,
                                    timeoutInterval: 30))
        }
    }

    func makeCoordinator() -> Coordinator { Coordinator(onFailure: onFailure) }

    final class Coordinator: NSObject, WKNavigationDelegate {
        let onFailure: () -> Void
        var lastReloadID = UUID()
        init(onFailure: @escaping () -> Void) { self.onFailure = onFailure }

        func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
            onFailure()
        }
        func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
            onFailure()
        }
        func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction,
                     decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
            guard let url = navigationAction.request.url else {
                decisionHandler(.cancel); return
            }
            if url.host == "aksesi.space" || url.host?.hasSuffix(".aksesi.space") == true {
                decisionHandler(.allow)
            } else if url.scheme == "https" {
                UIApplication.shared.open(url)
                decisionHandler(.cancel)
            } else {
                decisionHandler(.cancel)
            }
        }
    }
}
