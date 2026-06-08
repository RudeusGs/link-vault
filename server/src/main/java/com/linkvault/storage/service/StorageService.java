package com.linkvault.storage.service;

import com.linkvault.storage.dto.StorageFile;
import com.linkvault.storage.dto.StorageResult;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    StorageResult upload(MultipartFile file);

    StorageFile download(String url);
}