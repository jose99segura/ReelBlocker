"""Limpia el alfa sucio de una imagen RGBA ya transparente (fleco/borde dentado):
despeckle (mediana) + erosion + feather + recorte al bbox.

Uso: python clean_alpha.py in.png out.png [erode_px]
"""
import sys
import numpy as np
from PIL import Image, ImageFilter

inp = sys.argv[1]
out = sys.argv[2]
erode = int(sys.argv[3]) if len(sys.argv) > 3 else 1

im = Image.open(inp).convert("RGBA")
arr = np.asarray(im).astype(np.float32)
R, G, B, A = arr[..., 0], arr[..., 1], arr[..., 2], arr[..., 3]

a_img = Image.fromarray(A.astype(np.uint8), "L")
a_img = a_img.filter(ImageFilter.MedianFilter(3))          # quita specks sueltos
a_img = a_img.filter(ImageFilter.MinFilter(erode * 2 + 1))  # erosiona el fleco claro
a_img = a_img.filter(ImageFilter.GaussianBlur(0.7))         # feather AA
A2 = np.asarray(a_img).astype(np.uint8)

res = Image.fromarray(np.dstack([R, G, B, A2]).astype(np.uint8), "RGBA")
bbox = res.getbbox()
if bbox:
    res = res.crop(bbox)
res.save(out)

for name, bg in (("dark", (18, 18, 24)), ("white", (255, 255, 255))):
    c = Image.new("RGBA", res.size, bg + (255,))
    c.alpha_composite(res)
    c.convert("RGB").save(out.replace(".png", "_on_%s.png" % name))
print("OK ->", out, res.size)
