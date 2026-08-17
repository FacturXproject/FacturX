# UPDATES.md


## 🇫🇷 Version française



### Ce qui existait déjà

La bonne base technique était déjà en place : Java + Spring Boot + PostgreSQL
dans Docker 

 Mais l'authentification elle-même n'existait pas :

- Le mot de passe n'était sauvegardé nulle part (la colonne n'existait même
  pas dans la base de données).
- Il n'y avait ni connexion, ni déconnexion, ni moyen de savoir "qui est
  connecté en ce moment".
- `GET /api/users` renvoyait la liste de tous les utilisateurs sans aucune
  vérification — n'importe qui sur internet pouvait la consulter.

Autrement dit : un "squelette" de projet correct, mais sans serrure sur la
porte.



### Ce qui a été ajouté

**Backend (Java, `backend/`)**

- La table `users` stocke maintenant le mot de passe sous forme de
  **hash** (via BCrypt) — le mot de passe lui-même n'est jamais stocké, seule
  une "empreinte" illisible l'est. Même si quelqu'un vole la base de données,
  il ne peut pas lire les mots de passe.
- 4 routes ont été ajoutées (API) :
  - `POST /api/auth/register` — créer un compte
  - `POST /api/auth/login` — se connecter
  - `POST /api/auth/logout` — se déconnecter
  - `GET /api/auth/me` — "qui suis-je actuellement", utilisé par le site au
    chargement de la page
- La session (ce qui permet au navigateur de se souvenir "cette personne est
  connectée") n'est stockée ni en JavaScript ni dans un cookie contenant un
  jeton (token) : elle est gérée côté serveur via un cookie sécurisé.
  Elle ne peut pas être lue par JavaScript (protection contre le XSS) et peut
  réellement être "révoquée" à la déconnexion (contrairement aux jetons JWT
  à la mode).
- Une protection contre les tentatives de mot de passe a été ajoutée : 5
  échecs de connexion consécutifs bloquent cet email pendant 15 minutes.
- Le message d'erreur est identique pour "mot de passe incorrect" et pour
  "cet email n'existe pas" — afin qu'on ne puisse pas deviner quels emails
  sont réellement enregistrés.
- Auparavant, la liste des utilisateurs (`/api/users`) était accessible à
  tous — désormais, toutes les routes du site exigent d'être connecté par
  défaut, sauf les pages de connexion/inscription elles-mêmes.

**Frontend (React, `frontend/`)**

- La page de connexion interroge maintenant réellement le serveur (avant,
  c'était juste une image de formulaire qui ne vérifiait rien).
- Une page d'inscription a été ajoutée.
- Les messages d'erreur s'affichent en français, comme l'exige le cahier des
  charges (le projet cible des cabinets comptables français) : "Identifiants
  invalides", "Cette adresse email est déjà utilisée", etc.
- Le site retient si la personne est connectée ou non, et bloque l'accès aux
  pages internes (tableau de bord, etc.) si ce n'est pas le cas.

### Comment tout cela a été vérifié

Pas seulement "le code compile" — tout a été réellement exécuté et testé :

1. Le projet entier a été construit via Docker (`docker compose up --build`).
2. Tous les scénarios ont été testés en ligne de commande (curl) :
   inscription, connexion avec bon et mauvais mot de passe, déconnexion,
   blocage après 5 échecs, double inscription avec le même email.
3. La base de données PostgreSQL elle-même a été inspectée — les mots de
   passe y sont bien stockés sous forme de hash, jamais en clair.
4. Des tests automatiques (JUnit) ont été écrits pour rejouer tous ces
   scénarios sur une vraie base de données — ils peuvent être relancés à
   chaque futur changement pour éviter de casser l'authentification par
   accident.




### Ce qui n'a PAS été fait (volontairement, hors périmètre)

- La réinitialisation de mot de passe par email et la vérification d'email
  ne sont pas demandées par le cahier des charges (cela nécessite l'envoi
  d'emails, hors périmètre de F01). La connexion via Google/2FA est aussi une
  tâche séparée.
- Les organisations, les rôles et les droits d'accès (qui est admin, qui est
  client) sont la prochaine tâche F02/F04, pas F01.

---

