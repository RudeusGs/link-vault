package com.linkvault.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.linkvault.common.exception.StorageException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CloudinaryStorageService implements StorageService {

    private final Cloudinary cloudinary;
    private final CloudinaryProperties properties;

    public CloudinaryStorageService(Cloudinary cloudinary, CloudinaryProperties properties) {
        this.cloudinary = cloudinary;
        this.properties = properties;
    }

    @Override
    public StorageResult upload(MultipartFile file) {
        validateFile(file);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                    "folder", properties.getFolder(),
                    "resource_type", "auto",
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
                file.getContentType()
            );
        } catch (IOException exception) {
            throw new StorageException("Could not read uploaded file", exception);
        } catch (RuntimeException exception) {
            throw new StorageException("Cloudinary upload failed", exception);
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
            throw new StorageException("Unsupported file extension: " + extension);
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

        return filename.substring(dotIndex + 1).toLowerCase();
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
