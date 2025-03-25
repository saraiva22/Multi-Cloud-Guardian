package pt.isel.leic.multicloudguardian

object Environment {
    fun getDbUrl() = System.getenv(KEY_DB_URL) ?: throw Exception("Missing env var $KEY_DB_URL")

    fun getBucketName() = System.getenv(BUCKET_NAME) ?: throw Exception("Missing env var $BUCKET_NAME")

    fun getGoogleCredentials() =
        System.getenv(GOOGLE_CREDENTIALS)
            ?: throw Exception("Missing env var $GOOGLE_CREDENTIALS")

    fun getGoogleIdentity() = System.getenv(GOOGLE_IDENTITY) ?: throw Exception("Missing env var $GOOGLE_IDENTITY")

    fun getAzureCredentials() =
        System.getenv(AZURE_CREDENTIALS)
            ?: throw Exception("Missing env var $AZURE_CREDENTIALS")

    fun getAzureIdentity() = System.getenv(AZURE_IDENTITY) ?: throw Exception("Missing env var $AZURE_IDENTITY")

    fun getAmazonCredentials() = System.getenv(AWS_CREDENTIALS) ?: throw Exception("Missing env var $AWS_CREDENTIALS")

    fun getAmazonIdentity() = System.getenv(AWS_IDENTITY) ?: throw Exception("Missing env var $AWS_IDENTITY")

    fun getBackBlazeCredentials() =
        System.getenv(BACK_BLAZE_CREDENTIALS)
            ?: throw Exception("Missing env var $BACK_BLAZE_CREDENTIALS")

    fun getBackBlazeIdentity() = System.getenv(BACK_BLAZE_IDENTITY) ?: throw Exception("Missing env var $BACK_BLAZE_IDENTITY")

    // Database
    private const val KEY_DB_URL = "DB_URL"

    // Storage
    private const val BUCKET_NAME = "BUCKET_NAME"
    private const val GOOGLE_CREDENTIALS = "GOOGLE_CREDENTIALS"
    private const val GOOGLE_IDENTITY = "GOOGLE_IDENTITY"
    private const val AZURE_CREDENTIALS = "AZURE_CREDENTIALS"
    private const val AZURE_IDENTITY = "AZURE_IDENTITY"
    private const val AWS_CREDENTIALS = "AWS_CREDENTIALS"
    private const val AWS_IDENTITY = "AWS_IDENTITY"
    private const val BACK_BLAZE_CREDENTIALS = "BACK_BLAZE_CREDENTIALS"
    private const val BACK_BLAZE_IDENTITY = "BACK_BLAZE_IDENTITY"
}
