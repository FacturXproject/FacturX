package com.facturx.app.document;

import com.facturx.app.organization.Organization;
import com.facturx.app.organization.OrganizationRepository;
import com.facturx.app.user.User;
import com.facturx.app.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    @Value("${app.storage.path:/app/uploads}")
    private String storageBasePath;

    public DocumentService(DocumentRepository documentRepository,
                            UserRepository userRepository,
                            OrganizationRepository organizationRepository) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
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

    public byte[] readFileBytes(Document document) {
        try {
            return Files.readAllBytes(Paths.get(document.getStoragePath()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read stored file", e);
        }
    }

    public void deleteDocument(Long documentId) {
        Document document = getDocument(documentId);
        try {
            Files.deleteIfExists(Paths.get(document.getStoragePath()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete stored file", e);
        }
        documentRepository.delete(document);
    }
}