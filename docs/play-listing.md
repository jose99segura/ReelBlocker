# Textos para la ficha de Google Play

Marca: **Basta!** (con descriptor "Reel Blocker"). Copia/pega cuando crees la
ficha en Play Console.

---

## Título de la app (max 30 caracteres)

```
Basta! — Reel Blocker
```

(20 caracteres con em dash, entra de sobra.)

---

## Descripción corta (max 80 caracteres)

```
Basta! de Reels y Shorts. Recupera tu atención.
```

(46 caracteres.)

---

## Descripción larga (max 4000 caracteres)

```
Basta! es la app que dice "Basta!" por ti. Cada vez que entras en Reels de
Instagram o en Shorts de YouTube, la app te saca con el botón atrás del
sistema. Si has descubierto que pierdes horas en vídeos cortos sin darte
cuenta, esto es un freno simple y honesto.

CÓMO FUNCIONA
La app vigila en segundo plano cuándo se abre el visor de Reels o de
Shorts y ejecuta el botón "atrás" para devolverte al feed normal. No
bloquea Instagram ni YouTube enteros — solo la sección de vídeos cortos.

UNA MASCOTA QUE CRECE CONTIGO
Cada día con el servicio activo, tu mascota evoluciona: huevo → cría →
juvenil → adulta. A los 30 días se gradúa y se guarda en tu inventario,
y aparece un huevo nuevo de otra especie. Hay 5 especies para coleccionar
(Clásica, Dragón, Tortuga, Lobo y Búho). Si rompes la racha antes de
graduar, mantienes la misma especie — solo la graduación cambia de
mascota. Un loop de recompensa alcanzable cada ~30 días, sin sermones.

QUÉ INCLUYE
• Bloqueo de Reels de Instagram
• Bloqueo de Shorts de YouTube
• Sistema de racha diaria con mascota que evoluciona en 5 niveles
• Inventario de mascotas graduadas (Pokédex de 5 especies coleccionables)
• Estadísticas locales: hoy, distribución IG/YT, gráfico de 7 días,
  tiempo recuperado, récord histórico
• Switch para activar o desactivar el bloqueo en cada app
• Detecta si Instagram o YouTube están instalados (oculta lo que no aplica)
• Permitir Reels que te mandan amigos por DM (opcional, activado por defecto)
• Bloquear Historias de Instagram (opcional, desactivado por defecto)
• Disponible en español y en inglés (sigue el idioma del sistema)
• Tema claro y oscuro automáticos

PRIVACIDAD
Basta! funciona 100 % en tu dispositivo. No envía ni recoge ningún dato
fuera de tu móvil. No tiene cuentas, no tiene publicidad, no tiene
analítica. El código fuente está disponible en GitHub para que puedas
verificarlo.

POR QUÉ NECESITA PERMISO DE ACCESIBILIDAD
Es la única API en Android que permite detectar qué pantalla concreta de
otra app estás viendo. Basta! la usa exclusivamente para reconocer el
visor de Reels o Shorts (por los identificadores internos de la pantalla)
y ejecutar la acción "atrás" del sistema. No lee el contenido de
mensajes, posts, imágenes ni ninguna información personal.

QUÉ NO HACE
• No envía datos a ningún servidor
• No tiene publicidad
• No tiene compras dentro de la app
• No comparte nada con terceros

CONSEJOS
Tras instalar, activa el servicio de accesibilidad y exime la app de la
optimización de batería. Si tienes un Xiaomi, Samsung o Huawei, activa
además el "Autoarranque" de la app en los ajustes del sistema para que
no la maten en segundo plano.

CONTACTO Y CÓDIGO ABIERTO
Si encuentras un fallo o quieres sugerir algo: senaprojectai@gmail.com
Código fuente: github.com/jose99segura/ReelBlocker

Basta! Recupera tu atención.
```

---

## Categorización

- Categoría primaria: **Productividad**
- Categoría secundaria: **Estilo de vida** o **Herramientas**
- Rating: PEGI 3 / Todos los públicos. No contiene violencia, lenguaje
  fuerte, etc.

---

## Etiquetas / keywords sugeridas

Basta!, reels, shorts, focus, productividad, anti-distracción, bloqueador,
instagram, youtube, screen time, atención, dopamina, pokédex, mascota,
streak, racha, coleccionable, gamificación

---

## Política de privacidad

URL pública (cuando GitHub Pages esté activo):

```
https://jose99segura.github.io/ReelBlocker/privacy.html
```

---

## Justificación del uso de Accessibility Service (formulario "Permitted Uses")

```
Basta! uses the AccessibilityService API for a single, clearly-disclosed
user-facing feature: detecting when the user has navigated into the
Instagram Reels viewer or the YouTube Shorts viewer, so the app can
trigger the system back action and return the user to a less addictive
surface. This is the only Android API that exposes which specific
in-app screen is visible, which is necessary to distinguish "Reels
viewer" from the normal Instagram feed or YouTube watch screen. The
app reads only resource-ids of UI containers (e.g.
clips_viewer_view_pager) and the active Activity/Fragment class name.
It never reads message content, post content, images, or any personal
data. No information is transmitted off-device; the app makes no
network requests. Source code is publicly available for audit.
```

---

## Capturas (4-8 necesarias)

1. Home con la mascota grande en su anillo + contador "X días" + cuenta
   atrás hasta la siguiente graduación.
2. Inventario (Pokédex) con 2-3 mascotas graduadas y el resto bloqueados
   con "?" — comunica de un vistazo el sistema de colección.
3. Estadísticas con el hero "Hoy" enorme + distribución IG/YT + gráfico 7 días.
4. Diálogo "Cómo funciona" mostrando la timeline 0/3/7/14/30 días.
5. Ajustes → sección "Apps a bloquear" con los iconos reales de
   Instagram/YouTube y las sub-opciones de IG expandidas.
6. Onboarding paso 2 ("Conoce a tu mascota") con la fila de evoluciones.
7. Opcional: graduación animada con confetti capturada.

Formato: 1080×1920 (portrait) o 1920×1080 (landscape).

---

## Feature graphic (1024 × 500 px, obligatorio)

Sugerencia visual:
- Fondo: índigo oscuro #1A1A2E
- Wordmark "Basta!" con el "!" en rojo #DC2626 (acento "stop")
- Subtítulo "REEL BLOCKER" pequeño, letterspacing amplio, en gris claro
- A la derecha, mockup del móvil con la home de la app
- Sin más adornos. Confianza minimalista.

Herramientas: Canva o Figma tienen plantillas "Feature graphic Play Store".

---

## Notas de la versión inicial

```
Versión 1.0 — Basta! nace.
• Bloqueo automático de Reels de Instagram y Shorts de YouTube.
• Switches por app, estadísticas locales, permitir Reels desde DM,
  opción para bloquear Historias.
• Detección automática de apps instaladas.
• 100 % offline, sin analítica, código abierto.
```

## Notas para versión con colección + i18n

```
Novedades:
• Sistema de mascotas coleccionables — tu mascota se gradúa a los 30
  días de racha activa y aparece otra de las 5 especies.
• Inventario (Pokédex) en la nueva pestaña inferior.
• Estadísticas rediseñadas: tiempo recuperado, récord histórico, gráfico
  de 7 días, distribución por app.
• Navegación inferior con 4 pestañas (Inicio · Estadísticas · Inventario
  · Ajustes).
• Diálogo "Cómo funciona" accesible desde el icono ? en la barra superior.
• App disponible en español e inglés (sigue el idioma del sistema).
```

---

## Versión inglesa (Play Store EN listing)

### Title (max 30 chars)

```
Basta! — Reel Blocker
```

### Short description (max 80 chars)

```
Enough Reels and Shorts. Get your attention back.
```

### Long description (max 4000 chars)

```
Basta! is the app that says "Enough!" for you. Every time you open
Instagram Reels or YouTube Shorts, the app pulls you out by triggering
the system Back action. If you've noticed you lose hours to short videos
without realizing it, this is a simple, honest brake.

HOW IT WORKS
The app watches in the background for when the Reels or Shorts viewer
opens and presses Back to return you to the normal feed. It doesn't
block Instagram or YouTube as a whole — only the short-video section.

A MASCOT THAT GROWS WITH YOU
Every day with the service active, your mascot evolves: egg → hatchling
→ juvenile → adult. At day 30 it graduates and gets saved in your
inventory, and a new egg of a different species appears. There are 5
species to collect (Classic, Dragon, Turtle, Wolf and Owl). If you
break the streak before graduating, you keep the same species — only
graduation changes the mascot. An achievable reward loop every ~30 days,
no lectures.

WHAT'S INCLUDED
• Instagram Reels blocking
• YouTube Shorts blocking
• Daily streak system with a 5-level evolving mascot
• Graduated-mascot inventory (Pokédex of 5 collectible species)
• Local stats: today, IG/YT split, 7-day chart, time recovered, record
• Per-app toggle for blocking
• Detects which apps are installed (hides what doesn't apply)
• Allow Reels sent by friends in DMs (optional, on by default)
• Block Instagram Stories (optional, off by default)
• Available in English and Spanish (follows system locale)
• Automatic light/dark theme

PRIVACY
Basta! runs 100% on your device. It doesn't send or collect any data
off your phone. No accounts, no ads, no analytics. The source code is
public on GitHub so you can verify it yourself.

WHY THE ACCESSIBILITY PERMISSION
It's the only Android API that lets the app detect which specific
in-app screen you're viewing. Basta! uses it exclusively to recognize
the Reels or Shorts viewer (by internal screen identifiers) and trigger
the system Back action. It never reads message content, posts, images
or any personal data.

WHAT IT DOESN'T DO
• Doesn't send data to any server
• No ads
• No in-app purchases (Pro is a one-time payment, no subscription)
• Doesn't share anything with third parties

TIPS
After installing, enable the accessibility service and exclude the app
from battery optimization. On Xiaomi, Samsung or Huawei devices, also
enable "Auto-launch" in system settings so the OS doesn't kill it in
the background.

CONTACT & OPEN SOURCE
Bug or feature: senaprojectai@gmail.com
Source code: github.com/jose99segura/ReelBlocker

Basta! Get your attention back.
```
