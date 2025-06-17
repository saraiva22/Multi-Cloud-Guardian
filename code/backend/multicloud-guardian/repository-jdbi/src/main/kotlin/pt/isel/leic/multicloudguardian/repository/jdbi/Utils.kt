package pt.isel.leic.multicloudguardian.repository.jdbi

import kotlinx.datetime.Instant
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.postgres.PostgresPlugin
import pt.isel.leic.multicloudguardian.domain.credentials.Credentials
import pt.isel.leic.multicloudguardian.domain.file.File
import pt.isel.leic.multicloudguardian.domain.folder.Folder
import pt.isel.leic.multicloudguardian.domain.folder.FolderPrivateInvite
import pt.isel.leic.multicloudguardian.domain.folder.InviteStatus
import pt.isel.leic.multicloudguardian.domain.token.TokenValidationInfo
import pt.isel.leic.multicloudguardian.domain.user.PasswordValidationInfo
import pt.isel.leic.multicloudguardian.domain.user.User
import pt.isel.leic.multicloudguardian.domain.user.UserInfo
import pt.isel.leic.multicloudguardian.domain.user.UserStorageInfo
import pt.isel.leic.multicloudguardian.repository.jdbi.mappers.CredentialsMapper
import pt.isel.leic.multicloudguardian.repository.jdbi.mappers.EmailMapper
import pt.isel.leic.multicloudguardian.repository.jdbi.mappers.FileMapper
import pt.isel.leic.multicloudguardian.repository.jdbi.mappers.FolderMapper
import pt.isel.leic.multicloudguardian.repository.jdbi.mappers.FolderPrivateInviteMapper
import pt.isel.leic.multicloudguardian.repository.jdbi.mappers.IdMapper
import pt.isel.leic.multicloudguardian.repository.jdbi.mappers.InstantMapper
import pt.isel.leic.multicloudguardian.repository.jdbi.mappers.PasswordValidationInfoMapper
import pt.isel.leic.multicloudguardian.repository.jdbi.mappers.StatusMapper
import pt.isel.leic.multicloudguardian.repository.jdbi.mappers.TokenValidationInfoMapper
import pt.isel.leic.multicloudguardian.repository.jdbi.mappers.TypeMapper
import pt.isel.leic.multicloudguardian.repository.jdbi.mappers.UserInfoMapper
import pt.isel.leic.multicloudguardian.repository.jdbi.mappers.UserMapper
import pt.isel.leic.multicloudguardian.repository.jdbi.mappers.UserStorageInfoMapper
import pt.isel.leic.multicloudguardian.repository.jdbi.mappers.UsernameMapper

fun Jdbi.configureWithAppRequirements(): Jdbi {
    installPlugin(KotlinPlugin())
    installPlugin(PostgresPlugin())

    registerColumnMapper(PasswordValidationInfo::class.java, PasswordValidationInfoMapper())
    registerColumnMapper(TokenValidationInfo::class.java, TokenValidationInfoMapper())
    registerColumnMapper(Instant::class.java, InstantMapper())
    registerColumnMapper(IdMapper())
    registerColumnMapper(EmailMapper::class.java, EmailMapper())
    registerColumnMapper(UsernameMapper::class.java, UsernameMapper())
    registerColumnMapper(TypeMapper::class.java, TypeMapper())
    registerColumnMapper(InviteStatus::class.java, StatusMapper())

    registerRowMapper(User::class.java, UserMapper())
    registerRowMapper(UserInfo::class.java, UserInfoMapper())
    registerRowMapper(UserStorageInfo::class.java, UserStorageInfoMapper())
    registerRowMapper(Folder::class.java, FolderMapper())
    registerRowMapper(File::class.java, FileMapper())
    registerRowMapper(Credentials::class.java, CredentialsMapper())
    registerRowMapper(FolderPrivateInvite::class.java, FolderPrivateInviteMapper())

    return this
}
