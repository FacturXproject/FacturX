package com.facturx.app.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByOwnerIdOrderByUploadedAtDesc(Long userId);
    
    Page<Document> findByOrganizationId(Long organizationId, Pageable pageable);
    Page<Document> findByOrganizationIdAndOwnerId(Long organizationId, Long userId, Pageable pageable);
}