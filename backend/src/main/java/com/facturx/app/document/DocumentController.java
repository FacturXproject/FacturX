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
    private final InvoiceViewService invoiceViewService;//F10

    public DocumentController(DocumentService documentService, InvoiceViewService invoiceViewService)
    {
        this.documentService = documentService;
        this.invoiceViewService = invoiceViewService;
    }//F10

    private Long currentUserId(Authentication authentication) {
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        return principal.getUser().getId();
    }

    // POST /api/documents?organizationId=... (multipart/form-data, champ "file")
    @PostMapping
    public DocumentResponse upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam Long organizationId,
                                    Authentication authentication) {
        Document document = documentService.upload(file, organizationId, currentUserId(authentication));
        return DocumentResponse.from(document);
    }

    // GET /api/documents/mine - mes documents
    @GetMapping("/mine")
    public List<DocumentResponse> getMyDocuments(Authentication authentication) {
        return documentService.getMyDocuments(currentUserId(authentication))
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    // GET /api/documents/{id} - telecharger le fichier
    @GetMapping("/{id}/download")
    public ResponseEntity<ByteArrayResource> download(@PathVariable Long id) {
        Document document = documentService.getDocument(id);
        byte[] bytes = documentService.readFileBytes(document);

        ByteArrayResource resource = new ByteArrayResource(bytes);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(document.getFilename()).build().toString())
                .body(resource);
    }

    //GET /api/documents/{id}/invoice-view   F10
    @GetMapping("/{id}/invoice-view")
    public InvoiceViewResponse getInvoiceView(@PathVariable Long id) {
        return invoiceViewService.getInvoiceView(id);
    }

    // DELETE /api/documents/{id}
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        documentService.deleteDocument(id);
    }
}
