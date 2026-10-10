package com.facturx.app.publicapi;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByKeyHash(String keyHash);

    List<ApiKey> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<ApiKey> findByIdAndUserId(Long id, Long userId);

    // Single UPDATE rather than load + save: this runs on every public API request.
    @Modifying
    @Transactional
    @Query("update ApiKey k set k.lastUsedAt = :now where k.id = :id")
    void touchLastUsedAt(@Param("id") Long id, @Param("now") Instant now);
}
