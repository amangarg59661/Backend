package com.edss.integrations.storage;

import com.edss.shared.config.StorageProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Supabase Storage backend. Presigned uploads use {@code /object/upload/sign};
 * downloads use {@code /object/sign}. Authenticated with the project's
 * service-role key. Buckets are prefixed by {@code edss.storage.supabase
 * .bucket-prefix} so multiple environments can share the same project.
 */
@Component
@ConditionalOnProperty(
        name = "edss.features.storage.file-backend",
        havingValue = "supabase",
        matchIfMissing = true)
public class SupabaseStorageClient implements StorageClient {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageClient.class);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper objectMapper;
    private final StorageProperties.Supabase config;

    public SupabaseStorageClient(StorageProperties properties, ObjectMapper objectMapper) {
        this.config = properties.supabase();
        this.objectMapper = objectMapper;
        if (config == null || config.url() == null || config.url().isBlank()
                || config.serviceRoleKey() == null || config.serviceRoleKey().isBlank()) {
            throw new IllegalStateException(
                    "SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY must be set when file-backend=supabase.");
        }
    }

    @Override
    public String providerId() {
        return "supabase";
    }

    @Override
    public Presigned presignUpload(String bucket, String key, String contentType, Duration ttl) {
        String prefixed = prefixed(bucket);
        String path = "/storage/v1/object/upload/sign/" + prefixed + "/" + key;
        String body;
        try {
            body = objectMapper.writeValueAsString(Map.of("expiresIn", (int) ttl.getSeconds()));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
        HttpRequest req =
                HttpRequest.newBuilder(URI.create(config.url() + path))
                        .header("Authorization", "Bearer " + config.serviceRoleKey())
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build();
        JsonNode payload = send(req, "presign upload");
        String signedUrl = payload.path("url").asText();
        URI absolute =
                signedUrl.startsWith("http")
                        ? URI.create(signedUrl)
                        : URI.create(config.url() + signedUrl);
        return new Presigned(absolute, Instant.now().plus(ttl));
    }

    @Override
    public URI presignDownload(String bucket, String key, Duration ttl) {
        String prefixed = prefixed(bucket);
        String path = "/storage/v1/object/sign/" + prefixed + "/" + key;
        String body;
        try {
            body = objectMapper.writeValueAsString(Map.of("expiresIn", (int) ttl.getSeconds()));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
        HttpRequest req =
                HttpRequest.newBuilder(URI.create(config.url() + path))
                        .header("Authorization", "Bearer " + config.serviceRoleKey())
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build();
        JsonNode payload = send(req, "presign download");
        String signedUrl = payload.path("signedURL").asText();
        return signedUrl.startsWith("http")
                ? URI.create(signedUrl)
                : URI.create(config.url() + signedUrl);
    }

    @Override
    public void delete(String bucket, String key) {
        String prefixed = prefixed(bucket);
        String path = "/storage/v1/object/" + prefixed + "/" + key;
        HttpRequest req =
                HttpRequest.newBuilder(URI.create(config.url() + path))
                        .header("Authorization", "Bearer " + config.serviceRoleKey())
                        .DELETE()
                        .build();
        try {
            HttpResponse<Void> res = http.send(req, HttpResponse.BodyHandlers.discarding());
            if (res.statusCode() >= 300) {
                log.warn("Supabase delete failed [{}]", res.statusCode());
            }
        } catch (Exception ex) {
            log.warn("Supabase delete threw", ex);
        }
    }

    private String prefixed(String bucket) {
        return config.bucketPrefix() == null || config.bucketPrefix().isBlank()
                ? bucket
                : config.bucketPrefix() + "-" + bucket;
    }

    private JsonNode send(HttpRequest req, String action) {
        HttpResponse<String> res;
        try {
            res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new StorageException("Supabase " + action + " threw", ex);
        }
        if (res.statusCode() >= 300) {
            throw new StorageException(
                    "Supabase " + action + " failed [" + res.statusCode() + "]: " + res.body());
        }
        try {
            return objectMapper.readTree(res.body());
        } catch (Exception ex) {
            throw new StorageException("Supabase " + action + " parse failed", ex);
        }
    }
}
