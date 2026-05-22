package app.reelblocker

/**
 * Frases rotativas que aparecen en la home. Mezcla de:
 * - Datos reales sobre consumo de vídeo corto
 * - Alternativas concretas y sencillas
 * - Reflexiones sin moralina
 *
 * Fuentes principales:
 * - Mark, G. (UC Irvine): tiempo de recuperación de atención tras
 *   distracción (~23 min).
 * - Data.ai / Statista 2024: tiempos medios de TikTok / Reels.
 * - Sleep Foundation: efecto de pantalla nocturna en sueño profundo.
 */
object Tips {

    private val stats = listOf(
        "El usuario medio pasa 95 min al día en Reels y Shorts. Casi una peli.",
        "Tu cerebro tarda 23 min en recuperar la concentración tras cada distracción.",
        "Reels reproducen 30 vídeos en 5 min. 30 micro-dosis de dopamina.",
        "El scroll infinito está diseñado para que tu cerebro no decida parar.",
        "1 hora de pantalla antes de dormir = 22 min menos de sueño profundo.",
        "El 70% de los usuarios pierde la noción del tiempo en Reels.",
        "TikTok engancha en 8 min de uso. Más rápido que el café.",
        "Una sesión media de Reels dura 24 min. Casi medio episodio de algo bueno.",
        "Cuanto más joven el usuario, más tiempo en vídeo corto. Cuesta verlo en abuelos.",
        "Cada like que recibes en Reels no es tuyo. Es del algoritmo decidiendo por ti.",
        "Adolescentes con +3h/día en vídeo corto tienen 2x más síntomas de ansiedad.",
        "El 60% de la gente revisa el móvil en los primeros 10 min al despertar.",
        "Pasamos de media 3h 15min al día con el móvil. Eso son 49 días enteros al año.",
        "La luz azul de la pantalla retrasa la liberación de melatonina 1,5 horas."
    )

    private val alternatives = listOf(
        "5 min de Reels = 5 min leyendo. ¿Cuándo terminaste un libro entero?",
        "Si tienes 20 min, sal a andar. El sol no se ve haciendo doomscrolling.",
        "Llama a un amigo. Una conversación supera a 100 reels juntos.",
        "Aburrirte es la señal de que el cerebro va a crear algo. No lo apagues.",
        "Cocina algo sencillo en vez de scrollear. Comes mejor y te sale lo que querías.",
        "10 min de estiramientos al despertar > 10 min de Reels en la cama.",
        "Apunta 3 cosas que quieras hacer hoy. Empieza por una, sin pensar.",
        "Sal al balcón 2 min. La pantalla no se va a ir a ningún lado.",
        "20 sentadillas ahora te dan más energía que 20 reels.",
        "Escribir un mensaje largo a alguien que te importa pega más que cualquier scroll.",
        "Un café con alguien en persona vale por una semana de stories.",
        "Si te aburres, mira por la ventana 30 segundos. En serio.",
        "Escucha una canción con los ojos cerrados. Suficiente como pausa.",
        "Pon una canción y baila 3 minutos. El cuerpo lo agradece.",
        "Ducha fría 30 segundos. Reseteas mejor que con 50 reels.",
        "Riega una planta. Trae al cerebro a algo lento y vivo."
    )

    private val reflections = listOf(
        "El primer scroll no es el problema. El número 50 sí.",
        "No tienes que ser productivo todo el rato. Aburrirte un poco también vale.",
        "Lo difícil no es dejar los Reels. Es no abrirlos sin pensar.",
        "El algoritmo se acuerda de ti cada segundo. Tú, ¿cuánto te acuerdas del algoritmo?",
        "Cada vez que esta app te saca, ganas tiempo que no veías gastar.",
        "Tu tiempo libre no es 'tiempo muerto'. Es de los pocos ratos que son tuyos.",
        "Escoge tus distracciones a propósito. Las que te sorprenden son las que más duelen.",
        "Si entras en Reels para 'descansar', acabas más cansado. Comprobado.",
        "La curiosidad real dura horas. La de los Reels, 8 segundos.",
        "Cierra los ojos 10 segundos. Eso ya es más descanso que media hora de scroll.",
        "Lo bueno tarda. Lo de Reels llega en 15 segundos y se va igual de rápido.",
        "La atención es el recurso más escaso del siglo. Y la regalas gratis.",
        "Pregunta: ¿de quién era el último reel que viste? Exacto.",
        "Antes de coger el móvil, respira tres veces. A veces se te quita."
    )

    private val all: List<String> = stats + alternatives + reflections

    fun random(): String = all.random()

    /** Devuelve N frases distintas (sin repetir). */
    fun randomDistinct(count: Int): List<String> = all.shuffled().take(count)
}
