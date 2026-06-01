# -*- coding: utf-8 -*-
"""
Motor de creativos publicitarios de Basta! (Pillow).
Genera un set ES+EN en varios formatos (vertical, cuadrado, Play, banner)
a partir de las capturas de marketing/raw/.
"""
import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

BASE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.join(BASE, "raw")
OUT = os.path.join(BASE, "out")

# ----------------------------------------------------------------------------
# Marca
# ----------------------------------------------------------------------------
BG0 = "#0C0C10"
BG1 = "#17151E"
INK = (245, 245, 250)
MUTE = (180, 178, 190)

ACCENTS = {
    "lavender": "#B69DF8",
    "green":    "#56C271",
    "orange":   "#FF7A3D",
    "blue":     "#7FB0FF",
    "gold":     "#F2C14E",
}

# Huevos por especie -> captura existente (mezcla d1/d2 según lo que haya en raw).
# El día no cambia el arte del huevo, solo el contador.
EGG_TILES = [
    ("egg_d2_normal", "lavender"),
    ("egg_d2_verde",  "green"),
    ("egg_d2_lila",   "blue"),
    ("egg_d1_brasa",  "orange"),
    ("egg_d1_chispa", "gold"),
]

F_BLACK = "C:/Windows/Fonts/seguibl.ttf"     # Segoe UI Black
F_BOLD  = "C:/Windows/Fonts/segoeuib.ttf"    # Segoe UI Bold
F_SEMI  = "C:/Windows/Fonts/seguisb.ttf"     # Segoe UI Semibold
F_REG   = "C:/Windows/Fonts/segoeui.ttf"     # Segoe UI

_fc = {}
def font(path, size):
    k = (path, size)
    if k not in _fc:
        try:
            _fc[k] = ImageFont.truetype(path, size)
        except OSError:
            _fc[k] = ImageFont.truetype(F_BOLD, size)
    return _fc[k]

def hx(c):
    c = c.lstrip("#")
    return tuple(int(c[i:i+2], 16) for i in (0, 2, 4))

def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

def darken(c, t):
    return mix(c, (0, 0, 0), t)

# ----------------------------------------------------------------------------
# Helpers de dibujo
# ----------------------------------------------------------------------------
def vgradient(w, h, top, bottom):
    col = Image.new("RGB", (1, h))
    px = col.load()
    for y in range(h):
        px[0, y] = mix(top, bottom, y / max(1, h - 1))
    return col.resize((w, h)).convert("RGBA")

def radial_glow(canvas, cx, cy, radius, color, alpha):
    layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.ellipse([cx - radius, cy - radius, cx + radius, cy + radius],
              fill=color + (alpha,))
    layer = layer.filter(ImageFilter.GaussianBlur(radius // 3))
    canvas.alpha_composite(layer)

def rounded_mask(size, radius):
    m = Image.new("L", size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size[0], size[1]], radius=radius, fill=255)
    return m

def background(w, h, accent, style):
    """style: 'dark' (premium oscuro) o 'color' (vivo)."""
    if style == "color":
        top = darken(accent, 0.45)
        bot = darken(accent, 0.68)
        cv = vgradient(w, h, top, bot)
        radial_glow(cv, int(w * 0.5), int(h * 0.30), int(max(w, h) * 0.55),
                    mix(accent, (255, 255, 255), 0.25), 110)
        radial_glow(cv, int(w * 0.15), int(h * 0.9), int(max(w, h) * 0.4), darken(accent, 0.2), 90)
    else:
        cv = vgradient(w, h, hx(BG0), mix(hx(BG0), accent, 0.16))
        radial_glow(cv, int(w * 0.5), int(h * 0.42), int(max(w, h) * 0.5), accent, 70)
    return cv

def framed(shot, target_w, radius=46):
    scale = target_w / shot.width
    img = shot.resize((target_w, int(shot.height * scale)), Image.LANCZOS).convert("RGBA")
    img.putalpha(rounded_mask(img.size, radius))
    # borde sutil
    bd = ImageDraw.Draw(img)
    bd.rounded_rectangle([0, 0, img.width - 1, img.height - 1], radius=radius,
                         outline=(255, 255, 255, 42), width=2)
    return img

def paste_phone(canvas, shot, cx, cy, target_w, angle=0, shadow=True):
    img = framed(shot, target_w)
    if angle:
        img = img.rotate(angle, expand=True, resample=Image.BICUBIC)
    ox, oy = int(cx - img.width / 2), int(cy - img.height / 2)
    if shadow:
        alpha = img.split()[3].point(lambda v: int(v * 0.55))
        sh = Image.new("RGBA", img.size, (0, 0, 0, 0))
        sh.putalpha(alpha)
        layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
        layer.paste((0, 0, 0), (ox, oy + 22), alpha)
        layer = layer.filter(ImageFilter.GaussianBlur(34))
        canvas.alpha_composite(layer)
    canvas.paste(img, (ox, oy), img)
    return img

def fit_lines(lines, fnt_path, size, max_w):
    """Reduce el tamaño hasta que la línea más larga quepa en max_w."""
    s = size
    while s > 24:
        f = font(fnt_path, s)
        d = ImageDraw.Draw(Image.new("RGB", (10, 10)))
        if all(d.textlength(ln, font=f) <= max_w for ln in lines):
            return f, s
        s -= 4
    return font(fnt_path, s), s

def draw_block(canvas, lines, cx, top, fnt, fill, gap, align="center"):
    d = ImageDraw.Draw(canvas)
    y = top
    asc, desc = fnt.getmetrics()
    lh = asc + desc
    for ln in lines:
        w = d.textlength(ln, font=fnt)
        if align == "center":
            x = cx - w / 2
        elif align == "left":
            x = cx
        else:
            x = cx - w
        d.text((x, y), ln, font=fnt, fill=fill)
        y += lh + gap
    return y

def wordmark(canvas, x, y, accent, size=46, align="left"):
    d = ImageDraw.Draw(canvas)
    f = font(F_BLACK, size)
    txt = "Basta!"
    w = d.textlength(txt, font=f)
    if align == "center":
        x = x - w / 2
    d.text((x, y), txt, font=f, fill=INK)
    # punto de acento en el "!"
    d.text((x + w + 6, y), "·", font=f, fill=accent)
    return w

def accent_bar(canvas, cx, y, accent, w=86, h=10):
    ImageDraw.Draw(canvas).rounded_rectangle(
        [cx - w / 2, y, cx + w / 2, y + h], radius=h // 2, fill=accent)

def crop_egg(shot):
    """Recorte circular del huevo+anillo (capturas 1080x2424)."""
    box = (130, 330, 950, 1150)  # x1,y1,x2,y2  -> 820x820
    tile = shot.crop(box).convert("RGBA")
    m = Image.new("L", tile.size, 0)
    ImageDraw.Draw(m).ellipse([0, 0, tile.size[0], tile.size[1]], fill=255)
    tile.putalpha(m)
    return tile

_shots = {}
def shot(name):
    if name not in _shots:
        _shots[name] = Image.open(os.path.join(RAW, name + ".png")).convert("RGBA")
    return _shots[name]

# ----------------------------------------------------------------------------
# Formatos
# ----------------------------------------------------------------------------
FORMATS = {
    "vert":    (1080, 1920),
    "square":  (1080, 1080),
    "play":    (1080, 1920),
    "feature": (1024, 500),
    "display": (1200, 628),
}

# ----------------------------------------------------------------------------
# Copy deck (ES / EN)
# ----------------------------------------------------------------------------
COPY = {
    "caer":       {"accent": "lavender", "es": (["Sal de Reels", "antes de caer"], "Tu racha crece cada día que no scrolleas"),
                                          "en": (["Get out before", "the scroll wins"], "Your streak grows every day you don't scroll")},
    "roba":       {"accent": "orange",   "es": (["El scroll te", "roba la vida"], "Recupera tus horas"),
                                          "en": (["Scrolling steals", "your life"], "Take your hours back")},
    "infinito":   {"accent": "blue",     "es": (["Adiós al", "scroll infinito"], "Reels, Shorts y TikTok se cierran solos"),
                                          "en": (["Kill the", "infinite scroll"], "Reels, Shorts & TikTok close themselves")},
    "tiempo":     {"accent": "green",    "es": (["Recupera tu tiempo,", "día a día"], "Cada caída evitada cuenta"),
                                          "en": (["Reclaim your time,", "day by day"], "Every avoided fall counts")},
    "cierran":    {"accent": "green",    "es": (["Se cierran", "solos"], "Reels, Shorts y TikTok — sin bloquear la app"),
                                          "en": (["They close", "themselves"], "Reels, Shorts & TikTok — without blocking the app")},
    "racha":      {"accent": "lavender", "es": (["Construye una racha.", "Cuida tu mascota."], "Un hábito que ves crecer"),
                                          "en": (["Build a streak.", "Grow your pet."], "A habit you watch grow")},
    "colecciona": {"accent": "gold",     "es": (["Colecciona", "5 especies"], "Tu autocontrol, recompensado"),
                                          "en": (["Collect", "5 species"], "Self-control, rewarded")},
    "grad":       {"accent": "orange",   "es": (["21 días,", "nueva mascota"], "Gradúa tu hábito y empieza otra especie"),
                                          "en": (["21 days,", "a new pet"], "Graduate your habit, start a new species")},
}

STAT = {
    "dias":  {"accent": "green", "num": "30", "es": ("DÍAS SIN CAER", "Récord que se queda contigo"),
                                              "en": ("DAYS, ZERO SCROLLING", "A record that stays with you")},
    "min":   {"accent": "blue",  "num": "39", "es": ("MIN RECUPERADOS HOY", "Cada bloqueo suma"),
                                              "en": ("MIN RECLAIMED TODAY", "Every block adds up")},
}

def L(d, lang):  # selecciona idioma
    return d[lang]

# ----------------------------------------------------------------------------
# Plantillas
# ----------------------------------------------------------------------------
def tpl_hero(screen, msg, fmt, lang, style):
    w, h = FORMATS[fmt]
    accent = hx(ACCENTS[msg["accent"]])
    cv = background(w, h, accent, style)
    head, sub = L(msg, lang)
    tall = h >= w * 1.4

    wordmark(cv, w // 2, int(h * 0.045), accent, size=int(w * 0.045), align="center")
    fhead, _ = fit_lines(head, F_BLACK, int(w * 0.105), w * 0.86)
    y = draw_block(cv, head, w // 2, int(h * 0.105), fhead, INK, gap=int(w*0.004))
    accent_bar(cv, w // 2, y + 14, accent, w=int(w*0.09))
    fsub = font(F_SEMI, int(w * 0.034))
    draw_block(cv, [sub], w // 2, y + 40, fsub, mix(accent,(255,255,255),0.35) if style=="dark" else INK, gap=0)

    if tall:
        pw = int(w * 0.62)
        paste_phone(cv, shot(screen), w // 2, int(h * 0.70), pw)
    else:  # square: el móvil sangra por abajo
        pw = int(w * 0.52)
        paste_phone(cv, shot(screen), w // 2, int(h * 0.92), pw)
    return cv

def tpl_duo(s1, s2, msg, fmt, lang, style):
    w, h = FORMATS[fmt]
    accent = hx(ACCENTS[msg["accent"]])
    cv = background(w, h, accent, style)
    head, sub = L(msg, lang)
    wordmark(cv, w // 2, int(h * 0.045), accent, size=int(w * 0.045), align="center")
    fhead, _ = fit_lines(head, F_BLACK, int(w * 0.10), w * 0.86)
    y = draw_block(cv, head, w // 2, int(h * 0.10), fhead, INK, gap=int(w*0.004))
    accent_bar(cv, w // 2, y + 12, accent, w=int(w*0.09))
    cy = int(h * 0.70) if h >= w * 1.2 else int(h * 0.92)
    pw = int(w * 0.42)
    paste_phone(cv, shot(s2), int(w * 0.62), cy, pw, angle=-7)
    paste_phone(cv, shot(s1), int(w * 0.40), cy, pw, angle=5)
    return cv

def tpl_trio(s1, s2, s3, msg, fmt, lang, style):
    w, h = FORMATS[fmt]
    accent = hx(ACCENTS[msg["accent"]])
    cv = background(w, h, accent, style)
    head, sub = L(msg, lang)
    wordmark(cv, w // 2, int(h * 0.045), accent, size=int(w * 0.045), align="center")
    fhead, _ = fit_lines(head, F_BLACK, int(w * 0.095), w * 0.86)
    draw_block(cv, head, w // 2, int(h * 0.10), fhead, INK, gap=int(w*0.004))
    cy = int(h * 0.66) if h >= w * 1.2 else int(h * 0.96)
    pw = int(w * 0.36)
    paste_phone(cv, shot(s2), int(w * 0.27), cy, pw, angle=-9)
    paste_phone(cv, shot(s3), int(w * 0.73), cy, pw, angle=9)
    paste_phone(cv, shot(s1), int(w * 0.50), cy - int(h*0.02), pw, angle=0)
    return cv

def tpl_eggposter(msg, fmt, lang, style):
    w, h = FORMATS[fmt]
    accent = hx(ACCENTS[msg["accent"]])
    cv = background(w, h, accent, "dark")
    head, sub = L(msg, lang)
    wordmark(cv, w // 2, int(h * 0.045), accent, size=int(w * 0.045), align="center")
    fhead, _ = fit_lines(head, F_BLACK, int(w * 0.10), w * 0.86)
    y = draw_block(cv, head, w // 2, int(h * 0.105), fhead, INK, gap=int(w*0.004))
    fsub = font(F_SEMI, int(w * 0.032))
    draw_block(cv, [sub], w // 2, y + 16, fsub, MUTE, gap=0)

    eggs = [e for e, _ in EGG_TILES]
    cols = {e: c for e, c in EGG_TILES}
    tile = int(w * 0.27)
    if h >= w * 1.2:
        rows = [eggs[:3], eggs[3:]]
        cy0 = int(h * 0.46)
        rowgap = int(tile * 1.05)
    else:
        rows = [eggs[:3], eggs[3:]]
        cy0 = int(h * 0.50)
        rowgap = int(tile * 0.95)
    for r, row in enumerate(rows):
        n = len(row)
        spacing = int(w * 0.30)
        x0 = w // 2 - spacing * (n - 1) // 2
        cyr = cy0 + r * rowgap
        for i, e in enumerate(row):
            cx = x0 + i * spacing
            radial_glow(cv, cx, cyr, int(tile * 0.85), hx(ACCENTS[cols[e]]), 120)
            t = crop_egg(shot(e)).resize((tile, tile), Image.LANCZOS)
            cv.paste(t, (int(cx - tile/2), int(cyr - tile/2)), t)
    return cv

def tpl_stat(key, screen, fmt, lang, style):
    w, h = FORMATS[fmt]
    s = STAT[key]
    accent = hx(ACCENTS[s["accent"]])
    cv = background(w, h, accent, "color")
    label, sub = L(s, lang)
    wordmark(cv, w // 2, int(h * 0.05), accent, size=int(w * 0.045), align="center")
    fnum = font(F_BLACK, int(w * 0.42))
    d = ImageDraw.Draw(cv)
    nw = d.textlength(s["num"], font=fnum)
    asc, desc = fnum.getmetrics()
    ny = int(h * 0.28) if h >= w*1.2 else int(h*0.20)
    d.text((w/2 - nw/2, ny), s["num"], font=fnum, fill=INK)
    flab = font(F_BLACK, int(w * 0.05))
    lw = d.textlength(label, font=flab)
    d.text((w/2 - lw/2, ny + asc + desc - int(w*0.02)), label, font=flab, fill=INK)
    fsub = font(F_SEMI, int(w * 0.034))
    sw = d.textlength(sub, font=fsub)
    d.text((w/2 - sw/2, ny + asc + desc + int(w*0.04)), sub, font=fsub, fill=mix(INK,accent,0.0))
    if h >= w * 1.2:
        paste_phone(cv, shot(screen), w // 2, int(h * 0.84), int(w * 0.46))
    return cv

def tpl_banner(screen, msg, fmt, lang, style):
    w, h = FORMATS[fmt]
    accent = hx(ACCENTS[msg["accent"]])
    cv = background(w, h, accent, style)
    head, sub = L(msg, lang)
    lx = int(w * 0.06)
    wordmark(cv, lx, int(h * 0.10), accent, size=int(h * 0.075), align="left")
    fhead, _ = fit_lines(head, F_BLACK, int(h * 0.135), w * 0.56)
    y = draw_block(cv, head, lx, int(h * 0.26), fhead, INK, gap=4, align="left")
    accent_bar(cv, lx + int(w*0.045), y + 10, accent, w=int(w*0.09))
    fsub = font(F_SEMI, int(h * 0.052))
    draw_block(cv, [sub], lx, y + 34, fsub, mix(accent,(255,255,255),0.4) if style=="dark" else INK, gap=0, align="left")
    # móvil a la derecha, sangrando
    paste_phone(cv, shot(screen), int(w * 0.82), int(h * 0.62), int(w * 0.26), angle=-6)
    return cv

# ----------------------------------------------------------------------------
# Matriz de generación
# ----------------------------------------------------------------------------
def save(img, lang, fmt, name):
    d = os.path.join(OUT, lang, fmt)
    os.makedirs(d, exist_ok=True)
    p = os.path.join(d, name + ".png")
    img.convert("RGB").save(p, "PNG")
    return p

# Huevos (arte nuevo que se queda). NUNCA usamos pantallas con mascota crecida
# (01_home=cría, 03_bestiario, 05_graduacion) porque esos sprites van a cambiar.
EGGS = [
    ("egg_d1_verde",  "racha",      "green"),
    ("egg_d1_lila",   "infinito",   "blue"),
    ("egg_d1_brasa",  "roba",       "orange"),
    ("egg_d1_chispa", "colecciona", "gold"),
    ("egg_d1_normal", "caer",       "lavender"),
]

def msg_acc(mid, accent_key):
    m = dict(COPY[mid]); m["accent"] = accent_key
    return m

def generate(lang):
    out = []

    # ---- VERTICAL (1080x1920) ----
    # Un hero por huevo, con el acento del propio huevo.
    for cap, mid, acc in EGGS:
        out.append(save(tpl_hero(cap, msg_acc(mid, acc), "vert", lang, "color"),
                        lang, "vert", f"hero_{cap}"))
    # Heroes de pantallas sin mascota
    out.append(save(tpl_hero("02_diario", COPY["tiempo"], "vert", lang, "color"), lang, "vert", "hero_diario"))
    out.append(save(tpl_hero("04_ajustes", COPY["cierran"], "vert", lang, "dark"), lang, "vert", "hero_ajustes"))
    # Composiciones con huevos
    out.append(save(tpl_duo("egg_d1_lila", "egg_d1_brasa", COPY["colecciona"], "vert", lang, "color"), lang, "vert", "duo_huevos"))
    out.append(save(tpl_duo("04_ajustes", "02_diario", COPY["infinito"], "vert", lang, "color"), lang, "vert", "duo_app"))
    out.append(save(tpl_trio("egg_d1_verde", "egg_d1_lila", "egg_d1_chispa", COPY["colecciona"], "vert", lang, "color"), lang, "vert", "trio_huevos"))
    out.append(save(tpl_eggposter(COPY["colecciona"], "vert", lang, "dark"), lang, "vert", "poster_huevos"))
    out.append(save(tpl_stat("dias", "egg_d1_verde", "vert", lang, "color"), lang, "vert", "stat_dias"))
    out.append(save(tpl_stat("min", "02_diario", "vert", lang, "color"), lang, "vert", "stat_min"))

    # ---- CUADRADO (1080x1080) ----
    for cap, mid, acc in EGGS[:3]:
        out.append(save(tpl_hero(cap, msg_acc(mid, acc), "square", lang, "color"), lang, "square", f"hero_{cap}"))
    out.append(save(tpl_hero("04_ajustes", COPY["cierran"], "square", lang, "dark"), lang, "square", "hero_ajustes"))
    out.append(save(tpl_duo("egg_d1_chispa", "egg_d1_verde", COPY["colecciona"], "square", lang, "color"), lang, "square", "duo_huevos"))
    out.append(save(tpl_trio("egg_d1_verde", "egg_d1_lila", "egg_d1_brasa", COPY["tiempo"], "square", lang, "color"), lang, "square", "trio_huevos"))
    out.append(save(tpl_eggposter(COPY["colecciona"], "square", lang, "dark"), lang, "square", "poster_huevos"))
    out.append(save(tpl_stat("dias", "egg_d1_lila", "square", lang, "color"), lang, "square", "stat_dias"))
    out.append(save(tpl_stat("min", "02_diario", "square", lang, "color"), lang, "square", "stat_min"))

    # ---- FICHA PLAY (1080x1920, oscuro) ----
    PLAY = [("egg_d1_lila", "infinito"), ("egg_d1_brasa", "caer"),
            ("02_diario", "tiempo"), ("04_ajustes", "cierran")]
    for i, (cap, mid) in enumerate(PLAY, 1):
        out.append(save(tpl_hero(cap, COPY[mid], "play", lang, "dark"), lang, "play", f"play_{i}_{mid}"))

    # ---- BANNERS ----
    out.append(save(tpl_banner("egg_d1_lila", COPY["infinito"], "feature", lang, "dark"), lang, "feature", "banner_huevo"))
    out.append(save(tpl_banner("04_ajustes", COPY["cierran"], "feature", lang, "dark"), lang, "feature", "banner_app"))
    out.append(save(tpl_banner("egg_d1_brasa", COPY["roba"], "display", lang, "color"), lang, "display", "banner_huevo"))
    out.append(save(tpl_banner("02_diario", COPY["tiempo"], "display", lang, "color"), lang, "display", "banner_diario"))
    return out

def contact_sheet(lang, paths):
    cols = 6
    th = 360
    rows = (len(paths) + cols - 1) // cols
    pad = 16
    cw = th  # ancho celda (recorte a cuadrado por miniatura encajada)
    sheet_w = cols * (cw + pad) + pad
    sheet_h = rows * (th + pad) + pad
    sheet = Image.new("RGB", (sheet_w, sheet_h), hx("#0A0A0E"))
    for i, p in enumerate(paths):
        im = Image.open(p)
        scale = th / im.height
        im = im.resize((int(im.width * scale), th), Image.LANCZOS)
        if im.width > cw:
            im = im.crop(((im.width - cw)//2, 0, (im.width - cw)//2 + cw, th))
        r, c = divmod(i, cols)
        x = pad + c * (cw + pad) + (cw - im.width)//2
        y = pad + r * (th + pad)
        sheet.paste(im, (x, y))
    out = os.path.join(OUT, f"index_{lang}.png")
    sheet.save(out, "PNG")
    return out

if __name__ == "__main__":
    total = 0
    for lang in ("es", "en"):
        paths = generate(lang)
        idx = contact_sheet(lang, paths)
        total += len(paths)
        print(f"[{lang}] {len(paths)} creativos -> index: {idx}")
    print("TOTAL:", total)
