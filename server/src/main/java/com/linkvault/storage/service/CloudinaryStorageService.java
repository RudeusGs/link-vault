package com.linkvault.storage.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.linkvault.common.exception.StorageException;
import com.linkvault.folders.entity.Folder;
import com.linkvault.resources.enums.ResourceType;
import com.linkvault.storage.config.CloudinaryProperties;
import com.linkvault.storage.dto.StorageFile;
import com.linkvault.storage.dto.StorageResult;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CloudinaryStorageService implements StorageService {

    private final Cloudinary cloudinary;
    private final CloudinaryProperties properties;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public CloudinaryStorageService(Cloudinary cloudinary, CloudinaryProperties properties) {
        this.cloudinary = cloudinary;
        this.properties = properties;
    }

    @Override
    public StorageResult upload(MultipartFile file) {
        validateFile(file);
        String extension = extensionOf(file.getOriginalFilename());
        String mimeType = resolveMimeType(file.getContentType(), extension);
        String resourceType = cloudinaryResourceType(mimeType, extension);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                    "folder", properties.getFolder(),
                    "resource_type", resourceType,
                    "use_filename", true,
                    "unique_filename", true
                )
            );

            return new StorageResult(
                asString(result.get("url")),
                asString(result.get("secure_url")),
                asString(result.get("public_id")),
                file.getOriginalFilename(),
                file.getSize(),
                mimeType
            );
        } catch (IOException exception) {
            throw new StorageException("Could not read uploaded file", exception);
        } catch (RuntimeException exception) {
            throw new StorageException("Cloudinary upload failed", exception);
        }
    }

    @Override
    public StorageFile download(String url) {
        if (url == null || url.isBlank()) {
            throw new StorageException("File URL is missing");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url.trim()))
                .timeout(Duration.ofSeconds(35))
                .GET()
                .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new StorageException("Could not download file from storage");
            }

            String mimeType = response.headers()
                .firstValue("content-type")
                .map(value -> value.split(";")[0].trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .orElse("application/octet-stream");

            return new StorageFile(response.body(), mimeType);
        } catch (IllegalArgumentException exception) {
            throw new StorageException("Invalid storage file URL", exception);
        } catch (IOException exception) {
            throw new StorageException("Could not read file from storage", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new StorageException("File download was interrupted", exception);
        }
    }

    private void validateFile(MultipartFile file) {
        if (!properties.hasCredentials()) {
            throw new StorageException("Cloudinary is not configured");
        }

        if (file == null || file.isEmpty()) {
            throw new StorageException("Uploaded file is empty");
        }

        if (file.getSize() > properties.maxFileSizeBytes()) {
            throw new StorageException("Uploaded file is larger than " + properties.getMaxFileSizeMb() + "MB");
        }

        String extension = extensionOf(file.getOriginalFilename());
        Set<String> allowed = properties.allowedFormatSet();
        if (extension.isBlank() || !allowed.contains(extension)) {
            String label = extension.isBlank() ? "missing" : extension;
            throw new StorageException("Unsupported file extension: " + label);
        }
    }

    private String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }

        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String cloudinaryResourceType(String mimeType, String extension) {
        if (mimeType.startsWith("image/") || isOneOf(extension, "jpg", "jpeg", "png", "webp", "gif", "svg")) {
            return "image";
        }

        if (
            mimeType.startsWith("video/") ||
            mimeType.startsWith("audio/") ||
            isOneOf(extension, "mp3", "wav", "mp4", "webm", "mov")
        ) {
            return "video";
        }

        return "raw";
    }

    private String resolveMimeType(String contentType, String extension) {
        if (contentType != null && !contentType.isBlank() && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            return contentType.toLowerCase(Locale.ROOT);
        }

        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "csv" -> "text/csv";
            case "txt", "md", "log" -> "text/plain";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "yaml", "yml" -> "application/x-yaml";
            case "html" -> "text/html";
            case "css", "scss" -> "text/css";
            case "js", "jsx" -> "text/javascript";
            case "ts", "tsx" -> "text/typescript";
            case "java", "kt", "py", "sql", "sh", "ps1", "c", "cpp", "h", "hpp", "cs", "go", "rs", "php", "rb", "swift", "dart" -> "text/plain";
            case "zip" -> "application/zip";
            case "rar" -> "application/vnd.rar";
            case "7z" -> "application/x-7z-compressed";
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "mov" -> "video/quicktime";
            default -> "application/octet-stream";
        };
    }

    private boolean isOneOf(String value, String... candidates) {
        return List.of(candidates).contains(value);
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}