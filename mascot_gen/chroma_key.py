"""Recorta un fondo de color solido (magenta #FF00FF o verde #00FF00) dejando
alfa limpio con anti-aliasing, despeckle y despill (quita el tinte del borde).

Uso: python chroma_key.py in.png out.png [key]
  key = "magenta" (def) | "green"  -> elige segun el color del SUJETO:
        sujeto calido/verde -> magenta ; sujeto morado/magenta -> green
"""
import sys
import numpy as np
from PIL import Image, ImageFilter

inp = sys.argv[1]
out = sys.argv[2]
key = sys.argv[3] if len(sys.argv) > 3 else "magenta"

im = Image.open(inp).convert("RGB")
arr = np.asarray(im).astype(np.float32)
R, G, B = arr[..., 0], arr[..., 1], arr[..., 2]

T_lo, T_hi = 40.0, 120.0
if key == "green":
    diff = G - (R + B) / 2.0            # alto = fondo verde
    # despill verde: limita G a max(R,B)
    cap = np.maximum(R, B)
    G2 = np.minimum(G, cap)
    R2, B2 = R, B
else:  # magenta
    diff = (R + B) / 2.0 - G            # alto = fondo magenta
    m = np.minimum(R, B)
    excess = np.clip(m - G, 0.0, None)
    R2 = R - excess
    B2 = B - excess
    G2 = G

alpha = np.clip((T_hi - diff) / (T_hi - T_lo), 0.0, 1.0)

# Limpieza del alfa: despeckle + erosion 1px + feather.
a_img = Image.fromarray((alpha * 255.0).astype(np.uint8), "L")
a_img = a_img.filter(ImageFilter.MedianFilter(3))
a_img = a_img.filter(ImageFilter.MinFilter(3))
a_img = a_img.filter(ImageFilter.GaussianBlur(0.7))
a255 = np.asarray(a_img).astype(np.uint8)

rgba = np.dstack([
    np.clip(R2, 0, 255),
    np.clip(G2, 0, 255),
    np.clip(B2, 0, 255),
    a255,
]).astype(np.uint8)
res = Image.fromarray(rgba, "RGBA")

bbox = res.getbbox()
if bbox:
    res = res.crop(bbox)
res.save(out)

for name, bg in (("white", (255, 255, 255)), ("dark", (18, 18, 24))):
    canvas = Image.new("RGBA", res.size, bg + (255,))
    canvas.alpha_composite(res)
    canvas.convert("RGB").save(out.replace(".png", "_on_%s.png" % name))

print("OK -> %s size=%s key=%s" % (out, res.size, key))
