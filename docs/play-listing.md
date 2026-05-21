# Textos para la ficha de Google Play

Copia/pega estos textos en Play Console cuando crees la ficha. Si necesitas
afinarlos al carácter exacto, Play Console te avisa al pegar.

---

## Título de la app (max 30 caracteres)

```
ReelBlocker
```

Alternativa más descriptiva (29 chars):
```
ReelBlocker — Sin Reels y Shorts
```

---

## Descripción corta (max 80 caracteres)

```
Bloquea Reels de Instagram y Shorts de YouTube. Recupera tu atención.
```

(67 caracteres — entra.)

---

## Descripción larga (max 4000 caracteres)

```
ReelBlocker te saca automáticamente de los Reels de Instagram y los Shorts
de YouTube cuando entras en ellos. Si te has dado cuenta de que pierdes horas
en vídeos cortos sin darte cuenta, esta app es un freno simple y honesto.

CÓMO FUNCIONA
La app vigila en segundo plano cuándo abres el visor de Reels o de Shorts
y ejecuta el botón "atrás" del sistema para devolverte al feed normal.
No bloquea Instagram ni YouTube entero — solo la sección de vídeos cortos.

QUÉ INCLUYE
• Bloqueo de Reels de Instagram
• Bloqueo de Shorts de YouTube
• Switch para activar o desactivar el bloqueo en cada app
• Estadísticas locales: cuántos has bloqueado hoy y en los últimos 7 días
• Permitir Reels que te mandan amigos por DM (opcional)
• Bloquear Historias de Instagram (opcional, por defecto desactivado)
• Tema claro y oscuro automáticos

PRIVACIDAD
ReelBlocker funciona 100 % en tu dispositivo. No envía ni recoge ningún
dato fuera de tu móvil. No tiene cuentas, no tiene publicidad, no tiene
analítica. El código fuente está disponible en GitHub para que puedas
verificarlo.

POR QUÉ NECESITA PERMISO DE ACCESIBILIDAD
Es la única API en Android que permite detectar qué pantalla concreta de
otra app estás viendo. La app la usa exclusivamente para reconocer el
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
```

---

## Categorización

- Categoría primaria: **Productividad**
- Categoría secundaria (si la pide): **Estilo de vida** o **Herramientas**
- Rating: PEGI 3 / Todos los públicos. Marca "No contiene": violencia, lenguaje
  fuerte, etc. (es una app de utilidad pura).

---

## Etiquetas / tags (las que Play Console te deje)

reels, shorts, focus, productividad, anti-distracción, bloqueador, instagram,
youtube, screen time, atención

---

## Política de privacidad

URL pública (cuando GitHub Pages esté activo):

```
https://jose99segura.github.io/ReelBlocker/privacy.html
```

---

## Justificación del uso de Accessibility Service (formulario "Permitted Uses")

Play Console pide que justifiques por qué necesitas el permiso. Esta es la
respuesta a meter:

```
ReelBlocker uses the AccessibilityService API for a single, clearly-disclosed
user-facing feature: detecting when the user has navigated into the Instagram
Reels viewer or the YouTube Shorts viewer, so the app can trigger the system
back action and return the user to a less addictive surface. This is the
only Android API that exposes which specific in-app screen is visible, which
is necessary to distinguish "Reels viewer" from the normal Instagram feed
or YouTube watch screen. The app reads only resource-ids of UI containers
(e.g. clips_viewer_view_pager) and the active Activity/Fragment class name.
It never reads message content, post content, images, or any personal data.
No information is transmitted off-device; the app makes no network requests.
Source code is publicly available for audit.
```

---

## Capturas (necesitas 4-8)

Cuando reinstales la app y la abras tras el rediseño, sugeridas:

1. Pantalla home con el bloque de stats grande y el chart semanal.
2. Card de "Apps a bloquear" con los logos de Instagram y YouTube.
3. Sub-opciones de Instagram (DM, Historias) abiertas.
4. Toast "ReelBlocker: bloqueado" capturado al entrar en Reels (puedes
   forzar el toast modificando temporalmente el código para que aparezca
   en la home).
5. Onboarding paso 1 (Bienvenida).
6. Onboarding paso 2 (Activar accesibilidad).

Formato Play Store:
- Teléfono: 1080×1920 (portrait) o 1920×1080 (landscape)
- 16:9 o 9:16

Cómo capturar en el móvil: bloquea volumen + encender (Pixel/Stock) o
Power + Volumen abajo (Samsung). En el dispositivo `57021FDCR0014N` lo
sabrás según el fabricante.

---

## Feature graphic

1024×500 px, requerido. Sin texto crítico (Play Console añade el título
encima en algunas pantallas). Sugerencia visual: fondo índigo (#1A1A2E)
con el icono grande a la izquierda y a la derecha un mockup del móvil
mostrando la home de la app.

Si no quieres diseñarlo a mano: cualquier herramienta como Canva tiene
plantilla "Feature graphic Play Store" lista.

---

## Notas de la versión (release notes) para la primera publicación

```
Versión 1.0 — lanzamiento inicial.
• Bloqueo automático de Reels de Instagram y Shorts de YouTube.
• Switches por app, estadísticas locales, permitir Reels desde DM,
  opción para bloquear Historias.
• 100 % offline, sin analítica, código abierto.
```

(Si tu Play Console te limita longitud, recórtalo.)
