package com.facturx.app.exportimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.facturx.app.AbstractIntegrationTest;
import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentRepository;
import com.facturx.app.document.DocumentStatus;
import com.facturx.app.organization.EmailService;
import com.facturx.app.organization.Invitation;
import com.facturx.app.organization.InvitationRepository;
import com.facturx.app.organization.InvitationStatus;
import com.facturx.app.organization.Organization;
import com.facturx.app.organization.OrganizationMember;
import com.facturx.app.organization.OrganizationMemberRepository;
import com.facturx.app.organization.OrganizationService;
import com.facturx.app.organization.Role;
import com.facturx.app.user.User;
import com.facturx.app.user.UserRepository;
import com.facturx.app.validation.ValidationErrorEntity;
import com.facturx.app.validation.ValidationErrorRepository;
import com.facturx.app.validation.ValidationLayer;
import com.facturx.app.validation.ValidationRun;
import com.facturx.app.validation.ValidationRunRepository;
import com.facturx.app.validation.ValidationSeverity;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * F20 - export CSV / JSON / PDF and invitation import from CSV: the export has the
 * right columns and stays inside the organisation, roles are enforced, and an invalid
 * import is refused as a whole without creating anything (no invitation, no email).
 */
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExportImportFlowTest extends AbstractIntegrationTest {

    private static final String BOM = "﻿";
    private static final String DOCUMENTS_CSV_HEADER =
            "\"Identifiant\",\"Organisation\",\"Déposé par\",\"Nom du fichier\",\"Type\","
            + "\"Taille (MB)\",\"Statut\",\"Date de dépôt\",\"Adresse e-mail\",\"Conforme\","
            + "\"Nombre d’erreurs\",\"Codes des règles\"";
    private static final String MEMBERS_CSV_HEADER = "\"Nom complet\",\"Adresse e-mail\",\"Rôle\"";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationService organizationService;
    @Autowired private OrganizationMemberRepository memberRepository;
    @Autowired private InvitationRepository invitationRepository;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private ValidationRunRepository runRepository;
    @Autowired private ValidationErrorRepository errorRepository;

    // CI must not depend on Mailpit: we only check that emails are requested.
    @MockitoBean private EmailService emailService;

    private record Account(User user, Cookie session) {}

    private Account admin;
    private Account accountant;
    private Account client;
    private Account outsider;
    private Organization organization;
    private Organization otherOrganization;

    @BeforeEach
    void setUp() throws Exception {
        admin = register("admin");
        accountant = register("accountant");
        client = register("client");
        outsider = register("outsider");

        organization = organizationService.createOrganization("Cabinet-" + UUID.randomUUID(), admin.user().getId());
        addMember(organization, accountant, Role.ACCOUNTANT);
        addMember(organization, client, Role.CLIENT);
        otherOrganization = organizationService.createOrganization("Autre-" + UUID.randomUUID(), outsider.user().getId());

        document(organization, admin, "facture-001.pdf", "application/pdf", DocumentStatus.VALID);
        Document invalid = document(organization, client, "facture-002.xml", "text/xml", DocumentStatus.INVALID);
        validationRun(invalid, "BR-02", "BR-CO-15");
        // A spreadsheet must not run this as a formula.
        document(organization, client, "=HYPERLINK(\"x\").pdf", "application/pdf", DocumentStatus.UPLOADED);
        // Must never leak into the first organisation's export.
        document(otherOrganization, outsider, "secret-autre-organisation.pdf", "application/pdf", DocumentStatus.VALID);
    }

    // --- export -----------------------------------------------------------------------

    @Test
    void documentsCsvHasTheExpectedColumnsAndOnlyTheOrganisationDocuments() throws Exception {
        MvcResult result = export(admin, "documents", "csv")
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"documents.csv\""))
                .andReturn();

        String csv = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).startsWith(BOM);
        List<String> lines = csv.substring(1).lines().toList();

        assertThat(lines.get(0)).isEqualTo(DOCUMENTS_CSV_HEADER);
        assertThat(lines).hasSize(4);
        assertThat(csv).contains("\"facture-001.pdf\"", "\"facture-002.xml\"", "\"PDF\"", "\"XML\"");
        assertThat(csv).doesNotContain("secret-autre-organisation");

        String invalidLine = lines.stream().filter(line -> line.contains("facture-002.xml")).findFirst().orElseThrow();
        assertThat(invalidLine).contains("\"INVALID\"", "\"Non\"", "\"2\"", "\"BR-02;BR-CO-15\"");
        String notValidated = lines.stream().filter(line -> line.contains("facture-001.pdf")).findFirst().orElseThrow();
        assertThat(notValidated).contains("\"Non validé\"");
    }

    @Test
    void documentsCsvNeutralisesSpreadsheetFormulas() throws Exception {
        String csv = export(admin, "documents", "csv").andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(csv).contains("\"'=HYPERLINK(\"\"x\"\").pdf\"");
    }

    @Test
    void documentsJsonListsTheOrganisationDocuments() throws Exception {
        export(admin, "documents", "json")
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"documents.json\""))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.filename == 'facture-002.xml')].ruleCodes[0]").value("BR-02"))
                .andExpect(jsonPath("$[?(@.filename == 'facture-002.xml')].errorCount").value(2))
                .andExpect(jsonPath("$[?(@.filename == 'facture-002.xml')].valid").value("Non"))
                .andExpect(jsonPath("$[?(@.filename == 'facture-001.pdf')].organizationName")
                        .value(organization.getName()));
    }

    @Test
    void documentsPdfIsAReadableTable() throws Exception {
        MvcResult result = export(admin, "documents", "pdf")
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"documents.pdf\""))
                .andReturn();

        String text = pdfText(result.getResponse().getContentAsByteArray());
        assertThat(text).contains(organization.getName(), "Nom du fichier", "facture-001.pdf",
                "facture-002.xml", "Invalide", "BR-02, BR-CO-15", "3 document(s)");
        assertThat(text).doesNotContain("secret-autre-organisation");
    }

    @Test
    void documentsPdfSpansSeveralPagesAndSurvivesUnusualFilenames() throws Exception {
        for (int i = 0; i < 60; i++) {
            document(organization, admin, "lot-" + i + ".pdf", "application/pdf", DocumentStatus.VALID);
        }
        document(organization, admin, "счёт-фактура 📄.pdf", "application/pdf", DocumentStatus.VALID);

        byte[] pdf = export(admin, "documents", "pdf").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("lot-0.pdf", "lot-59.pdf", "Page 1 / " + document.getNumberOfPages());
        }
    }

    @Test
    void emptyOrganisationExportsOnlyTheHeader() throws Exception {
        Organization empty = organizationService.createOrganization("Vide-" + UUID.randomUUID(), admin.user().getId());

        String csv = mockMvc.perform(get("/api/organizations/{orgId}/export/documents", empty.getId())
                        .cookie(admin.session()).param("format", "csv"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).isEqualTo(BOM + DOCUMENTS_CSV_HEADER + "\n");

        byte[] pdf = mockMvc.perform(get("/api/organizations/{orgId}/export/documents", empty.getId())
                        .cookie(admin.session()).param("format", "pdf"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(pdfText(pdf)).contains("Aucun document");
    }

    @Test
    void membersCsvAndJsonListTheMembersWithTheirRole() throws Exception {
        String csv = export(admin, "members", "csv")
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"members.csv\""))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        List<String> lines = csv.substring(1).lines().toList();
        assertThat(lines.get(0)).isEqualTo(MEMBERS_CSV_HEADER);
        assertThat(lines).hasSize(4);
        assertThat(csv).contains(
                "\"" + accountant.user().getEmail() + "\",\"ACCOUNTANT\"",
                "\"" + client.user().getEmail() + "\",\"CLIENT\"",
                "\"" + admin.user().getEmail() + "\",\"ADMIN\"");
        assertThat(csv).doesNotContain(outsider.user().getEmail());

        export(admin, "members", "json")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.email == '%s')].role", client.user().getEmail()).value("CLIENT"));
    }

    @Test
    void exportRespectsRoles() throws Exception {
        // Accountant: documents yes, members no.
        export(accountant, "documents", "csv").andExpect(status().isOk());
        export(accountant, "documents", "pdf").andExpect(status().isOk());
        export(accountant, "members", "csv").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));

        // Client: nothing.
        export(client, "documents", "json").andExpect(status().isForbidden());
        export(client, "members", "json").andExpect(status().isForbidden());

        // Member of another organisation: nothing.
        export(outsider, "documents", "csv").andExpect(status().isForbidden());
        export(outsider, "members", "csv").andExpect(status().isForbidden());

        // Not logged in.
        mockMvc.perform(get("/api/organizations/{orgId}/export/documents", organization.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownFormatIsRejected() throws Exception {
        export(admin, "documents", "xlsx")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_EXPORT_FORMAT"));
        export(admin, "members", "pdf")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_EXPORT_FORMAT"));
        // The format is case-insensitive.
        export(admin, "documents", "CSV").andExpect(status().isOk());
    }

    // --- import -----------------------------------------------------------------------

    @Test
    void dryRunPreviewsTheFileWithoutCreatingAnything() throws Exception {
        String first = uniqueEmail("preview-a");
        String second = uniqueEmail("preview-b");

        importCsv(admin, "email,role\n" + first + ",CLIENT\n" + second + ",ACCOUNTANT\n", true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].lineNumber").value(2))
                .andExpect(jsonPath("$[0].email").value(first))
                .andExpect(jsonPath("$[0].role").value("CLIENT"))
                .andExpect(jsonPath("$[0].valid").value(true))
                .andExpect(jsonPath("$[1].valid").value(true));

        assertThat(invitationRepository.findByOrganizationId(organization.getId())).isEmpty();
        verify(emailService, never()).sendInvitation(anyString(), anyString(), anyString());
    }

    @Test
    void validImportCreatesTheInvitationsAndSendsTheEmails() throws Exception {
        String first = uniqueEmail("import-a");
        String second = uniqueEmail("import-b");

        importCsv(admin, "email,role\n" + first + ",CLIENT\n" + second + ",ACCOUNTANT\n", false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        List<Invitation> invitations = invitationRepository.findByOrganizationId(organization.getId());
        assertThat(invitations).extracting(Invitation::getEmail).containsExactlyInAnyOrder(first, second);
        assertThat(invitations).allMatch(invitation -> invitation.getStatus() == InvitationStatus.PENDING);
        assertThat(invitations).filteredOn(invitation -> invitation.getEmail().equals(second))
                .extracting(Invitation::getRole).containsExactly(Role.ACCOUNTANT);

        verify(emailService).sendInvitation(eq(first), eq(organization.getName()), anyString());
        verify(emailService).sendInvitation(eq(second), eq(organization.getName()), anyString());
    }

    @Test
    void invalidImportIsRefusedAsAWholeWithoutCorruption() throws Exception {
        String valid = uniqueEmail("valid");

        importCsv(admin, "email,role\n" + valid + ",CLIENT\npas-un-email,CLIENT\n", false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].valid").value(true))
                .andExpect(jsonPath("$[1].lineNumber").value(3))
                .andExpect(jsonPath("$[1].valid").value(false))
                .andExpect(jsonPath("$[1].error").value("Email vide ou invalide"));

        // Not even the valid line was created.
        assertThat(invitationRepository.findByOrganizationId(organization.getId())).isEmpty();
        verify(emailService, never()).sendInvitation(anyString(), anyString(), anyString());
    }

    @Test
    void importReportsEveryKindOfLineError() throws Exception {
        String pending = uniqueEmail("pending");
        pendingInvitation(pending, LocalDateTime.now().plusDays(3));
        String expiredPending = uniqueEmail("expired");
        pendingInvitation(expiredPending, LocalDateTime.now().minusDays(1));
        String duplicate = uniqueEmail("duplicate");

        importCsv(admin, String.join("\n",
                        "email,role",
                        duplicate + ",CLIENT",
                        duplicate.toUpperCase() + ",CLIENT",
                        client.user().getEmail() + ",CLIENT",
                        pending + ",CLIENT",
                        expiredPending + ",CLIENT",
                        uniqueEmail("role") + ",DIRECTEUR",
                        ",CLIENT"), true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].valid").value(true))
                .andExpect(jsonPath("$[1].error").value("Email en double dans le CSV"))
                .andExpect(jsonPath("$[2].error").value("Déjà membre de l'organisation"))
                .andExpect(jsonPath("$[3].error").value("Une invitation est déjà en attente pour cet email"))
                // Same rule as the manual invitation: an expired PENDING one still blocks.
                .andExpect(jsonPath("$[4].error").value("Une invitation est déjà en attente pour cet email"))
                .andExpect(jsonPath("$[5].error").value("Rôle invalide : ADMIN, ACCOUNTANT ou CLIENT attendu"))
                .andExpect(jsonPath("$[6].error").value("Email vide ou invalide"));
    }

    @Test
    void importAcceptsTheMembersExportFormat() throws Exception {
        // Excel (French locale) style: BOM, semicolons, the members export headers,
        // and the role labels shown in the application.
        String first = uniqueEmail("excel-a");
        String second = uniqueEmail("excel-b");
        String csv = BOM + "Nom complet;Adresse e-mail;Rôle\n"
                + "Anne Martin;" + first + ";Comptable\n"
                + "Paul Durand;" + second.toUpperCase() + ";administrateur\n";

        importCsv(admin, csv, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("ACCOUNTANT"))
                .andExpect(jsonPath("$[1].email").value(second))
                .andExpect(jsonPath("$[1].role").value("ADMIN"));

        assertThat(invitationRepository.findByOrganizationId(organization.getId()))
                .extracting(Invitation::getEmail).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void exportedMembersCanBeImportedIntoAnotherOrganisation() throws Exception {
        byte[] exported = export(admin, "members", "csv").andReturn().getResponse().getContentAsByteArray();
        Organization target = organizationService.createOrganization("Cible-" + UUID.randomUUID(), admin.user().getId());

        mockMvc.perform(multipart("/api/organizations/{orgId}/import/invitations", target.getId())
                        .file(new MockMultipartFile("file", "members.csv", "text/csv", exported))
                        .param("dryRun", "true")
                        .cookie(admin.session())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.email == '%s')].error", admin.user().getEmail())
                        .value("Déjà membre de l'organisation"))
                .andExpect(jsonPath("$[?(@.email == '%s')].valid", client.user().getEmail()).value(true));
    }

    @Test
    void unreadableFilesAreRejectedWithAClearMessage() throws Exception {
        importCsv(admin, "", true)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_IMPORT_FILE"))
                .andExpect(jsonPath("$.message").value("Le fichier est vide."));
        importCsv(admin, "mail,profil\na@x.fr,CLIENT\n", true)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Colonnes obligatoires : email et role."));
        importCsv(admin, "email,role\n", true)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le fichier ne contient aucune ligne de données."));
        importCsv(admin, "email,role\n\"a@x.fr,CLIENT\n", true)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Fichier CSV invalide ou illisible."));
        importCsv(admin, "email,email,role\na@x.fr,a@x.fr,CLIENT\n", true)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_IMPORT_FILE"));

        StringBuilder tooLong = new StringBuilder("email,role\n");
        for (int i = 0; i < 501; i++) {
            tooLong.append("user").append(i).append("@x.fr,CLIENT\n");
        }
        importCsv(admin, tooLong.toString(), true)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le fichier dépasse 500 lignes."));

        mockMvc.perform(multipart("/api/organizations/{orgId}/import/invitations", organization.getId())
                        .cookie(admin.session())
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_IMPORT_FILE"));

        assertThat(invitationRepository.findByOrganizationId(organization.getId())).isEmpty();
    }

    @Test
    void onlyAnAdminOfTheOrganisationCanImport() throws Exception {
        String csv = "email,role\n" + uniqueEmail("x") + ",CLIENT\n";

        importCsv(accountant, csv, false).andExpect(status().isForbidden());
        importCsv(client, csv, false).andExpect(status().isForbidden());
        importCsv(outsider, csv, false).andExpect(status().isForbidden());

        mockMvc.perform(multipart("/api/organizations/{orgId}/import/invitations", organization.getId())
                        .file(new MockMultipartFile("file", "x.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        assertThat(invitationRepository.findByOrganizationId(organization.getId())).isEmpty();
        verify(emailService, times(0)).sendInvitation(any(), any(), any());
    }

    @Test
    void importRequiresTheCsrfToken() throws Exception {
        mockMvc.perform(multipart("/api/organizations/{orgId}/import/invitations", organization.getId())
                        .file(new MockMultipartFile("file", "x.csv", "text/csv",
                                ("email,role\n" + uniqueEmail("csrf") + ",CLIENT\n").getBytes(StandardCharsets.UTF_8)))
                        .param("dryRun", "false")
                        .cookie(admin.session()))
                .andExpect(status().isForbidden());

        assertThat(invitationRepository.findByOrganizationId(organization.getId())).isEmpty();
    }

    // --- helpers ----------------------------------------------------------------------

    private ResultActions export(Account account, String what, String format) throws Exception {
        return mockMvc.perform(get("/api/organizations/{orgId}/export/{what}", organization.getId(), what)
                .cookie(account.session())
                .param("format", format));
    }

    private ResultActions importCsv(Account account, String csv, boolean dryRun) throws Exception {
        return mockMvc.perform(multipart("/api/organizations/{orgId}/import/invitations", organization.getId())
                .file(new MockMultipartFile("file", "invitations.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)))
                .param("dryRun", String.valueOf(dryRun))
                .cookie(account.session())
                .with(csrf()));
    }

    private static String pdfText(byte[] pdf) throws Exception {
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private static String uniqueEmail(String label) {
        return label + "-" + UUID.randomUUID() + "@x.fr";
    }

    private Account register(String label) throws Exception {
        String email = uniqueEmail(label);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correcthorsebattery","firstName":"Test","lastName":"%s"}"""
                                .formatted(email, label)))
                .andExpect(status().isCreated())
                .andReturn();
        Cookie session = result.getResponse().getCookie("EFACTURE_SESSION");
        assertThat(session).isNotNull();
        return new Account(userRepository.findByEmail(email).orElseThrow(), session);
    }

    private void addMember(Organization org, Account account, Role role) {
        OrganizationMember member = new OrganizationMember();
        member.setOrganization(org);
        member.setUser(account.user());
        member.setRole(role);
        memberRepository.save(member);
    }

    private Document document(Organization org, Account owner, String filename, String type, DocumentStatus status) {
        Document document = new Document();
        document.setOrganization(org);
        document.setOwner(owner.user());
        document.setFilename(filename);
        document.setType(type);
        document.setSize(52_000);
        document.setStatus(status);
        document.setStoragePath("/nonexistent/" + UUID.randomUUID());
        return documentRepository.save(document);
    }

    private void validationRun(Document document, String... ruleCodes) {
        ValidationRun run = new ValidationRun();
        run.setDocumentId(document.getId());
        run.setFilename(document.getFilename());
        run.setValid(false);
        run.setLayerReached(ValidationLayer.SCHEMATRON);
        run.setFinishedAt(OffsetDateTime.now());
        run = runRepository.save(run);

        for (String ruleCode : ruleCodes) {
            ValidationErrorEntity error = new ValidationErrorEntity();
            error.setRun(run);
            error.setLayer(ValidationLayer.SCHEMATRON);
            error.setSeverity(ValidationSeverity.ERROR);
            error.setRuleCode(ruleCode);
            error.setMessage("Rule " + ruleCode + " failed.");
            errorRepository.save(error);
        }
    }

    private void pendingInvitation(String email, LocalDateTime expiresAt) {
        Invitation invitation = new Invitation();
        invitation.setOrganization(organization);
        invitation.setEmail(email);
        invitation.setRole(Role.CLIENT);
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setExpiresAt(expiresAt);
        invitationRepository.save(invitation);
    }
}
