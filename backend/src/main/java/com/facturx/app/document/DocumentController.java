package com.facturx.app.document;

import com.facturx.app.auth.AppUserPrincipal;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    private Long currentUserId(Authentication authentication) {
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        return principal.getUser().getId();
    }

    // POST /api/documents (multipart/form-data, champ "file")
    @PostMapping
    public DocumentResponse upload(@RequestParam("file") MultipartFile file, Authentication authentication) {
        Document document = documentService.upload(file, currentUserId(authentication));
        return DocumentResponse.from(document);
    }

    // GET /api/documents - mes documents
    @GetMapping
    public List<DocumentResponse> getMyDocuments(Authentication authentication) {
        return documentService.getMyDocuments(currentUserId(authentication))
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    // GET /api/documents/{id} - telecharger le fichier
    @GetMapping("/{id}")
    public ResponseEntity<ByteArrayResource> download(@PathVariable Long id) {
        Document document = documentService.getDocument(id);

        ByteArrayResource resource = new ByteArrayResource(document.getData());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(document.getFilename()).build().toString())
                .body(resource);
    }

    // DELETE /api/documents/{id}
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        documentService.deleteDocument(id);
    }
}