package burvy.api.utilities

/**
 * Track tick time
 */
object TickChecker {
    private const val THRESHOLD_MS = 35.0

    // this is the only thing you will need
    fun isLagging(): Boolean = tickPrev > THRESHOLD_MS

    private var tickStart: Long = 0L
    private var tickPrev: Double = 0.0

    fun tickStart() {
        tickStart = System.nanoTime()
    }

    fun tickEnd() {
        tickPrev = (System.nanoTime() - tickStart) / 1_000_000.0
    }
}
