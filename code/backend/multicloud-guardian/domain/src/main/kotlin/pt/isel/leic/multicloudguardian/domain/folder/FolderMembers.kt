package pt.isel.leic.multicloudguardian.domain.folder

import pt.isel.leic.multicloudguardian.domain.user.UserInfo

data class FolderMembers(
    val folder: Folder,
    val members: List<UserInfo>,
)
