package com.facturx.app.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {
    List<Document> findByOwnerIdOrderByUploadedAtDesc(Long userId);
    List<Document> findByOrganizationIdOrderByUploadedAtDesc(Long organizationId);

    boolean existsByOwnerId(Long ownerId);
    
    Page<Document> findByOrganizationId(Long organizationId, Pageable pageable);
    Page<Document> findByOrganizationIdAndOwnerId(Long organizationId, Long userId, Pageable pageable);


    boolean existsByOrganizationId(Long organizationId);
}
