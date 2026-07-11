package com.edss.integrations.storage;

import java.net.URI;
import java.time.Duration;

/**
 * Object-storage port. Two impls (Supabase Storage, S3), each
 * {@code @ConditionalOnProperty} on the {@code edss.features.storage
 * .file-backend} selector. Browser uploads and downloads go direct to the
 * bucket via presigned URLs so the backend is never in the file byte path.
 */
public interface StorageClient {

    /** Provider label surfaced in logs + persisted on {@code knowledge.files.bucket}. */
    String providerId();

    Presigned presignUpload(String bucket, String key, String contentType, Duration ttl);

    URI presignDownload(String bucket, String key, Duration ttl);

    void delete(String bucket, String key);

    record Presigned(URI url, java.time.Instant expiresAt) {}
}
