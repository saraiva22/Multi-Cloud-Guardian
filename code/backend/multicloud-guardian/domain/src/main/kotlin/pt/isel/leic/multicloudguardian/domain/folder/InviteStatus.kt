package pt.isel.leic.multicloudguardian.domain.folder

/**
 * Represents the possible statuses for a folder operation.
 *
 * @constructor Creates an enum with the following possible values:
 * - PENDING: The operation is awaiting a decision.
 * - ACCEPT: The operation has been accepted.
 * - REJECT: The operation has been rejected.
 */
enum class InviteStatus {
    PENDING,
    ACCEPT,
    REJECT,
    ;

    companion object {
        fun fromInt(value: Int): InviteStatus = InviteStatus.entries[value]
    }
}
