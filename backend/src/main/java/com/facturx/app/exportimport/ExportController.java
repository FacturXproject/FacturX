package com.facturx.app.exportimport;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.facturx.app.auth.AppUserPrincipal;



@RestController
@RequestMapping("/api/organizations/{orgId}/export")
public class ExportController {
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
        if (format.equals("csv"))
        {
            String csv = exportService.exportMembersCsv(userId, orgId);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"members.csv\"")
                    .body(csv);
        }
        if (format.equals("json"))
        {
            String json = exportService.transformJson(exportService.exportMembers(userId, orgId));
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"members.json\"")
                    .body(json);
        }
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "format invalid");

    }

    @GetMapping("/documents")
    public ResponseEntity<?>exportDocuments(@AuthenticationPrincipal AppUserPrincipal principal,
        @PathVariable Long orgId,
        @RequestParam(defaultValue = "json") String format)
    {
        Long userId = principal.getUser().getId();
        if (format.equals("csv"))
        {
            String csv = exportService.exportDocumentsCsv(userId, orgId);
            return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"documents.csv\"")
                        .body(csv);
        }
        if (format.equals("json"))
        {
           String json = exportService.transformJson(exportService.exportDocuments(userId, orgId));
           return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                 "attachment; filename=\"documents.json\"")
                            .body(json);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "format invalid");
    }

}
