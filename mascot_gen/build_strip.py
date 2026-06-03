"""Monta una carpeta <species>_evolution/ con las 4 fases + tira comparativa.
Uso: python build_strip.py <outdir> <egg.png> <crack.png> <hatch.png> <adult.png>
"""
import os
import sys
from PIL import Image, ImageDraw, ImageFont

MG = r"C:\Users\jose9\OneDrive\Escritorio\ReelBlocker\mascot_gen"
outdir = os.path.join(MG, sys.argv[1])
os.makedirs(outdir, exist_ok=True)

srcs = sys.argv[2:6]
names = ["1_egg.png", "2_cracking.png", "3_hatchling.png", "4_adult.png"]
labels = ["Huevo - dia 0", "Grieta - dia 3", "Eclosion - dia 8", "Adulto - dia 21"]

imgs = []
for src, name, label in zip(srcs, names, labels):
    im = Image.open(os.path.join(MG, src)).convert("RGBA")
    im.save(os.path.join(outdir, name))
    imgs.append((im, label))

CELL, IMG_AREA, LABEL_H, PAD = 360, 320, 46, 20
W = CELL * len(imgs)
H = IMG_AREA + LABEL_H + PAD * 2
strip = Image.new("RGBA", (W, H), (18, 18, 26, 255))
draw = ImageDraw.Draw(strip)
try:
    font = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 24)
    afont = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 40)
except Exception:
    font = afont = ImageFont.load_default()

for i, (im, label) in enumerate(imgs):
    im2 = im.copy()
    im2.thumbnail((IMG_AREA, IMG_AREA), Image.LANCZOS)
    cx = i * CELL + CELL // 2
    strip.alpha_composite(im2, (cx - im2.width // 2, PAD + (IMG_AREA - im2.height) // 2))
    tb = draw.textbbox((0, 0), label, font=font)
    draw.text((cx - (tb[2] - tb[0]) // 2, PAD + IMG_AREA + 6), label, fill=(235, 235, 240, 255), font=font)
    if i < len(imgs) - 1:
        draw.text(((i + 1) * CELL - 12, PAD + IMG_AREA // 2 - 24), ">", fill=(150, 150, 160, 255), font=afont)

strip.convert("RGB").save(os.path.join(outdir, "_evolution_strip.png"))
print("OK ->", outdir, os.listdir(outdir))
