package pt.isel.leic.multicloudguardian.domain.metadata


import pt.isel.leic.multicloudguardian.domain.components.Id

/**
 * Represents the metadata of a file.
 * @property metadataId The unique identifier for the metadata.
 * @property tags A list of keywords associated with the file for search optimization.
 * @property contentType The MIME type of the file (e.g., image/png, application/pdf).
 * @property accessCount The number of times the file has been accessed.
 * @property indexedAt The timestamp of the last metadata processing.
 * @property createdAt The timestamp when the metadata was created.
 */

data class Metadata(
    val metadataId: Id,
    val tags: String,
    val contentType: String,
    val accessCount: Int,
    val indexedAt: Long,
    val createdAt: Long
)