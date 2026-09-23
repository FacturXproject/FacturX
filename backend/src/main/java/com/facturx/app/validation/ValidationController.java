package com.facturx.app.validation;

import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * temporary entry point for F08, standing in until f06 (document upload) exists.
 * takes a raw file, no document/organisation/permission checks - once f06 lands this
 * ---
 * becomes {@code POST /documents/{id}/validate}, reading the file from storage and
 * checking permissions before delegating here, per CLAUDE.md's request  flow
 */
@RestController
@RequestMapping("/api/validate")
public class ValidationController {

    private final FacturXValidationService validationService;

    public ValidationController(FacturXValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ValidationResult validate(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier est vide."); 
        }
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.pdf";
        try {
            return validationService.validate(file.getBytes(), filename, null);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de lire le fichier.", e);
        }
    }
}
