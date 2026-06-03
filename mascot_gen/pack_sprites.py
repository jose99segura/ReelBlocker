"""Empaqueta los 20 sprites de evolucion a webp lossless cuadrados (512px max)
en res/drawable-nodpi/ con nombres mascot_<especie>_<fase>.webp."""
import os
from PIL import Image

SRC = r"C:\Users\jose9\OneDrive\Escritorio\ReelBlocker\mascot_gen"
DST = r"C:\Users\jose9\OneDrive\Escritorio\ReelBlocker\app\src\main\res\drawable-nodpi"
MAX = 512
phase = {"1_egg": "egg", "2_cracking": "cracking", "3_hatchling": "hatchling", "4_adult": "adult"}
species = ["clasica", "tortuga", "dragon", "lobo", "buho"]
# Escala del contenido dentro del cuadro: huevo y grieta un poco mas pequenos
# (margen transparente) para que no llenen tanto el marco.
content_scale = {"egg": 0.88, "cracking": 0.88, "hatchling": 1.0, "adult": 1.0}

count = 0
for sp in species:
    for fn, ph in phase.items():
        src = os.path.join(SRC, f"{sp}_evolution", f"{fn}.png")
        im = Image.open(src).convert("RGBA")
        side = max(im.size)
        sq = Image.new("RGBA", (side, side), (0, 0, 0, 0))
        sq.paste(im, ((side - im.width) // 2, (side - im.height) // 2))
        sq.thumbnail((MAX, MAX), Image.LANCZOS)
        scale = content_scale[ph]
        if scale < 1.0:
            cw, ch = sq.size
            inner = sq.resize((int(cw * scale), int(ch * scale)), Image.LANCZOS)
            canvas = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
            canvas.paste(inner, ((cw - inner.width) // 2, (ch - inner.height) // 2))
            sq = canvas
        out = os.path.join(DST, f"mascot_{sp}_{ph}.webp")
        sq.save(out, format="WEBP", lossless=True, method=6)
        count += 1
        print(f"{os.path.basename(out)}  {sq.size}  x{scale}")
print(f"TOTAL {count} sprites")
