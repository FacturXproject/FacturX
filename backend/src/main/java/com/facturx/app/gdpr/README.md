## GDPR / Data retention

Users can export their personal data from the "My Data" page.

Users can also request account deletion. A confirmation is required before the deletion request is processed.

When an account is deleted, the user record is anonymized instead of being physically removed from the database.

We use anonymization because some existing documents can still reference the user through database relationships. Deleting the user row completely could break these references or make historical document data inconsistent.

For this reason, the application:
- removes the user's personal information;
- replaces identifying information with anonymous values;
- disables the account so the user can no longer log in;
- removes organization memberships;
- keeps existing documents and their references;
- blocks deletion if the user is the last administrator of an organization.

The anonymized user record is therefore kept only to preserve technical and historical references to existing documents, while the user's personal data is no longer retained.
