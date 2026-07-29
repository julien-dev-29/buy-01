package com.jurol.buy01.media.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileValidatorTest {

    private FileValidator fileValidator;

    @BeforeEach
    void setUp() {
        fileValidator = new FileValidator("image/jpeg,image/png,image/gif,image/webp", 2097152);
    }

    @Test
    void validate_shouldAcceptValidJpeg() throws Exception {
        byte[] jpegContent = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", jpegContent);

        assertDoesNotThrow(() -> fileValidator.validate(file));
    }

    @Test
    void validate_shouldAcceptValidPng() throws Exception {
        byte[] pngContent = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", pngContent);

        assertDoesNotThrow(() -> fileValidator.validate(file));
    }

    @Test
    void validate_shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);

        assertThrows(FileValidator.ValidationException.class, () -> fileValidator.validate(file));
    }

    @Test
    void validate_shouldRejectOversizedFile() {
        byte[] largeContent = new byte[2097153]; // 2MB + 1 byte
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", largeContent);

        assertThrows(FileValidator.ValidationException.class, () -> fileValidator.validate(file));
    }

    @Test
    void validate_shouldRejectInvalidContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46});

        assertThrows(FileValidator.ValidationException.class, () -> fileValidator.validate(file));
    }

    @Test
    void validate_shouldRejectMismatchedMagicBytes() {
        byte[] content = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);

        assertThrows(FileValidator.ValidationException.class, () -> fileValidator.validate(file));
    }
}