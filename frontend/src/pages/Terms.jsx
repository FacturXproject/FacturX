import { Link } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import Footer from '../components/Footer';

export default function Terms() {
  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        background: '#f8f9fa',
        color: '#1a1a2e',
      }}
    >
      <main
        style={{
          flex: 1,
          padding: '56px 24px 72px',
        }}
      >
        <article
          style={{
            maxWidth: '900px',
            margin: '0 auto',
            background: '#ffffff',
            border: '1px solid #e5e7eb',
            borderRadius: '12px',
            padding: '48px clamp(24px, 6vw, 72px)',
            boxSizing: 'border-box',
          }}
        >
          <header
            style={{
              textAlign: 'center',
              marginBottom: '48px',
            }}
          >
            <h1
              style={{
                margin: 0,
                color: '#1a2744',
                fontSize: '32px',
              }}
            >
              Conditions d'utilisation
            </h1>

            <p
              style={{
                margin: '16px auto 0',
                maxWidth: '680px',
                color: '#6b7280',
                lineHeight: 1.7,
              }}
            >
              Ces conditions définissent les règles applicables à
              l'utilisation de Factur-X Validator et aux fonctionnalités
              proposées par l'application.
            </p>
          </header>

          <section>
            <h2>1. Objet du service</h2>

            <p>
              Factur-X Validator est une application destinée à fournir des
              outils permettant notamment de valider, lire et convertir des
              documents de facturation électronique.
            </p>

            <p>
              L'application peut également proposer des fonctionnalités de
              gestion de compte, de profil, d'organisations et d'invitations
              entre utilisateurs.
            </p>
          </section>

          <section>
            <h2>2. Création d'un compte</h2>

            <p>
              Certaines fonctionnalités nécessitent la création d'un compte
              utilisateur.
            </p>

            <p>
              L'utilisateur s'engage à fournir des informations exactes et à
              maintenir ses informations de compte à jour lorsque cela est
              nécessaire.
            </p>

            <p>
              Les identifiants permettant d'accéder au compte doivent rester
              confidentiels. L'utilisateur est responsable des actions
              effectuées depuis son compte.
            </p>
          </section>

          <section>
            <h2>3. Utilisation du service</h2>

            <p>
              L'utilisateur s'engage à utiliser l'application conformément aux
              lois et réglementations applicables ainsi qu'aux présentes
              conditions.
            </p>

            <p>
              Il est notamment interdit d'utiliser le service pour tenter
              d'obtenir un accès non autorisé à l'application, à ses données,
              à ses comptes ou à ses systèmes informatiques.
            </p>

            <p>
              L'utilisateur ne doit pas utiliser le service d'une manière
              susceptible de perturber son fonctionnement ou de porter atteinte
              aux autres utilisateurs.
            </p>
          </section>

          <section>
            <h2>4. Documents importés</h2>

            <p>
              L'utilisateur reste responsable des documents qu'il importe dans
              l'application et doit disposer des droits nécessaires pour les
              utiliser.
            </p>

            <p>
              Les documents peuvent contenir des informations commerciales,
              financières ou personnelles. L'utilisateur doit donc s'assurer
              qu'il est autorisé à transmettre ces informations au service.
            </p>

            <p>
              Les outils de validation et de conversion sont destinés à fournir
              une assistance technique. L'utilisateur reste responsable de la
              vérification finale des documents produits ou validés par
              l'application.
            </p>
          </section>

          <section>
            <h2>5. Organisations et invitations</h2>

            <p>
              L'application peut permettre aux utilisateurs de créer ou de
              rejoindre des organisations et d'inviter d'autres utilisateurs.
            </p>

            <p>
              Les utilisateurs disposant des droits nécessaires au sein d'une
              organisation sont responsables des invitations qu'ils créent et
              des accès qu'ils accordent.
            </p>

            <p>
              Un utilisateur ne doit pas utiliser les fonctionnalités
              d'organisation ou d'invitation pour obtenir ou fournir un accès
              non autorisé à des informations.
            </p>
          </section>

          <section>
            <h2>6. Disponibilité du service</h2>

            <p>
              L'application est conçue pour être accessible aux utilisateurs
              dans des conditions normales de fonctionnement.
            </p>

            <p>
              Toutefois, certaines fonctionnalités peuvent être temporairement
              indisponibles en raison de maintenance, de mises à jour,
              d'incidents techniques ou de circonstances indépendantes du
              fonctionnement normal de l'application.
            </p>
          </section>

          <section>
            <h2>7. Résultats de validation et de conversion</h2>

            <p>
              Les résultats produits par les fonctionnalités de validation,
              lecture ou conversion constituent une assistance technique.
            </p>

            <p>
              L'utilisateur doit vérifier les résultats avant de les utiliser
              dans un contexte professionnel, comptable, fiscal, commercial ou
              réglementaire.
            </p>

            <p>
              L'application ne remplace pas les contrôles ou validations qui
              peuvent être requis par les réglementations applicables ou par
              les procédures internes de l'utilisateur.
            </p>
          </section>

          <section>
            <h2>8. Propriété intellectuelle</h2>

            <p>
              Les éléments constituant l'application, notamment son interface,
              son code, sa structure et ses éléments graphiques, sont protégés
              par les droits applicables à la propriété intellectuelle.
            </p>

            <p>
              Sauf autorisation contraire, l'utilisateur ne peut pas reproduire,
              modifier, distribuer ou exploiter les éléments de l'application
              en dehors de l'utilisation normale du service.
            </p>
          </section>

          <section>
            <h2>9. Sécurité du compte</h2>

            <p>
              L'utilisateur doit prendre les mesures nécessaires pour protéger
              ses identifiants et empêcher tout accès non autorisé à son compte.
            </p>

            <p>
              En cas de suspicion d'utilisation non autorisée d'un compte,
              l'utilisateur doit prendre rapidement les mesures nécessaires
              pour sécuriser celui-ci.
            </p>
          </section>

          <section>
            <h2>10. Suspension ou restriction d'accès</h2>

            <p>
              L'accès à certaines fonctionnalités peut être temporairement
              suspendu ou restreint lorsqu'une utilisation du service est
              susceptible de compromettre sa sécurité, son fonctionnement ou
              les droits d'autres utilisateurs.
            </p>

            <p>
              Lorsque cela est possible, les utilisateurs concernés peuvent
              être informés de la raison de la restriction.
            </p>
          </section>

          <section>
            <h2>11. Données personnelles</h2>

            <p>
              Le traitement des données personnelles effectué dans le cadre de
              l'utilisation du service est décrit dans la
              <Link
                to="/privacy"
                style={{
                  color: '#2563eb',
                  textDecoration: 'none',
                  marginLeft: '4px',
                }}
              >
                Politique de confidentialité
              </Link>
              .
            </p>

            <p>
              En utilisant l'application, l'utilisateur est invité à prendre
              connaissance de cette politique afin de comprendre comment ses
              données peuvent être traitées.
            </p>
          </section>

          <section>
            <h2>12. Évolution du service</h2>

            <p>
              Les fonctionnalités de Factur-X Validator peuvent évoluer afin
              d'améliorer le service, de corriger des problèmes techniques ou
              d'ajouter de nouvelles fonctionnalités.
            </p>

            <p>
              Certaines fonctionnalités peuvent ainsi être modifiées,
              remplacées ou supprimées lorsque cela est nécessaire au
              fonctionnement ou à l'évolution de l'application.
            </p>
          </section>

          <section>
            <h2>13. Modification des conditions</h2>

            <p>
              Les présentes conditions peuvent être mises à jour lorsque
              l'application ou les règles applicables évoluent.
            </p>

            <p>
              La version publiée dans l'application correspond à la version
              consultable par les utilisateurs au moment de leur utilisation
              du service.
            </p>
          </section>

          <section>
            <h2>14. Acceptation des conditions</h2>

            <p>
              L'utilisation des fonctionnalités de Factur-X Validator implique
              que l'utilisateur a pris connaissance des présentes conditions
              et s'engage à les respecter.
            </p>
          </section>

          <div
            style={{
              textAlign: 'center',
              marginTop: '48px',
            }}
          >
            <Link
              to="/"
              aria-label="Retour à l'accueil"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: '42px',
                height: '42px',
                borderRadius: '50%',
                border: '1px solid #d1d5db',
                textDecoration: 'none',
                color: '#1a2744',
                background: '#ffffff',
              }}
            >
              <ArrowLeft size={20} />
            </Link>
          </div>
        </article>
      </main>

      <Footer />
    </div>
  );
}