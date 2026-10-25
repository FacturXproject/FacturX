package com.facturx.app.exportimport;

import com.facturx.app.permission.Permission;
import com.facturx.app.permission.PermissionService;
import com.facturx.app.organization.OrganizationMember;
import com.facturx.app.organization.OrganizationMemberRepository;
import com.facturx.app.organization.OrganizationNotFoundException;
import com.facturx.app.organization.OrganizationRepository;
import com.facturx.app.document.DocumentRepository;
import com.facturx.app.document.Document;
import com.facturx.app.validation.ValidationReportService;
import com.facturx.app.validation.ValidationReport;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.json.JsonMapper;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;



@Service
public class ExportService {
    private final PermissionService permissionService;
    private final DocumentRepository documentRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final JsonMapper objectMapper;
    private final ValidationReportService validationReportService;
    private final OrganizationRepository organizationRepository;
    private final DocumentPdfExporter documentPdfExporter;

    public ExportService(PermissionService permissionService,
        DocumentRepository documentRepository,
        OrganizationMemberRepository organizationMemberRepository,
        JsonMapper objectMapper,
        ValidationReportService validationReportService,
        OrganizationRepository organizationRepository,
        DocumentPdfExporter documentPdfExporter)
        {
            this.permissionService = permissionService;
            this.documentRepository = documentRepository;
            this.organizationMemberRepository = organizationMemberRepository;
            this.objectMapper = objectMapper;
            this.validationReportService = validationReportService;
            this.organizationRepository = organizationRepository;
            this.documentPdfExporter = documentPdfExporter;
        }

    public String transformJson(List<?> data) {
        return objectMapper.writeValueAsString(data);
    }

    public String transformCsv(List<List<String>> data) {
        StringBuilder result = new StringBuilder();

        for (List<String> row : data) {
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) {
                    result.append(",");
                }

                String value = row.get(i);
                if (value == null) {
                    value = "";
                }

                if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) {
                    value = "'" + value;
                }

                result.append("\"");
                result.append(value.replace("\"", "\"\""));
                result.append("\"");
            }

            result.append("\n");
        }

        return result.toString();
    }

    public List<DocumentExportResponse> exportDocuments(Long userId, Long orgId)
    {
            permissionService.requirePermission(
            userId,
            orgId,
            Permission.EXPORT_DOCUMENTS
        );
        List<Document> documents = documentRepository.findByOrganizationIdOrderByUploadedAtDesc(orgId);

        List <DocumentExportResponse> result = new ArrayList<>();

        for(Document document :documents)
        {
            ValidationReport report = null;
            try{
                report = validationReportService.getReportForDocument(document.getId());
            }catch(ResponseStatusException exception){
                if (!exception.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                    throw exception;
                }
        }
        result.add(DocumentExportResponse.from(document, report));
    }
        return result;
    }

    public List<MemberExportResponse> exportMembers(Long userId, Long orgId)
    {
        permissionService.requirePermission(userId, orgId, Permission.EXPORT_MEMBERS);
        List<OrganizationMember> members = organizationMemberRepository.findByOrganizationId(orgId);

        List <MemberExportResponse> result = new ArrayList<>();
        for (OrganizationMember organizationMember :members)
        {
            MemberExportResponse response = MemberExportResponse.from(organizationMember);
            result.add(response);
        }

        return result;
    }

    public String exportMembersCsv(Long userId, Long orgId)
    {
        List<MemberExportResponse> members = exportMembers(userId, orgId);
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Nom complet", "Adresse e-mail", "Rôle"));
        for(MemberExportResponse member : members){
        rows.add(java.util.Arrays.asList(
            member.name(),
            member.email(),
            member.role().name()
        ));
        }
        return "\uFEFF" + transformCsv(rows);
    }

    public String exportDocumentsCsv(Long userId, Long orgId)
    {
        List<DocumentExportResponse> documents = exportDocuments(userId, orgId);
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of(
            "Identifiant",
            "Organisation",
            "Déposé par",
            "Nom du fichier",
            "Type",
            "Taille (MB)",
            "Statut",
            "Date de dépôt",
            "Adresse e-mail",
            "Conforme",
            "Nombre d’erreurs",
            "Codes des règles"
        ));
        for(DocumentExportResponse doc : documents){
            rows.add(java.util.Arrays.asList(
            java.util.Objects.toString(doc.id(), ""),
            doc.organizationName(),
            doc.uploadedBy(),
            doc.filename(),
            doc.type(),
            String.valueOf(doc.sizeMb()),
            java.util.Objects.toString(doc.status(), ""),
            java.util.Objects.toString(doc.uploadedAt(), ""),
            doc.email(),
            doc.valid(),
            java.util.Objects.toString(doc.errorCount(), ""),
            String.join(";", doc.ruleCodes())


        ));
        }
         return "\uFEFF" + transformCsv(rows);
    }

    public byte[] exportDocumentsPdf(Long userId, Long orgId)
    {
        List<DocumentExportResponse> documents = exportDocuments(userId, orgId);
        String organizationName = organizationRepository.findById(orgId)
            .orElseThrow(OrganizationNotFoundException::new)
            .getName();
        return documentPdfExporter.render(organizationName, documents);
    }

}
