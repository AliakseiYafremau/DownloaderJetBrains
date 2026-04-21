package downloader.domain.algorithms

class Timer {
    private var startTime: Long = 0

    fun start() {
        startTime = System.currentTimeMillis()
    }

    fun elapsedMillis(): Long {
        return System.currentTimeMillis() - startTime
    }
}

