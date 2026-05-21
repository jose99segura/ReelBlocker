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

QUÉ INCLUYE
• Bloqueo de Reels de Instagram
• Bloqueo de Shorts de YouTube
• Switch para activar o desactivar el bloqueo en cada app
• Estadísticas locales: cuántos has bloqueado hoy y los últimos 7 días
• Detecta si Instagram o YouTube están instalados (oculta lo que no aplica)
• Permitir Reels que te mandan amigos por DM (opcional, activado por defecto)
• Bloquear Historias de Instagram (opcional, desactivado por defecto)
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
Si encuentras un fallo o quieres sugerir algo: [CORREO_CONTACTO]
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
instagram, youtube, screen time, atención, dopamina

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

1. Home con stats grandes y chart semanal.
2. Card de "Apps a bloquear" con los iconos reales de Instagram/YouTube.
3. Sub-opciones de Instagram (DM, Historias) visibles.
4. Toast "¡Basta!" capturado al entrar en Reels (puedes forzarlo
   trasteando temporalmente para que se muestre en la home).
5. Onboarding paso 1 ("Bienvenido a Basta!").
6. Onboarding paso 2 (Activar accesibilidad).

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
