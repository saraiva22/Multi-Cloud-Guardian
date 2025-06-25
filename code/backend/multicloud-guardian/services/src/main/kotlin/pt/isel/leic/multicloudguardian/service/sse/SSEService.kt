package pt.isel.leic.multicloudguardian.service.sse

import jakarta.inject.Named
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import pt.isel.leic.multicloudguardian.domain.folder.FolderInfo
import pt.isel.leic.multicloudguardian.domain.folder.InviteStatus
import pt.isel.leic.multicloudguardian.domain.sse.Event
import pt.isel.leic.multicloudguardian.domain.sse.EventEmitter
import pt.isel.leic.multicloudguardian.domain.sse.FolderInfoOutput
import pt.isel.leic.multicloudguardian.domain.sse.UserInfoOutput
import pt.isel.leic.multicloudguardian.domain.user.UserInfo
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Named
class SSEService : NeedsShutdown {
    // Important: mutable state on a singleton service
    // private val listeners = mutableListOf<EventEmitter>()
    // Map<UserId,List<Pair<TokenId,EventEmitter>>>()
    private val userListeners = mutableMapOf<Int, MutableList<Pair<String, EventEmitter>>>()
    private var currentId = 0L
    private var currentInviteId = 0L
    private var currentNewMemberId = 0L
    private val lock = ReentrantLock()

    // A scheduler to send the periodic keep-alive events
    private val scheduler: ScheduledExecutorService =
        Executors.newScheduledThreadPool(1).also {
            it.scheduleAtFixedRate({ keepAlive() }, 2, 2, TimeUnit.SECONDS)
        }

    override fun shutdown() {
        logger.info("shutting down")
        scheduler.shutdown()
    }

    fun addEventEmitter(
        user: Int,
        token: String,
        listener: EventEmitter,
    ) = lock.withLock {
        logger.info("adding listener")
        val element = Pair(token, listener)

        // Get the listener list for the user, or create a new one if it doesn't exist
        val listeners = userListeners.getOrPut(user) { mutableListOf() }

        // Remove any existing listener with the same token to avoid duplicates
        listeners.removeIf { it.first == token }

        // Add the new listener
        listeners.add(element)

        // Automatically remove the listener when the connection is closed
        listener.onCompletion {
            logger.info("onCompletion")
            removeListener(user, token)
        }

        // Automatically remove the listener when an error occurs
        listener.onError {
            logger.info("onError")
            removeListener(user, token)
        }
        listener
    }

    fun sendFile(
        fileId: Int,
        user: UserInfo,
        folderInfo: FolderInfo?,
        fileName: String,
        path: String,
        size: Long,
        contentType: String,
        createdAt: String,
        encryption: Boolean,
        members: List<UserInfo>,
    ) = lock.withLock {
        logger.info("sendFile")
        val id = currentId++
        val membersFolder = members.map { it.id.value }

        sendEventToAll(
            membersFolder,
            Event.File(
                id,
                fileId,
                UserInfoOutput.fromDomain(user),
                FolderInfoOutput.fromDomain(folderInfo),
                fileName,
                path,
                size,
                contentType,
                createdAt,
                encryption,
            ),
        )
    }

    fun sendInvite(
        inviteId: Int,
        status: InviteStatus,
        inviterInfo: UserInfo,
        guestId: Int,
        folderId: Int,
        folderName: String,
    ) = lock.withLock {
        logger.info("sendInvite")
        val id = currentInviteId++
        sendEventToAll(listOf(guestId), Event.Invite(id, inviteId, status, UserInfoOutput.fromDomain(inviterInfo), folderId, folderName))
    }

    fun sendNewMember(
        ownerId: Int,
        newMember: UserInfo,
        folderId: Int,
        folderName: String,
    ) = lock.withLock {
        logger.info("newMember")
        val id = currentNewMemberId++
        sendEventToAll(listOf(ownerId), Event.NewMember(id, ownerId, UserInfoOutput.fromDomain(newMember), folderId, folderName))
    }

    fun disconnectListener(
        userId: Int,
        token: String,
    ) = removeListener(userId, token)

    private fun removeListener(
        userId: Int,
        token: String,
    ) = lock.withLock {
        logger.info("removing listener")
        val listeners = userListeners[userId] ?: return

        // Find and remove the matching listener
        val iterator = listeners.iterator()
        while (iterator.hasNext()) {
            val (storedToken, emitter) = iterator.next()
            if (storedToken == token) {
                emitter.complete()
                iterator.remove()
                break
            }
        }

        // Remove the user entry if the list is now empty
        if (listeners.isEmpty()) {
            userListeners.remove(userId)
        }
    }

    private fun keepAlive() =
        lock.withLock {
            if (userListeners.isEmpty()) {
                return@withLock
            }
            logger.info(
                "keepAlive, sending to {} users and {} listeners",
                userListeners.count(),
                userListeners.values.flatten().count(),
            )
            sendEventToAll(userListeners.map { it.key }, Event.KeepAlive(Clock.System.now()))
        }

    private fun sendEventToAll(
        membersFolder: List<Int>,
        event: Event,
    ) {
        membersFolder.forEach { member ->
            userListeners[member]?.let {
                try {
                    it.forEach { ev -> ev.second.emit(event) }
                } catch (ex: Exception) {
                    logger.info("Exception while sending event - {}", ex.message)
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SSEService::class.java)
    }
}
