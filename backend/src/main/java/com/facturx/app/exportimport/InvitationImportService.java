package com.facturx.app.exportimport;

import com.facturx.app.organization.InvitationRepository;
import com.facturx.app.organization.InvitationRequest;
import com.facturx.app.organization.InvitationService;
import com.facturx.app.organization.InvitationStatus;
import com.facturx.app.organization.OrganizationMemberRepository;
import com.facturx.app.organization.Role;
import com.facturx.app.permission.Permission;
import com.facturx.app.permission.PermissionService;

import jakarta.validation.Validator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class InvitationImportService {

    private static final int MAX_ROWS = 500;

    private final PermissionService permissionService;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final InvitationRepository invitationRepository;
    private final InvitationService invitationService;
    private final Validator validator;

    public InvitationImportService(
            PermissionService permissionService,
            OrganizationMemberRepository organizationMemberRepository,
            InvitationRepository invitationRepository,
            InvitationService invitationService,
            Validator validator) {

        this.permissionService = permissionService;
        this.organizationMemberRepository = organizationMemberRepository;
        this.invitationRepository = invitationRepository;
        this.invitationService = invitationService;
        this.validator = validator;
    }

    @Transactional
    public List<InvitationImportResponse> importInvitations(
            MultipartFile file,
            Long userId,
            Long orgId,
            boolean dryRun) {

        permissionService.requirePermission(
                userId, orgId, Permission.IMPORT_INVITATIONS
        );

        // La création utilise également cette permission.
        permissionService.requirePermission(
                userId, orgId, Permission.INVITE_MEMBER
        );

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide");
        }

        List<InvitationImportResponse> result = new ArrayList<>();
        List<InvitationRequest> requests = new ArrayList<>();
        Set<String> seenEmails = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();

        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                BOMInputStream.builder()
                                        .setInputStream(file.getInputStream())
                                        .get(),
                                StandardCharsets.UTF_8
                        )
                )
        ) {
            reader.mark(8192);
            String firstLine = reader.readLine();
            reader.reset();

            char delimiter =
                    firstLine != null && firstLine.contains(";") ? ';' : ',';

            try (
                    CSVParser parser = CSVFormat.DEFAULT.builder()
                            .setDelimiter(delimiter)
                            .setHeader()
                            .setSkipHeaderRecord(true)
                            .setIgnoreHeaderCase(true)
                            .setTrim(true)
                            .get()
                            .parse(reader)
            ) {
                MapHeaders headers = resolveHeaders(parser);
                int rowCount = 0;

                for (CSVRecord record : parser) {
                    rowCount++;

                    if (rowCount > MAX_ROWS) {
                        throw new IllegalArgumentException(
                                "Maximum " + MAX_ROWS + " lignes"
                        );
                    }

                    String email = record.isSet(headers.email())
                            ? record.get(headers.email())
                                    .trim().toLowerCase(Locale.ROOT)
                            : "";

                    String roleText = record.isSet(headers.role())
                            ? record.get(headers.role())
                                    .trim().toUpperCase(Locale.ROOT)
                            : "";

                    Role role = null;
                    String error = null;

                    try {
                        role = Role.valueOf(roleText);
                    } catch (IllegalArgumentException e) {
                        error = "Rôle invalide : ADMIN, ACCOUNTANT ou CLIENT attendu";
                    }

                    InvitationRequest request =
                            new InvitationRequest(email, role);

                    if (error == null
                            && !validator.validate(request).isEmpty()) {
                        error = "Email vide ou invalide";
                    }

                    if (error == null && !seenEmails.add(email)) {
                        error = "Email en double dans le CSV";
                    }

                    if (error == null
                            && organizationMemberRepository
                                    .existsByOrganizationIdAndUserEmailIgnoreCase(
                                            orgId, email
                                    )) {
                        error = "Déjà membre de l'organisation";
                    }

                    if (error == null
                            && invitationRepository
                                    .existsByOrganizationIdAndEmailIgnoreCaseAndStatusAndExpiresAtAfter(
                                            orgId,
                                            email,
                                            InvitationStatus.PENDING,
                                            now
                                    )) {
                        error = "Une invitation active existe déjà";
                    }

                    result.add(new InvitationImportResponse(
                            rowCount + 1,
                            email,
                            roleText,
                            error == null,
                            error
                    ));

                    requests.add(request);
                }
            }
        } catch (UncheckedIOException | IOException e) {
            throw new IllegalArgumentException(
                    "Fichier CSV invalide ou illisible", e
            );
        }

        if (result.isEmpty()) {
            throw new IllegalArgumentException(
                    "Le CSV ne contient aucune donnée"
            );
        }

        boolean hasErrors = result.stream()
                .anyMatch(row -> !row.valid());

        if (dryRun || hasErrors) {
            return result;
        }

        for (InvitationRequest request : requests) {
            invitationService.create(orgId, request, userId);
        }

        return result;
    }

    private record MapHeaders(String email, String role) {}

    private MapHeaders resolveHeaders(CSVParser parser) {
        // Accepte aussi les noms de colonnes de l'export des membres.
        String emailHeader = parser.getHeaderMap().containsKey("email")
                ? "email"
                : "Adresse e-mail";

        String roleHeader = parser.getHeaderMap().containsKey("role")
                ? "role"
                : "Rôle";

        if (!parser.getHeaderMap().containsKey(emailHeader)
                || !parser.getHeaderMap().containsKey(roleHeader)) {
            throw new IllegalArgumentException(
                    "Colonnes obligatoires : email, role"
            );
        }

        return new MapHeaders(emailHeader, roleHeader);
    }
}