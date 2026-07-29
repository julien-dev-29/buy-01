package com.jurol.buy01.media.repository;

import com.jurol.buy01.media.model.Media;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaRepository extends MongoRepository<Media, String> {
    List<Media> findByProductId(String productId);

    List<Media> findBySellerId(String sellerId);

    void deleteByProductId(String productId);
}