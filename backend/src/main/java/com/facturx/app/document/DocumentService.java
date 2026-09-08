package com.facturx.app.document;

import com.facturx.app.user.User;
import com.facturx.app.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    public DocumentService(DocumentRepository documentRepository, UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
    }

    public Document upload(MultipartFile file, Long uploaderId) {
        try {
            byte[] bytes = file.getBytes();

            if (!FileValidator.isWithinSizeLimit(file.getSize())) {
                throw new FileTooLargeException();
            }

            if (!FileValidator.isValidType(bytes)) {
                throw new InvalidFileTypeException();
            }

            User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            Document document = new Document();
            document.setFilename(file.getOriginalFilename());
            document.setContentType(file.getContentType());
            document.setSize(file.getSize());
            document.setData(bytes);
            document.setUploadedBy(uploader);

            return documentRepository.save(document);

        } catch (IOException e) {
            throw new RuntimeException("Failed to read file", e);
        }
    }

    public List<Document> getMyDocuments(Long userId) {
        return documentRepository.findByUploadedByIdOrderByUploadedAtDesc(userId);
    }

    public Document getDocument(Long documentId) {
        return documentRepository.findById(documentId)
            .orElseThrow(DocumentNotFoundException::new);
    }

    public void deleteDocument(Long documentId) {
        Document document = getDocument(documentId);
        documentRepository.delete(document);
    }
}