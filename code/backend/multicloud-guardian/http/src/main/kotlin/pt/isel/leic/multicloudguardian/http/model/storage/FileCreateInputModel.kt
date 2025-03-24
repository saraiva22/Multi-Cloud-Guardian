package pt.isel.leic.multicloudguardian.http.model.storage

import org.springframework.web.multipart.MultipartFile
import pt.isel.leic.multicloudguardian.domain.file.FileCreate
import java.util.UUID

data class FileCreateInputModel(
    val file: MultipartFile,
    val encryption: Boolean,
) {
    fun toDomain(): FileCreate =
        FileCreate(
            fileName = safeFileName(file.originalFilename),
            fileContent = file.bytes,
            contentType = file.contentType ?: "application/unknown",
            size = file.size,
            encryption = encryption,
        )

    private fun safeFileName(originalName: String?): String {
        val safeName = originalName?.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        return safeName?.takeIf { it.isNotBlank() } ?: "file_${UUID.randomUUID()}"
    }
}
