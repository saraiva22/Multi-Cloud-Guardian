package pt.isel.leic.multicloudguardian.domain.utils

/**
 * Represents pagination information for a query result.
 *
 * @property sort The sorting information for the query result.
 * @property pageNumber The current page number (zero-based).
 * @property pageSize The number of items per page.
 * @property offset The offset of the first item in the current page (zero-based).
 */
data class Pageable(
    val sort: Sort = Sort(),
    val pageNumber: Int = 0,
    val pageSize: Int = 10,
    val offset: Long = 0L,
)
