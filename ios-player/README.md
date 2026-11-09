# AKSESI iOS Player

Native SwiftUI iPhone/iPad app wrapper for the AKSESI monitor player.

- Bundle ID: `space.aksesi.player`
- Player endpoint: `https://aksesi.space/player.html`
- Uses a persistent WKWebView website data store so the existing pairing code and web storage survive normal app relaunches.
- Keeps the display awake while the player is open and hides system overlays for kiosk-style playback.

## Build

Open `AKSESI.xcodeproj` in Xcode on macOS, select an iPhone or iPad destination, then build.

The GitHub Actions workflow builds an unsigned iOS Simulator app for CI verification. A real-device IPA, TestFlight build, or App Store release requires an Apple Developer signing identity and provisioning profile; these must be supplied by the app owner and are not included in this repository.
