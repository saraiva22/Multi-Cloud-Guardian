package pt.isel.leic.multicloudguardian.repository.jdbi

import kotlinx.datetime.Instant
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.postgres.PostgresPlugin
import pt.isel.leic.multicloudguardian.repository.jdbi.mappers.InstantMapper
import pt.isel.leic.multicloudguardian.repository.jdbi.mappers.PasswordValidationInfoMapper
import pt.isel.leic.multicloudguardian.repository.jdbi.mappers.TokenValidationInfoMapper
import pt.isel.leic.multicloudguardian.domain.token.TokenValidationInfo
import pt.isel.leic.multicloudguardian.domain.user.PasswordValidationInfo

fun Jdbi.configureWithAppRequirements(): Jdbi {
    installPlugin(KotlinPlugin())
    installPlugin(PostgresPlugin())

    registerColumnMapper(PasswordValidationInfo::class.java, PasswordValidationInfoMapper())
    registerColumnMapper(TokenValidationInfo::class.java, TokenValidationInfoMapper())

    registerColumnMapper(Instant::class.java, InstantMapper())

    return this
}