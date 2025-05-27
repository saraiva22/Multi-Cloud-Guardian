package pt.isel.leic.multicloudguardian.domain.utils

/**
 * Represents a paginated result for a list of items.
 *
 * @param T The type of the items in the paginated result.
 * @property content The list of items in the current page.
 * @property pageable Information about the pagination and sorting of the result.
 * @property totalElements The total number of elements across all pages.
 * @property totalPages The total number of pages available.
 * @property last Indicates if the current page is the last page.
 * @property first Indicates if the current page is the first page.
 * @property size The number of items per page.
 * @property number The current page number (zero-based).
 */
data class PageResult<T>(
    val content: List<T>,
    val pageable: Pageable,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean,
    val first: Boolean,
    val size: Int,
    val number: Int,
) {
    companion object {
        /**
         * Creates a `PageResult` from a partial result of a paginated list.
         *
         * @param T The type of the items in the paginated result.
         * @param paginatedList The list of items in the current page.
         * @param totalElements The total number of elements across all pages.
         * @param limit The maximum number of items per page.
         * @param offset The offset of the current page (zero-based).
         * @param sorted Indicates if the result is sorted.
         * @return A `PageResult` containing the paginated data and metadata.
         */
        fun <T> fromPartialResult(
            paginatedList: List<T>,
            totalElements: Long,
            limit: Int,
            offset: Int,
            sorted: Boolean = true,
        ): PageResult<T> {
            val totalPages = if (totalElements == 0L) 0 else ((totalElements - 1) / limit + 1).toInt()
            val pageNumber = if (limit == 0) 0 else offset / limit

            return PageResult(
                content = paginatedList,
                pageable =
                    Pageable(
                        sort = Sort(sorted = sorted, unsorted = !sorted),
                        pageNumber = pageNumber,
                        pageSize = limit,
                        offset = offset.toLong(),
                    ),
                totalElements = totalElements,
                totalPages = totalPages,
                last = pageNumber + 1 >= totalPages,
                first = pageNumber == 0,
                size = limit,
                number = pageNumber,
            )
        }
    }
}
