# F14 — Queue et traitement asynchrone

## Objectif

Cette branche ajoute un système de traitements asynchrones pour les opérations longues de l’application.

Les deux traitements concernés sont :

- la validation Factur-X ;
- la génération Factur-X.

L’objectif est d’éviter que l’utilisateur attende directement la fin du traitement dans la requête HTTP.

Les fonctionnalités existantes restent utilisées : F14 ajoute une couche d’orchestration asynchrone autour des services déjà présents.

---

## Modèle Job

Un modèle `Job` persistant a été ajouté.

Un job contient notamment :

- son type : `VALIDATION` ou `GENERATION` ;
- le document concerné ;
- son statut ;
- son nombre de tentatives ;
- son profil de génération si nécessaire ;
- l’utilisateur ayant demandé la génération ;
- sa date de début ;
- sa date de fin ;
- une éventuelle erreur technique.

Cycle normal :

```text
QUEUED
   ↓
PROCESSING
   ↓
DONE
```

En cas d’erreur technique :

```text
QUEUED
   ↓
PROCESSING
   ↓
FAILED
```

Les jobs sont persistés en base de données. Leur statut reste donc disponible après un redémarrage du backend.

---

## Traitement asynchrone

Le traitement est exécuté en arrière-plan avec Spring `@Async`.

Lorsqu’un job est créé :

```text
Création du job
      ↓
QUEUED
      ↓
Réponse immédiate au frontend
      ↓
Worker asynchrone
      ↓
PROCESSING
      ↓
DONE ou FAILED
```

Le worker réutilise les services métier existants.

Pour une validation :

```text
JobWorker
   ↓
FacturXValidationService
```

Pour une génération :

```text
JobWorker
   ↓
GenerationService
```

La logique métier existante n’est donc pas dupliquée.

---

## Validation et résultat métier

Le statut technique du job est différent du résultat métier de la validation.

Par exemple, si une génération se termine correctement mais produit un document non conforme :

```text
GenerationStatus = INVALID
JobStatus = DONE
```

Le job est `DONE` car le traitement technique s’est terminé correctement.

`FAILED` est réservé aux erreurs techniques empêchant le traitement de terminer.

---

## Retry

Un job en `FAILED` peut être relancé.

Le nombre maximal de tentatives est :

```text
3
```

Exemple dans l’interface :

```text
Échec
Tentative 1/3
[Réessayer]
```

Après la troisième tentative :

```text
Échec
Tentative 3/3
Nombre maximal de tentatives atteint.
```

Le retry est refusé si :

- le job n’est pas en `FAILED` ;
- les 3 tentatives ont déjà été utilisées.

---

## Gestion de la concurrence

Un document ne peut pas avoir plusieurs traitements actifs simultanément.

Si un job du même document est déjà :

```text
QUEUED
```

ou :

```text
PROCESSING
```

la création d’un nouveau job est refusée.

L’API renvoie :

```text
HTTP 409 Conflict
```

avec le message :

```text
Un traitement est déjà en cours pour ce document.
```

Une exception dédiée `JobConflictException` est utilisée pour distinguer ce conflit des autres erreurs.

La création des jobs et le retry sont également synchronisés au niveau du backend afin d’éviter deux opérations concurrentes dans la même instance.

---

## API F14

### Créer un job de validation

```http
POST /api/jobs/validation?documentId={documentId}
```

### Créer un job de génération

```http
POST /api/jobs/generation?documentId={documentId}
```

Body :

```json
{
  "profile": "EN16931"
}
```

### Consulter un job

```http
GET /api/jobs/{jobId}
```

### Relancer un job en échec

```http
POST /api/jobs/{jobId}/retry
```

---

## Permissions

Les permissions existantes sont conservées.

Pour la validation :

```text
VALIDATE_DOCUMENT
```

Pour la génération :

```text
GENERATE_DOCUMENT
```

Les permissions sont également vérifiées lors de la consultation et du retry des jobs.

---

# Intégration frontend

## 1. Page « Vérifier »

L’utilisateur ne clique pas sur un bouton spécifique pour lancer la validation après l’upload.

Il choisit ou dépose simplement son fichier :

```text
Choisir / déposer un fichier
        ↓
Upload du document
        ↓
Création automatique d’un job VALIDATION
        ↓
QUEUED
        ↓
PROCESSING
        ↓
DONE / FAILED
```

Quand le job est terminé avec succès :

```text
DONE
 ↓
Navigation vers le rapport de validation
```

En cas d’échec, l’utilisateur peut utiliser le bouton :

```text
Réessayer
```

---

## 2. Page « Détails du document »

Pour un document déjà présent dans l’application, l’utilisateur dispose du bouton :

```text
Lancer la validation Factur-X
```

Le flow devient :

```text
Lancer la validation Factur-X
        ↓
Création d’un job VALIDATION
        ↓
QUEUED
        ↓
PROCESSING
        ↓
DONE / FAILED
```

Lorsque le job atteint `DONE`, le document et son rapport de validation sont rechargés automatiquement.

---

## 3. Page « Conversion — vérification des données »

Après extraction et correction des données, l’utilisateur dispose du bouton :

```text
Générer la facture Factur-X
```

Le flow est :

```text
Générer la facture Factur-X
        ↓
Création d’un job GENERATION
        ↓
QUEUED
        ↓
PROCESSING
        ↓
DONE / FAILED
```

Pendant le traitement :

- le bouton de génération est désactivé ;
- le profil ne peut pas être modifié ;
- le statut du job est affiché.

Après `DONE`, l’historique des générations est rechargé.

L’utilisateur peut ensuite :

- télécharger la facture Factur-X générée ;
- consulter son rapport de validation.

---

## JobStatusPanel

Un composant commun `JobStatusPanel` gère l’affichage des jobs.

Il affiche notamment :

```text
En attente
Traitement en cours
Échec
```

Le frontend interroge périodiquement :

```http
GET /api/jobs/{jobId}
```

jusqu’à ce que le job atteigne :

```text
DONE
```

ou :

```text
FAILED
```

En cas d’erreur réseau temporaire lors du polling, une nouvelle tentative est effectuée automatiquement.

Les erreurs techniques internes du backend restent stockées en base mais ne sont pas affichées directement à l’utilisateur.

---

## Base de données

Les jobs sont persistés dans PostgreSQL.

Le champ :

```text
error
```

est stocké en `TEXT` afin de pouvoir conserver des messages techniques longs.

Un index est également déclaré sur :

```text
jobs.status
```

Le projet utilise :

```properties
spring.jpa.hibernate.ddl-auto=update
```

Hibernate applique donc les évolutions du schéma au démarrage.

---

## Tests backend

Les tests F14 couvrent notamment :

- validation réussie ;
- passage du job à `DONE` ;
- stockage d’une erreur lors d’un `FAILED` ;
- retry après erreur ;
- limite à 3 tentatives ;
- refus d’un deuxième job actif sur le même document ;
- permissions de validation ;
- génération réussie ;
- génération échouée ;
- génération `INVALID` considérée comme un job `DONE` ;
- permissions de génération.

Commande utilisée :

```bash
./mvnw -Dtest=JobFlowTest test
```

Résultat final :

```text
10 tests
0 failure
0 error
BUILD SUCCESS
```

---

## Vérifications manuelles

Les principaux parcours ont également été testés depuis l’interface :

```text
Upload → validation → rapport       ✅
Validation depuis Document Detail   ✅
Génération Factur-X                 ✅
Retry                               ✅
Limite à 3 tentatives               ✅
Concurrence sur le même document    ✅
HTTP 409 en cas de conflit          ✅
Persistance après restart backend   ✅
Conversion existante                ✅
Téléchargement Factur-X             ✅
```

---

## Résultat

F14 permet maintenant d’exécuter la validation et la génération Factur-X en arrière-plan avec :

- des statuts persistants ;
- un suivi frontend par polling ;
- un retry limité ;
- une gestion des erreurs ;
- une protection contre les traitements concurrents sur le même document ;
- le respect des permissions existantes ;
- la réutilisation des services métier existants.

Les flows précédents restent fonctionnels avec cette nouvelle orchestration asynchrone.
