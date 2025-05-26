package pt.isel.leic.multicloudguardian.service.utils

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi

fun runWithHandle(
    jdbi: Jdbi,
    block: (Handle) -> Unit,
) = jdbi.useTransaction<Exception>(block)

fun clearData(
    jdbi: Jdbi,
    tableName: String,
    attributeName: String,
    value: Int,
) {
    runWithHandle(jdbi, { handle ->
        handle
            .createUpdate(
                """
                delete from $tableName
                where $attributeName = :value
            """,
            ).bind("value", value)
            .execute()
    })
}
