package com.edss.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edss.storage")
public record StorageProperties(Supabase supabase, S3 s3, Buckets buckets) {

    public record Supabase(String url, String serviceRoleKey, String bucketPrefix) {}

    public record S3(String region, String accessKeyId, String secretAccessKey, String bucket) {}

    /** Logical bucket names mapped per kind. Concrete buckets prefixed per backend. */
    public record Buckets(
            String contracts, String milestones, String avatars, String files, String tickets) {}
}
