package pt.isel.leic.multicloudguardian.repository.jdbi

import org.jdbi.v3.core.Handle
import pt.isel.leic.multicloudguardian.domain.file.FileCreate
import pt.isel.leic.multicloudguardian.domain.utils.Id
import pt.isel.leic.multicloudguardian.repository.FileRepository

class JdbiFileRepository (
    private val handle: Handle,
) : FileRepository{
    override fun storeFile(file: FileCreate, path: String, url: String, encryption: Boolean): Id {
        TODO("Not yet implemented")
    }
}