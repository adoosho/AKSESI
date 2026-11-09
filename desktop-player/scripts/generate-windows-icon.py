from PIL import Image
from pathlib import Path
root = Path(__file__).resolve().parents[1]
source = Image.open(root / "assets" / "aksesi-logo.png").convert("RGBA")
source.save(root / "assets" / "aksesi.ico", format="ICO", sizes=[(16,16),(24,24),(32,32),(48,48),(64,64),(128,128),(256,256)])
