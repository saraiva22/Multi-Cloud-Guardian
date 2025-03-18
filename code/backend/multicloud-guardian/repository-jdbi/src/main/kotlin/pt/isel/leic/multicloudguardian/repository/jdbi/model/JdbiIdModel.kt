package pt.isel.leic.multicloudguardian.repository.jdbi.model

import pt.isel.leic.multicloudguardian.domain.components.Id
import pt.isel.leic.multicloudguardian.domain.utils.get

class JdbiIdModel(val id: Int) : JdbiModel<Id> {
    override fun toDomainModel(): Id = Id(id).get()
}