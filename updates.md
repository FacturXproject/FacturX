
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