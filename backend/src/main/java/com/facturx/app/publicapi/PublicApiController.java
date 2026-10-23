package com.facturx.app.publicapi;

import com.facturx.app.apikey.ApiKeyPrincipal;
import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentResponse;
import com.facturx.app.job.JobResponse;
import com.facturx.app.validation.ValidationReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * F17 : API publique, authentifiee par cle API (en-tete X-API-Key), limitee en debit.
 * L'organisation est celle de la cle : aucun organizationId a fournir.
 * Documentation interactive : /api/docs/swagger-ui/index.html
 */
@RestController
@RequestMapping("/api/public/v1/documents")
@Tag(name = "Documents", description = "Dépôt, suivi, rapport de conformité et téléchargement des factures.")
@SecurityRequirement(name = OpenApiConfig.API_KEY_SCHEME)
@ApiResponse(responseCode = "401", description = "Clé API manquante, invalide ou révoquée.")
@ApiResponse(responseCode = "429", description = "Limite de débit de la clé dépassée (voir Retry-After).")
public class PublicApiController {

    private final PublicApiService publicApiService;

    public PublicApiController(PublicApiService publicApiService) {
        this.publicApiService = publicApiService;
    }

    @GetMapping
    @Operation(summary = "Lister les documents",
            description = "Documents de l'organisation de la clé, du plus récent au plus ancien. Scope DOCUMENTS_READ.")
    public PublicPage<DocumentResponse> list(
            @AuthenticationPrincipal ApiKeyPrincipal key,
            @Parameter(description = "Numéro de page, à partir de 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page, 100 maximum") @RequestParam(defaultValue = "20") int size) {

        return publicApiService.listDocuments(key, page, size);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Déposer un document",
            description = "Fichier PDF, XML ou Factur-X de 10 Mo maximum, champ multipart \"file\". Scope DOCUMENTS_WRITE.")
    @ApiResponse(responseCode = "201", description = "Document enregistré (statut UPLOADED).")
    @ApiResponse(responseCode = "413", description = "Fichier trop volumineux.")
    @ApiResponse(responseCode = "415", description = "Ni un PDF ni un XML.")
    public DocumentResponse upload(
            @AuthenticationPrincipal ApiKeyPrincipal key,
            @RequestParam("file") MultipartFile file) {

        return DocumentResponse.from(publicApiService.upload(key, file));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Statut d'un document",
            description = "Métadonnées et statut (UPLOADED, QUEUED, PROCESSING, VALID, INVALID, FAILED). Scope DOCUMENTS_READ.")
    @ApiResponse(responseCode = "404", description = "Document inconnu ou hors de l'organisation de la clé.")
    public DocumentResponse status(
            @AuthenticationPrincipal ApiKeyPrincipal key,
            @PathVariable Long id) {

        return DocumentResponse.from(publicApiService.getDocument(key, id));
    }

    @GetMapping("/{id}/report")
    @Operation(summary = "Rapport de conformité",
            description = "Rapport lisible de la dernière validation Factur-X du document. Scope DOCUMENTS_READ.")
    @ApiResponse(responseCode = "404", description = "Document inconnu, ou aucune validation encore effectuée.")
    public ValidationReport report(
            @AuthenticationPrincipal ApiKeyPrincipal key,
            @PathVariable Long id) {

        return publicApiService.getReport(key, id);
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Télécharger le fichier", description = "Le fichier tel que déposé (ou généré). Scope DOCUMENTS_READ.")
    @ApiResponse(responseCode = "200", description = "Contenu binaire du fichier.")
    public ResponseEntity<ByteArrayResource> download(
            @AuthenticationPrincipal ApiKeyPrincipal key,
            @PathVariable Long id) {

        Document document = publicApiService.getDocument(key, id);
        byte[] bytes = publicApiService.download(document);

        MediaType type;
        try {
            type = MediaType.parseMediaType(document.getType());
        } catch (RuntimeException e) {
            type = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(document.getFilename(), StandardCharsets.UTF_8).build().toString())
                .body(new ByteArrayResource(bytes));
    }

    @PostMapping("/{id}/validate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Lancer la validation Factur-X",
            description = "Crée un job de validation asynchrone ; suivre ensuite le statut du document puis lire son rapport. Scope DOCUMENTS_VALIDATE.")
    @ApiResponse(responseCode = "202", description = "Job de validation mis en file d'attente.")
    @ApiResponse(responseCode = "409", description = "Un traitement est déjà en cours pour ce document.")
    public JobResponse validate(
            @AuthenticationPrincipal ApiKeyPrincipal key,
            @PathVariable Long id) {

        return JobResponse.from(publicApiService.requestValidation(key, id));
    }
}
