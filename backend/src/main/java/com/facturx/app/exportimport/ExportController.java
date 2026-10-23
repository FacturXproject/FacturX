package com.facturx.app.exportimport;

import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.facturx.app.auth.AppUserPrincipal;



@RestController
@RequestMapping("/api/organizations/{orgId}/export")
public class ExportController {
    private static final MediaType CSV = MediaType.parseMediaType("text/csv;charset=UTF-8");

    private final ExportService exportService;

    public  ExportController(ExportService exportService)
    {
        this.exportService = exportService;
    }

    @GetMapping("/members")
    public ResponseEntity<?> exportMembers(@AuthenticationPrincipal AppUserPrincipal principal,
        @PathVariable Long orgId,
        @RequestParam(defaultValue = "json") String format)
    {
        Long userId = principal.getUser().getId();
        switch (format.toLowerCase(Locale.ROOT))
        {
            case "csv":
                return file(CSV, "members.csv", exportService.exportMembersCsv(userId, orgId));
            case "json":
                return file(MediaType.APPLICATION_JSON, "members.json",
                    exportService.transformJson(exportService.exportMembers(userId, orgId)));
            default:
                throw new InvalidExportFormatException("csv, json");
        }
    }

    @GetMapping("/documents")
    public ResponseEntity<?>exportDocuments(@AuthenticationPrincipal AppUserPrincipal principal,
        @PathVariable Long orgId,
        @RequestParam(defaultValue = "json") String format)
    {
        Long userId = principal.getUser().getId();
        switch (format.toLowerCase(Locale.ROOT))
        {
            case "csv":
                return file(CSV, "documents.csv", exportService.exportDocumentsCsv(userId, orgId));
            case "json":
                return file(MediaType.APPLICATION_JSON, "documents.json",
                    exportService.transformJson(exportService.exportDocuments(userId, orgId)));
            case "pdf":
                return file(MediaType.APPLICATION_PDF, "documents.pdf",
                    exportService.exportDocumentsPdf(userId, orgId));
            default:
                throw new InvalidExportFormatException("csv, json, pdf");
        }
    }

    private static ResponseEntity<Object> file(MediaType type, String filename, Object body)
    {
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(body);
    }

}
