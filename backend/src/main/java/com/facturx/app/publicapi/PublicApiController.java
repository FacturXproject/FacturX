package com.facturx.app.publicapi;

import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentResponse;
import com.facturx.app.validation.ValidationReport;
import com.facturx.app.validation.ValidationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * F17: the public API. Every route is authenticated by an API key (X-API-Key header),
 * rate limited per key, and works inside the single organization the key is bound to -
 * which is why no route takes an organizationId.
 */
@RestController
@RequestMapping(PublicApiSecurityConfig.BASE_PATH + "/documents")
@Tag(name = "Documents", description = "Déposer, contrôler et récupérer les factures d'une organisation.")
@SecurityRequirement(name = OpenApiConfig.API_KEY_SCHEME)
public class PublicApiController {

    private final PublicApiService publicApiService;

    public PublicApiController(PublicApiService publicApiService) {
        this.publicApiService = publicApiService;
    }

    private ApiKeyPrincipal currentKey(Authentication authentication) {
        return (ApiKeyPrincipal) authentication.getPrincipal();
    }

    // GET /api/public/v1/documents
    @GetMapping
    @Operation(summary = "Lister les documents",
            description = "Documents de l'organisation, du plus récent au plus ancien. Scope : documents:read.")
    public PublicDocumentPage list(
            @Parameter(description = "Numéro de page, à partir de 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page, 100 au maximum") @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return PublicDocumentPage.from(publicApiService.list(currentKey(authentication), page, size));
    }

    // POST /api/public/v1/documents (multipart/form-data, champ "file")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Déposer un document",
            description = "PDF ou XML, 10 Mo au maximum. Scope : documents:write.")
    public DocumentResponse upload(@RequestPart("file") MultipartFile file, Authentication authentication) {
        return DocumentResponse.from(publicApiService.upload(currentKey(authentication), file));
    }

    // GET /api/public/v1/documents/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Consulter un document et son statut",
            description = "Statut : UPLOADED, QUEUED, PROCESSING, VALID, INVALID ou FAILED. Scope : documents:read.")
    public DocumentResponse get(@PathVariable Long id, Authentication authentication) {
        return DocumentResponse.from(publicApiService.get(currentKey(authentication), id));
    }

    // PUT /api/public/v1/documents/{id}
    @PutMapping("/{id}")
    @Operation(summary = "Renommer un document", description = "Scope : documents:write.")
    public DocumentResponse rename(@PathVariable Long id,
                                   @Valid @RequestBody DocumentUpdateRequest request,
                                   Authentication authentication) {
        return DocumentResponse.from(
                publicApiService.rename(currentKey(authentication), id, request.filename()));
    }

    // DELETE /api/public/v1/documents/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un document", description = "Scope : documents:write.")
    public void delete(@PathVariable Long id, Authentication authentication) {
        publicApiService.delete(currentKey(authentication), id);
    }

    // POST /api/public/v1/documents/{id}/validate
    @PostMapping("/{id}/validate")
    @Operation(summary = "Lancer le contrôle de conformité Factur-X",
            description = "Met à jour le statut du document (VALID ou INVALID). Scope : documents:write.")
    public ValidationResult validate(@PathVariable Long id, Authentication authentication) {
        return publicApiService.validate(currentKey(authentication), id);
    }

    // GET /api/public/v1/documents/{id}/report
    @GetMapping("/{id}/report")
    @Operation(summary = "Lire le rapport de validation",
            description = "Rapport du contrôle le plus récent ; 404 si le document n'a jamais été contrôlé. "
                    + "Scope : documents:read.")
    public ValidationReport report(@PathVariable Long id, Authentication authentication) {
        return publicApiService.report(currentKey(authentication), id);
    }

    // GET /api/public/v1/documents/{id}/download
    @GetMapping("/{id}/download")
    @Operation(summary = "Télécharger le fichier d'origine", description = "Scope : documents:read.")
    public ResponseEntity<ByteArrayResource> download(@PathVariable Long id, Authentication authentication) {
        Document document = publicApiService.get(currentKey(authentication), id);
        byte[] bytes = publicApiService.content(document);

        MediaType contentType = document.getType() != null
                ? MediaType.parseMediaType(document.getType())
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(document.getFilename()).build().toString())
                .body(new ByteArrayResource(bytes));
    }
}
