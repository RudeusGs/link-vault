package com.linkvault.resources.util;

import com.linkvault.common.exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class FileValidationUtil {

    private static final Map<String, byte[][]> MAGIC_NUMBERS = new HashMap<>();

    static {
        // JPEG
        MAGIC_NUMBERS.put("image/jpeg", new byte[][]{
            {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        });
        // PNG
        MAGIC_NUMBERS.put("image/png", new byte[][]{
            {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        });
        // GIF
        MAGIC_NUMBERS.put("image/gif", new byte[][]{
            {'G', 'I', 'F', '8', '7', 'a'},
            {'G', 'I', 'F', '8', '9', 'a'}
        });
        // PDF
        MAGIC_NUMBERS.put("application/pdf", new byte[][]{
            {0x25, 0x50, 0x44, 0x46} // %PDF
        });
        // ZIP based (docx, xlsx, pptx, zip)
        byte[] zipMagic = {0x50, 0x4B, 0x03, 0x04};
        MAGIC_NUMBERS.put("application/zip", new byte[][]{zipMagic});
        MAGIC_NUMBERS.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", new byte[][]{zipMagic});
        MAGIC_NUMBERS.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[][]{zipMagic});
        MAGIC_NUMBERS.put("application/vnd.openxmlformats-officedocument.presentationml.presentation", new byte[][]{zipMagic});
    }

    public static void validateMagicNumber(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            return; // Unknown content type, skip magic number validation
        }
        
        byte[][] expectedMagics = MAGIC_NUMBERS.get(contentType.toLowerCase());
        if (expectedMagics == null) {
            return; // Not a format we validate strictly
        }

        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8]; // Read up to 8 bytes
            int read = is.read(header);
            if (read < 3) {
                throw new BadRequestException("File is too small to be valid");
            }

            boolean match = false;
            for (byte[] expected : expectedMagics) {
                if (read >= expected.length) {
                    boolean currentMatch = true;
                    for (int i = 0; i < expected.length; i++) {
                        if (header[i] != expected[i]) {
                            currentMatch = false;
                            break;
                        }
                    }
                    if (currentMatch) {
                        match = true;
                        break;
                    }
                }
            }

            if (!match) {
                throw new BadRequestException("File content does not match its MIME type");
            }
        } catch (IOException e) {
            throw new BadRequestException("Could not read file for validation");
        }
    }
}
