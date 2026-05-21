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
        "Cada like que recibes en Reels no es tuyo. Es del algoritmo decidiendo por ti."
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
        "Si te aburres, mira por la ventana 30 segundos. En serio."
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
        "Cierra los ojos 10 segundos. Eso ya es mas descanso que media hora de scroll."
    )

    private val all: List<String> = stats + alternatives + reflections

    fun random(): String = all.random()

    /** Devuelve N frases distintas (sin repetir). */
    fun randomDistinct(count: Int): List<String> = all.shuffled().take(count)
}
