package pt.isel.leic.multicloudguardian.repository.jdbi.model


@FunctionalInterface
fun interface JdbiModel<R> {
    fun toDomainModel(): R
}