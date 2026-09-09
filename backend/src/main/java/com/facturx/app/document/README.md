# Feature : Depot de documents (F06)

**Auteur** : Yseddiki
**Branche** : `feature/organization_yseddiki`
**Module sujet** : Systeme de gestion de fichiers

---

## 1. Ce que ca permet de faire

Un utilisateur connecte peut deposer des factures au format PDF ou XML dans une organisation dont il est membre. La feature couvre le cycle complet : depot, listing, telechargement, suppression.

### Endpoints

| Action | Endpoint | Detail |
|---|---|---|
| Deposer un fichier | `POST /api/documents?organizationId={id}` | multipart/form-data, champ `file` |
| Lister mes documents | `GET /api/documents` | Documents de l'utilisateur connecte, du plus recent au plus ancien |
| Telecharger un document | `GET /api/documents/{id}` | Renvoie le fichier avec son type MIME et son nom d'origine |
| Supprimer un document | `DELETE /api/documents/{id}` | Supprime la ligne en base ET le fichier physique |

Toutes les routes exigent d'etre connecte (session).

---

## 2. Comment c'est construit

```
document/
├── Document.java                  -> table "documents"
├── DocumentStatus.java             -> enum des statuts possibles
├── DocumentRepository.java
├── FileValidator.java              -> verification du type reel du fichier
├── DocumentService.java            -> logique metier (validation, ecriture/lecture disque)
├── DocumentController.java         -> les 4 endpoints
├── DocumentResponse.java           -> ce qui est renvoye au front
├── DocumentExceptionHandler.java
├── DocumentNotFoundException.java
├── InvalidFileTypeException.java
└── FileTooLargeException.java
```

### Le schema de la table `documents`

| Colonne | Type | Description |
|---|---|---|
| id | bigint | Cle primaire |
| organization_id | bigint | FK vers `organizations` — a quelle orga appartient le document |
| owner_id | bigint | FK vers `users` — qui l'a depose |
| filename | varchar | Nom d'origine du fichier |
| type | varchar | Type MIME (`application/pdf`, `application/xml`) |
| size | bigint | Taille en octets |
| status | varchar | Un des 6 statuts (voir plus bas), contrainte CHECK en base |
| storage_path | varchar | Chemin vers le fichier physique sur le volume |
| uploaded_at | timestamp | Date de depot |

### Les statuts

```java
public enum DocumentStatus {
    UPLOADED, QUEUED, PROCESSING, VALID, INVALID, FAILED
}
```

Un document est cree avec le statut `UPLOADED`. Les transitions suivantes (`QUEUED` -> `PROCESSING` -> `VALID`/`INVALID`/`FAILED`) relevent de la feature de validation de conformite Factur-X, geree ailleurs dans le projet — cette feature ne fait que poser le statut initial.

### Validation du fichier

Deux controles independants avant tout stockage, dans `FileValidator.java` :

- **Taille** : 10 Mo maximum
- **Type** : verification du contenu reel du fichier (les premiers octets — signature `%PDF-` pour un PDF, `<?xml` ou `<` pour un XML), jamais de son extension ou de son nom. Un fichier renomme en `.pdf` qui n'est pas un vrai PDF est rejete.

Cette verification est une validation de **format**, pas de contenu metier : elle ne lit pas et n'interprete pas les donnees de la facture (montants, conformite EN 16931...).

### Stockage sur volume

Le fichier est ecrit physiquement sur un volume Docker, avec un nom unique genere (`UUID + nom original`) pour eviter toute collision entre deux fichiers du meme nom.

```java
// DocumentService.java
String uniqueName = UUID.randomUUID() + "_" + file.getOriginalFilename();
Path targetPath = storageDir.resolve(uniqueName);
Files.write(targetPath, bytes);
document.setStoragePath(targetPath.toString());
```

Seul le **chemin** (`storagePath`) est stocke en base — jamais le contenu binaire.

Le telechargement et la suppression relisent/effacent le fichier a partir de ce chemin :

```java
public byte[] readFileBytes(Document document) {
    return Files.readAllBytes(Paths.get(document.getStoragePath()));
}
```

### Les erreurs renvoyees

| Cas | Code HTTP | Erreur |
|---|---|---|
| Fichier ni PDF ni XML | 415 | `INVALID_FILE_TYPE` |
| Fichier trop volumineux | 413 | `FILE_TOO_LARGE` |
| Document introuvable | 404 | `DOCUMENT_NOT_FOUND` |
| Pas connecte | 401 | `UNAUTHENTICATED` |

---

## 3. Docker Compose

Le fichier physique vit dans un volume Docker dedie, monte dans le container backend.

```yaml
services:
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: facturx-backend
    environment:
      - DB_HOST=postgres
      - DB_USER=${POSTGRES_USER}
      - DB_PASSWORD=${POSTGRES_PASSWORD}
    volumes:
      - documents-storage:/app/uploads
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - facturx-network

volumes:
  postgres-data:
  documents-storage:
```

Le chemin `/app/uploads` a l'interieur du container correspond a la propriete `app.storage.path` configurable dans le backend :

```java
@Value("${app.storage.path:/app/uploads}")
private String storageBasePath;
```

Verifier que le volume est bien cree :
```bash
docker volume ls | grep documents-storage
```

Verifier le contenu du dossier de stockage a l'interieur du container :
```bash
docker exec -it facturx-backend ls -la /app/uploads
```

---

## 4. Deroulement des tests

### Tests automatises (JUnit + MockMvc)

`DocumentFlowTest.java`, 6 scenarios :

1. Upload d'un PDF valide -> succes, statut `UPLOADED`, `organizationId` correct
2. Upload d'un XML valide -> succes
3. Upload d'un fichier invalide (faux PDF) -> rejete en 415
4. Upload sans etre connecte -> rejete en 401
5. Liste des documents -> le fichier depose apparait
6. Telechargement puis suppression -> le document redevient introuvable (404) apres suppression

Ces tests tournent hors Docker, directement sur la machine. Ils ont donc besoin d'un chemin de stockage local different de celui du container :

`src/test/resources/application-test.properties` :
```properties
app.storage.path=/tmp/facturx-test-uploads
```

Et la classe de test active ce profil :
```java
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentFlowTest extends AbstractIntegrationTest {
```

Lancer les tests :
```bash
cd backend
./mvnw test -Dtest=DocumentFlowTest
```

Resultat attendu :
```
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Tests manuels (curl)

Batterie complete utilisee pour valider le flow reel via l'API HTTPS (nginx + backend) :

```bash
# 1. Cookie CSRF + login
curl -c cookies.txt https://localhost:8443/api/healthcheck -k
TOKEN=$(grep XSRF-TOKEN cookies.txt | awk '{print $7}')
curl -X POST https://localhost:8443/api/auth/login -k \
  -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $TOKEN" \
  -c cookies.txt -b cookies.txt \
  -d '{"email": "...", "password": "..."}'
TOKEN=$(grep XSRF-TOKEN cookies.txt | awk '{print $7}')

# 2. Upload PDF valide (necessite un organizationId existant)
curl -X POST "https://localhost:8443/api/documents?organizationId=87" -k \
  -H "X-XSRF-TOKEN: $TOKEN" -b cookies.txt -c cookies.txt \
  -F "file=@/tmp/test.pdf"

# 3. Upload fichier invalide (doit renvoyer 415)
curl -X POST "https://localhost:8443/api/documents?organizationId=87" -k \
  -H "X-XSRF-TOKEN: $TOKEN" -b cookies.txt -c cookies.txt \
  -F "file=@/tmp/fake.pdf"

# 4. Liste
curl "https://localhost:8443/api/documents" -k -H "X-XSRF-TOKEN: $TOKEN" -b cookies.txt -c cookies.txt

# 5. Telechargement (verifie que le fichier recu est un vrai PDF)
curl "https://localhost:8443/api/documents/1" -k -H "X-XSRF-TOKEN: $TOKEN" -b cookies.txt -c cookies.txt -o /tmp/downloaded.pdf
file /tmp/downloaded.pdf

# 6. Suppression puis verification (404 attendu)
curl -X DELETE "https://localhost:8443/api/documents/1" -k -H "X-XSRF-TOKEN: $TOKEN" -b cookies.txt -c cookies.txt
curl "https://localhost:8443/api/documents/1" -k -H "X-XSRF-TOKEN: $TOKEN" -b cookies.txt -c cookies.txt
```

Verification directe sur le volume et en base, en parallele des tests curl :

```bash
# le fichier apparait/disparait bien sur le disque
docker exec -it facturx-backend ls -la /app/uploads

# le schema de la table respecte les requirements F06
docker exec -it facturx-postgres psql -U postgres -d facturx -c "\d documents"
```

### Resultats obtenus

| Test | Resultat |
|---|---|
| Upload PDF valide | 200, `status: UPLOADED` |
| Upload XML valide | 200 |
| Fichier invalide | 415, `INVALID_FILE_TYPE` |
| Fichier trop volumineux | 413, `FILE_TOO_LARGE` |
| Liste | tableau correct |
| Telechargement | fichier recupere, confirme PDF valide par la commande `file` |
| Suppression | fichier disparait du volume ET de la base |
| Acces sans session | 401, `UNAUTHENTICATED` |

---

## 5. Ce qui n'est pas couvert par cette feature

- La lecture/interpretation du contenu metier du document (conformite Factur-X, extraction des montants, validation EN 16931) : c'est une autre feature de l'equipe, qui s'appuie sur les fichiers stockes ici via `storage_path`
- Les transitions de `status` au-dela de `UPLOADED` : posees par la feature de validation
- Les permissions fines (qui a le droit de deposer/voir/supprimer un document selon son role dans l'organisation) : a integrer avec la feature Permissions