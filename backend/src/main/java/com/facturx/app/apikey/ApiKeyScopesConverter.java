package com.facturx.app.apikey;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Stocke les scopes d'une cle dans une seule colonne : "DOCUMENTS_READ,DOCUMENTS_WRITE". */
@Converter
public class ApiKeyScopesConverter implements AttributeConverter<Set<ApiKeyScope>, String> {

    @Override
    public String convertToDatabaseColumn(Set<ApiKeyScope> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return "";
        }
        return scopes.stream()
                .sorted()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }

    @Override
    public Set<ApiKeyScope> convertToEntityAttribute(String column) {
        Set<ApiKeyScope> scopes = EnumSet.noneOf(ApiKeyScope.class);
        if (column == null || column.isBlank()) {
            return scopes;
        }
        Arrays.stream(column.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(ApiKeyScope::valueOf)
                .forEach(scopes::add);
        return scopes;
    }
}
