package pt.isel.leic.multicloudguardian.domain.sse

interface EventEmitter {
    fun emit(event: Event)

    fun onCompletion(callback: () -> Unit)

    fun onError(callback: (Throwable) -> Unit)

    fun complete()
}
