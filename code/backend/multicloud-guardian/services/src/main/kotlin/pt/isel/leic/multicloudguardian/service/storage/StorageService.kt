package pt.isel.leic.multicloudguardian.service.storage

import org.springframework.stereotype.Service
import pt.isel.leic.multicloudguardian.domain.file.FileCreate
import pt.isel.leic.multicloudguardian.domain.user.User
import pt.isel.leic.multicloudguardian.repository.TransactionManager
import pt.isel.leic.multicloudguardian.service.security.SecurityService
import pt.isel.leic.multicloudguardian.service.storage.jclouds.StorageFileJclouds

@Service
class StorageService(
    private val transactionManager: TransactionManager,
    private val securityService: SecurityService,
    private val jcloudsStorage: StorageFileJclouds,
) {
    fun createFile(
        file: FileCreate,
        encryption: Boolean,
        user: User,
    ): FileCreationResult {
        val contextStorage = jcloudsStorage.initializeBlobStoreContext()
    }
}
