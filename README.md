# ReelBlocker

App Android que detecta cuando entras en **Reels de Instagram** o **Shorts de
YouTube** y te saca automaticamente con el boton atras. No bloquea Instagram ni
YouTube enteros: solo la seccion de videos cortos.

## Como funciona

Usa un **AccessibilityService**. Este servicio "ve" la pantalla de Instagram y
YouTube, busca pistas en los identificadores de los elementos para saber si
estas en Reels/Shorts y, si te detecta ahi, ejecuta el boton atras.

> Una app normal de Android **no puede** borrar ni esconder de verdad el boton de
> Reels de Instagram, porque cada app esta aislada del resto (sandbox). Lo que si
> puede es impedir que la seccion llegue a verse, que es lo que hace esta app. El
> resultado para ti es el mismo: no llegas a ver Reels.

## Instalacion

1. Abre el proyecto en **Android Studio** (Archivo > Abrir > carpeta ReelBlocker).
2. Deja que Gradle sincronice (descargara las dependencias).
3. Conecta tu movil con depuracion USB activada y pulsa **Run**, o genera el APK
   con `Build > Build APK(s)` e instalalo a mano.
4. Abre la app y pulsa **"Abrir ajustes de accesibilidad"**.
5. Activa **ReelBlocker** en la lista de servicios de accesibilidad.

A partir de ahi, cada vez que entres en Reels o Shorts, te sacara solo.

## Mantenimiento (importante)

La deteccion depende de unos identificadores internos de Instagram y YouTube que
**cambian con las actualizaciones** de esas apps. Si algun dia deja de
bloquear, casi seguro hay que actualizar las listas de pistas en el archivo:

`app/src/main/java/com/example/reelblocker/BlockerService.kt`

Busca `INSTAGRAM_REEL_HINTS` y `YOUTUBE_SHORTS_HINTS`. Para descubrir los nuevos
identificadores puedes usar la herramienta **Layout Inspector** de Android
Studio o registrar en el Logcat los `viewIdResourceName` que aparecen cuando
estas en la seccion de Reels.

## Anadir mas apps (TikTok, Facebook, etc.)

1. Anade el nombre del paquete en `accessibility_config.xml` (atributo
   `android:packageNames`).
2. Crea una nueva lista de pistas en `BlockerService.kt` y enchufala en el
   `when (pkg)`.

## Limitaciones honestas

- Puede haber un parpadeo de medio segundo al entrar y salir.
- Si Instagram cambia mucho su interfaz, hay que actualizar las pistas.
- No funciona dentro de navegadores (solo las apps nativas configuradas).
- Esconder literalmente el boton solo es posible con root + Xposed/LSPosed, que
  queda fuera del alcance de esta app.
