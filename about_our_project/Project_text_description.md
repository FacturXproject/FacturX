# CLAUDE.md — e-facture

> Project context file. Read this before writing any code.
> Written in English because the codebase, commits and README are in English.
> **User-facing text is in French.** See "Language rules".

| | |
|---|---|
| Repository | `ft_transcendence` — 42 final Common Core project |
| Product | **e-facture** |
| Team | Salma, Fedor, Ana, Isabella, Yannis (5) |
| Duration | 6 weeks |
| Subject version | ft_transcendence v21.2 |
| Target score | 14 mandatory + 5 bonus |

---

## 1. What we are building

**e-facture** — a multi-tenant web service that lets French accounting firms and their
clients check, read and convert electronic invoices in the **Factur-X** format.

### The problem

From **1 September 2026**, every French VAT-registered company must be able to
**receive** electronic invoices. Large companies and ETI must also **emit** them from
that date; SMEs, small businesses and micro-enterprises from 1 September 2027.

Factur-X is a hybrid format: a normal PDF with a structured XML file embedded inside
it. A human opens the PDF and sees an invoice; a machine reads the XML and gets
structured data.

Three concrete pains:

1. A small business produces plain PDFs and has no way to turn them into Factur-X.
2. A raw XML invoice arrives and is literally unreadable for a human.
3. Existing validators return `[BR-CO-10] Sum of Invoice line net amount MUST be
   equal...` with an XPath address. Nobody outside the standard understands this.

### The product

- **Vérification** — upload an invoice, get a readable report in French:
  *"Le total HT indiqué est de 1 250,00 €, mais la somme des lignes donne
  1 245,00 € — écart de 5,00 €"*, not a rule code.
- **Lecture** — a raw XML is rendered as a normal, readable invoice.
- **Conversion** — a plain PDF becomes a valid Factur-X. Fields are extracted
  automatically, the user checks and corrects them in a form, then confirms.

Multi-tenancy: an **organisation** is an accounting firm. It invites clients and
collaborators. Clients upload invoices; accountants see them arrive in real time.

### Our differentiator

Open-source validators already exist (Mustangproject). We are **not** competing on
validation itself — we stand on those libraries. Our value is the **translation layer**:
turning Schematron rule codes into plain French diagnostics that name the offending
field, show the actual and expected values, and say how to fix it.

---

## 2. Hard constraints from the subject (non-negotiable)

Violating any of these gets the project **rejected**, not just marked down.

- Web application with **frontend + backend + database**.
- **Git**: commits from every team member, clear messages, visible work distribution.
- **Containerised**, starts with a **single command**.
- Compatible with the latest stable **Google Chrome**.
- **No JavaScript warnings or errors** in the browser console.
- **Privacy Policy** and **Terms of Service** pages, reachable from the footer,
  with real content — not placeholders.
- **Multi-user**: several users active simultaneously, concurrent actions handled
  correctly, real-time updates propagated, no race conditions, no data corruption.
- Frontend clear, responsive, accessible on all devices.
- A **CSS framework** (we use Tailwind).
- Credentials in a `.env` ignored by Git, plus a committed `.env.example`.
- Database with a **clear schema and well-defined relations**.
- User management: sign-up and log-in with **hashed and salted passwords**.
- **All form input validated on both frontend and backend.**
- **HTTPS** for every connection reaching the backend from outside.
- **README in English** with all required sections (see §17).

---

## 3. Architecture

```
                   Browser — React + Tailwind
                              |
                          HTTPS
                              |
                    +---------v---------+
                    |      Nginx        |
                    |  entry point,     |
                    |  TLS termination  |
                    +---------+---------+
                              |
                    +---------v---------+
                    |  backend-core     |
                    |  Java/Spring Boot |
                    |                   |
                    |  - conformity     |
                    |    engine         |
                    |  - rule catalogue |
                    |  - orgs, roles    |
                    |  - documents,jobs |
                    |  - WebSocket      |
                    +----+---------+----+
                         |         |
              REST (internal only) |
                         |         |
        +----------------v--+   +--v----------------+
        | backend-extract   |   |   PostgreSQL      |
        | Python / FastAPI  |   |                   |
        |                   |   +-------------------+
        | - PDF extraction  |
        | - Factur-X        |
        |   generation      |
        |                   |
        | STATELESS:        |
        | no DB, no auth,   |
        | not exposed       |
        +-------------------+
```

### Why Java

The entire EN 16931 tooling ecosystem lives on the JVM: Mustangproject, Saxon,
ph-schematron, veraPDF. This is not a stylistic choice — it is where the tools are.

### Why two services, and how we keep the second one cheap

`backend-core` is the deterministic authority: it validates, owns all business state,
and is the only service that talks to the database or to the outside world.

`backend-extract` does probabilistic work — reading PDFs, guessing fields, writing
files. Python is genuinely better here: `pdfplumber` for parsing and the `factur-x`
library for PDF/A-3 embedding have no equivalent of similar quality on the JVM.

**Critical design rule — keep this service dumb:**

> `backend-extract` has **no database access, no authentication, no session state,
> no migrations, no ORM**. It is not reachable from Nginx. Only `backend-core`
> calls it. It receives a file, returns JSON.

Two endpoints, a few hundred lines:

```
POST /extract    PDF file        ->  { fields, confidence per field }
POST /generate   { fields }      ->  Factur-X file
```

This keeps the cost of the second service near zero while still earning the
*Backend as microservices* module honestly: two loosely-coupled services, a clear
REST interface, one responsibility each.

**The key architectural property:** Python generates, Java validates —
independently. CI fails if a single generated invoice comes back non-conformant.
The generator can never mark its own homework.

---

## 4. Repository structure

```
e-facture/
├── frontend/              React + Vite + Tailwind
├── backend-core/          Spring Boot
├── backend-extract/       FastAPI (stateless)
├── database/              init.sql, migrations
├── nginx/                 config, TLS
├── docs/
│   ├── architecture.md
│   ├── adr/               architecture decision records
│   └── openapi.yaml       contract between services — frozen in week 1
├── samples/               reference invoices from FNFE-MPE
├── docker-compose.yml
├── .env.example
└── .github/workflows/ci.yml
```

---

## 5. Stack and conventions

| Layer | Choice |
|---|---|
| Frontend | React, Vite, Tailwind CSS, react-router |
| Core backend | Java 21, Spring Boot, Hibernate (ORM module) |
| Extraction backend | Python 3.12, FastAPI — no ORM, no DB |
| Database | PostgreSQL |
| Reverse proxy | Nginx, HTTPS |
| CI | GitHub Actions |

### Libraries for the domain

- **Mustangproject** (Java) — Factur-X reading and validation; embeds veraPDF for
  PDF/A checks and ph-schematron for Schematron. Check its licence and record it
  in a `LICENSES` section of the README.
- **veraPDF** — PDF/A-3 conformance.
- **Saxon-HE** — XSLT engine for compiled Schematron.
- **factur-x** (Python, Akretion) — generation and PDF/A-3 embedding.
- **pdfplumber / PyMuPDF** — PDF text-layer parsing for extraction.

### Language rules

- Code, identifiers, comments, commits, README, ADRs: **English**.
- UI labels, error messages, rule explanations, legal pages: **French**.
- The `rule_catalog` table stores French text. Keep it out of the code —
  it is data, not logic.

### Code conventions

- No business logic in controllers.
- Every sensitive route checks permission **server-side**. Hiding a button in the
  UI is not access control.
- Every query touching tenant data filters by `organization_id`. No exceptions.
- Secrets only via environment variables. Never committed.

---

## 6. Domain glossary

Read this before touching validation code.

| Term | Meaning |
|---|---|
| **Factur-X** | Franco-German hybrid invoice standard. Current version 1.09.2 (= ZUGFeRD 2.5.2). A PDF/A-3 with `factur-x.xml` embedded. |
| **EN 16931** | European semantic standard for e-invoicing that Factur-X implements. |
| **CII** | Cross Industry Invoice — the UN/CEFACT XML syntax we support. |
| **UBL** | The other allowed syntax. **Out of scope for us.** |
| **Profile** | Data completeness level: MINIMUM, BASIC WL, BASIC, EN 16931 (COMFORT), EXTENDED. Reference profiles: XRECHNUNG (DE), EXTENDED-CTC-FR (FR). |
| **BT-xxx** | Business Term — a single field, e.g. BT-109 = invoice total without VAT. |
| **BG-xx** | Business Group — a group of related fields. |
| **BR-xx** | Business Rule — a validation rule. `BR-CO-10` = line sums must match the total. |
| **Schematron** | Rule language used to express EN 16931 business rules over XML. |
| **PDF/A-3** | Archival PDF variant that permits embedded attachments. Required by Factur-X. |
| **PDP** | Plateforme de Dématérialisation Partenaire — state-approved platform. **We are not one and never will be.** |
| **Chorus Pro** | Public-sector invoicing portal. **Out of scope.** |

### The four validation layers

Run in this order; stop reporting downstream noise once an upstream layer fails hard.

1. **PDF/A-3 structure** — is it a valid PDF/A-3, is `factur-x.xml` actually embedded,
   is the XMP metadata correct? (veraPDF)
2. **XSD** — is the XML schema-valid CII?
3. **Schematron EN 16931** — the CEN business rules.
4. **Factur-X profile rules** — profile-specific constraints.

Then our own layer: map each failure to a `rule_catalog` entry and produce a French
message with the real values substituted in.

---

## 7. Data model

Core tables, all owned by `backend-core`. Every tenant-scoped table carries
`organization_id`.

```
users               id, email, password_hash, first_name, last_name,
                    created_at, status

organizations       id, name, siren, created_at

organization_       user_id, organization_id, role
  members           role in {admin, accountant, client}

invitations         id, organization_id, email, role, token,
                    status, expires_at

documents           id, organization_id, owner_id, filename, type,
                    size, status, storage_path, created_at
                    status in {uploaded, queued, processing,
                               valid, invalid, failed}

validation_runs     id, document_id, profile_detected, facturx_version,
                    verdict, started_at, finished_at

validation_errors   id, run_id, rule_code, severity, field_bt,
                    actual_value, expected_value
                    severity in {blocking, warning, info}

rule_catalog        code, title_fr, description_fr, correction_hint_fr

draft_invoices      id, document_id, header fields, status
draft_lines         id, draft_id, description, quantity, unit_price, vat_rate
draft_taxes         id, draft_id, rate, base, amount

extracted_fields    id, document_id, field_name, value, confidence

jobs                id, type, document_id, status, attempts,
                    error, started_at, finished_at

api_keys            id, organization_id, key_hash, scopes, revoked_at
                    (bonus scope — F17)
```

Indexes on `organization_id`, `created_at`, `status`, `rule_code`.

---

## 8. Module plan — 14 mandatory

Chosen so that **no point requires work we would not do anyway**. Every module below
is either forced by the subject's mandatory part or is the product itself.

| Module | Type | Pts | Why it is not extra work |
|---|---|---|---|
| Frameworks front + back | Major | 2 | React and Spring Boot are our stack regardless |
| ORM | Minor | 1 | Hibernate ships with Spring Boot |
| Backend as microservices | Major | 2 | Falls out of the architecture |
| Organisation system | Major | 2 | Firm + its clients *is* the product |
| Advanced permissions and roles | Major | 2 | Accountant and client must see different things |
| File upload and management | Minor | 1 | No upload, no product |
| Real-time (WebSocket) | Major | 2 | Subject mandates simultaneous multi-user |
| **Factur-X conformity engine** (module of choice) | Major | 2 | The core of the project |
| | | **14** | |

### Module of choice justification (draft for the README)

The conformity engine qualifies as Major because it implements four chained validation
layers over a formal standard, maintains a rule catalogue mapping EN 16931 rule codes
to human-readable French diagnostics with actual/expected value substitution, and is
verified by mutation testing. It is the substantive engineering contribution of the
project and is not available off the shelf in this form.

### Explicitly NOT taken: *Standard user management* (Major, 2)

It requires a friends system and online status. In a B2B tool for accountants that is
absurd, and the evaluators will ask why it exists. We take *Organisation system* and
*Advanced permissions* instead — same category, more points, coherent with the product.

---

## 9. Module plan — 5 bonus

These can be dropped without breaking anything. Build in this order.

| Module | Type | Pts | Difficulty | Note |
|---|---|---|---|---|
| Public API: key, rate limit, docs, 5+ endpoints | Major | 2 | Medium | Take first — makes sense for a B2B tool and reads best on a CV |
| Advanced search: filters, sort, pagination | Minor | 1 | Low | |
| Export / import | Minor | 1 | Low | |
| GDPR compliance | Minor | 1 | Low | |
| | | **5** | | |

**Reserve, only if week 5 finishes early:** 2FA (Minor, 1).

Bonus is capped at 5 points by the subject. Anything beyond **19 total** is wasted
effort. Do not overbuild.

---

## 10. Feature to module map

The detailed backlog (mini-tasks, definition of done per feature) lives in
**`ft_transcendence_features.md`**. That file is authoritative for task breakdown;
this one is authoritative for architecture, scope and constraints.

| Feature | Module it earns | Pts | Bucket |
|---|---|---|---|
| **F00** Baseline | Frameworks + ORM + microservices | 5 | mandatory 14 |
| **F01** Authentication | — subject's mandatory part | 0 | mandatory 14 |
| **F02** Organisations | Organisation system | 2 | mandatory 14 |
| **F03** Invitations and members | part of Organisation system | ^ | mandatory 14 |
| **F04** Roles and permissions | Advanced permissions | 2 | mandatory 14 |
| **F05** Legal pages + profile | — subject's mandatory part | 0 | mandatory 14 |
| **F06** Document upload | File upload and management | 1 | mandatory 14 |
| **F07** Document list and statuses | — product | 0 | mandatory 14 |
| **F08** Java validation engine | Module of choice, part 1 | 2 | mandatory 14 |
| **F09** Readable report | Module of choice, part 2 | ^ | mandatory 14 |
| **F10** XML invoice reader | — product | 0 | mandatory 14 |
| **F11** PDF extraction (Python) | — product | 0 | mandatory 14 |
| **F12** Human correction form | — product | 0 | mandatory 14 |
| **F13** Factur-X generation | — product | 0 | mandatory 14 |
| **F14** Queue and async jobs | — subject's mandatory part | 0 | mandatory 14 |
| **F15** WebSocket real-time | Real-time features | 2 | mandatory 14 |
| **F17** Public secured API | Public API | 2 | bonus 5 |
| **F16** Advanced search | Advanced search | 1 | bonus 5 |
| **F20** Export / import | Export / import | 1 | bonus 5 |
| **F21** GDPR | GDPR compliance | 1 | bonus 5 |
| **F22** 2FA | 2FA | 1 | reserve |

### Two things to understand from this table

**Six features earn zero points** — F07, F10, F11, F12, F13, F14. That is fine: they
*are* the product. Without them the conformity engine has nothing to be a Major module
about, because there would be nothing to demonstrate.

**F11–F13 carry 2 points indirectly.** Drop extraction and generation and the Python
service has no work left, at which point the microservices module collapses. These are
load-bearing, not nice-to-haves to cut in the last week.

---

## 11. Way of working

```
Baseline -> Features -> Tasks -> Branch -> Pull Request -> green CI -> main
```

- **We do not split by technology.** A feature crosses frontend, backend and
  database, and one person carries it end to end. Nobody "owns" Java or Python.
- **Référent, not owner.** For deep domain areas one person digs first and then
  teaches the others. Knowledge must not stay in one head — the evaluation asks
  *anyone* about *any* part.
- Every change goes through a PR reviewed by at least one other member.
- `main` stays green and deployable at all times.
- Short PRs. A three-day branch is a merge conflict waiting to happen.

---

## 12. Roles

| Member | Role | Main feature blocks |
|---|---|---|
| **Fedor** | Product Owner | Factur-X spike, rule catalogue, readable report (F08, F09) |
| **Ana** | Project Manager | Organisations, invitations, roles, permissions (F02, F03, F04) |
| **Salma** | Technical Lead | Baseline, Docker, CI, OpenAPI contract, validation layers (F00, F08) |
| **Isabella** | Developer | Auth, legal pages, upload, GDPR (F01, F05, F06, F21) |
| **Yannis** | Developer | Extraction, generation, public API, export (F11, F12, F13, F17) |

Roles are hats worn **on top of** development. Every member writes code and defends
it. A PM who only plans will fail the evaluation.

Référents: **Fedor** on Factur-X rules and diagnostics, **Yannis** on extraction
and generation.

---

## 13. Six-week plan

| Week | Milestone | Content |
|---|---|---|
| **1** | M0 — Baseline | Repo, 4 containers, Docker, CI, health end-to-end, OpenAPI contract frozen. **In parallel: Factur-X spike** — one person runs a reference invoice from FNFE-MPE through Mustang on the CLI and reports what the errors actually look like. Throwaway code, permanent knowledge. |
| **2** | M1 — Auth + tenancy | F01, F02, F03, F04, F05 |
| **3** | M2 — Documents + validation | F06, F07, F08 |
| **4** | M3 — Reports + reading | F09, F10, F11 |
| **5** | M4 — Conversion + real-time | F12, F13, F14, F15 — **the mandatory 14 are complete here** |
| **6** | M5 — **Feature freeze Monday** | Bonus features only if already started (F17, F16, F20, F21). Then: tests, README, ADRs, benchmarks, deployment, defence rehearsal |

Week 6 is not padding. The README needs eight mandatory sections including the
database schema, the feature list with per-member attribution, and a justification
for every claimed module. That is several days of real work.

---

## 14. Out of scope

Do not build these. If someone proposes them mid-project, the answer is no.

- Integration with **Chorus Pro** or any PDP — requires certification.
- **UBL** syntax. CII only.
- Generation in profiles other than **BASIC** and **EN 16931**.
- **E-reporting** (transaction data transmission to the tax authority).
- **Chat and friends system.** In a B2B tool for accountants this looks bolted on,
  and the evaluators will ask why it exists.
- Any database, auth or session state inside `backend-extract`.
- Anything that pushes the total meaningfully above 19 points.

---

## 15. Deep reserve — do not start these

**Only relevant if every mandatory feature and every bonus feature is finished and
tested, with time to spare. That is unlikely in six weeks. Assume they will not
happen and plan without them.**

These were in the original backlog and are kept here for the record, so nobody
re-proposes them as new ideas mid-project.

### F18 — RAG conformity assistant (Major, 2)

Explains validation errors in plain French from a controlled corpus of rules.

*Why it is reserve:* the subject requires the RAG system to interact with "a large
dataset of information". A catalogue of a few hundred rules is arguable at best, and a
module that fails the demo scores zero. It is also an LLM component inside a compliance
tool, where determinism is the whole point.

*If it is ever built:* the Java validator remains the single source of truth, the
assistant only explains what the validator already found, and it must answer
"je ne sais pas" when retrieval returns nothing. Never let it invent a rule.

### F19 — Audit log

Traces sensitive actions: upload, validation, generation, deletion, invitation, role
change, API key issuance. Table `audit_logs` with `organization_id`, `actor_id`,
`action`, `target_type`, `target_id`, `metadata`, `created_at`.

*Why it is reserve:* earns **no module at all**. It is good practice for a B2B product
and it would strengthen the GDPR and permissions demos, but it costs real time and
returns zero points.

### F23 — Analytics dashboard (Major, 2)

Per-organisation statistics: document counts, validity rate, most frequent errors,
simple charts.

*Why it is reserve:* it is worth 2 points, but we are already at the 19-point ceiling
with the mandatory 14 plus the 5 bonus. Building it would displace nothing and gain
nothing. Only becomes interesting if one of the planned bonus modules is rejected
during evaluation — and by then it is too late to build.

---

## 16. Numbers to publish in the README

A project without measurements looks like homework. Collect at minimum:

- Field-level extraction accuracy over N invoices, broken down per field
  (numéro, date, SIREN, TVA, total HT, total TTC, lines).
- Round-trip: N generated invoices, 100% pass independent validation.
- Validation latency p50 / p95.
- Rule coverage: how many EN 16931 rules have a French explanation, out of the total.
- Number of mutation tests passing.

### Mutation testing — our test strategy

Take a valid reference invoice, mutate exactly one thing (change a total, delete a
mandatory field, corrupt a SIREN), and assert the validator catches **the specific
rule** that was broken. A few hundred such tests generate automatically from a
handful of reference invoices. Cheap to build, and it is the kind of thing that gets
asked about in interviews.

---

## 17. AI usage policy

The subject requires an honest description in the README of where AI was used, for
which tasks and which parts of the project. It also warns that during evaluation any
member can be asked to explain any piece of code and to make a small live
modification.

Practical rule:

- **Fine:** scaffolding, boilerplate, UI mockups, test fixtures, documentation drafts,
  explaining a library's API.
- **Not fine:** generating the conformity engine or the rule catalogue. That is the
  core we will be defending. Write it, understand it, be able to change it live.
- Anything AI-generated that stays in the repo must be understood by the person who
  merged it. If you cannot explain it in review, it does not merge.

---

## 18. Definition of done for the whole project

- [ ] `docker compose up --build` brings up the full stack
- [ ] CI green on `main`, blocking merges when red
- [ ] Commits from all five members, evenly distributed
- [ ] No console errors or warnings in Chrome
- [ ] Privacy Policy and Terms reachable from the footer, with real content
- [ ] Two users acting concurrently produce no corruption; statuses update live
- [ ] All 8 mandatory-bucket modules demonstrable end to end (14 pts)
- [ ] Bonus modules demonstrable or honestly not claimed
- [ ] README complete in English: description, instructions, resources, AI usage,
      team information, project management, technical stack, database schema,
      features list with attribution, modules with justification and per-member
      attribution, individual contributions
- [ ] Every member can explain every part of the project
