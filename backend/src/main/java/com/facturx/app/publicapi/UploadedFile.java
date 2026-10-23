package com.facturx.app.publicapi;

import com.facturx.app.document.FileValidator;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.web.multipart.MultipartFile;

/**
 * Fichier depose via l'API publique, tel qu'il est transmis a DocumentService :
 * - le type MIME est deduit du contenu (signature %PDF- ou XML), pas de ce que le
 *   client HTTP a declare (curl envoie souvent application/octet-stream) ;
 * - le nom est reduit a son dernier segment (pas de "../" dans le nom stocke).
 */
class UploadedFile implements MultipartFile {

    private final String name;
    private final String filename;
    private final byte[] bytes;

    UploadedFile(MultipartFile source) throws IOException {
        this.name = source.getName();
        this.bytes = source.getBytes();
        this.filename = sanitize(source.getOriginalFilename());
    }

    private static String sanitize(String original) {
        if (original == null || original.isBlank()) {
            return "document";
        }
        String normalized = original.replace('\\', '/');
        String last = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        return last.isEmpty() || last.equals("..") || last.equals(".") ? "document" : last;
    }

    @Override
    public String getName() { return name; }

    @Override
    public String getOriginalFilename() { return filename; }

    @Override
    public String getContentType() {
        return FileValidator.isPdf(bytes) ? "application/pdf" : "application/xml";
    }

    @Override
    public boolean isEmpty() { return bytes.length == 0; }

    @Override
    public long getSize() { return bytes.length; }

    @Override
    public byte[] getBytes() { return bytes; }

    @Override
    public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }

    @Override
    public void transferTo(File dest) throws IOException {
        Files.write(dest.toPath(), bytes);
    }

    @Override
    public void transferTo(Path dest) throws IOException {
        Files.write(dest, bytes);
    }
}
