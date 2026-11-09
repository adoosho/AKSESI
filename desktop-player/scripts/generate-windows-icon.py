from PIL import Image
from pathlib import Path
root = Path(__file__).resolve().parents[1]
src = Image.open(root / "assets" / "aksesi-logo.png").convert("RGBA")
canvas = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
src.thumbnail((232, 232), Image.Resampling.LANCZOS)
canvas.alpha_composite(src, ((256-src.width)//2, (256-src.height)//2))
canvas.save(root / "assets" / "aksesi.ico", format="ICO", sizes=[(16,16),(24,24),(32,32),(48,48),(64,64),(128,128),(256,256)])
