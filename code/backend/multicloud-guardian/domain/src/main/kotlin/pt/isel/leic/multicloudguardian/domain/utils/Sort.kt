package pt.isel.leic.multicloudguardian.domain.utils

/**
 * Represents sorting information with two Boolean properties.
 *
 * @property sorted Indicates if the data is sorted. Defaults to `true`.
 * @property unsorted Indicates if the data is unsorted. Defaults to `false`.
 */
data class Sort(
    val sorted: Boolean = true,
    val unsorted: Boolean = false,
)
