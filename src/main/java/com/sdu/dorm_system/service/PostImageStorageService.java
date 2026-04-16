package com.sdu.dorm_system.service;

import com.sdu.dorm_system.config.AppProperties;
import com.sdu.dorm_system.exception.BusinessException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PostImageStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        MediaType.IMAGE_JPEG_VALUE,
        MediaType.IMAGE_PNG_VALUE,
        "image/webp",
        MediaType.IMAGE_GIF_VALUE
    );

    private final AppProperties appProperties;
    private final RestClient restClient;
    private final AtomicBoolean bucketEnsured = new AtomicBoolean(false);

    public PostImageStorageService(AppProperties appProperties, RestClient.Builder restClientBuilder) {
        this.appProperties = appProperties;
        this.restClient = restClientBuilder.build();
    }

    public StoredImage storePostImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("Image file is required");
        }

        AppProperties.SupabaseProperties supabase = requireSupabaseConfig();
        String contentType = resolveContentType(file);
        String extension = resolveExtension(file, contentType);
        ensureBucketReady(supabase);

        LocalDate today = LocalDate.now();
        String objectPath = buildObjectPath(supabase.folder(), today, extension);

        try {
            restClient.post()
                .uri(buildObjectUrl(supabase, objectPath))
                .header("apikey", supabase.serviceKey())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabase.serviceKey())
                .header("x-upsert", "false")
                .contentType(MediaType.parseMediaType(contentType))
                .body(file.getBytes())
                .retrieve()
                .toBodilessEntity();
        } catch (IOException exception) {
            throw BusinessException.conflict("Image file could not be read");
        } catch (ResourceAccessException exception) {
            throw BusinessException.conflict("Supabase Storage could not be reached. Check SUPABASE_URL and your internet connection.");
        } catch (RestClientResponseException exception) {
            throw mapUploadException(exception);
        }

        return new StoredImage(
            objectPath,
            buildPublicUrl(supabase, objectPath),
            contentType,
            file.getSize()
        );
    }

    public List<String> storePostImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        return files.stream()
            .filter(file -> file != null && !file.isEmpty())
            .map(file -> storePostImage(file).publicUrl())
            .toList();
    }

    private AppProperties.SupabaseProperties requireSupabaseConfig() {
        AppProperties.SupabaseProperties supabase = appProperties.storage().supabase();
        if (supabase == null
            || !StringUtils.hasText(supabase.url())
            || !StringUtils.hasText(supabase.serviceKey())
            || !StringUtils.hasText(supabase.bucket())) {
            throw BusinessException.conflict("Supabase Storage is not configured");
        }
        return supabase;
    }

    private void ensureBucketReady(AppProperties.SupabaseProperties supabase) {
        if (bucketEnsured.get()) {
            return;
        }

        synchronized (bucketEnsured) {
            if (bucketEnsured.get()) {
                return;
            }

            try {
                BucketResponse bucket = restClient.get()
                    .uri(normalizeBaseUrl(supabase.url()) + "/storage/v1/bucket/" + supabase.bucket())
                    .header("apikey", supabase.serviceKey())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabase.serviceKey())
                    .retrieve()
                    .body(BucketResponse.class);

                if (bucket == null || !bucket.publicBucket()) {
                    updateBucketToPublic(supabase);
                }
            } catch (RestClientResponseException exception) {
                if (exception.getStatusCode().value() == 404) {
                    createPublicBucket(supabase);
                } else {
                    throw mapBucketException("checked", exception);
                }
            } catch (ResourceAccessException exception) {
                throw BusinessException.conflict("Supabase Storage could not be reached. Check SUPABASE_URL and your internet connection.");
            }

            bucketEnsured.set(true);
        }
    }

    private void createPublicBucket(AppProperties.SupabaseProperties supabase) {
        try {
            restClient.post()
                .uri(normalizeBaseUrl(supabase.url()) + "/storage/v1/bucket")
                .header("apikey", supabase.serviceKey())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabase.serviceKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "id", supabase.bucket(),
                    "name", supabase.bucket(),
                    "public", true,
                    "allowed_mime_types", List.of(
                        MediaType.IMAGE_JPEG_VALUE,
                        MediaType.IMAGE_PNG_VALUE,
                        "image/webp",
                        MediaType.IMAGE_GIF_VALUE
                    )
                ))
                .retrieve()
                .toBodilessEntity();
        } catch (ResourceAccessException exception) {
            throw BusinessException.conflict("Supabase Storage could not be reached. Check SUPABASE_URL and your internet connection.");
        } catch (RestClientResponseException exception) {
            throw mapBucketException("created", exception);
        }
    }

    private void updateBucketToPublic(AppProperties.SupabaseProperties supabase) {
        try {
            restClient.put()
                .uri(normalizeBaseUrl(supabase.url()) + "/storage/v1/bucket/" + supabase.bucket())
                .header("apikey", supabase.serviceKey())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabase.serviceKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("public", true))
                .retrieve()
                .toBodilessEntity();
        } catch (ResourceAccessException exception) {
            throw BusinessException.conflict("Supabase Storage could not be reached. Check SUPABASE_URL and your internet connection.");
        } catch (RestClientResponseException exception) {
            throw mapBucketException("updated", exception);
        }
    }

    private String buildObjectPath(String folder, LocalDate today, String extension) {
        StringBuilder path = new StringBuilder();
        if (StringUtils.hasText(folder)) {
            path.append(trimSlashes(folder)).append('/');
        }
        path.append(today.getYear())
            .append('/')
            .append(String.format("%02d", today.getMonthValue()))
            .append('/')
            .append(UUID.randomUUID())
            .append('.')
            .append(extension);
        return path.toString();
    }

    private String buildObjectUrl(AppProperties.SupabaseProperties supabase, String objectPath) {
        return normalizeBaseUrl(supabase.url()) + "/storage/v1/object/" + supabase.bucket() + "/" + objectPath;
    }

    private String buildPublicUrl(AppProperties.SupabaseProperties supabase, String objectPath) {
        return normalizeBaseUrl(supabase.url()) + "/storage/v1/object/public/" + supabase.bucket() + "/" + objectPath;
    }

    private String normalizeBaseUrl(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String trimSlashes(String value) {
        String trimmed = value.trim();
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)) {
            throw BusinessException.badRequest("Image content type is required");
        }

        String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
        if (!ALLOWED_MIME_TYPES.contains(normalizedContentType)) {
            throw BusinessException.badRequest("Unsupported image format");
        }
        return normalizedContentType;
    }

    private String resolveExtension(MultipartFile file, String contentType) {
        String originalFileName = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFileName);
        if (!StringUtils.hasText(extension)) {
            extension = contentType.substring(contentType.indexOf('/') + 1);
        }

        String normalizedExtension = extension.toLowerCase(Locale.ROOT);
        if ("image/jpg".equals(contentType)) {
            normalizedExtension = "jpg";
        }
        if (!ALLOWED_EXTENSIONS.contains(normalizedExtension)) {
            throw BusinessException.badRequest("Unsupported image format");
        }

        return normalizedExtension;
    }

    private BusinessException mapUploadException(RestClientResponseException exception) {
        if (exception.getStatusCode().value() == 400) {
            return BusinessException.badRequest("Supabase rejected the uploaded image" + formatSupabaseErrorDetails(exception));
        }
        if (exception.getStatusCode().value() == 401 || exception.getStatusCode().value() == 403) {
            return BusinessException.conflict("Supabase credentials are invalid for image upload" + formatSupabaseErrorDetails(exception));
        }
        return BusinessException.conflict("Supabase image upload failed" + formatSupabaseErrorDetails(exception));
    }

    private BusinessException mapBucketException(String action, RestClientResponseException exception) {
        if (exception.getStatusCode().value() == 401 || exception.getStatusCode().value() == 403) {
            return BusinessException.conflict("Supabase credentials are invalid for bucket access" + formatSupabaseErrorDetails(exception));
        }
        if (exception.getStatusCode().value() == 400) {
            return BusinessException.conflict("Supabase bucket request was rejected" + formatSupabaseErrorDetails(exception));
        }
        return BusinessException.conflict("Supabase bucket could not be " + action + formatSupabaseErrorDetails(exception));
    }

    private String formatSupabaseErrorDetails(RestClientResponseException exception) {
        String responseBody = exception.getResponseBodyAsString();
        if (!StringUtils.hasText(responseBody)) {
            return " (HTTP " + exception.getStatusCode().value() + ")";
        }

        String normalized = responseBody.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.length() > 200) {
            normalized = normalized.substring(0, 200) + "...";
        }

        return " (HTTP " + exception.getStatusCode().value() + ": " + normalized + ")";
    }

    public record StoredImage(
        String fileName,
        String publicUrl,
        String contentType,
        long size
    ) {
    }

    public record BucketResponse(
        String id,
        @JsonProperty("public") Boolean publicBucket
    ) {
        public Boolean publicBucket() {
            return publicBucket;
        }
    }
}
