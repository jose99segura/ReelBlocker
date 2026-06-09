# -*- coding: utf-8 -*-
"""
Creativos de redes sociales v2 — mascotas 3D + capturas nuevas.
Genera ES+EN para Instagram, TikTok, YouTube y Google Play.
Importa el motor existente (studio.py) sin tocarlo.

Uso:
    cd marketing
    python social.py
"""
import os
import studio as S
from PIL import Image, ImageDraw, ImageFilter, ImageFont

BASE  = os.path.dirname(os.path.abspath(__file__))
MASCOT = os.path.join(BASE, "..", "mascot_gen")
SOC   = os.path.join(BASE, "social")

# Registrar formatos nuevos en el motor
S.FORMATS["ig_portrait"] = (1080, 1350)
S.FORMATS["yt_thumb"]    = (1280, 720)

# Redirigir OUT del motor para que save() escriba aqui
# (sobreescrito por save_net — usamos nuestra propia funcion)

# -----------------------------------------------------------------------
# Paleta de acentos por especie (hex RGB)
# -----------------------------------------------------------------------
SPECIES_ACCENT = {
    "clasica": S.hx("#FF7A3D"),   # orange
    "tortuga": S.hx("#56C271"),   # green
    "dragon":  S.hx("#B69DF8"),   # lavender
    "lobo":    S.hx("#FF9F45"),   # amber/ember (slate queda apagado)
    "buho":    S.hx("#F2C14E"),   # gold
}
SPECIES_ORDER = ["clasica", "tortuga", "dragon", "lobo", "buho"]

# -----------------------------------------------------------------------
# Banco de hooks emocionales (ES + EN, voz directa)
# -----------------------------------------------------------------------
HOOKS = {
    "roba":       {"es": (["El scroll te", "roba la vida"], "Recupera tus horas. Un dia a la vez."),
                   "en": (["Scrolling steals", "your life"], "Take your hours back. One day at a time.")},
    "saca":       {"es": (["Cuando entras,", "esta app te saca"], "Sin coaches. Sin sermones. Solo funciona."),
                   "en": (["When you go in,", "it pulls you out"], "No coaches. No lectures. It just works.")},
    "detox":      {"es": (["Detox de", "dopamina"], "Tu cerebro necesita 23 min para recuperar el foco. Cada vez."),
                   "en": (["Dopamine", "detox"], "Your brain needs 23 min to refocus. Every single time.")},
    "menosmas":   {"es": (["Menos scroll.", "Más vida."], "Basta! hace el trabajo dificil por ti."),
                   "en": (["Less scroll.", "More life."], "Basta! does the hard work for you.")},
    "95min":      {"es": (["95 min al dia.", "Cada dia."], "Casi hora y media perdida en Reels y Shorts. Hoy."),
                   "en": (["95 min a day.", "Every day."], "Almost an hour and a half lost to Reels and Shorts. Today.")},
    "silencio":   {"es": (["Pequeña, local", "y silenciosa."], "Sin algoritmo. Sin tracking. El resto es tuyo."),
                   "en": (["Small, local,", "silent."], "No algorithm. No tracking. The rest is yours.")},
    "colecciona": {"es": (["Colecciona", "5 especies"], "Tu autocontrol, recompensado. Dia a dia."),
                   "en": (["Collect", "5 species"], "Self-control, rewarded. Day by day.")},
    "crece":      {"es": (["Tu mascota crece", "cuando tu aguantas"], "21 dias. Una nueva especie te espera."),
                   "en": (["Your pet grows", "when you hold back"], "21 days. A new species is waiting for you.")},
    "adhd":       {"es": (["Para el TDAH,", "la ansiedad,", "el scroll compulsivo"], "Una herramienta real. No una app de productividad."),
                   "en": (["For ADHD,", "anxiety,", "compulsive scrolling"], "A real tool. Not a productivity app.")},
    "infinito":   {"es": (["Adios al", "scroll infinito"], "Reels, Shorts y TikTok se cierran solos."),
                   "en": (["Kill the", "infinite scroll"], "Reels, Shorts & TikTok close themselves.")},
    "founder":    {"es": (["Edicion Founder", "4,99 €"], "El precio sube a 6,99 €. Fecha publica, sin enganos."),
                   "en": (["Founder Edition", "4.99 €"], "Price rises to 6.99 €. Public date, no tricks.")},
    "21dias":     {"es": (["21 dias,", "nueva mascota"], "Gradua tu habito. Empieza otra especie."),
                   "en": (["21 days,", "a new pet"], "Graduate your habit. Start a new species.")},
    "5species":   {"es": (["5 criaturas", "por descubrir"], "Cada una nace de un huevo distinto. ¿Cuantas coleccionas?"),
                   "en": (["5 creatures", "to discover"], "Each born from a different egg. How many will you collect?")},
    # --- Misterio / dopamina detox ---
    "misterio":      {"es": (["¿Cuál desbloquearás", "si haces detox?"], "5 criaturas. Cada una, 21 dias de racha."),
                      "en": (["Which one will you", "unlock with detox?"], "5 creatures. Each one, 21 days of streak.")},
    "desbloquea":    {"es": (["Desbloquea", "las 5 especies"], "Cada graduacion es un habito mas solido."),
                      "en": (["Unlock", "all 5 species"], "Each graduation is a stronger habit.")},
    "dopamina_loop": {"es": (["Tu cerebro en Reels:", "dopamina cada 8 seg"], "Basta! corta el bucle antes de que lo notes."),
                      "en": (["Your brain on Reels:", "dopamine every 8 sec"], "Basta! breaks the loop before you notice.")},
    # --- Explicativos del loop de 21 dias ---
    "21dias_loop":   {"es": (["21 dias", "sin Reels."], "Tu mascota se gradua. Nace otra especie."),
                      "en": (["21 days", "without Reels."], "Your pet graduates. A new species hatches.")},
    "howto":         {"es": (["Asi funciona", "Basta!"], "Sin bloquear Instagram. Sin suscripcion."),
                      "en": (["How Basta!", "works"], "Without blocking Instagram. No subscription.")},
}

def hook(key, accent_key, lang):
    """Devuelve un msg compatible con las plantillas de studio."""
    h = HOOKS[key]
    return {"accent": accent_key, "es": h["es"], "en": h["en"]}

def hmsg(key, acc_hex, lang):
    """Resuelve hook directo con color hex (para plantillas que usan accent como hex)."""
    return HOOKS[key][lang]

# -----------------------------------------------------------------------
# Helpers de criaturas
# -----------------------------------------------------------------------
_creatures = {}

def load_creature(species, phase):
    """Carga y recorta el bbox transparente de un sprite de mascota."""
    k = (species, phase)
    if k not in _creatures:
        phases = {"egg":"1_egg","cracking":"2_cracking","hatchling":"3_hatchling","adult":"4_adult"}
        p = os.path.join(MASCOT, f"{species}_evolution", phases[phase] + ".png")
        im = Image.open(p).convert("RGBA")
        bbox = im.getbbox()
        if bbox:
            im = im.crop(bbox)
        _creatures[k] = im
    return _creatures[k]

def paste_creature(canvas, species, phase, cx, cy, target_h, glow=True):
    """Pega la criatura centrada en (cx,cy) con altura target_h y halo de acento."""
    im = load_creature(species, phase)
    scale = target_h / im.height
    nw, nh = int(im.width * scale), int(im.height * scale)
    im2 = im.resize((nw, nh), Image.LANCZOS)
    ox, oy = int(cx - nw / 2), int(cy - nh / 2)

    if glow:
        acc = SPECIES_ACCENT[species]
        S.radial_glow(canvas, cx, cy, int(target_h * 0.68), acc, 100)

    # Sombra suave debajo
    alpha = im2.split()[3].point(lambda v: int(v * 0.5))
    shadow_layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    shadow_layer.paste((0, 0, 0), (ox + 18, oy + 28), alpha)
    shadow_layer = shadow_layer.filter(ImageFilter.GaussianBlur(28))
    canvas.alpha_composite(shadow_layer)

    canvas.paste(im2, (ox, oy), im2)
    return im2

# -----------------------------------------------------------------------
# Plantillas nuevas
# -----------------------------------------------------------------------

def tpl_creature_hero(species, phase, hook_key, fmt, lang, style="dark"):
    """Hero con la criatura 3D como protagonista (sin screenshot de movil)."""
    w, h = S.FORMATS[fmt]
    acc = SPECIES_ACCENT[species]
    cv = S.background(w, h, acc, style)

    head, sub = HOOKS[hook_key][lang]
    # Wordmark
    S.wordmark(cv, w // 2, int(h * 0.05), acc, size=int(w * 0.046), align="center")
    # Titular
    fhead, _ = S.fit_lines(head, S.F_BLACK, int(w * 0.105), w * 0.84)
    y = S.draw_block(cv, head, w // 2, int(h * 0.115), fhead, S.INK, gap=int(w * 0.004))
    S.accent_bar(cv, w // 2, y + 14, acc, w=int(w * 0.09))
    fsub = S.font(S.F_SEMI, int(w * 0.034))
    S.draw_block(cv, [sub], w // 2, y + 40, fsub,
                 S.mix(acc, (255, 255, 255), 0.35) if style == "dark" else S.INK, gap=0)

    # Criatura centrada, ocupa el espacio medio-bajo
    tall = h >= w * 1.1
    if tall:
        paste_creature(cv, species, phase, w // 2, int(h * 0.70), int(h * 0.38))
    else:
        paste_creature(cv, species, phase, w // 2, int(h * 0.65), int(h * 0.50))
    return cv


def tpl_evolution_row(species, hook_key, fmt, lang):
    """Muestra las 4 fases (egg->cracking->hatchling->adult) con etiquetas de dia."""
    w, h = S.FORMATS[fmt]
    acc = SPECIES_ACCENT[species]
    cv = S.background(w, h, acc, "dark")

    head, sub = HOOKS[hook_key][lang]
    S.wordmark(cv, w // 2, int(h * 0.05), acc, size=int(w * 0.046), align="center")
    fhead, _ = S.fit_lines(head, S.F_BLACK, int(w * 0.10), w * 0.84)
    y = S.draw_block(cv, head, w // 2, int(h * 0.115), fhead, S.INK, gap=int(w * 0.004))
    S.accent_bar(cv, w // 2, y + 14, acc, w=int(w * 0.09))
    fsub = S.font(S.F_SEMI, int(w * 0.034))
    S.draw_block(cv, [sub], w // 2, y + 40, fsub, S.MUTE, gap=0)

    phases = [("egg", "Día 0" if lang == "es" else "Day 0"),
              ("cracking", "Día 3" if lang == "es" else "Day 3"),
              ("hatchling", "Día 8" if lang == "es" else "Day 8"),
              ("adult", "Día 21" if lang == "es" else "Day 21")]
    n = 4
    spacing = w // (n + 1)
    row_y = int(h * 0.64) if h >= w * 1.1 else int(h * 0.60)
    item_h = int(min(w * 0.22, h * 0.28))
    d = ImageDraw.Draw(cv)
    flabel = S.font(S.F_SEMI, int(w * 0.030))
    for i, (ph, label) in enumerate(phases):
        cx = spacing * (i + 1)
        paste_creature(cv, species, ph, cx, row_y, item_h, glow=(ph == "adult"))
        # etiqueta de dia
        lw = d.textlength(label, font=flabel)
        d.text((cx - lw / 2, row_y + item_h // 2 + int(w * 0.015)), label,
               font=flabel, fill=S.MUTE)
        # flecha separadora
        if i < n - 1:
            arr_x = cx + spacing // 2
            d.text((arr_x - 12, row_y - int(w * 0.016)), ">",
                   font=S.font(S.F_BLACK, int(w * 0.038)), fill=S.mix(acc, (80, 80, 90), 0.6))
    return cv


def tpl_collection(hook_key, fmt, lang):
    """Las 5 criaturas adultas, cada una con su glow de acento."""
    w, h = S.FORMATS[fmt]
    # Acento neutral dorado para el fondo
    acc = S.hx("#F2C14E")
    cv = S.background(w, h, acc, "dark")

    head, sub = HOOKS[hook_key][lang]
    S.wordmark(cv, w // 2, int(h * 0.05), acc, size=int(w * 0.046), align="center")
    fhead, _ = S.fit_lines(head, S.F_BLACK, int(w * 0.10), w * 0.84)
    y = S.draw_block(cv, head, w // 2, int(h * 0.115), fhead, S.INK, gap=int(w * 0.004))
    S.accent_bar(cv, w // 2, y + 14, acc, w=int(w * 0.09))
    fsub = S.font(S.F_SEMI, int(w * 0.034))
    S.draw_block(cv, [sub], w // 2, y + 40, fsub, S.MUTE, gap=0)

    tall = h >= w * 1.1
    item_h = int(w * 0.23) if tall else int(h * 0.40)

    if tall:
        # 3 arriba + 2 abajo (piramide)
        top3 = SPECIES_ORDER[:3]
        bot2 = SPECIES_ORDER[3:]
        row1_y = int(h * 0.57)
        row2_y = int(h * 0.79)
        sp3 = w // 4
        sp2 = w // 3
        for i, sp in enumerate(top3):
            paste_creature(cv, sp, "adult", sp3 * (i + 1), row1_y, item_h)
        for i, sp in enumerate(bot2):
            paste_creature(cv, sp, "adult", sp2 * (i + 1), row2_y, item_h)
    else:
        # 5 en una fila
        sp = w // 6
        row_y = int(h * 0.60)
        for i, spec in enumerate(SPECIES_ORDER):
            paste_creature(cv, spec, "adult", sp * (i + 1), row_y, item_h)
    return cv


def tpl_thumb(species, phase, hook_key, lang):
    """YouTube thumbnail 1280x720 — titular grande izq + criatura dcha."""
    w, h = S.FORMATS["yt_thumb"]
    acc = SPECIES_ACCENT[species]
    cv = S.background(w, h, acc, "dark")

    head, sub = HOOKS[hook_key][lang]
    lx = int(w * 0.06)
    S.wordmark(cv, lx, int(h * 0.07), acc, size=int(h * 0.09), align="left")
    fhead, _ = S.fit_lines(head, S.F_BLACK, int(h * 0.195), w * 0.52)
    y = S.draw_block(cv, head, lx, int(h * 0.23), fhead, S.INK, gap=6, align="left")
    S.accent_bar(cv, lx + int(w * 0.04), y + 10, acc, w=int(w * 0.08), h=8)
    fsub = S.font(S.F_SEMI, int(h * 0.056))
    S.draw_block(cv, [sub], lx, y + 28, fsub, S.MUTE, gap=0, align="left")

    # Criatura grande a la derecha, sangrando ligeramente
    paste_creature(cv, species, phase, int(w * 0.795), int(h * 0.50), int(h * 0.85))
    return cv


def tpl_grad_hero(screen_name, hook_key, fmt, lang, style="dark"):
    """Hero de graduacion: captura de graduacion enmarcada en movil."""
    msg = {"accent": "orange", "es": HOOKS[hook_key]["es"], "en": HOOKS[hook_key]["en"]}
    return S.tpl_hero(screen_name, msg, fmt, lang, style)


def tpl_feature_creature(species, hook_key, lang, style="dark"):
    """Play feature 1024x500 — criatura dcha + hook izq."""
    w, h = S.FORMATS["feature"]
    acc = SPECIES_ACCENT[species]
    cv = S.background(w, h, acc, style)

    head, sub = HOOKS[hook_key][lang]
    lx = int(w * 0.05)
    S.wordmark(cv, lx, int(h * 0.10), acc, size=int(h * 0.11), align="left")
    fhead, _ = S.fit_lines(head, S.F_BLACK, int(h * 0.175), w * 0.52)
    y = S.draw_block(cv, head, lx, int(h * 0.30), fhead, S.INK, gap=4, align="left")
    S.accent_bar(cv, lx + int(w * 0.045), y + 10, acc, w=int(w * 0.09), h=8)
    fsub = S.font(S.F_SEMI, int(h * 0.074))
    S.draw_block(cv, [sub], lx, y + 28, fsub, S.MUTE, gap=0, align="left")

    paste_creature(cv, species, "adult", int(w * 0.80), int(h * 0.50), int(h * 0.85))
    return cv


# -----------------------------------------------------------------------
# Helpers de silueta
# -----------------------------------------------------------------------

def creature_silhouette(species, phase, blur_r=10):
    """Sprite RGBA convertido a silueta oscura con tinte de acento muy oscurecido."""
    im = load_creature(species, phase).copy()
    _, _, _, a = im.split()
    # Color: mix entre el acento de la especie y casi-negro (82% negro)
    acc = SPECIES_ACCENT[species]
    tint = S.mix(acc, (12, 10, 20), 0.82)
    colored = Image.new("RGBA", im.size, tint + (255,))
    colored.putalpha(a)
    blurred = colored.filter(ImageFilter.GaussianBlur(blur_r))
    return blurred


def paste_silhouette(canvas, species, phase, cx, cy, target_h):
    """Pega una silueta centrada en (cx,cy), sin glow propio."""
    im = creature_silhouette(species, phase)
    scale = target_h / im.height
    nw, nh = max(1, int(im.width * scale)), max(1, int(im.height * scale))
    im2 = im.resize((nw, nh), Image.LANCZOS)
    ox, oy = int(cx - nw / 2), int(cy - nh / 2)
    canvas.paste(im2, (ox, oy), im2)


def draw_question_mark(canvas, cx, cy, size, acc):
    """Dibuja un circulo semitransparente con '?' como overlay de misterio."""
    d = ImageDraw.Draw(canvas)
    r = size // 2
    overlay = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay)
    od.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(255, 255, 255, 28))
    canvas.alpha_composite(overlay)
    fq = S.font(S.F_BLACK, int(size * 0.55))
    tw = d.textlength("?", font=fq)
    asc, desc = fq.getmetrics()
    d.text((cx - tw / 2, cy - (asc + desc) / 2), "?",
           font=fq, fill=S.mix(acc, (255, 255, 255), 0.55) + (160,))


# -----------------------------------------------------------------------
# Template: misterio — siluetas de las 5 criaturas
# -----------------------------------------------------------------------

def tpl_mystery(hook_key, fmt, lang, reveal_one=None):
    """5 siluetas oscuras con '?' — intriga de coleccion Pokedex.
    Si reveal_one es un species name, esa criatura se muestra a color (ya desbloqueada)."""
    w, h = S.FORMATS[fmt]
    acc = S.hx("#B69DF8")  # lavender como fondo neutro de misterio
    cv = S.background(w, h, acc, "dark")

    head, sub = HOOKS[hook_key][lang]
    S.wordmark(cv, w // 2, int(h * 0.05), acc, size=int(w * 0.046), align="center")
    fhead, _ = S.fit_lines(head, S.F_BLACK, int(w * 0.105), w * 0.84)
    y = S.draw_block(cv, head, w // 2, int(h * 0.115), fhead, S.INK, gap=int(w * 0.004))
    S.accent_bar(cv, w // 2, y + 14, acc, w=int(w * 0.09))
    fsub = S.font(S.F_SEMI, int(w * 0.034))
    S.draw_block(cv, [sub], w // 2, y + 40, fsub, S.MUTE, gap=0)

    tall = h >= w * 1.1
    item_h = int(w * 0.22) if tall else int(h * 0.38)
    qsize = int(item_h * 0.28)

    if tall:
        # 3 arriba + 2 abajo
        top3, bot2 = SPECIES_ORDER[:3], SPECIES_ORDER[3:]
        row1_y = int(h * 0.57)
        row2_y = int(h * 0.80)
        sp3, sp2 = w // 4, w // 3
        positions = ([(sp3 * (i + 1), row1_y) for i in range(3)] +
                     [(sp2 * (i + 1), row2_y) for i in range(2)])
        species_list = top3 + bot2
    else:
        # Fila de 5
        sp = w // 6
        row_y = int(h * 0.62)
        positions = [(sp * (i + 1), row_y) for i in range(5)]
        species_list = SPECIES_ORDER

    for (cx, cy), sp in zip(positions, species_list):
        if reveal_one and sp == reveal_one:
            paste_creature(cv, sp, "adult", cx, cy, item_h, glow=True)
        else:
            paste_silhouette(cv, sp, "adult", cx, cy, item_h)
            draw_question_mark(cv, cx, cy - int(item_h * 0.12), qsize, acc)

    return cv


# -----------------------------------------------------------------------
# Template: 21 dias — explica el loop de 21 dias con flujo visual
# -----------------------------------------------------------------------

def tpl_21days(species, hook_key, fmt, lang):
    """Explica el loop: entra en Reels -> app te saca -> racha crece -> dia 21 = nueva especie."""
    w, h = S.FORMATS[fmt]
    acc = SPECIES_ACCENT[species]
    cv = S.background(w, h, acc, "dark")

    head, sub = HOOKS[hook_key][lang]
    S.wordmark(cv, w // 2, int(h * 0.05), acc, size=int(w * 0.046), align="center")
    fhead, _ = S.fit_lines(head, S.F_BLACK, int(w * 0.115), w * 0.84)
    y = S.draw_block(cv, head, w // 2, int(h * 0.115), fhead, S.INK, gap=int(w * 0.004))
    S.accent_bar(cv, w // 2, y + 14, acc, w=int(w * 0.09))
    fsub = S.font(S.F_SEMI, int(w * 0.034))
    S.draw_block(cv, [sub], w // 2, y + 40, fsub, S.MUTE, gap=0)

    tall = h >= w * 1.1
    steps_es = [
        ("Entras en Reels", "App detecta. Back automatico."),
        ("Racha crece", "Cada dia sin caer cuenta."),
        ("Dia 21", "Nueva especie desbloqueada."),
    ]
    steps_en = [
        ("You open Reels", "App detects. Back automatic."),
        ("Streak grows", "Every day without falling counts."),
        ("Day 21", "New species unlocked."),
    ]
    steps = steps_es if lang == "es" else steps_en

    if tall:
        # Layout vertical: 3 pasos en columna + criatura adulta debajo
        step_x = w // 2
        step_start_y = int(h * 0.36)
        step_gap = int(h * 0.12)
        d = ImageDraw.Draw(cv)
        fstep = S.font(S.F_BOLD, int(w * 0.042))
        fsub2 = S.font(S.F_SEMI, int(w * 0.030))
        farrow = S.font(S.F_BLACK, int(w * 0.042))

        for i, (title, detail) in enumerate(steps):
            sy = step_start_y + i * step_gap
            # Numero en circulo
            nr = int(w * 0.038)
            overlay = Image.new("RGBA", cv.size, (0, 0, 0, 0))
            od = ImageDraw.Draw(overlay)
            od.ellipse([step_x - nr * 3 - nr, sy, step_x - nr * 3 + nr, sy + nr * 2],
                       fill=acc + (220,))
            cv.alpha_composite(overlay)
            nd = ImageDraw.Draw(cv)
            nf = S.font(S.F_BLACK, int(nr * 1.1))
            nd.text((step_x - nr * 3 - nr // 4, sy - nr // 8), str(i + 1), font=nf, fill=S.INK)
            # Texto
            tw = d.textlength(title, font=fstep)
            d.text((step_x - nr, sy - 4), title, font=fstep, fill=S.INK)
            d.text((step_x - nr, sy + int(w * 0.044)), detail, font=fsub2, fill=S.MUTE)
            # Flecha entre pasos
            if i < len(steps) - 1:
                d.text((step_x - nr * 3 - nr // 4, sy + step_gap - int(w * 0.018)),
                       "↓", font=farrow, fill=S.mix(acc, (80, 80, 90), 0.5) + (180,))

        # Criatura adulta en el tercio inferior
        paste_creature(cv, species, "adult", w // 2, int(h * 0.84), int(h * 0.22), glow=True)
    else:
        # Layout horizontal compacto (square / yt_thumb): 3 columnas + criatura
        n = len(steps)
        col_w = int(w * 0.28)
        start_x = int(w * 0.08)
        cy = int(h * 0.62)
        d = ImageDraw.Draw(cv)
        fstep = S.font(S.F_BOLD, int(h * 0.068))
        fsub2 = S.font(S.F_SEMI, int(h * 0.048))
        farrow = S.font(S.F_BLACK, int(h * 0.072))

        for i, (title, detail) in enumerate(steps):
            cx_col = start_x + i * (col_w + int(w * 0.02))
            # Numero
            nd = ImageDraw.Draw(cv)
            nf = S.font(S.F_BLACK, int(h * 0.09))
            nd.text((cx_col, cy - int(h * 0.10)), str(i + 1) + ".",
                    font=nf, fill=acc + (230,))
            d.text((cx_col, cy + int(h * 0.01)), title, font=fstep, fill=S.INK)
            # Wrap detail si es largo
            d.text((cx_col, cy + int(h * 0.085)), detail, font=fsub2, fill=S.MUTE)
            if i < n - 1:
                d.text((cx_col + col_w - int(w * 0.01), cy + int(h * 0.01)),
                       ">", font=farrow, fill=S.mix(acc, (80, 80, 90), 0.5) + (180,))

        # Criatura a la derecha
        paste_creature(cv, species, "adult", int(w * 0.88), cy, int(h * 0.68), glow=True)

    # CTA abajo
    d2 = ImageDraw.Draw(cv)
    fcta = S.font(S.F_SEMI, int(w * 0.028))
    cta = "Gratis en Google Play" if lang == "es" else "Free on Google Play"
    ctaw = d2.textlength(cta, font=fcta)
    d2.text((w / 2 - ctaw / 2, int(h * 0.955)), cta, font=fcta,
            fill=S.mix(acc, (255, 255, 255), 0.5) + (180,))
    return cv


# -----------------------------------------------------------------------
# Template: howto — 4 pasos de como funciona la app
# -----------------------------------------------------------------------

def tpl_howto(fmt, lang):
    """Creativo explicativo: 4 pasos de como funciona Basta!"""
    w, h = S.FORMATS[fmt]
    acc = S.hx("#F2C14E")  # dorado neutro
    cv = S.background(w, h, acc, "dark")

    head, sub = HOOKS["howto"][lang]
    S.wordmark(cv, w // 2, int(h * 0.05), acc, size=int(w * 0.046), align="center")
    fhead, _ = S.fit_lines(head, S.F_BLACK, int(w * 0.105), w * 0.84)
    y = S.draw_block(cv, head, w // 2, int(h * 0.115), fhead, S.INK, gap=int(w * 0.004))
    S.accent_bar(cv, w // 2, y + 14, acc, w=int(w * 0.09))
    fsub = S.font(S.F_SEMI, int(w * 0.034))
    S.draw_block(cv, [sub], w // 2, y + 40, fsub, S.MUTE, gap=0)

    steps_es = [
        ("Abres Reels o Shorts",     "App detecta. Cierra la seccion."),
        ("Sin bloquear Instagram",   "Solo la parte que engancha."),
        ("Cada dia sin caer: +1",    "Tu mascota crece con la racha."),
        ("Dia 21: mascota graduada", "Nueva especie desbloqueada."),
    ]
    steps_en = [
        ("You open Reels or Shorts",  "App detects. Closes the section."),
        ("Without blocking the app",  "Only the addictive part."),
        ("Every day without: +1",     "Your pet grows with the streak."),
        ("Day 21: pet graduates",     "New species unlocked."),
    ]
    steps = steps_es if lang == "es" else steps_en

    tall = h >= w * 1.1
    if tall:
        # Columna de 4 pasos
        step_start = int(h * 0.36)
        step_gap = int(h * 0.135)
        nr = int(w * 0.035)
        d = ImageDraw.Draw(cv)
        fstep = S.font(S.F_BOLD, int(w * 0.040))
        fdet = S.font(S.F_SEMI, int(w * 0.029))

        for i, (title, detail) in enumerate(steps):
            sy = step_start + i * step_gap
            lx = int(w * 0.10)
            # Circulo con numero
            overlay = Image.new("RGBA", cv.size, (0, 0, 0, 0))
            od = ImageDraw.Draw(overlay)
            od.ellipse([lx, sy, lx + nr * 2, sy + nr * 2], fill=acc + (200,))
            cv.alpha_composite(overlay)
            nd = ImageDraw.Draw(cv)
            nf = S.font(S.F_BLACK, int(nr * 1.1))
            nd.text((lx + nr // 4, sy + nr // 8), str(i + 1), font=nf, fill=(10, 8, 18))
            # Textos
            tx = lx + nr * 2 + int(w * 0.04)
            d.text((tx, sy - 2), title, font=fstep, fill=S.INK)
            d.text((tx, sy + int(w * 0.043)), detail, font=fdet, fill=S.MUTE)

        # Criatura clasica abajo
        paste_creature(cv, "clasica", "adult", w // 2, int(h * 0.88), int(h * 0.18), glow=True)
    else:
        # 2x2 grid compacto
        positions = [
            (int(w * 0.28), int(h * 0.48)),
            (int(w * 0.72), int(h * 0.48)),
            (int(w * 0.28), int(h * 0.74)),
            (int(w * 0.72), int(h * 0.74)),
        ]
        d = ImageDraw.Draw(cv)
        fstep = S.font(S.F_BOLD, int(w * 0.036))
        fdet = S.font(S.F_SEMI, int(w * 0.026))
        nr = int(w * 0.030)
        for i, ((cx, cy_p), (title, detail)) in enumerate(zip(positions, steps)):
            # Numero
            overlay = Image.new("RGBA", cv.size, (0, 0, 0, 0))
            od = ImageDraw.Draw(overlay)
            od.ellipse([cx - nr - int(w * 0.12), cy_p - nr, cx - int(w * 0.12) + nr, cy_p + nr],
                       fill=acc + (200,))
            cv.alpha_composite(overlay)
            nd = ImageDraw.Draw(cv)
            nd.text((cx - int(w * 0.12) - nr // 2, cy_p - nr // 2), str(i + 1),
                    font=S.font(S.F_BLACK, int(nr * 1.2)), fill=(10, 8, 18))
            # Texto centrado
            tw = d.textlength(title, font=fstep)
            d.text((cx - tw / 2, cy_p - int(h * 0.025)), title, font=fstep, fill=S.INK)
            tw2 = d.textlength(detail, font=fdet)
            d.text((cx - tw2 / 2, cy_p + int(h * 0.032)), detail, font=fdet, fill=S.MUTE)

    return cv


# -----------------------------------------------------------------------
# Template: reveal — split silueta / criatura revelada
# -----------------------------------------------------------------------

def tpl_reveal(species, hook_key, fmt, lang):
    """Split izquierda: silueta con '?' / derecha: criatura revelada a color."""
    w, h = S.FORMATS[fmt]
    acc = SPECIES_ACCENT[species]
    cv = S.background(w, h, acc, "dark")

    head, sub = HOOKS[hook_key][lang]
    S.wordmark(cv, w // 2, int(h * 0.05), acc, size=int(w * 0.046), align="center")
    fhead, _ = S.fit_lines(head, S.F_BLACK, int(w * 0.105), w * 0.84)
    y = S.draw_block(cv, head, w // 2, int(h * 0.115), fhead, S.INK, gap=int(w * 0.004))
    S.accent_bar(cv, w // 2, y + 14, acc, w=int(w * 0.09))
    fsub = S.font(S.F_SEMI, int(w * 0.034))
    S.draw_block(cv, [sub], w // 2, y + 40, fsub, S.MUTE, gap=0)

    tall = h >= w * 1.1
    mid_y = int(h * 0.66) if tall else int(h * 0.62)
    item_h = int(h * 0.36) if tall else int(h * 0.50)
    split_x = w // 2

    # Linea divisoria sutil
    div = ImageDraw.Draw(cv)
    div.line([(split_x, int(h * 0.30)), (split_x, int(h * 0.92))],
             fill=acc + (60,), width=2)

    # Izquierda: silueta + label "?" + texto
    paste_silhouette(cv, species, "adult", int(w * 0.27), mid_y, item_h)
    qsize = int(item_h * 0.30)
    draw_question_mark(cv, int(w * 0.27), mid_y - int(item_h * 0.1), qsize, acc)
    d = ImageDraw.Draw(cv)
    qlabel = "¿Cuál es?" if lang == "es" else "Which one?"
    fq = S.font(S.F_SEMI, int(w * 0.032))
    qw = d.textlength(qlabel, font=fq)
    d.text((int(w * 0.27) - qw / 2, mid_y + item_h // 2 + int(h * 0.012)),
           qlabel, font=fq, fill=S.MUTE)

    # Derecha: criatura a color + nombre
    paste_creature(cv, species, "adult", int(w * 0.73), mid_y, item_h, glow=True)
    # Nombre de especie en acento
    species_names = {
        "clasica": ("Clasica", "Classic"), "tortuga": ("Tortuga", "Turtle"),
        "dragon": ("Dragon", "Dragon"), "lobo": ("Lobo", "Wolf"), "buho": ("Buho", "Thunderbird"),
    }
    sp_name = species_names[species][0 if lang == "es" else 1]
    fname = S.font(S.F_BOLD, int(w * 0.038))
    nw = d.textlength(sp_name, font=fname)
    d.text((int(w * 0.73) - nw / 2, mid_y + item_h // 2 + int(h * 0.012)),
           sp_name, font=fname, fill=acc)

    return cv


# -----------------------------------------------------------------------
# save_net: guarda en social/<lang>/<network>/<name>.png
# -----------------------------------------------------------------------
def save_net(img, lang, network, name):
    d = os.path.join(SOC, lang, network)
    os.makedirs(d, exist_ok=True)
    p = os.path.join(d, name + ".png")
    img.convert("RGB").save(p, "PNG")
    return p


# -----------------------------------------------------------------------
# Generacion
# -----------------------------------------------------------------------
def gen_assets():
    """Copia evolution strips y renders adultos recortados a social/assets/."""
    d = os.path.join(SOC, "assets")
    os.makedirs(d, exist_ok=True)
    for sp in SPECIES_ORDER:
        # Evolution strip
        src = os.path.join(MASCOT, f"{sp}_evolution", "_evolution_strip.png")
        if os.path.exists(src):
            dst = os.path.join(d, f"strip_{sp}.png")
            Image.open(src).save(dst)
        # Render adulto recortado
        im = load_creature(sp, "adult")
        im.save(os.path.join(d, f"adult_{sp}.png"))
    print(f"  assets: {len(os.listdir(d))} archivos")


def gen(lang):
    paths = []
    acc_map = {sp: list(SPECIES_ACCENT.keys())[list(SPECIES_ACCENT.values()).index(v)]
               if False else sp for sp, v in SPECIES_ACCENT.items()}

    # Alias para accents por nombre de studio
    sp_to_acc = {
        "clasica": "orange", "tortuga": "green",
        "dragon": "lavender", "lobo": "orange", "buho": "gold"
    }

    # ---- INSTAGRAM square 1080x1080 ----
    # 5 heroes de criaturas adultas (una por especie)
    for sp in SPECIES_ORDER:
        hook_k = {"clasica": "roba", "tortuga": "silencio", "dragon": "detox",
                  "lobo": "saca", "buho": "menosmas"}[sp]
        paths.append(save_net(tpl_creature_hero(sp, "adult", hook_k, "square", lang, "dark"),
                               lang, "instagram", f"sq_hero_{sp}"))
    # Collection square
    paths.append(save_net(tpl_collection("5species", "square", lang),
                          lang, "instagram", "sq_collection"))

    # ---- INSTAGRAM portrait 1080x1350 ----
    # 3 heroes de criaturas (las mas visuales)
    for sp in ["clasica", "dragon", "buho"]:
        hook_k = {"clasica": "crece", "dragon": "detox", "buho": "95min"}[sp]
        paths.append(save_net(tpl_creature_hero(sp, "adult", hook_k, "ig_portrait", lang, "color"),
                               lang, "instagram", f"port_hero_{sp}"))
    # Evolution clasica
    paths.append(save_net(tpl_evolution_row("clasica", "crece", "ig_portrait", lang),
                          lang, "instagram", "port_evolucion_clasica"))
    # Collection portrait
    paths.append(save_net(tpl_collection("colecciona", "ig_portrait", lang),
                          lang, "instagram", "port_collection"))

    # ---- INSTAGRAM vert 1080x1920 ----
    # Graduaciones enmarcadas (las capturas nuevas son el gancho)
    for screen, hook_k in [("12_graduacion", "crece"), ("11_bestiario_5de5", "5species"),
                             ("12_grad_buho", "menosmas")]:
        paths.append(save_net(
            S.tpl_hero(screen, {"accent": sp_to_acc.get("clasica", "orange"),
                                "es": HOOKS[hook_k]["es"], "en": HOOKS[hook_k]["en"]},
                       "vert", lang, "dark"),
            lang, "instagram", f"vert_screen_{screen}"))
    # Hero ADHD (texto potente)
    paths.append(save_net(tpl_creature_hero("lobo", "adult", "adhd", "vert", lang, "dark"),
                          lang, "instagram", "vert_adhd_lobo"))
    # Evolution en vertical
    paths.append(save_net(tpl_evolution_row("dragon", "21dias", "vert", lang),
                          lang, "instagram", "vert_evo_dragon"))

    # ---- TIKTOK vert 1080x1920 ----
    for sp, hook_k in [("clasica", "roba"), ("tortuga", "infinito"), ("dragon", "detox")]:
        paths.append(save_net(tpl_creature_hero(sp, "adult", hook_k, "vert", lang, "color"),
                               lang, "tiktok", f"hero_{sp}"))
    paths.append(save_net(tpl_evolution_row("lobo", "crece", "vert", lang),
                          lang, "tiktok", "evo_lobo"))
    paths.append(save_net(tpl_collection("colecciona", "vert", lang),
                          lang, "tiktok", "collection"))
    paths.append(save_net(tpl_creature_hero("buho", "adult", "founder", "vert", lang, "dark"),
                          lang, "tiktok", "scarcity_founder"))

    # ---- YOUTUBE thumb 1280x720 ----
    for sp, hook_k in [("clasica", "roba"), ("dragon", "detox"), ("lobo", "saca")]:
        paths.append(save_net(tpl_thumb(sp, "adult", hook_k, lang),
                               lang, "youtube", f"thumb_{sp}"))
    paths.append(save_net(tpl_evolution_row("buho", "5species", "yt_thumb", lang),
                          lang, "youtube", "thumb_evo_buho"))

    # ---- GOOGLE PLAY feature + screenshots ----
    # Feature banner con criatura
    paths.append(save_net(tpl_feature_creature("clasica", "roba", lang, "dark"),
                          lang, "play", "feature_clasica"))
    paths.append(save_net(tpl_feature_creature("dragon", "detox", lang, "dark"),
                          lang, "play", "feature_dragon"))
    # Collection feature
    paths.append(save_net(tpl_collection("5species", "feature", lang),
                          lang, "play", "feature_collection"))
    # Screenshots Play enmarcadas con hook (dark, para Play Store)
    for screen, hook_k, acc in [
        ("12_graduacion",    "crece",    "orange"),
        ("11_bestiario_5de5","5species", "gold"),
        ("14_home_egg",      "crece",    "lavender"),
        ("04_ajustes_new",   "infinito", "green"),
    ]:
        paths.append(save_net(
            S.tpl_hero(screen, {"accent": acc,
                                "es": HOOKS[hook_k]["es"], "en": HOOKS[hook_k]["en"]},
                       "play", lang, "dark"),
            lang, "play", f"play_{screen}"))

    # ---- STATS: capturas del Diario con datos reales ----
    # Record 30 dias
    for fmt, net, acc in [("vert","instagram","green"), ("play","play","green"), ("square","instagram","green")]:
        paths.append(save_net(
            S.tpl_hero("16_diario_record",
                       {"accent": acc, "es": (["30 dias.", "Récord."], "93 bloqueos. 46 min recuperados. Real."),
                                        "en": (["30 days.", "Record."], "93 blocks. 46 min reclaimed. Real.")},
                       fmt, lang, "dark"),
            lang, net, f"stats_record_{fmt}"))
    # Semana con grafico de barras
    for fmt, net, acc in [("vert","instagram","blue"), ("play","play","blue")]:
        paths.append(save_net(
            S.tpl_hero("15_diario_semana",
                       {"accent": acc, "es": (["Cada bloqueo", "cuenta"], "Reels, Shorts y TikTok evitados. Día a día."),
                                        "en": (["Every block", "counts"], "Reels, Shorts & TikTok avoided. Day by day.")},
                       fmt, lang, "dark"),
            lang, net, f"stats_semana_{fmt}"))
    # Stats grandes con tpl_stat del motor (30 dias, 46 min)
    S.STAT["dias_real"] = {"accent": "green", "num": "30",
                           "es": ("DÍAS DE RÉCORD", "Tu historial. Sin trucos."),
                           "en": ("DAYS RECORD", "Your history. No tricks.")}
    S.STAT["min_real"]  = {"accent": "blue",  "num": "46",
                           "es": ("MIN RECUPERADOS HOY", "93 bloqueos × 30 s estimados"),
                           "en": ("MIN RECLAIMED TODAY", "93 blocks × 30 s estimated")}
    paths.append(save_net(S.tpl_stat("dias_real", "16_diario_record", "vert",  lang, "color"), lang, "instagram", "stat_30dias_vert"))
    paths.append(save_net(S.tpl_stat("dias_real", "16_diario_record", "square", lang, "color"), lang, "instagram", "stat_30dias_sq"))
    paths.append(save_net(S.tpl_stat("min_real",  "15_diario_semana", "vert",  lang, "color"), lang, "tiktok",    "stat_46min_tiktok"))
    paths.append(save_net(S.tpl_stat("dias_real", "16_diario_record", "yt_thumb", lang, "color"), lang, "youtube", "stat_30dias_thumb"))

    # ---- MISTERIO: siluetas + "¿Cuál desbloquearás?" ----
    # 5 siluetas puras (ninguna revelada)
    for fmt, net in [("square", "instagram"), ("ig_portrait", "instagram"), ("vert", "instagram")]:
        paths.append(save_net(tpl_mystery("misterio", fmt, lang),
                              lang, net, f"mystery_5sil_{fmt}"))
    # Silueta con la Clasica ya revelada ("ya tienes esta, ¿cuántas más?")
    for fmt, net in [("square", "instagram"), ("vert", "tiktok")]:
        paths.append(save_net(tpl_mystery("desbloquea", fmt, lang, reveal_one="clasica"),
                              lang, net, f"mystery_reveal1_{fmt}"))
    # Misterio en TikTok vertical
    paths.append(save_net(tpl_mystery("misterio", "vert", lang), lang, "tiktok", "mystery_tiktok"))

    # ---- 21 DIAS LOOP: mecanica explicada ----
    for fmt, net in [("vert", "instagram"), ("ig_portrait", "instagram"), ("yt_thumb", "youtube")]:
        paths.append(save_net(tpl_21days("clasica", "21dias_loop", fmt, lang),
                              lang, net, f"21days_{fmt}"))
    paths.append(save_net(tpl_21days("dragon", "21dias_loop", "vert", lang),
                          lang, "tiktok", "21days_dragon_tiktok"))

    # ---- HOW TO: como funciona la app ----
    paths.append(save_net(tpl_howto("vert", lang), lang, "instagram", "howto_vert"))
    paths.append(save_net(tpl_howto("square", lang), lang, "instagram", "howto_square"))
    paths.append(save_net(tpl_howto("yt_thumb", lang), lang, "youtube", "howto_thumb"))

    # ---- REVEAL: split silueta / revelada ----
    paths.append(save_net(tpl_reveal("dragon", "dopamina_loop", "ig_portrait", lang),
                          lang, "instagram", "reveal_dragon_port"))
    paths.append(save_net(tpl_reveal("buho", "misterio", "square", lang),
                          lang, "instagram", "reveal_buho_sq"))
    paths.append(save_net(tpl_reveal("lobo", "dopamina_loop", "vert", lang),
                          lang, "tiktok", "reveal_lobo_tiktok"))

    return paths


def contact_sheet(lang, paths):
    """Contact sheet de todos los creativos (9 columnas)."""
    cols = 9
    th = 320
    pad = 12
    rows = (len(paths) + cols - 1) // cols
    cw = th
    sheet = Image.new("RGB", (cols * (cw + pad) + pad, rows * (th + pad) + pad), S.hx("#0A0A0E"))
    for i, p in enumerate(paths):
        try:
            im = Image.open(p)
            scale = th / im.height
            im = im.resize((int(im.width * scale), th), Image.LANCZOS)
            if im.width > cw:
                im = im.crop(((im.width - cw) // 2, 0, (im.width - cw) // 2 + cw, th))
            r, c = divmod(i, cols)
            x = pad + c * (cw + pad) + (cw - im.width) // 2
            y = pad + r * (th + pad)
            sheet.paste(im, (x, y))
        except Exception:
            pass
    out = os.path.join(SOC, f"index_{lang}.png")
    sheet.save(out, "PNG")
    return out


if __name__ == "__main__":
    print("Generando assets...")
    gen_assets()

    total = 0
    for lang in ("es", "en"):
        print(f"\n[{lang}] generando creativos...")
        paths = gen(lang)
        idx = contact_sheet(lang, paths)
        total += len(paths)
        print(f"[{lang}] {len(paths)} creativos -> {idx}")

    print(f"\nTOTAL: {total} creativos en marketing/social/")
