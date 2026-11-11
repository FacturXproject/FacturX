package com.facturx.app.document;

import com.facturx.app.organization.Organization;
import com.facturx.app.organization.OrganizationRepository;
import com.facturx.app.user.User;
import com.facturx.app.user.UserRepository;
import com.facturx.app.websocket.DocumentStatusChangedEvent;
import com.facturx.app.websocket.DocumentWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import com.facturx.app.permission.Permission;
import com.facturx.app.permission.PermissionService;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PermissionService permissionService;
    private final DocumentWebSocketHandler webSocketHandler;

    @Value("${app.storage.path:/app/uploads}")
    private String storageBasePath;

    public DocumentService(
            DocumentRepository documentRepository,
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            PermissionService permissionService,
            DocumentWebSocketHandler webSocketHandler) {

        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.permissionService = permissionService;
        this.webSocketHandler = webSocketHandler;
    }

    public Document upload(MultipartFile file, Long organizationId, Long ownerId) {
        try {
            byte[] bytes = file.getBytes();

            if (!FileValidator.isWithinSizeLimit(file.getSize())) {
                throw new FileTooLargeException();
            }

            if (!FileValidator.isValidType(bytes)) {
                throw new InvalidFileTypeException();
            }

            Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

            User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            // Nom de fichier unique pour eviter les collisions sur le disque
            String uniqueName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path storageDir = Paths.get(storageBasePath);
            Files.createDirectories(storageDir);
            Path targetPath = storageDir.resolve(uniqueName);
            Files.write(targetPath, bytes);

            Document document = new Document();
            document.setOrganization(organization);
            document.setOwner(owner);
            document.setFilename(file.getOriginalFilename());
            document.setType(file.getContentType());
            document.setSize(file.getSize());
            document.setStoragePath(targetPath.toString());
            document.setStatus(DocumentStatus.UPLOADED);

            Document saved = documentRepository.save(document);
            notifyUploaded(saved);
            return saved;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    // F15 : les autres membres de l'organisation voient le nouveau document sans recharger.
    private void notifyUploaded(Document document) {
        try {
            webSocketHandler.broadcast(new DocumentStatusChangedEvent(
                    "document.status.changed",
                    document.getId(),
                    document.getOrganization().getId(),
                    document.getStatus(),
                    document.getOwner().getId()));
        } catch (Exception e) {
            log.warn("WebSocket: failed to notify upload of document {}", document.getId(), e);
        }
    }

    /**
     * F13 : enregistre un fichier produit par l'application (Factur-X genere) comme un
     * document de l'organisation, au meme titre qu'un depot : il apparait dans la
     * liste des documents, se telecharge et se valide comme les autres.
     */
    public Document storeGenerated(byte[] bytes, String filename, Organization organization, Long ownerId) {
        User owner = userRepository.findById(ownerId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        try {
            Path storageDir = Paths.get(storageBasePath);
            Files.createDirectories(storageDir);
            Path targetPath = storageDir.resolve(UUID.randomUUID() + "_" + filename);
            Files.write(targetPath, bytes);

            Document document = new Document();
            document.setOrganization(organization);
            document.setOwner(owner);
            document.setFilename(filename);
            document.setType("application/pdf");
            document.setSize(bytes.length);
            document.setStoragePath(targetPath.toString());
            document.setStatus(DocumentStatus.UPLOADED);
            return documentRepository.save(document);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public List<Document> getMyDocuments(Long userId) {
        return documentRepository.findByOwnerIdOrderByUploadedAtDesc(userId);
    }

    public Document getDocument(Long documentId) {
        return documentRepository.findById(documentId)
            .orElseThrow(DocumentNotFoundException::new);
    }

    /**
     * Lecture d'un document : tout membre ayant VIEW_ALL_DOCUMENTS dans l'organisation
     * du document, ou son proprietaire s'il a VIEW_OWN_DOCUMENTS.
     */
    public void requireViewPermission(Document document, Long currentUserId) {
        Long organizationId = document.getOrganization().getId();

        if (permissionService.hasPermission(currentUserId, organizationId, Permission.VIEW_ALL_DOCUMENTS)) {
            return;
        }

        boolean isOwner = document.getOwner() != null
            && document.getOwner().getId().equals(currentUserId);

        if (isOwner && permissionService.hasPermission(currentUserId, organizationId, Permission.VIEW_OWN_DOCUMENTS)) {
            return;
        }

        throw new com.facturx.app.permission.AccessDeniedException();
    }

    public byte[] readFileBytes(Document document) {
        try {
            return Files.readAllBytes(Paths.get(document.getStoragePath()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read stored file", e);
        }
    }

    public void deleteDocument(Long documentId,Long currentUserId)
    {
        Document document = getDocument(documentId);

        Long organizationId = document.getOrganization().getId();

        boolean canDeleteAny = permissionService.hasPermission(
            currentUserId,
            organizationId,
            Permission.DELETE_ANY_DOCUMENT
        );

        boolean canDeleteOwnPending = permissionService.hasPermission(
            currentUserId,
            organizationId,
            Permission.DELETE_OWN_PENDING_DOCUMENT
        );

        boolean isOwner =
            document.getOwner() != null
            && document.getOwner().getId().equals(currentUserId);

        boolean isPending =
            document.getStatus() == DocumentStatus.UPLOADED;

        if (!canDeleteAny
                && !(canDeleteOwnPending && isOwner && isPending)) {
            throw new com.facturx.app.permission.AccessDeniedException();
        }

        try {
            Files.deleteIfExists(
                Paths.get(document.getStoragePath())
            );
        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to delete stored file",
                e
            );
        }

        documentRepository.delete(document);
    }
}
