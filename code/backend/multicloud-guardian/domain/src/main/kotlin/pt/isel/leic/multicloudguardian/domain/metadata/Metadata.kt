package pt.isel.leic.multicloudguardian.domain.metadata

import kotlinx.datetime.Instant
import pt.isel.leic.multicloudguardian.domain.utils.Id

/**
 * Represents the metadata of a file.
 * @property metadataId The unique identifier for the metadata.
 * @property fileId The unique identifier for the file.
 * @property contentType The MIME type of the file (e.g., image/png, application/pdf).
 * @property accessCount The number of times the file has been accessed.
 * @property tags A list of keywords associated with the file for search optimization.
 * @property createdAt The [Instant]  when the metadata was created.
 * @property indexedAt The [Instant] when the metadata was last indexed.
 */

data class Metadata(
    val metadataId: Id,
    val fileId: Id,
    val contentType: String,
    val accessCount: Int,
    val tags: String,
    val createdAt: Instant,
    val indexedAt: Instant,
)
