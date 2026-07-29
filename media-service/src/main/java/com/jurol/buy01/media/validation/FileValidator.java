package com.jurol.buy01.media.validation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class FileValidator {

    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47};
    private static final byte[] GIF_MAGIC_87 = "GIF87a".getBytes();
    private static final byte[] GIF_MAGIC_89 = "GIF89a".getBytes();
    private static final byte[] WEBP_MAGIC_RIFF = "RIFF".getBytes();
    private static final byte[] WEBP_MAGIC_WEBP = "WEBP".getBytes();

    private final long maxSize;
    private final List<String> allowedTypes;

    public FileValidator(
            @Value("${media.allowed-types:image/jpeg,image/png,image/gif,image/webp}") String allowedTypesStr,
            @Value("${media.max-size:2097152}") long maxSize) {
        this.maxSize = maxSize;
        this.allowedTypes = Arrays.asList(allowedTypesStr.split(","));
    }

    public void validate(MultipartFile file) throws IOException, ValidationException {
        if (file.isEmpty()) {
            throw new ValidationException("FILE_EMPTY", "Uploaded file is empty");
        }

        if (file.getSize() > maxSize) {
            throw new ValidationException("FILE_TOO_LARGE",
                    String.format("File exceeds %d bytes limit", maxSize));
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new ValidationException("INVALID_FILE_TYPE",
                    "Only JPEG, PNG, GIF, WebP images allowed. Got: " + contentType);
        }

        byte[] header = file.getBytes();
        if (header.length < 12) {
            throw new ValidationException("INVALID_FILE", "File is too small to be a valid image");
        }

        if (!matchesMagicBytes(header, contentType)) {
            throw new ValidationException("INVALID_FILE",
                    "File content does not match declared type: " + contentType);
        }
    }

    private boolean matchesMagicBytes(byte[] header, String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> startsWith(header, JPEG_MAGIC);
            case "image/png" -> startsWith(header, PNG_MAGIC);
            case "image/gif" -> startsWith(header, GIF_MAGIC_87) || startsWith(header, GIF_MAGIC_89);
            case "image/webp" -> startsWith(header, WEBP_MAGIC_RIFF) && startsWith(header, 8, WEBP_MAGIC_WEBP);
            default -> false;
        };
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        return startsWith(data, 0, prefix);
    }

    private boolean startsWith(byte[] data, int offset, byte[] prefix) {
        if (data.length - offset < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[offset + i] != prefix[i]) return false;
        }
        return true;
    }

    public static class ValidationException extends Exception {
        private final String errorCode;

        public ValidationException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
}

