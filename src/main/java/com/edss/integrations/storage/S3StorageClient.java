package com.edss.integrations.storage;

import com.edss.shared.config.StorageProperties;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * Presigned uploads via PUT and downloads via GET. Bucket names on S3 are
 * global — the {@code edss.storage.buckets.*} logical names map into the
 * single {@code AWS_S3_BUCKET} using a key prefix per logical bucket.
 */
@Component
@ConditionalOnProperty(name = "edss.features.storage.file-backend", havingValue = "s3")
public class S3StorageClient implements StorageClient {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final StorageProperties.S3 config;

    public S3StorageClient(StorageProperties properties) {
        this.config = properties.s3();
        if (config == null
                || config.accessKeyId() == null
                || config.accessKeyId().isBlank()
                || config.secretAccessKey() == null
                || config.secretAccessKey().isBlank()
                || config.bucket() == null
                || config.bucket().isBlank()) {
            throw new IllegalStateException(
                    "AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_S3_BUCKET must be set when"
                            + " file-backend=s3.");
        }
        StaticCredentialsProvider creds =
                StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.accessKeyId(), config.secretAccessKey()));
        Region region = Region.of(config.region());
        this.s3 = S3Client.builder().region(region).credentialsProvider(creds).build();
        this.presigner = S3Presigner.builder().region(region).credentialsProvider(creds).build();
    }

    @Override
    public String providerId() {
        return "s3";
    }

    @Override
    public Presigned presignUpload(String bucket, String key, String contentType, Duration ttl) {
        String objectKey = bucket + "/" + key;
        PutObjectRequest put =
                PutObjectRequest.builder()
                        .bucket(config.bucket())
                        .key(objectKey)
                        .contentType(contentType)
                        .build();
        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(ttl)
                        .putObjectRequest(put)
                        .build();
        try {
            URI url = presigner.presignPutObject(presignRequest).url().toURI();
            return new Presigned(url, Instant.now().plus(ttl));
        } catch (java.net.URISyntaxException ex) {
            throw new StorageException("Malformed presigned URL", ex);
        }
    }

    @Override
    public URI presignDownload(String bucket, String key, Duration ttl) {
        String objectKey = bucket + "/" + key;
        GetObjectRequest get =
                GetObjectRequest.builder().bucket(config.bucket()).key(objectKey).build();
        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(ttl)
                        .getObjectRequest(get)
                        .build();
        try {
            return presigner.presignGetObject(presignRequest).url().toURI();
        } catch (java.net.URISyntaxException ex) {
            throw new StorageException("Malformed presigned URL", ex);
        }
    }

    @Override
    public void delete(String bucket, String key) {
        String objectKey = bucket + "/" + key;
        s3.deleteObject(
                DeleteObjectRequest.builder().bucket(config.bucket()).key(objectKey).build());
    }
}
