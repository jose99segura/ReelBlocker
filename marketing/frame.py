# -*- coding: utf-8 -*-
"""Compone capturas de Basta! en mockups de publicidad (marco + degradado + titular)."""
import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

BASE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.join(BASE, "raw")
OUT = os.path.join(BASE, "out")
os.makedirs(OUT, exist_ok=True)

W, H = 1200, 2200
PHONE_W = 700                      # ancho del móvil dentro del lienzo
RADIUS = 60                        # redondeo de esquinas de la captura

# Fuentes (Windows). Fallbacks por si alguna no está.
def font(paths, size):
    for p in paths:
        try:
            return ImageFont.truetype(p, size)
        except OSError:
            continue
    return ImageFont.load_default()

F_HEAD = ["C:/Windows/Fonts/segoeuib.ttf", "C:/Windows/Fonts/arialbd.ttf"]
F_SUB  = ["C:/Windows/Fonts/segoeui.ttf",  "C:/Windows/Fonts/arial.ttf"]

def hx(c):
    c = c.lstrip("#")
    return tuple(int(c[i:i+2], 16) for i in (0, 2, 4))

def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

def gradient(top, bottom):
    """Degradado vertical suave."""
    img = Image.new("RGB", (W, H), top)
    px = img.load()
    for y in range(H):
        t = y / (H - 1)
        col = mix(top, bottom, t)
        for x in range(W):
            px[x, y] = col
    return img

def glow(img, center, color, radius, alpha):
    """Halo radial de acento detrás del móvil."""
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    cx, cy = center
    d.ellipse([cx - radius, cy - radius, cx + radius, cy + radius],
              fill=color + (alpha,))
    layer = layer.filter(ImageFilter.GaussianBlur(180))
    img.paste(Image.alpha_composite(img.convert("RGBA"), layer).convert("RGB"), (0, 0))
    return img

def center_text(draw, y, text, fnt, fill):
    bbox = draw.textbbox((0, 0), text, font=fnt)
    w = bbox[2] - bbox[0]
    draw.text(((W - w) / 2, y), text, font=fnt, fill=fill)
    return bbox[3] - bbox[1]

SCREENS = [
    # archivo, titular(es líneas), subtítulo, acento
    ("01_home.png",       ["Sal de Reels", "antes de caer"],        "Tu racha crece cada día que no scrolleas", "#B69DF8"),
    ("02_diario.png",     ["Cada caída", "evitada, contada"],       "Mira el tiempo que recuperas",             "#7FB0FF"),
    ("03_bestiario.png",  ["Colecciona", "5 especies"],             "Tu autocontrol, recompensado",             "#F2B33D"),
    ("04_ajustes.png",    ["Reels, Shorts", "y TikTok: fuera"],     "Se cierran solos. Sin bloquear la app",    "#56C271"),
    ("05_graduacion.png", ["21 dias,", "nueva mascota"],            "Gradua tu habito y empieza otra especie",  "#FF8A3D"),
]

def compose(fname, lines, sub, accent_hex):
    accent = hx(accent_hex)
    top = hx("#0C0C10")
    bottom = mix(hx("#0C0C10"), accent, 0.22)
    canvas = gradient(top, bottom)

    shot = Image.open(os.path.join(RAW, fname)).convert("RGBA")
    scale = PHONE_W / shot.width
    pw, ph = PHONE_W, int(shot.height * scale)
    shot = shot.resize((pw, ph), Image.LANCZOS)

    px_ = (W - pw) // 2
    py_ = H - ph - 70

    canvas = canvas.convert("RGB")
    canvas = glow(canvas, (W // 2, py_ + ph // 2), accent, 520, 90)
    canvas = canvas.convert("RGBA")

    # Sombra del móvil
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ds = ImageDraw.Draw(shadow)
    ds.rounded_rectangle([px_ - 6, py_ + 26, px_ + pw + 6, py_ + ph + 26],
                         radius=RADIUS + 8, fill=(0, 0, 0, 170))
    shadow = shadow.filter(ImageFilter.GaussianBlur(34))
    canvas = Image.alpha_composite(canvas, shadow)

    # Esquinas redondeadas de la captura
    mask = Image.new("L", (pw, ph), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, pw, ph], radius=RADIUS, fill=255)
    shot.putalpha(mask)
    canvas.alpha_composite(shot, (px_, py_))

    # Borde sutil del marco
    dframe = ImageDraw.Draw(canvas)
    dframe.rounded_rectangle([px_, py_, px_ + pw, py_ + ph], radius=RADIUS,
                             outline=(255, 255, 255, 38), width=2)

    # Titular + subtítulo
    draw = ImageDraw.Draw(canvas)
    fh = font(F_HEAD, 92)
    fs = font(F_SUB, 40)
    y = 150
    for ln in lines:
        h = center_text(draw, y, ln, fh, (245, 245, 250))
        y += 112
    y += 14
    center_text(draw, y, sub, fs, accent + (255,))

    # Barra de acento bajo el titular
    bw = 90
    draw.rounded_rectangle([(W - bw) / 2, 128, (W + bw) / 2, 138],
                           radius=5, fill=accent + (255,))

    out = os.path.join(OUT, "ad_" + fname)
    canvas.convert("RGB").save(out, "PNG")
    return out

if __name__ == "__main__":
    for s in SCREENS:
        print("ok ->", compose(*s))
