
F08 — Invoice Validation (Factur-X conformity check)


What it is: a deterministic engine that checks whether a Factur-X invoice PDF is standard-compliant. You send it a file, it tells you pass/fail plus a structured list of exactly which rules failed and where.

How it works: built on top of Mustangproject (open-source, Apache-2.0), which runs 4 checks in a fixed order, stopping early if an earlier one fails hard:

1. PDF/A-3 — is the XML actually embedded in the PDF, is the metadata correct
2. CII XSD — is the embedded XML schema-valid
3. Schematron EN 16931 — business rules (e.g. VAT totals, mandatory fields)
4. Factur-X profile — constraints for the declared profile (BASIC/EN16931/EXTENDED)

Flow: POST /api/validate → ValidationController → FacturXValidationService → MustangValidationClient (wraps the library) → MustangReportParser (turns the library's raw XML report into our own error list) → result saved to DB (validation_runs + validation_errors) → JSON response.

Note: this endpoint is temporary (no document/permission checks yet) — it'll be replaced by POST /documents/{id}/validate once F06 (document upload) exists.

This update: ported the whole feature (12 classes, tests, samples, docs) from the working branch into last_version/backend. Also carried over a Testcontainers lifecycle fix in AbstractIntegrationTest needed for F08's tests to run cleanly alongside the existing auth tests. Verified: full build + 19/19 tests green.

---------------------------------------------------------------------------------------------------------------------------------------------------------------
---------------------------------------------------------------------------------------------------------------------------------------------------------------
---------------------------------------------------------------------------------------------------------------------------------------------------------------


List of new/Modified files for F08:

New files — (backend/src/main/java/com/facturx/app/validation/):
- ValidationLayer.java — enum of levels (PDF_A3, XSD, SCHEMATRON, PROFILE)
- ValidationSeverity.java — enum of severity (ERROR, WARNING, INFO)
- ValidationError.java — record for a single error
- ValidationResult.java — record for the result of the whole check
- MustangValidationClient.java — wrapper around the Mustangproject library
- MustangReportParser.java — parses the library's raw XML report
- FacturXValidationService.java — main service, orchestrates the whole process
- ValidationRun.java — JPA entity (validation_runs table)
- ValidationErrorEntity.java — JPA entity (validation_errors table)
- RuleCatalog.java — JPA entity (rule_catalog table, still an empty stub)
- ValidationRunRepository.java, ValidationErrorRepository.java, RuleCatalogRepository.java — repositories
- ValidationController.java — HTTP endpoint POST /api/validate

New files — tests (backend/src/test/java/com/facturx/app/validation/):
- MustangValidationClientSmokeTest.java
- FacturXValidationServiceTest.java
- ValidationControllerTest.java

New files — test data:
- backend/src/test/resources/facturx-samples/EN16931_Einfach.pdf (valid sample)
- backend/src/test/resources/facturx-samples/veraPDFtestsuite6-7-11-t01-fail-a.pdf (invalid sample)

Modified shared files:
- backend/pom.xml — added one dependency (Mustangproject)
- backend/src/main/resources/application.properties — added two properties (multipart file size limit)
- backend/src/test/java/com/facturx/app/AbstractIntegrationTest.java — fixed a Testcontainers lifecycle bug (not directly related to F08, but had to be fixed for the tests to run stably at all)
- backend/README.md — added two documentation sections (feature description + API example)



Total: 12 new Java classes + 3 test classes + 2 test PDFs, and targeted (non-breaking) edits.