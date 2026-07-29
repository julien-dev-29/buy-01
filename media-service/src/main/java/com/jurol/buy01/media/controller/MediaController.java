package com.jurol.buy01.media.controller;

import com.jurol.buy01.common.dto.MediaDTO;
import com.jurol.buy01.media.service.MediaService;
import com.jurol.buy01.media.validation.FileValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "productId", required = false) String productId) {
        String sellerId = (String) authentication.getPrincipal();
        try {
            MediaDTO media = mediaService.uploadFile(file, productId, sellerId);
            return ResponseEntity.status(HttpStatus.CREATED).body(media);
        } catch (FileValidator.ValidationException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getErrorCode(),
                    "message", e.getMessage(),
                    "timestamp", Instant.now().toString()
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "UPLOAD_FAILED",
                    "message", "Failed to upload file: " + e.getMessage(),
                    "timestamp", Instant.now().toString()
            ));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<MediaDTO> getMediaById(@PathVariable String id) {
        return ResponseEntity.ok(mediaService.getMediaById(id));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<MediaDTO>> getMediaByProductId(@PathVariable String productId) {
        return ResponseEntity.ok(mediaService.getMediaByProductId(productId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedia(Authentication authentication, @PathVariable String id) {
        String sellerId = (String) authentication.getPrincipal();
        mediaService.deleteMedia(id, sellerId);
        return ResponseEntity.noContent().build();
    }
}