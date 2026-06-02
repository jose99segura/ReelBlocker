# -*- coding: utf-8 -*-
"""
Pantalla limpia de marketing con SOLO el widget.
Renderiza el tile del widget (mismo diseño que StreakWidget) a partir del asset
real del huevo, sin depender de una captura del dispositivo.
"""
import os
from PIL import Image, ImageDraw, ImageFilter
import studio as S

EGG_DIR = os.path.join(S.BASE, "..", "app", "src", "main", "res", "drawable-nodpi")
EGGS = {
    "brasa":  ("egg_brasa_preview.webp",  "#FF7A3D"),
    "lila":   ("egg_lila_preview.webp",   "#7FB0FF"),
    "verde":  ("egg_verde_preview.webp",  "#56C271"),
    "chispa": ("egg_chispa_preview.webp", "#F2C14E"),
    "normal": ("egg_normal_preview.webp", "#FB923C"),
}

def widget_tile(tw, th, egg_file, accent_hex, days_text):
    acc = S.hx(accent_hex)
    rad = int(tw * 0.14)
    tile = S.vgradient(tw, th, S.hx("#211D2A"), S.hx("#0E0D12"))
    # halo de color tras el huevo
    cx, ey = tw // 2, int(th * 0.40)
    S.radial_glow(tile, cx, ey, int(tw * 0.46), acc, 135)
    # huevo
    egg = Image.open(os.path.join(EGG_DIR, egg_file)).convert("RGBA")
    es = int(tw * 0.60)
    egg = egg.resize((es, es), Image.LANCZOS)
    tile.paste(egg, (cx - es // 2, ey - es // 2), egg)
    # textos
    d = ImageDraw.Draw(tile)
    fdays = S.font(S.F_BLACK, int(tw * 0.16))
    dw = d.textlength(days_text, font=fdays)
    d.text((cx - dw / 2, int(th * 0.66)), days_text, font=fdays, fill=(245, 245, 250))
    flab = S.font(S.F_BLACK, int(tw * 0.085))
    lw = d.textlength("Basta!", font=flab)
    d.text((cx - lw / 2, int(th * 0.82)), "Basta!", font=flab, fill=acc)
    # borde + esquinas redondeadas
    d.rounded_rectangle([0, 0, tw - 1, th - 1], radius=rad, outline=(255, 255, 255, 45), width=2)
    tile.putalpha(S.rounded_mask((tw, th), rad))
    return tile

def clean_screen(W, H, tile, accent_hex, name):
    acc = S.hx(accent_hex)
    cv = S.vgradient(W, H, S.hx("#15131C"), S.hx("#0A0A0E"))
    S.radial_glow(cv, W // 2, int(H * 0.46), int(max(W, H) * 0.44), acc, 60)
    cx, cy = W // 2, int(H * 0.47)
    ox, oy = cx - tile.width // 2, cy - tile.height // 2
    # sombra
    layer = Image.new("RGBA", cv.size, (0, 0, 0, 0))
    layer.paste((0, 0, 0), (ox, oy + 28), tile.split()[3].point(lambda v: int(v * 0.6)))
    layer = layer.filter(ImageFilter.GaussianBlur(44))
    cv.alpha_composite(layer)
    cv.paste(tile, (ox, oy), tile)
    os.makedirs(os.path.dirname(name), exist_ok=True)
    cv.convert("RGB").save(name, "PNG")
    return name

if __name__ == "__main__":
    OUT = os.path.join(S.BASE, "out2")
    egg_file, acc = EGGS["brasa"]
    for lang, days in (("es", "2 días"), ("en", "2 days")):
        tv = widget_tile(560, 620, egg_file, acc, days)
        ts = widget_tile(520, 575, egg_file, acc, days)
        print(clean_screen(1080, 1920, tv, acc, os.path.join(OUT, lang, "vert", "widget_clean.png")))
        print(clean_screen(1080, 1080, ts, acc, os.path.join(OUT, lang, "square", "widget_clean.png")))
    # PNG suelto del tile (solo el widget, fondo transparente) para componer
    widget_tile(560, 620, egg_file, acc, "2 días").save(os.path.join(S.BASE, "raw", "widget_tile.png"))
    print("tile -> raw/widget_tile.png")
