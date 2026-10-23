package com.facturx.app.publicapi;

import java.util.List;
import org.springframework.data.domain.Page;

/** Page de resultats de l'API publique : un format stable, independant de Spring Data. */
public record PublicPage<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PublicPage<T> from(Page<T> page) {
        return new PublicPage<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
