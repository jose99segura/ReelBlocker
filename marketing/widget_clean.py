# -*- coding: utf-8 -*-
"""Pantalla limpia con SOLO el widget centrado (showcase)."""
import os
from PIL import Image, ImageDraw, ImageFilter
import studio as S

BASE = S.BASE
RAW = os.path.join(BASE, "raw")

# Recorte exacto del widget en widget_home.png (bounds uiautomator).
src = Image.open(os.path.join(RAW, "widget_home.png")).convert("RGBA")
box = (305, 478, 776, 1004)
widget = src.crop(box)  # 471 x 526
# Esquinas redondeadas limpias (radio ~28dp @ 2.625 ≈ 74 px) para quitar el fondo.
mask = Image.new("L", widget.size, 0)
ImageDraw.Draw(mask).rounded_rectangle([0, 0, widget.size[0], widget.size[1]], radius=74, fill=255)
widget.putalpha(mask)

def make(w, h, scale, name, accent="#FB923C"):
    acc = S.hx(accent)
    cv = S.vgradient(w, h, S.hx("#15131C"), S.hx("#0A0A0E"))
    S.radial_glow(cv, w // 2, int(h * 0.46), int(max(w, h) * 0.42), acc, 70)
    wsc = widget.resize((int(widget.width * scale), int(widget.height * scale)), Image.LANCZOS)
    cx, cy = w // 2, int(h * 0.47)
    ox, oy = cx - wsc.width // 2, cy - wsc.height // 2
    # sombra
    layer = Image.new("RGBA", cv.size, (0, 0, 0, 0))
    layer.paste((0, 0, 0), (ox, oy + 26), wsc.split()[3].point(lambda v: int(v * 0.6)))
    layer = layer.filter(ImageFilter.GaussianBlur(40))
    cv.alpha_composite(layer)
    cv.paste(wsc, (ox, oy), wsc)
    out = os.path.join(S.OUT if hasattr(S, "_") else os.path.join(BASE, "out2"), name)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    cv.convert("RGB").save(out, "PNG")
    return out

# Vertical 9:16 y cuadrado, en out2.
S.OUT = os.path.join(BASE, "out2")
print(make(1080, 1920, 2.1, os.path.join("es", "vert", "widget_clean.png")))
print(make(1080, 1920, 2.1, os.path.join("en", "vert", "widget_clean.png")))
print(make(1080, 1080, 1.7, os.path.join("es", "square", "widget_clean.png")))
print(make(1080, 1080, 1.7, os.path.join("en", "square", "widget_clean.png")))
# También un PNG suelto a tamaño "pantalla" en raw.
print(make(1080, 2424, 2.4, os.path.join("..", "raw", "widget_clean.png")))
