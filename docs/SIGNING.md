# Build firmado para release

Pasos que tú tienes que hacer una sola vez. Yo no puedo generar la keystore
porque contiene tu clave privada de firma — debes guardarla a buen recaudo y
**nunca commitearla a git**.

## 1) Generar la keystore

En PowerShell, en cualquier carpeta segura (NO dentro del repo):

```
& "C:\Program Files\Android\Android Studio1\jbr\bin\keytool.exe" `
  -genkey -v `
  -keystore reelblocker-release.jks `
  -alias reelblocker `
  -keyalg RSA -keysize 2048 -validity 10000
```

Te pedirá:

- Contraseña del keystore (mínimo 6 caracteres). Apúntala.
- Nombre, apellidos, organización, ciudad, etc. — pon datos coherentes con
  tu identidad de desarrollador. Es lo que firmará tus APKs.
- Contraseña del alias (puede ser la misma que la del keystore).

El archivo `reelblocker-release.jks` se crea en la carpeta donde estás.
**Guárdalo en al menos dos sitios** (p. ej. Dropbox cifrado + USB físico).
Si pierdes esta clave, ya no podrás publicar actualizaciones de la app
con el mismo identificador en Play Store. Es irremplazable.

## 2) Crear `keystore.properties` en la raíz del proyecto

Crea un archivo `keystore.properties` (no lo commitees — ya está en
`.gitignore`) en `C:\Users\jose9\OneDrive\Escritorio\ReelBlocker\` con:

```
storeFile=keystore/basta-release.jks
storePassword=la_que_pusiste
keyAlias=basta
keyPassword=la_misma_o_la_otra
```

El `storeFile` puede ser:
- **Path relativo** (recomendado): se resuelve respecto a la raíz del
  repo. Crea una carpeta `keystore/` ahí mismo y mete el .jks dentro
  (también está en `.gitignore`).
- **Path absoluto**: p. ej. `C:\\dev\\keystores\\basta-release.jks` —
  útil si guardas las keystores fuera del repo (más seguro).
  (Doble backslash en Windows.)

## 3) Compilar el AAB de release

```
.\gradlew.bat bundleRelease
```

El resultado:

```
app\build\outputs\bundle\release\app-release.aab
```

Ese `.aab` (Android App Bundle) es lo que se sube a Play Console. Play
genera los APKs específicos por dispositivo a partir de él.

## 4) Verificar la firma

```
& "C:\Program Files\Android\Android Studio1\jbr\bin\keytool.exe" `
  -list -v -keystore reelblocker-release.jks
```

Deberías ver el SHA-1 y SHA-256 de tu clave. Apúntalos — Play Console
los muestra también después de subir el AAB para confirmar.

## 5) Subir a Play Console

1. Crea la app en Play Console.
2. Track interno (Internal testing) primero — añade tu email como tester.
3. Sube `app-release.aab` en "App bundles".
4. Rellena la ficha con los textos de `play-listing.md`.
5. Sección "Privacy & Safety" → pega la URL de la privacy policy:
   `https://jose99segura.github.io/ReelBlocker/privacy.html`
6. Sección "Permitted Uses" (Accessibility) → pega la justificación del
   apartado correspondiente de `play-listing.md`.
7. Manda a revisión. Espera 1-7 días.

## Problemas comunes

- **"Tu APK no está firmado":** revisa que `keystore.properties` existe
  y `bundleRelease` lo encuentra. El bloque `signingConfigs.release` en
  `app/build.gradle` carga el archivo si existe.
- **"R8 minify hizo cosas raras y la app crashea en release":** prueba
  `assembleRelease` primero (APK directo) y revisa el log. Si necesitas
  desactivar minify temporalmente, en `app/build.gradle` cambia
  `minifyEnabled true` a `false`. Después intenta diagnosticar con
  proguard mapping.
- **"Play Console me rechaza por uso de accessibility":** revisa que la
  privacy policy menciona explícitamente accessibility, y que has
  justificado el uso en el formulario. El texto en `play-listing.md`
  suele pasar.
