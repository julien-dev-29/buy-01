package com.jurol.buy01.media.service;

import com.jurol.buy01.common.dto.MediaDTO;
import com.jurol.buy01.media.model.Media;
import com.jurol.buy01.media.repository.MediaRepository;
import com.jurol.buy01.media.validation.FileValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MediaService {

    private final MediaRepository mediaRepository;
    private final FileValidator fileValidator;
    private final String storagePath;

    public MediaService(MediaRepository mediaRepository, FileValidator fileValidator,
                        @Value("${media.storage.path:./media-storage}") String storagePath) {
        this.mediaRepository = mediaRepository;
        this.fileValidator = fileValidator;
        this.storagePath = storagePath;
    }

    public MediaDTO uploadFile(MultipartFile file, String productId, String sellerId) throws IOException, FileValidator.ValidationException {
        fileValidator.validate(file);

        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path sellerDir = Paths.get(storagePath, sellerId);
        Files.createDirectories(sellerDir);
        Path filePath = sellerDir.resolve(filename);
        file.transferTo(filePath.toFile());

        Media media = new Media(
                filename,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                filePath.toString(),
                productId,
                sellerId
        );

        Media saved = mediaRepository.save(media);
        return toDTO(saved);
    }

    public MediaDTO getMediaById(String id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Media not found"));
        return toDTO(media);
    }

    public List<MediaDTO> getMediaByProductId(String productId) {
        return mediaRepository.findByProductId(productId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public void deleteMedia(String id, String sellerId) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Media not found"));

        if (!media.getSellerId().equals(sellerId)) {
            throw new RuntimeException("Not authorized to delete this media");
        }

        try {
            Path filePath = Paths.get(media.getPath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log but don't fail - file might already be deleted
        }

        mediaRepository.deleteById(id);
    }

    public void deleteMediaByProductId(String productId) {
        List<Media> mediaList = mediaRepository.findByProductId(productId);
        for (Media media : mediaList) {
            try {
                Path filePath = Paths.get(media.getPath());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Log but continue
            }
        }
        mediaRepository.deleteByProductId(productId);
    }

    private MediaDTO toDTO(Media media) {
        return new MediaDTO(
                media.getId(),
                media.getFilename(),
                media.getOriginalName(),
                media.getContentType(),
                media.getSize(),
                media.getProductId(),
                media.getSellerId(),
                media.getCreatedAt()
        );
    }
}