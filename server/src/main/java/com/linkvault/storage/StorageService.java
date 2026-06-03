package com.linkvault.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    StorageResult upload(MultipartFile file);
}
