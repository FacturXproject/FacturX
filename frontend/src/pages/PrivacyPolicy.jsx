import { Link } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import Footer from '../components/Footer';

export default function PrivacyPolicy() {
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
              Politique de confidentialité
            </h1>

            <p
              style={{
                margin: '16px auto 0',
                maxWidth: '680px',
                color: '#6b7280',
                lineHeight: 1.7,
              }}
            >
              Cette politique explique quelles données peuvent être traitées
              lorsque vous utilisez Factur-X Validator, pourquoi elles sont
              utilisées et quels droits vous pouvez exercer concernant vos
              données personnelles.
            </p>
          </header>

          <section>
            <h2>1. Présentation</h2>

            <p>
              Factur-X Validator est une application permettant notamment de
              valider, lire et convertir des documents de facturation
              électronique. L'application propose également des fonctionnalités
              liées aux comptes utilisateurs, aux profils, aux organisations et
              aux invitations.
            </p>

            <p>
              La présente politique a pour objectif de présenter de manière
              transparente les principes appliqués au traitement des données
              dans le cadre de l'utilisation de l'application.
            </p>
          </section>

          <section>
            <h2>2. Données pouvant être collectées</h2>

            <p>
              Selon les fonctionnalités utilisées, l'application peut traiter
              différentes catégories de données.
            </p>

            <ul>
              <li>
                <strong>Données du compte :</strong> adresse e-mail, prénom,
                nom et informations nécessaires à l'authentification.
              </li>

              <li>
                <strong>Données du profil :</strong> informations que
                l'utilisateur choisit de renseigner ou de modifier dans son
                profil.
              </li>

              <li>
                <strong>Données liées aux organisations :</strong> informations
                nécessaires à la gestion des organisations, des membres et des
                invitations.
              </li>

              <li>
                <strong>Données contenues dans les documents :</strong> les
                fichiers transmis à l'application peuvent contenir des
                informations présentes dans des factures électroniques.
              </li>
            </ul>
          </section>

          <section>
            <h2>3. Utilisation des données</h2>

            <p>
              Les données sont utilisées uniquement dans le cadre des
              fonctionnalités proposées par l'application et peuvent notamment
              servir à :
            </p>

            <ul>
              <li>créer et gérer un compte utilisateur ;</li>
              <li>permettre l'authentification ;</li>
              <li>afficher et modifier les informations du profil ;</li>
              <li>gérer les organisations et leurs membres ;</li>
              <li>gérer les invitations ;</li>
              <li>permettre la validation de documents ;</li>
              <li>permettre la lecture de documents XML ;</li>
              <li>permettre la conversion de documents.</li>
            </ul>
          </section>

          <section>
            <h2>4. Documents et factures électroniques</h2>

            <p>
              Les documents importés dans Factur-X Validator peuvent contenir
              des données relatives à des entreprises, des clients, des
              fournisseurs, des transactions ou des montants de facturation.
            </p>

            <p>
              L'utilisateur est responsable des documents qu'il transmet et
              doit s'assurer qu'il dispose des droits et autorisations
              nécessaires pour les utiliser avec l'application.
            </p>

            <p>
              Les fonctionnalités de validation et de conversion sont fournies
              comme des outils techniques. Les résultats doivent être vérifiés
              par l'utilisateur avant toute utilisation professionnelle,
              comptable, fiscale ou réglementaire.
            </p>
          </section>

          <section>
            <h2>5. Sécurité</h2>

            <p>
              La sécurité des données constitue un élément important de
              l'application. Des mesures techniques et organisationnelles sont
              mises en œuvre afin de limiter les risques d'accès, de
              modification, de perte ou de divulgation non autorisés.
            </p>

            <p>
              Malgré ces mesures, aucun système informatique connecté à un
              réseau ne peut garantir une sécurité absolue. Les utilisateurs
              doivent également protéger leurs identifiants et éviter de les
              communiquer à des tiers.
            </p>
          </section>

          <section>
            <h2>6. Organisations et partage des informations</h2>

            <p>
              Certaines fonctionnalités permettent de créer ou de rejoindre
              des organisations et d'inviter d'autres utilisateurs.
            </p>

            <p>
              Les utilisateurs doivent s'assurer qu'ils disposent de
              l'autorisation nécessaire avant de donner accès à des documents,
              informations ou fonctionnalités à d'autres personnes.
            </p>
          </section>

          <section>
            <h2>7. Conservation des données</h2>

            <p>
              Les données sont conservées pendant la durée nécessaire au
              fonctionnement des fonctionnalités concernées et, lorsque cela
              est applicable, pendant les périodes nécessaires au respect des
              obligations légales.
            </p>

            <p>
              Les durées précises de conservation peuvent dépendre du type de
              donnée, de la fonctionnalité utilisée et de l'environnement dans
              lequel l'application est déployée.
            </p>
          </section>

          <section>
            <h2>8. Droits des utilisateurs</h2>

            <p>
              Conformément à la réglementation applicable, les personnes
              concernées peuvent disposer de différents droits concernant leurs
              données personnelles.
            </p>

            <p>
              Ces droits peuvent notamment comprendre le droit d'accès, de
              rectification, d'effacement, de limitation du traitement et,
              lorsque les conditions sont réunies, le droit d'opposition ou de
              portabilité.
            </p>

            <p>
              Pour exercer un droit, l'utilisateur doit utiliser le moyen de
              contact prévu par l'organisation responsable du service.
            </p>
          </section>

          <section>
            <h2>9. Cookies et technologies similaires</h2>

            <p>
              L'application peut utiliser des mécanismes techniques nécessaires
              à son fonctionnement, notamment pour maintenir une session
              utilisateur et assurer l'authentification.
            </p>

            <p>
              Ces mécanismes ont pour objectif de permettre le fonctionnement
              du service et ne doivent pas être confondus avec des mécanismes
              destinés à suivre l'utilisateur à des fins publicitaires.
            </p>
          </section>

          <section>
            <h2>10. Modification de la politique</h2>

            <p>
              Cette politique peut être mise à jour lorsque l'application, ses
              fonctionnalités ou les exigences applicables évoluent.
            </p>

            <p>
              La version publiée dans l'application constitue la version
              consultable par les utilisateurs au moment de leur utilisation
              du service.
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