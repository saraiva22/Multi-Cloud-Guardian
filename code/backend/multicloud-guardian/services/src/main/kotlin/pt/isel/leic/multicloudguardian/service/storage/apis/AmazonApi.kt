package pt.isel.leic.multicloudguardian.service.storage.apis

import jakarta.inject.Named
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.time.Duration

@Named
class AmazonApi : ApiConfig {
    override fun generateSignedUrl(
        credentials: String,
        bucketName: String,
        blobPath: String,
        identity: String,
        location: String,
        validDurationInMinutes: Long,
    ): String {
        try {
            val credentialsProvider =
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        identity,
                        credentials,
                    ),
                )
            S3Presigner
                .builder()
                .region(Region.of(location))
                .credentialsProvider(credentialsProvider)
                .build()
                .use { preSigner ->
                    val objectRequest =
                        GetObjectRequest
                            .builder()
                            .bucket(bucketName)
                            .key(blobPath)
                            .build()
                    val preSignerRequest =
                        GetObjectPresignRequest
                            .builder()
                            .signatureDuration(Duration.ofMinutes(validDurationInMinutes))
                            .getObjectRequest(objectRequest)
                            .build()
                    val preSignedRequest = preSigner.presignGetObject(preSignerRequest)
                    return preSignedRequest.url().toExternalForm()
                }
        } catch (e: Exception) {
            logger.error("Error generating preSigned URL", e)
            return ""
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AmazonApi::class.java)
    }
}
