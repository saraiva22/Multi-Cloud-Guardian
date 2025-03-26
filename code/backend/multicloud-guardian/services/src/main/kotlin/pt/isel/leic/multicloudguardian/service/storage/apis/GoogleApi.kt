package pt.isel.leic.multicloudguardian.service.storage.apis

import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageException
import com.google.cloud.storage.StorageOptions
import jakarta.inject.Named
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.net.URL
import java.util.concurrent.TimeUnit

@Named
class GoogleApi : ApiConfig {
    override fun generateSignedUrl(
        credentials: String,
        bucketName: String,
        blobPath: String,
        identity: String,
        location: String,
    ): String {
        try {
            val storage =
                StorageOptions
                    .newBuilder()
                    .setCredentials(ServiceAccountCredentials.fromStream(FileInputStream(credentials)))
                    .build()
                    .service

            val blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, blobPath)).build()
            val url: URL = storage.signUrl(blobInfo, 15, TimeUnit.MINUTES, Storage.SignUrlOption.withV4Signature())
            return url.toString()
        } catch (error: StorageException) {
            logger.info("Info error $error")
            return ""
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GoogleApi::class.java)
    }
}
