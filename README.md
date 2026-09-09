# Backend — Organizations, Invitations and Permissions

This section documents the backend implementation of features F02, F03 and F04.

These features are closely related:

- F02 manages organizations and their members.
- F03 manages invitations to join an organization.
- F04 manages permissions and role-based access control inside an organization.

---

## F02 — Organizations

An authenticated user can create an organization.

The creator automatically becomes a member of the organization with the `ADMIN` role.

### Organization roles

Each membership has one role:

- `ADMIN`
- `ACCOUNTANT`
- `CLIENT`

The role belongs to the membership, not directly to the user.

This means that the same user can have different roles in different organizations.

Example:

```text
User A
├── Organization Alpha → ADMIN
└── Organization Beta  → ACCOUNTANT
