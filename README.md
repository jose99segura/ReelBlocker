# Basta! — Reel Blocker

> Basta! de Reels infinitos. Recupera tu atención.

App Android que detecta cuando entras en **Reels de Instagram** o **Shorts de
YouTube** y te saca automáticamente con el botón atrás. No bloquea Instagram
ni YouTube enteros: solo la sección de vídeos cortos.

El nombre interno del proyecto y el repositorio siguen siendo `ReelBlocker`
(no se cambia para no romper historial). La marca pública y user-facing es
`Basta! — Reel Blocker` desde la versión 1.0.

**Idiomas soportados**: español (por defecto) e inglés. La app sigue el
idioma del sistema automáticamente.

## Cómo funciona

Usa un **AccessibilityService**. Este servicio "ve" la pantalla de Instagram y
YouTube, busca pistas en los identificadores de los elementos para saber si
estás en Reels/Shorts y, si te detecta ahí, ejecuta el botón atrás.

> Una app normal de Android **no puede** borrar ni esconder de verdad el botón de
> Reels de Instagram, porque cada app está aislada del resto (sandbox). Lo que sí
> puede es impedir que la sección llegue a verse, que es lo que hace esta app. El
> resultado para ti es el mismo: no llegas a ver Reels.

## Mascotas coleccionables

Además del bloqueo, Basta! tiene una capa de gamificación:

- **Racha diaria**: cada día con el servicio activo suma un día. La rompes si
  desactivas el servicio o saltas un día completo. El récord histórico se queda.
- **Tu mascota evoluciona**:
  - Día 0 → Huevo
  - Día 3 → Se agrieta
  - Día 7 → Cría
  - Día 14 → Juvenil
  - Día 30 → Adulto
- **Graduación a los 30 días**: tu mascota se va a tu inventario y emerge un
  huevo nuevo de otra especie. Hay **5 especies** que coleccionar (Clásica,
  Dragón, Tortuga, Lobo, Búho). La siguiente sale aleatoria entre las que aún
  no tengas; cuando están las 5, se permite repetir.
- **Inventario** accesible desde la barra inferior — verás las graduadas, las
  que aún te faltan, y la activa con su progreso.

Si rompes la racha antes de graduar, mantienes la misma especie — solo
la graduación cambia de mascota.

## Instalación

1. Abre el proyecto en **Android Studio** (Archivo > Abrir > carpeta ReelBlocker).
2. Deja que Gradle sincronice (descargará las dependencias).
3. Conecta tu móvil con depuración USB activada y pulsa **Run**, o genera el APK
   con `Build > Build APK(s)` e instálalo a mano.
4. Abre la app, sigue el onboarding y pulsa **"Abrir ajustes de accesibilidad"**.
5. Activa **Basta!** en la lista de servicios de accesibilidad.

A partir de ahí, cada vez que entres en Reels o Shorts, te sacará solo.

## Navegación dentro de la app

Barra inferior con cuatro pestañas:

- **Inicio** — Mascota, racha, métricas resumen, cita rotativa.
- **Estadísticas** — Hoy, distribución IG/YT, gráfico de últimos 7 días,
  tiempo recuperado, récord histórico.
- **Inventario** — Tu Pokédex de mascotas graduadas (5 especies).
- **Ajustes** — Apps a bloquear, protección (servicio + batería), Pro,
  política de privacidad, ver tutorial otra vez, desactivar protección.

Encima del hero, un icono `?` abre el diálogo "Cómo funciona" con la
explicación completa del sistema.

## Mantenimiento (importante)

La detección depende de unos identificadores internos de Instagram y YouTube
que **cambian con las actualizaciones** de esas apps. Si algún día deja de
bloquear, casi seguro hay que actualizar las listas de pistas en el archivo:

`app/src/main/java/app/reelblocker/BlockerService.kt`

Busca `INSTAGRAM_REEL_HINTS` y `YOUTUBE_SHORTS_HINTS`. Para descubrir los
nuevos identificadores puedes usar `adb shell uiautomator dump` con la
pantalla del Reel abierta, y mirar los `resource-id` únicos del dump.

## Añadir más apps (TikTok, Facebook, etc.)

1. Añade el nombre del paquete a `<queries>` en `AndroidManifest.xml` y a
   `Stats.BLOCKABLE_APPS`.
2. Crea una nueva lista de pistas en `BlockerService.kt` y enchúfala en el
   `when (pkg)` del `onAccessibilityEvent`.

## Limitaciones honestas

- Puede haber un parpadeo de medio segundo al entrar y salir.
- Si Instagram cambia mucho su interfaz, hay que actualizar las pistas.
- No funciona dentro de navegadores (solo las apps nativas configuradas).
- Esconder literalmente el botón solo es posible con root + Xposed/LSPosed,
  que queda fuera del alcance de esta app.

---

## English summary

**Basta! — Reel Blocker** is an Android app that closes Instagram Reels and
YouTube Shorts the moment you open them. It's not a content filter — it just
fires the system Back action when its accessibility service detects you're on
the short-video screen. Everything else in those apps works normally.

The app also has a gamification layer: a daily streak with an evolving mascot
that **graduates at day 30** and is archived in your Pokédex-style inventory,
making room for a new egg of a different species. There are 5 species to
collect. The UI is available in **Spanish and English**, following the
system locale.

For build & detection-maintenance details, see the Spanish sections above
or `CLAUDE.md`.
