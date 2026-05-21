# Basta — Sistema de marca

Guía rápida para mantener coherencia visual y de voz en toda la
comunicación de la app (capturas, redes, web, futuras versiones).

---

## Nombre y lockup

- **Nombre principal:** `Basta`
- **Descriptor:** `Reel Blocker`
- **Lockup completo:** `Basta — Reel Blocker` (em dash `—`, no guion)
- **Lanzador móvil:** solo `Basta` (entra en una línea, máximo 6 letras)
- **Wordmark estilizado:** `Basta.` con el punto en rojo de acento

El nombre funciona en español, italiano y francés. Una sola palabra,
fácil de recordar, fácil de pronunciar en voz alta. Sintetiza la
intención del usuario: "basta, no más Reels".

---

## Tipografía

Sistema (no hace falta cargar fuentes para una v1):
- **iOS / macOS:** SF Pro
- **Android:** Roboto / Google Sans
- **Web:** `-apple-system, BlinkMacSystemFont, "Segoe UI", system-ui`

Para el wordmark "Basta" en marketing usa peso **Black** (900) y
tracking ligeramente negativo (`-0.04em`). Para el descriptor
"REEL BLOCKER" usa peso regular en mayúsculas con `letter-spacing`
amplio (`0.3em`).

---

## Paleta

| Token | Hex | Uso |
|---|---|---|
| `--ink` | `#1A1A2E` | Fondo del icono, texto principal sobre claro |
| `--bg-soft` | `#FAFAFA` | Fondo de la web |
| `--accent` | `#DC2626` | "Basta" energy. Punto del wordmark, alertas críticas |
| `--ok` | `#2E7D32` | Estados positivos (servicio activo, batería exenta) |
| `--muted` | `#666666` | Texto secundario |

En la app Android se usa la paleta dinámica de Material 3 (`dynamicLightColorScheme`/`darkColorScheme`) para integrarse con el tema del usuario. Los acentos hardcoded (#DC2626 rojo de "basta") aparecen en marketing/landing y, opcionalmente, en estados destacados de la UI.

---

## Icono

Vector minimalista en `app/src/main/res/drawable/ic_launcher_foreground.xml`:
rectángulo blanco vertical (formato "móvil/reel") con flecha back recortada
en color del fondo (#1A1A2E). El cutout es la marca visual — no se cambia.

No incluir el nombre "Basta" dentro del icono. El nombre vive en el
sistema (lanzador) y en marketing.

---

## Voz y tono

- **Directa.** "Te saca", no "te ofrece la opción de salir".
- **Sin moralina.** No le decimos al usuario "te ayudamos a estar
  presente" — somos un freno, no un coach.
- **Ligeramente irreverente.** "Basta" es la palabra del usuario,
  no nuestra. Devolvemos la decisión.
- **Honesta sobre los límites.** Si Instagram cambia mañana,
  decimos que puede romperse. Sin promesas heroicas.

### Tagline corto
`Basta de Reels. Recupera tu atención.`

### Variantes para marketing
- `Basta de scrolls infinitos.`
- `Cuando entras en Reels, te saca. Eso es todo.`
- `No bloquea Instagram. Solo los Reels.`
- `Cierra los Reels. No tu vida social.`

### Lo que NO decimos
- "Mejora tu salud mental"
- "Recupera horas de tu vida"
- "Sé más productivo"
- Cualquier promesa que no podemos cumplir

---

## Aplicación en piezas

### Pantalla home (app)
Top bar centrado:
- `Basta` en titleLarge, FontWeight.Black
- `Reel Blocker` debajo en labelSmall, color `onSurfaceVariant`

### Toast al bloquear
`¡Basta!` (corto, contundente, sin verbo). No hace falta explicar más.

### Feature graphic Play Store (1024×500)
- Fondo `#1A1A2E`
- Izquierda: wordmark `Basta.` (punto rojo) + `REEL BLOCKER` debajo
- Derecha: mockup del móvil con la home
- Sin texto extra. Confianza minimalista.

### Capturas Play Store
- Encuadre limpio, status bar oculta si es posible.
- Si añades anotaciones, en blanco sobre el índigo de fondo, peso medio.

### Redes sociales
- Avatar: el icono cuadrado tal cual (rect blanco + flecha cutout sobre índigo).
- Banner: wordmark + tagline + fondo índigo.

---

## Lo que cambia con esta marca

| Antes | Ahora |
|---|---|
| Nombre app: `ReelBlocker` | `Basta` |
| Toast: `ReelBlocker: bloqueado` | `¡Basta!` |
| Service label: `ReelBlocker` | `Basta — Reel Blocker` |
| Top bar app: `ReelBlocker` | `Basta` + `Reel Blocker` |
| Onboarding paso 1: `Bienvenido a ReelBlocker` | `Bienvenido a Basta` |

Identificadores técnicos NO cambian (mantienen historial y compatibilidad):
- Package: `app.reelblocker`
- Repo GitHub: `ReelBlocker`
- Clases internas: `BlockerService`, `MainActivity`, etc.
- Resource IDs y SharedPreferences keys
