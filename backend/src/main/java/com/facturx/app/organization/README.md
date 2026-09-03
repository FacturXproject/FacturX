Feature : Système d'organisations (F02)


Ce que ça permet de faire

Cette feature gère les cabinets comptables (organisations) et leurs membres : qui appartient à quelle orga, et avec quel rôle (ADMIN, ACCOUNTANT, CLIENT).

Côté API, voici ce qui est possible
Action	Endpoint	Détail
Créer une organisation	POST /api/organizations?name=...	Le créateur devient automatiquement ADMIN
Voir mes organisations	GET /api/organizations	Liste les orgas dont je suis membre
Voir les membres d'une orga	GET /api/organizations/{id}/members	Liste avec email, nom, rôle de chacun
Ajouter un membre	POST /api/organizations/{id}/members?userId=...&role=...	Refuse les doublons
Retirer un membre	DELETE /api/organizations/{id}/members/{userId}	
Renommer une orga	PUT /api/organizations/{id}?name=...	
Supprimer une orga	DELETE /api/organizations/{id}	Supprime aussi tous ses membres

Toutes les routes exigent d'être connecté (session). L'identité vient toujours de la session, jamais d'un paramètre — impossible d'agir au nom de quelqu'un d'autre.

Comment c'est construit (structure du code)
organization/
├── Organization.java              -> table "organizations"
├── OrganizationMember.java        -> table de liaison user <-> orga, avec le role
├── Role.java                      -> enum ADMIN / ACCOUNTANT / CLIENT
├── OrganizationRepository.java
├── OrganizationMemberRepository.java
├── OrganizationService.java       -> toute la logique métier
├── OrganizationController.java    -> les 7 endpoints
├── OrganizationResponse.java      -> ce qui est renvoyé pour une orga
├── MemberResponse.java            -> ce qui est renvoyé pour un membre
├── OrganizationExceptionHandler.java
├── OrganizationNotFoundException.java
├── UserAlreadyMemberException.java
└── MemberNotFoundException.java
Pourquoi une table de liaison (OrganizationMember)

Une organisation peut avoir plusieurs membres, et un même utilisateur peut appartenir à plusieurs organisations avec un rôle différent dans chacune. La table de liaison porte le rôle, pas la table users ni la table organizations.

Les erreurs renvoyées
Cas	Code HTTP	Erreur
Orga introuvable	404	ORGANIZATION_NOT_FOUND
Membre déjà présent	409	USER_ALREADY_MEMBER
Membre introuvable	404	MEMBER_NOT_FOUND
Pas connecté	401	UNAUTHENTICATED
Ce que les autres features peuvent utiliser
Invitations peut appeler OrganizationService.addMember(...) pour transformer une invitation acceptée en membre réel.
Permissions peut lire le role stocké sur OrganizationMember (via OrganizationMemberRepository.findByUserIdAndOrganizationId(...)) pour savoir si un utilisateur a le droit de faire une action dans une orga donnée.
N'importe quelle feature qui a besoin de savoir "cet utilisateur appartient-il à cette organisation ?" peut utiliser ce repository directement.
Tests
Tests manuels (curl) : couvrent création, ajout/retrait de membre, doublon, orga inexistante, suppression en cascade.
Tests automatisés (OrganizationFlowTest.java, 5 tests, JUnit + MockMvc) : mêmes scénarios, exécutés à chaque build et en CI.
bash
cd backend
./mvnw test -Dtest=OrganizationFlowTest
