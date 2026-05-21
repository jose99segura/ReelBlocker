package app.reelblocker

/**
 * Frases rotativas que aparecen en la home. Mezcla de:
 * - Datos reales sobre consumo de video corto
 * - Alternativas concretas y sencillas
 * - Reflexiones sin moralina ("no eres mala persona por hacer scroll,
 *   pero hay cosas mejores")
 *
 * Fuentes principales:
 * - Mark, G. (UC Irvine): tiempo de recuperacion de atencion tras
 *   distraccion (~23 min).
 * - Data.ai / Statista 2024: tiempos medios de TikTok / Reels.
 * - Sleep Foundation: efecto de pantalla nocturna en sueno profundo.
 */
object Tips {

    private val stats = listOf(
        "El usuario medio pasa 95 min al dia en Reels y Shorts. Casi una peli.",
        "Tu cerebro tarda 23 min en recuperar la concentracion tras cada distraccion.",
        "Reels reproducen 30 videos en 5 min. 30 micro-dosis de dopamina.",
        "El scroll infinito esta disenado para que tu cerebro no decida parar.",
        "1 hora de pantalla antes de dormir = 22 min menos de sueno profundo.",
        "El 70% de los usuarios pierde la nocion del tiempo en Reels.",
        "TikTok engancha en 8 min de uso. Mas rapido que el cafe.",
        "Una sesion media de Reels dura 24 min. Casi medio episodio de algo bueno.",
        "Cuanto mas joven el usuario, mas tiempo en short video. Cuesta verlo en abuelos.",
        "Cada like que recibes en Reels no es tuyo. Es del algoritmo decidiendo por ti.",
        "Adolescentes con +3h/dia en short video tienen 2x mas sintomas de ansiedad.",
        "El 60% de la gente revisa el movil en los primeros 10 min al despertar.",
        "Pasamos de media 3h 15min al dia con el movil. Eso son 49 dias enteros al ano.",
        "La luz azul de la pantalla retrasa la liberacion de melatonina 1.5 horas."
    )

    private val alternatives = listOf(
        "5 min de Reels = 5 min leyendo. ¿Cuando terminaste un libro entero?",
        "Si tienes 20 min, sal a andar. El sol no se ve haciendo doomscrolling.",
        "Llama a un amigo. Una conversacion supera a 100 reels juntos.",
        "Aburrirte es la senal de que el cerebro va a crear algo. No lo apagues.",
        "Cocina algo sencillo en vez de scrollear. Comes mejor y te sale lo que querias.",
        "10 min de estiramientos al despertar > 10 min de Reels en la cama.",
        "Apunta 3 cosas que quieras hacer hoy. Empieza por una, sin pensar.",
        "Sal al balcon 2 min. La pantalla no se va a ir a ningun lado.",
        "20 sentadillas ahora te dan mas energia que 20 reels.",
        "Escribir un mensaje largo a alguien que te importa pega mas que cualquier scroll.",
        "Un cafe con alguien en persona vale por una semana de stories.",
        "Si te aburres, mira por la ventana 30 segundos. En serio.",
        "Escucha una cancion con los ojos cerrados. Suficiente como pausa.",
        "Pon una canzon, baila 3 minutos. El cuerpo agradece.",
        "Ducha fria 30 segundos. Reseteas mejor que con 50 reels.",
        "Riega una planta. Trae al cerebro a algo lento y vivo."
    )

    private val reflections = listOf(
        "El primer scroll no es el problema. El numero 50 si.",
        "No tienes que ser productivo todo el rato. Aburrirte un poco tambien vale.",
        "Lo dificil no es dejar los Reels. Es no abrirlos sin pensar.",
        "El algoritmo se acuerda de ti cada segundo. Tu, ¿cuanto te acuerdas del algoritmo?",
        "Cada vez que esta app te saca, ganas tiempo que no veias gastar.",
        "Tu tiempo libre no es 'tiempo muerto'. Es de los pocos ratos que son tuyos.",
        "Escoge tus distracciones a proposito. Las que te sorprenden son las que mas duelen.",
        "Si entras en Reels para 'descansar', acabas mas cansado. Comprobado.",
        "La curiosidad real dura horas. La de los Reels, 8 segundos.",
        "Cierra los ojos 10 segundos. Eso ya es mas descanso que media hora de scroll.",
        "Lo bueno tarda. Lo de Reels llega en 15 segundos y se va igual de rapido.",
        "La atencion es el recurso mas escaso del siglo. Y la regalas gratis.",
        "Pregunta: ¿de quien era el ultimo reel que viste? Exacto.",
        "Antes de coger el movil, respira tres veces. A veces se te quita."
    )

    private val all: List<String> = stats + alternatives + reflections

    fun random(): String = all.random()

    /** Devuelve N frases distintas (sin repetir). */
    fun randomDistinct(count: Int): List<String> = all.shuffled().take(count)
}
