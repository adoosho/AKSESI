# AKSESI Desktop Player

Packaged desktop application for Windows and macOS (Electron shell with isolated renderer and no Node integration in the player page).

Local build: `npm install`, then `npm run dist`. Windows builds produce an NSIS installer and portable executable. macOS builds produce DMG and ZIP. Unsigned macOS builds may show Gatekeeper warnings; signing/notarization requires Apple Developer credentials. Windows SmartScreen reputation also depends on code signing.