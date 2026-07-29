package com.jurol.buy01.media.service;

import com.jurol.buy01.common.dto.MediaDTO;
import com.jurol.buy01.media.model.Media;
import com.jurol.buy01.media.repository.MediaRepository;
import com.jurol.buy01.media.validation.FileValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private FileValidator fileValidator;

    @InjectMocks
    private MediaService mediaService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mediaService, "storagePath", "./test-media-storage");
    }

    @Test
    void getMediaById_shouldReturnMedia() {
        Media media = new Media("test.jpg", "test.jpg", "image/jpeg", 1024L, "/path/test.jpg", "product123", "seller123");
        media.setId("media123");

        when(mediaRepository.findById("media123")).thenReturn(Optional.of(media));

        MediaDTO result = mediaService.getMediaById("media123");

        assertNotNull(result);
        assertEquals("test.jpg", result.getFilename());
    }

    @Test
    void getMediaById_shouldThrowIfNotFound() {
        when(mediaRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> mediaService.getMediaById("nonexistent"));
    }

    @Test
    void deleteMedia_shouldDeleteIfOwner() {
        Media media = new Media("test.jpg", "test.jpg", "image/jpeg", 1024L, "/path/test.jpg", "product123", "seller123");
        media.setId("media123");

        when(mediaRepository.findById("media123")).thenReturn(Optional.of(media));
        doNothing().when(mediaRepository).deleteById("media123");

        mediaService.deleteMedia("media123", "seller123");

        verify(mediaRepository).deleteById("media123");
    }

    @Test
    void deleteMedia_shouldThrowIfNotOwner() {
        Media media = new Media("test.jpg", "test.jpg", "image/jpeg", 1024L, "/path/test.jpg", "product123", "seller123");
        media.setId("media123");

        when(mediaRepository.findById("media123")).thenReturn(Optional.of(media));

        assertThrows(RuntimeException.class, () -> mediaService.deleteMedia("media123", "other-seller"));
    }
}

