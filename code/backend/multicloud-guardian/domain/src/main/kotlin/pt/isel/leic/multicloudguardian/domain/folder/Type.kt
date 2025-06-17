package pt.isel.leic.multicloudguardian.domain.folder

/**
 * Enum class representing the type of folder.
 * @property PRIVATE Represents a private folder.
 * @property SHARED Represents a shared folder.
 */
enum class Type {
    PRIVATE,
    SHARED,
    ;

    companion object {
        fun fromInt(value: Int): Type = entries[value]
    }
}
