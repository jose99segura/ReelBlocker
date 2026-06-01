# -*- coding: utf-8 -*-
"""
out2: set MASIVO de creativos reutilizando el motor studio.py y las raw existentes.
Muchísimos copys (ES/EN). Solo huevos + pantallas sin mascota (Diario/Ajustes).
"""
import os
import studio as S

S.OUT = os.path.join(S.BASE, "out2")

# Especie -> captura existente en raw (OneDrive se llevó varias; mezclamos d1/d2).
E = {
    "verde":  "egg_d2_verde",
    "lila":   "egg_d2_lila",
    "brasa":  "egg_d1_brasa",
    "chispa": "egg_d1_chispa",
    "normal": "egg_d2_normal",
}
# Pantallas sin mascota crecida (los sprites de mascota van a cambiar).
EGG_SCREENS = [E["verde"], E["lila"], E["brasa"], E["chispa"], E["normal"]]
ACC_CYCLE = ["green", "blue", "orange", "gold", "lavender"]

# ---------------------------------------------------------------------------
# Mazo gigante de copys.  id, acento, es=(líneas_titular, subtítulo), en=(...)
# ---------------------------------------------------------------------------
DECK = [
    ("caer",       "lavender", (["Sal de Reels", "antes de caer"], "Tu racha crece cada día que no scrolleas"),
                               (["Get out before", "the scroll wins"], "Your streak grows every day you don't scroll")),
    ("roba",       "orange",   (["El scroll te", "roba la vida"], "Recupera tus horas"),
                               (["Scrolling steals", "your life"], "Take your hours back")),
    ("infinito",   "blue",     (["Adiós al", "scroll infinito"], "Reels, Shorts y TikTok se cierran solos"),
                               (["Kill the", "infinite scroll"], "Reels, Shorts & TikTok close themselves")),
    ("tiempo",     "green",    (["Recupera tu tiempo,", "día a día"], "Cada caída evitada cuenta"),
                               (["Reclaim your time,", "day by day"], "Every avoided fall counts")),
    ("cierran",    "green",    (["Reels y Shorts", "se cierran solos"], "Sin bloquear la app entera"),
                               (["Reels & Shorts", "close themselves"], "Without blocking the whole app")),
    ("racha",      "lavender", (["Construye una racha.", "No la rompas."], "Un hábito que ves crecer"),
                               (["Build a streak.", "Don't break it."], "A habit you watch grow")),
    ("colecciona", "gold",     (["Colecciona", "5 especies"], "Tu autocontrol, recompensado"),
                               (["Collect", "5 species"], "Self-control, rewarded")),
    ("horas",      "orange",   (["¿Cuántas horas", "al día?"], "Descúbrelo. Luego recupéralas."),
                               (["How many hours", "a day?"], "Find out. Then take them back.")),
    ("cerebro",    "blue",     (["Tu cerebro", "te lo agradecerá"], "Menos dopamina barata"),
                               (["Your brain", "will thank you"], "Less cheap dopamine")),
    ("enganche",   "orange",   (["Diseñado para", "engancharte. Tú no."], "Recupera el control"),
                               (["Built to hook you.", "Not anymore."], "Take back control")),
    ("untoque",    "lavender", (["Un toque y", "fuera del scroll"], "Basta! pulsa atrás por ti"),
                               (["One tap and", "out of the scroll"], "Basta! taps back for you")),
    ("nomas",      "green",    (["No más", "'solo un vídeo'"], "Sales antes de empezar"),
                               (["No more", "'just one video'"], "You leave before you start")),
    ("foco",       "blue",     (["Recupera tu", "concentración"], "Tu atención es tuya"),
                               (["Get your", "focus back"], "Your attention is yours")),
    ("vida",       "green",    (["La vida pasa", "fuera del feed"], "Vuelve a ella"),
                               (["Life happens", "off the feed"], "Get back to it")),
    ("habito21",   "gold",     (["21 días para", "un nuevo hábito"], "Y una mascota nueva"),
                               (["21 days to", "a new habit"], "And a new pet")),
    ("paso",       "lavender", (["Cada día,", "un paso más"], "Tu racha lo demuestra"),
                               (["Every day,", "one step closer"], "Your streak proves it")),
    ("autocontrol","blue",     (["Autocontrol,", "no fuerza bruta"], "Una ayuda que actúa por ti"),
                               (["Self-control,", "not willpower"], "Help that acts for you")),
    ("ladron",     "orange",   (["El ladrón de tiempo", "tiene los días contados"], "Reels, Shorts, TikTok"),
                               (["The time thief's", "days are numbered"], "Reels, Shorts, TikTok")),
    ("despierta",  "lavender", (["Despierta sin", "abrir Instagram"], "Empieza el día en tus términos"),
                               (["Wake up without", "opening Instagram"], "Start the day on your terms")),
    ("minutos",    "green",    (["Recupera minutos.", "Gana horas."], "Cada bloqueo suma"),
                               (["Save minutes.", "Win hours."], "Every block adds up")),
    ("tiktok",     "blue",     (["TikTok engancha", "en 8 minutos"], "Tú sales en 1 segundo"),
                               (["TikTok hooks you", "in 8 minutes"], "You're out in 1 second")),
    ("dueno",      "gold",     (["Sé dueño de", "tu atención"], "No la regales al algoritmo"),
                               (["Own your", "attention"], "Don't gift it to the algorithm")),
    ("calma",      "green",    (["Menos scroll.", "Más calma."], "Tu mente lo nota"),
                               (["Less scroll.", "More calm."], "Your mind feels it")),
    ("presente",   "lavender", (["Vuelve al", "presente"], "Lo de verdad pasa aquí"),
                               (["Come back to", "the present"], "The real stuff is here")),
    ("gratis",     "green",    (["Bloquea Reels", "gratis"], "Lo esencial, sin pagar"),
                               (["Block Reels", "for free"], "The essentials, no charge")),
    ("local",      "blue",     (["Todo local.", "Cero tracking."], "Tus datos no salen del móvil"),
                               (["All local.", "Zero tracking."], "Your data never leaves the phone")),
    ("nace",       "gold",     (["Llega a 21", "y nace algo nuevo"], "Una especie distinta cada vez"),
                               (["Reach 21,", "something hatches"], "A different species each time")),
    ("manana",     "orange",   (["Empieza hoy.", "Brilla mañana."], "El cambio empieza con un día"),
                               (["Start today.", "Shine tomorrow."], "Change starts with one day")),
    ("doom",       "lavender", (["Adiós al", "doomscrolling"], "De verdad, esta vez"),
                               (["Goodbye", "doomscrolling"], "For real, this time")),
    ("reto30",     "green",    (["30 días sin", "caer en Reels"], "¿Tu nuevo récord?"),
                               (["30 days, zero", "Reels relapses"], "Your new record?")),
    ("pulgar",     "blue",     (["Tu pulgar", "merece descansar"], "Deja de deslizar sin fin"),
                               (["Your thumb", "deserves a rest"], "Stop the endless swiping")),
    ("algoritmo",  "orange",   (["El algoritmo", "no es tu amigo"], "Basta! sí"),
                               (["The algorithm", "isn't your friend"], "Basta! is")),
    ("atrapado",   "gold",     (["¿Atrapado en", "el scroll?"], "Te sacamos en 1 segundo"),
                               (["Stuck in", "the scroll?"], "We pull you out in 1 second")),
    ("libre",      "green",    (["Libre del", "scroll infinito"], "Empieza hoy"),
                               (["Free from", "the infinite scroll"], "Start today")),
    ("mejor",      "lavender", (["Tu mejor versión,", "sin Reels"], "Más foco, más tiempo, más calma"),
                               (["Your best self,", "minus the Reels"], "More focus, time and calm")),
    ("adhd",       "blue",     (["Hecho para mentes", "que se distraen"], "Apoyo real contra el scroll"),
                               (["Made for minds", "that wander"], "Real support against the scroll")),
]

def mk(item, lang):
    _id, acc, es, en = item
    return {"accent": acc, "es": es, "en": en}

def gen(lang):
    out = []
    # HERO vertical y cuadrado para CADA copy (rota pantalla de huevo + estilo)
    for i, item in enumerate(DECK):
        m = mk(item, lang)
        sc = EGG_SCREENS[i % len(EGG_SCREENS)]
        style = "dark" if i % 4 == 0 else "color"
        out.append(S.save(S.tpl_hero(sc, m, "vert", lang, style), lang, "vert", f"hero_{i:02d}_{item[0]}"))
    for i, item in enumerate(DECK):
        m = mk(item, lang)
        sc = EGG_SCREENS[(i + 3) % len(EGG_SCREENS)]
        style = "dark" if i % 5 == 0 else "color"
        out.append(S.save(S.tpl_hero(sc, m, "square", lang, style), lang, "square", f"hero_{i:02d}_{item[0]}"))

    # Extras: póster, dúos, tríos, stats, banners (variando copy)
    poster_msgs = ["colecciona", "nace", "habito21"]
    for j, mid in enumerate(poster_msgs):
        item = next(x for x in DECK if x[0] == mid)
        for fmt in ("vert", "square"):
            out.append(S.save(S.tpl_eggposter(mk(item, lang), fmt, lang, "dark"), lang, fmt, f"poster_{mid}"))

    duo_pairs = [(E["lila"], E["brasa"], "colecciona"),
                 (E["verde"], E["chispa"], "nace"),
                 ("04_ajustes", "02_diario", "cierran")]
    for a, b, mid in duo_pairs:
        item = next(x for x in DECK if x[0] == mid)
        for fmt in ("vert", "square"):
            out.append(S.save(S.tpl_duo(a, b, mk(item, lang), fmt, lang, "color"), lang, fmt, f"duo_{mid}"))

    for mid in ("colecciona", "tiempo"):
        item = next(x for x in DECK if x[0] == mid)
        for fmt in ("vert", "square"):
            out.append(S.save(S.tpl_trio(E["verde"], E["lila"], E["chispa"],
                                         mk(item, lang), fmt, lang, "color"), lang, fmt, f"trio_{mid}"))

    for key, sc in (("dias", E["verde"]), ("min", "02_diario")):
        for fmt in ("vert", "square"):
            out.append(S.save(S.tpl_stat(key, sc, fmt, lang, "color"), lang, fmt, f"stat_{key}"))

    banners = [(E["lila"], "infinito", "feature", "dark"),
               ("04_ajustes", "cierran", "feature", "dark"),
               (E["brasa"], "roba", "display", "color"),
               ("02_diario", "tiempo", "display", "color"),
               (E["chispa"], "colecciona", "display", "dark"),
               (E["verde"], "libre", "feature", "color")]
    for sc, mid, fmt, st in banners:
        item = next(x for x in DECK if x[0] == mid)
        out.append(S.save(S.tpl_banner(sc, mk(item, lang), fmt, lang, st), lang, fmt, f"banner_{mid}"))
    return out

def contact_sheet(lang, paths):
    from PIL import Image
    cols, th, pad = 9, 260, 12
    rows = (len(paths) + cols - 1) // cols
    cw = th
    W = cols * (cw + pad) + pad
    H = rows * (th + pad) + pad
    sheet = Image.new("RGB", (W, H), (10, 10, 14))
    for i, p in enumerate(paths):
        im = Image.open(p)
        sc = th / im.height
        im = im.resize((int(im.width * sc), th), Image.LANCZOS)
        if im.width > cw:
            im = im.crop(((im.width - cw) // 2, 0, (im.width - cw) // 2 + cw, th))
        r, c = divmod(i, cols)
        sheet.paste(im, (pad + c * (cw + pad) + (cw - im.width) // 2, pad + r * (th + pad)))
    out = os.path.join(S.OUT, f"index_{lang}.png")
    os.makedirs(S.OUT, exist_ok=True)
    sheet.save(out, "PNG")
    return out

if __name__ == "__main__":
    total = 0
    for lang in ("es", "en"):
        paths = gen(lang)
        idx = contact_sheet(lang, paths)
        total += len(paths)
        print(f"[{lang}] {len(paths)} creativos -> {idx}")
    print("TOTAL:", total)
