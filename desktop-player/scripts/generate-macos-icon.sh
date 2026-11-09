#!/bin/bash
set -euo pipefail
cd desktop-player
mkdir -p build/aksesi.iconset
for size in 16 32 128 256 512; do
  sips -s format png -z "$size" "$size" assets/aksesi-logo.png --out "build/aksesi.iconset/icon_${size}x${size}.png" >/dev/null
  double=$((size*2))
  if [ "$double" -le 1024 ]; then
    sips -s format png -z "$double" "$double" assets/aksesi-logo.png --out "build/aksesi.iconset/icon_${size}x${size}@2x.png" >/dev/null
  fi
done
iconutil -c icns build/aksesi.iconset -o assets/aksesi.icns
