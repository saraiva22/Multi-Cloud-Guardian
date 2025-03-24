package pt.isel.leic.multicloudguardian.repository

import pt.isel.leic.multicloudguardian.domain.file.FileCreate
import pt.isel.leic.multicloudguardian.domain.utils.Id

interface FileRepository {
    fun storeFile(
        file: FileCreate,
        path: String,
        url: String,
        encryption: Boolean,
    ): Id
}
