## GDPR / Data retention

Users can export their personal data from the "My Data" page (`GET /api/gdpr/export`).
The JSON export contains the profile, organization memberships with roles, owned
documents (metadata only), API keys (never the secret) and invitations sent to the
user's email.

Users can also delete their account (`POST /api/gdpr/delete`). The user must type
`SUPPRIMER` to confirm; the backend checks the same word and rejects anything else
(`400 INVALID_CONFIRMATION`).

### Deletion rule

Deletion is processed immediately, in a single transaction. There is no intermediate
`deletion_requested` status: no request waits for review, so such a status would never
be visible.

- **Last administrator**: deletion is refused (`409 LAST_ADMIN_REQUIRED`) while the user
  is the only admin of an organization. Another admin must be named first.
- **User without documents**: the user row is physically deleted. Organization
  memberships are removed and API keys are deleted with it (`ON DELETE CASCADE`).
- **User with documents**: the user row is anonymized instead, because documents keep a
  foreign key to their owner. The application:
  - replaces the email with `deleted-<id>@anonymized.local`;
  - replaces the name with "Deleted User";
  - invalidates the password hash;
  - sets the status to `anonymized`, so the account can no longer log in;
  - removes organization memberships (API keys stop working);
  - keeps existing documents and their references.

In both cases every session of the user is invalidated (not only the current one), so
the account cannot be used or restored from another browser.

The anonymized record is kept only to preserve technical and historical references to
existing documents; no personal data is retained in it.

### Known limitation

`generated_invoices.created_by` stores the creator's id as a plain number, without a
foreign key. After a physical deletion it can point to a user id that no longer exists.
Nothing breaks (no constraint, no join on it), but this id must not be treated as a
valid user reference.
