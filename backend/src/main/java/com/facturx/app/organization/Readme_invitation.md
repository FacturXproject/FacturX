## Invitations (F03)

### Flow
1. An admin sends an invitation for an `email` + `role` within their organization.
2. A unique `token` is generated and the invitation is stored with status `PENDING`.
3. The invited person receives an email with a link containing the token.
4. On accepting (`POST /api/invitations/{token}/accept`), the token is validated
   (not expired, still `PENDING`), and an `OrganizationMember` is created linking
   the existing `User` to the `Organization` with the invited `role`.
5. The invitation status becomes `ACCEPTED`.

### Design decision: the user must already have an account
Accepting an invitation does **not** create a new user account. It only links an
**existing** `User` (matched by email) to the `Organization`. If no account exists
for the invited email, acceptance fails and the person is asked to register first,
then retry the invitation link.

This keeps account creation entirely owned by the auth module (`AuthService`),
avoiding duplicated/conflicting user-creation logic between features.

### Entities
- `Invitation` — `id, organization_id, email, role, token, status, expires_at, createdAt`
- `Role` (enum) — `ADMIN, ACCOUNTANT, CLIENT`, shared across `Invitation` and `OrganizationMember`
- `InvitationStatus` (enum) — `PENDING, ACCEPTED, EXPIRED, REVOKED`

### Architecture

Invitation
  ├── depends on → Organization (via organization_id, ManyToOne)
  ├── depends on → Role (enum, shared with OrganizationMember)
  └── depends on → InvitationStatus (enum, own lifecycle)

Layers:
InvitationController → InvitationService → InvitationRepository → PostgreSQL
                            ↓
                   OrganizationRepository (to resolve orgId → Organization)
                            ↓
                   OrganizationMemberRepository (on accept, creates membership)
                   ### Note: shared `Role` enum

### Note: `organization/` contains multiple features

`organization/` holds both F02 (Organizations, Yanis) and F03 (Invitations,
Ana), grouped by **domain**, not by owner — per the "organized by domain"
convention in this README. Invitations, members, and roles are all part
of the same conceptual area (who belongs to an organization and how),
so they live together, even though each feature is developed independently
on its own branch.