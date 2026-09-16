Session de travail : Depot de documents (F06) — front + backend

Branche : feature/documents_yseddiki Module sujet : Systeme de gestion de fichiers

Ce document explique tout ce qui a ete construit et teste pour la feature de depot de documents, pour que l'equipe puisse s'y reperer, l'utiliser et l'integrer a ses propres features (F07, F03).

1. Vue d'ensemble

L'objectif : permettre a un membre autorise d'une organisation de deposer une facture (PDF, XML ou Factur-X), avec verification du fichier, stockage sur volume, et creation d'un enregistrement en base.

Utilisateur -> drag-and-drop / selection -> verification client
           -> POST /api/documents -> verification serveur (taille, type reel)
           -> ecriture sur le volume Docker -> ligne creee en base (status UPLOADED)
2. Backend
Structure des fichiers
backend/src/main/java/com/facturx/app/document/
├── Document.java                  entite JPA, table "documents"
├── DocumentStatus.java             enum des statuts
├── DocumentRepository.java
├── FileValidator.java              verification de la signature reelle du fichier
├── DocumentService.java            logique metier (validation, ecriture/lecture disque)
├── DocumentController.java         les 4 endpoints HTTP
├── DocumentResponse.java           DTO renvoye au front
├── DocumentExceptionHandler.java
├── DocumentNotFoundException.java
├── InvalidFileTypeException.java
└── FileTooLargeException.java
Schema de la table documents
Colonne	Type	Description
id	bigint	Cle primaire
organization_id	bigint	FK vers organizations
owner_id	bigint	FK vers users — qui a depose le fichier
filename	varchar	Nom d'origine
type	varchar	Type MIME (application/pdf, application/xml)
size	bigint	Taille en octets
status	varchar	UPLOADED / QUEUED / PROCESSING / VALID / INVALID / FAILED (contrainte CHECK)
storage_path	varchar	Chemin du fichier sur le volume
uploaded_at	timestamp	Date de depot

Le contenu du fichier n'est jamais stocke en base : seul son chemin (storage_path) l'est. Le fichier physique vit sur un volume Docker dedie (documents-storage, monte sur /app/uploads dans le container backend). Ce choix a ete fait pour respecter les requirements du sujet ("file stored on a volume"), apres une premiere version qui stockait le contenu directement en base (bytea) — abandonnee car non conforme.

Endpoints
Methode	Route	Description
POST	/api/documents?organizationId={id}	Upload d'un fichier (multipart, champ "file")
GET	/api/documents?organizationId={id}	Liste des documents de l'organisation, reponse paginee
GET	/api/documents/{id}	Telechargement du fichier
DELETE	/api/documents/{id}	Suppression (base + fichier physique)

Toutes les routes exigent une session active.

Validation

Deux controles independants avant tout stockage, dans FileValidator.java :

Taille : 10 Mo maximum
Type reel : lecture des premiers octets du fichier (signature %PDF- pour un PDF, <?xml ou < pour un XML) — jamais l'extension ou le nom, qui peuvent etre falsifies.
Erreurs renvoyees
Cas	Code HTTP	Erreur
Type de fichier invalide	415	INVALID_FILE_TYPE
Fichier trop volumineux	413	FILE_TOO_LARGE
Document introuvable	404	DOCUMENT_NOT_FOUND
Pas connecte	401	UNAUTHENTICATED
Tests backend

DocumentFlowTest.java (JUnit + MockMvc), 8 tests : upload PDF valide, upload XML valide, fichier invalide rejete, upload sans session rejete, liste, telechargement puis suppression, plus deux tests ajoutes lors de l'integration avec la feature F07.

Ces tests tournent hors Docker et ont besoin d'un profil de configuration separe pour ecrire dans un dossier temporaire plutot que dans le volume reel :

backend/src/test/resources/application-test.properties :

app.storage.path=/tmp/facturx-test-uploads
bash
cd backend
./mvnw test -Dtest=DocumentFlowTest
3. Frontend
Repartition des responsabilites (convenue avec l'equipe)

La partie front de F06/F07 est partagee :

Cote F06 (cette session) : upload, selection de fichier, progression, erreurs de validation
Cote F07 (avalent2) : page historique/liste des documents, tableau, badges de statut, pagination, page detail
Composant DocumentUploadForm.jsx
frontend/src/components/DocumentUploadForm.jsx

Composant reutilisable, integrable dans n'importe quelle page qui connait un organizationId.

Props

organizationId (obligatoire) : l'organisation dans laquelle deposer le document
onUploaded (optionnel) : callback appele avec les donnees du document apres un upload reussi, utile pour rafraichir une liste ailleurs dans l'app

Fonctionnalites

Zone de drag-and-drop, avec fallback clic pour ouvrir le selecteur de fichiers
Selection multiple de fichiers
Validation cote client avant envoi : taille (10 Mo), type (extension/MIME declare)
Barre de progression individuelle par fichier pendant l'upload (via onUploadProgress d'axios)
Retrait d'un fichier de la liste avant envoi (valide ou en erreur)
Affichage des erreurs, qu'elles viennent du client (validation immediate) ou du serveur (reponse d'echec)
Confirmation visuelle (icone) une fois l'upload termine

Utilisation

jsx
import DocumentUploadForm from '../components/DocumentUploadForm';

<DocumentUploadForm
  organizationId={id}
  onUploaded={(doc) => /* rafraichir une liste, etc. */}
/>

Actuellement integre dans OrganizationMembersPage.jsx, dans un bloc "Deposer un document", accessible a tout membre de l'organisation (pas restreint par role pour l'instant).

Point important sur la validation cote client

La verification cote client (taille, type declare par le navigateur) sert uniquement a donner un retour rapide a l'utilisateur avant l'envoi. Elle n'est pas fiable en soi : un fichier peut avoir un nom et un type MIME corrects sans que son contenu reel corresponde. La securite reelle vient toujours du serveur, qui inspecte les octets du fichier (FileValidator.java). Ce comportement est verifie explicitement par un test dedie (voir plus bas).

Tests frontend

DocumentUploadForm.test.jsx (Vitest + React Testing Library), 20 tests repartis en plusieurs groupes :

Affichage initial

Presence du texte d'invite au depot
Aucune liste de fichiers avant toute selection

Selection de fichiers valides

Ajout d'un PDF valide, d'un XML valide
Selection multiple en une fois
Cumul de plusieurs selections successives

Validation cote client

Rejet d'un fichier trop volumineux (>10 Mo)
Acceptation d'un fichier juste en dessous de la limite
Rejet d'un type de fichier non autorise
Absence du bouton de depot si tous les fichiers sont invalides
Melange de fichiers valides et invalides dans une meme selection

Suppression avant envoi

Retrait d'un fichier valide
Retrait d'un fichier en erreur
Disparition du bouton de depot quand plus aucun fichier n'est valide

Envoi reel au backend

Appel de l'API avec le bon endpoint et le bon organizationId
Declenchement du callback onUploaded avec les donnees recues
Envoi de plusieurs fichiers en parallele
Affichage d'une confirmation visuelle apres succes

Gestion des erreurs serveur

Affichage du message d'erreur renvoye par le serveur
Le scenario cle : un fichier accepte cote client (nom et type declare corrects) mais dont le contenu reel est invalide — verifie que le serveur reste le dernier rempart, meme quand le client se trompe
Message generique en l'absence de reponse structuree (erreur reseau)
Possibilite de retirer un fichier en echec et de reessayer

Lancer les tests :

bash
cd frontend
npx vitest run DocumentUploadForm

Mise en place technique necessaire (le projet n'avait aucun framework de test frontend au depart) :

Installation de vitest, @testing-library/react, @testing-library/jest-dom, @testing-library/dom, jsdom (versions fixees pour compatibilite avec l'environnement Node du projet)
Configuration dans vite.config.js (section test)
Fichier de setup : frontend/src/test-organization/setup.js
4. Docker Compose
yaml
backend:
  volumes:
    - documents-storage:/app/uploads
  depends_on:
    postgres:
      condition: service_healthy
    mailpit:
      condition: service_started

volumes:
  postgres-data:
  documents-storage:

Verifications utiles :

bash
docker volume ls | grep documents-storage
docker exec -it facturx-backend ls -la /app/uploads
docker exec -it facturx-postgres psql -U postgres -d facturx -c "\d documents"
5. Ce qui n'est pas couvert par cette feature
La lecture/interpretation du contenu metier du document (conformite Factur-X, montants, EN 16931) : une autre feature de l'equipe, qui s'appuie sur les fichiers stockes ici via storage_path
Les transitions de status au-dela de UPLOADED
L'affichage de la liste, des badges de statut et la pagination : geres par F07
Les permissions fines sur qui peut deposer/voir/supprimer un document selon son role : a integrer avec la feature Permissions
6. Points de coordination avec l'equipe
organization_id ajoute dans MemberResponse (fix commun avec la feature Permissions)
Endpoint GET /organizations/{id} ajoute pour permettre l'affichage des vrais noms d'organisation
GET /api/documents exige desormais organizationId et renvoie une reponse paginee (modification apportee lors de l'integration avec F07)
Le composant DocumentUploadForm est concu pour etre reutilise tel quel dans la page liste de F07, via sa prop onUploaded