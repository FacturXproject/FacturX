Ce que ça permet de faire

Interface complète pour gérer les organisations (cabinets comptables) dont l'utilisateur est membre ou administrateur.

Pages
Page	Route	Rôle
Liste des organisations	/organisations	Voir toutes ses organisations, créer, renommer, supprimer
Détail d'une organisation	/organisations/{id}	Voir les membres, retirer un membre, inviter un membre
OrganizationsPage.jsx
Fonctionnalités
Stats en haut : nombre total d'organisations, dont membre, dont administrateur (calculées côté client depuis la réponse de l'API, aucun appel réseau supplémentaire)
Filtres par onglet : Toutes / Membre / Administrateur
Recherche : filtre en direct sur le nom de l'organisation
Création : modal avec formulaire (nom), appelle POST /organizations
Renommage : menu contextuel (bouton "⋮") → modal avec le nom pré-rempli, appelle PUT /organizations/{id}
Suppression : menu contextuel → confirmation → DELETE /organizations/{id}
Navigation : bouton "Voir" redirige vers la page détail avec le bon id d'organisation
Nom réel affiché : un appel GET /organizations/{id} est fait pour chaque organisation de la liste afin de récupérer et afficher son vrai nom (au lieu d'un simple numéro)
Point d'attention technique

GET /organizations renvoie des objets qui représentent l'appartenance de l'utilisateur (son rôle, la date d'adhésion...), pas l'organisation elle-même. Le champ organizationId de la réponse est celui qui identifie réellement l'organisation ; il ne faut jamais utiliser l'id de premier niveau pour naviguer ou agir sur l'organisation, sous peine de cibler la mauvaise ressource.

Actions désactivées selon le rôle

Les actions "Renommer" et "Supprimer" sont désactivées dans le menu si le rôle de l'utilisateur sur cette organisation n'est pas Administrateur. Cette vérification est un premier filtre côté affichage ; la vérification de sécurité réelle est faite côté serveur.

OrganizationMembersPage.jsx
Fonctionnalités
En-tête : nom réel de l'organisation (récupéré via GET /organizations/{id}), avec un bouton retour vers la liste
Tableau des membres : nom, email, rôle, action de retrait (DELETE /organizations/{id}/members/{userId})
Formulaire d'invitation : email + choix du rôle, appelle POST /organizations/{id}/invitations (le système d'invitations est une autre feature, consommée ici uniquement pour ajouter des membres)
Suppression de l'organisation : bouton avec confirmation, redirige vers la liste après suppression
Tests automatisés

src/pages/__tests__/OrganizationsPage.test.jsx, avec Vitest et React Testing Library.

Scénarios couverts :

Affichage du loader pendant le chargement
Affichage de la liste une fois les données reçues
Message affiché quand la liste est vide
Gestion d'une erreur de l'API
Ouverture du modal de création
Filtrage par rôle au clic sur un onglet

Lancer les tests :

bash
cd frontend
npm test
Mise en place technique

Le projet ne disposait d'aucun framework de test frontend avant cette feature. Ajouts :

vitest, @testing-library/react, @testing-library/jest-dom, @testing-library/dom, jsdom
Configuration dans vite.config.js (section test)
Fichier de setup : src/test-organization/setup.js

Des versions précises ont dû être fixées pour certaines dépendances (jsdom, vitest) afin d'assurer la compatibilité avec la version de Node du projet.

Ce qui reste ouvert
Intégration des permissions front (masquer entièrement, pas seulement désactiver, les actions non autorisées) — travail à venir de la feature Permissions (F04)
Pas encore de page pour visualiser/accepter une invitation depuis le compte invité (relève de la feature Invitations, F03)
Tests automatisés pas encore écrits pour OrganizationMembersPage.jsx