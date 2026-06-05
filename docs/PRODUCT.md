# LinkUp — Product Reference

Source unique de vérité pour la **vision produit**, les **acteurs**, les **features** et le **backlog**.
CLAUDE.md ne contient que les règles techniques ; ce fichier porte le **quoi** et le **pourquoi**.

---

## 1. Vision

### 1.1 Contexte

Dans les grandes métropoles d'Afrique de l'Ouest (lancement Abidjan), un paradoxe social :
les individus sont **hyper connectés digitalement mais isolés dans la vie réelle**.
Les activités sociales existent, mais elles sont :

- dispersées sur WhatsApp / Facebook / bouche-à-oreille,
- mal structurées,
- peu accessibles,
- rarement digitalisées pour la réservation et le paiement.

### 1.2 Proposition de valeur

LinkUp est une plateforme Web & Mobile qui **connecte les personnes via des activités réelles et locales**.

### 1.3 Mission

1. Faciliter les rencontres sociales réelles.
2. Centraliser les activités locales.
3. Digitaliser la réservation et le paiement (Mobile Money first).
4. Aider les organisateurs à monétiser leurs événements.
5. Créer un écosystème social basé sur l'expérience réelle.

### 1.4 Positionnement

LinkUp combine quatre produits de référence :

| Référence | Apport |
|-----------|--------|
| Meetup | social events |
| Eventbrite | ticketing + paiement |
| Instagram | social discovery |
| Marketplace | expériences locales monétisables |

---

## 2. Acteurs du système

### 2.1 Participants

**Profils** : jeunes actifs (22-40 ans), étudiants, expatriés, touristes.
**Besoins** : découvrir des activités locales, rencontrer des personnes, réserver facilement, payer via Mobile Money.

### 2.2 Organisateurs

**Profils** : coachs sportifs, restaurants, agences événementielles, particuliers.
**Besoins** : publier des activités, gérer les inscriptions, encaisser les paiements, maximiser la visibilité.

### 2.3 Partenaires

**Profils** : hôtels, centres de loisirs, offices de tourisme.
**Objectif** : générer du trafic qualifié.

---

## 3. Features

### 3.1 MVP — Core Product

| Domaine | Capabilities |
|---------|-------------|
| 🔐 Auth (Keycloak) | login email/téléphone, OAuth2/OIDC, rôles `user`, `organizer`, `admin` |
| 👤 Profil | bio, centres d'intérêt, historique, préférences sociales |
| 🗂️ Catalogue activité | **catégorie obligatoire** (Culture, Formation, Soirée, Tourisme, Sport, Festival, Science, Gastronomie, Business) — alignée sur les centres d'intérêt participant |
| 🎟️ Billetterie multi-tier | une activité = N **forfaits** (Grand Public, ECO, VIP, VVIP, Enfant, Adulte…) chacun avec son prix + sa capacité. Total places = somme des capacités des forfaits. |
| 📅 Discovery Feed | activités recommandées, filtres catégorie/prix/localisation/date |
| 📍 Détail activité | description, programme, **liste des forfaits avec prix "à partir de"**, places restantes par forfait, Google Maps |
| 💳 Réservation & paiement | sélection multi-forfait (ex. 2 VIP + 1 ECO), stock temps réel **par forfait**, Orange Money / MTN / Wave, QR Code par billet |
| 🧑‍💼 Dashboard organisateur | création activités (catégorie + forfaits), suivi inscriptions par forfait, statistiques, revenus |

### 3.2 V2 — Social & Growth

| Domaine | Capabilities |
|---------|-------------|
| 💬 Social layer | chat par activité, liste participants, ajout d'amis |
| 🎮 Gamification | badges (Explorer, Social King), points, niveaux |
| 🤖 IA / Reco | suggestions personnalisées (basées sur catégories + intérêts), matching social, comportement-based |
| 🔥 Viral | "Qui sort ce soir ?", activités mystères, sondages communautaires |

### 3.3 V3 — Scale

Analytics avancées, catalog editorialisé, modération, multi-ville.

### 3.4 Mobile — App participants (Flutter)

L'**expérience participant** (browsing, booking, ticket QR, push notifications,
géolocalisation, paiement Mobile Money in-app) bascule vers une **app native
Flutter** dans une phase dédiée. Raisons :

1. Le booking + scan QR à l'entrée + push notifications sont nativement mobile.
2. Mobile Money via SDK natif > redirections web.
3. Le SPA web reste pour les **organisateurs** (création / dashboard) et les
   **admins** (modération / métriques) — qui sont desktop-first.
4. La **landing** reste web (SSR pour SEO).

L'app Flutter consomme le **même backend Spring** — aucune refonte API,
juste un nouveau client. Plan détaillé : voir EPIC 8 ci-dessous.

| Plateforme | Audience | Tech |
|------------|----------|------|
| **landing.linkup.io** (SSR) | visiteurs anonymes | Angular 21 + SSR |
| **app.linkup.io** (SPA) | organisateurs + admins | Angular 21 SPA |
| **LinkUp Mobile** (iOS + Android) | participants | **Flutter** (à venir) |

---

## 4. Roadmap & mapping modules

| Phase | Sprint | Modules backend | Statut |
|-------|--------|-----------------|--------|
| MVP S1 | Auth & Onboarding | `auth`, `profile` | auth ✅, profile ✅ |
| MVP S2 | Discovery | `activity`, `profile` | activity ✅ |
| MVP S3 | Détail + Booking | `activity`, `booking` | booking 🚧 (1-tier seulement) |
| **MVP S3.5** | **Catégories + Tickets multi-tier** | `activity`, `booking` | **À venir** — refonte data model AVANT paiement |
| MVP S4 | Paiement | `payment`, `notification` | — (bloqué par S3.5) |
| MVP S5 | Dashboard organisateur (web) | `organizer` | — (organizer page web ✅, ApexCharts ✅) |
| **MVP S6** | **App Mobile Flutter** | new repo `linkup-mobile-flutter` | **À venir** — participants |
| V2 | Social & Reco | `social`, `recommendation`, `moderation` | — |
| V3 | Scale | `analytics`, `catalog` | — |

État détaillé d'implémentation : voir [`memory.md`](../memory.md).

---

## 5. Backlog — User Stories

### Conventions

- **Priorité** : MUST / SHOULD / COULD (MoSCoW).
- **RICE** : Reach × Impact × Confidence / Effort. Plus haut = plus prioritaire.
- **Sprint** : assignation indicative (S1 = sprint 1, etc.).

### EPIC 1 — AUTH & ONBOARDING

| ID | User Story | Critères d'acceptation | Priorité | RICE | Sprint |
|----|-----------|------------------------|----------|------|--------|
| US-001 | En tant que visiteur, je veux m'inscrire avec mon email | Email unique, confirmation envoyée, mdp > 8 chars, regex email | MUST | 2400 | S1 |
| US-002 | M'inscrire avec mon numéro de téléphone | Format intl, unicité, OTP SMS, timeout 60s | MUST | 2600 | S1 |
| US-003 | Me connecter avec email/mdp | Feedback erreur clair, JWT généré, persistance session | MUST | 2800 | S1 |
| US-004 | Réinitialiser mon mot de passe | Deep-link email, expiration 15min, mdp sécurisé | MUST | 1800 | S1 |
| US-005 | Choisir mes centres d'intérêt (onboarding) | Liste prédéfinie (min 5), multi-sélection, impact feed | MUST | 3200 | S1 |
| US-006 | Me connecter via Google OAuth | Flux OAuth2 standard, récupération email/photo/nom | SHOULD | 1600 | S3 |
| US-007 | Compléter mon profil | Upload photo (crop), bio (150 chars), ville, date naissance | MUST | 2200 | S2 |
| US-008 | Modifier mes centres d'intérêt | Accès via paramètres, mise à jour immédiate du feed | SHOULD | 1400 | S2 |
| US-009 | Gérer mes notifications | Toggles push/email, catégories marketing/transactionnel | SHOULD | 1200 | S3 |
| US-010 | Voir un tutoriel d'onboarding | 3 slides max, skip, ne s'affiche qu'une fois | COULD | 800 | S4 |
| US-011 | Me déconnecter de tous mes appareils | Invalidation tokens JWT, redirection login | SHOULD | 900 | S3 |
| US-012 | Supprimer mon compte | Double confirmation, soft delete 30j, feedback raison | MUST | 1100 | S2 |

### EPIC 2 — DISCOVERY FEED

| ID | User Story | Critères d'acceptation | Priorité | RICE | Sprint |
|----|-----------|------------------------|----------|------|--------|
| US-013 | Feed d'activités personnalisé | Algo basé intérêts + localisation, pagination infinie | MUST | 3500 | S2 |
| US-014 | Recherche par mot-clé | Full-text (Elasticsearch), autocomplétion, historique récent | MUST | 2800 | S2 |
| US-015 | Filtrer par date (Ce soir/Demain/Week-end) | Chips rapides, sélecteur date custom | MUST | 2900 | S2 |
| US-016 | Filtrer par catégorie | Icônes, multi-sélection | MUST | 2700 | S2 |
| US-017 | Filtrer par prix max | Slider, toggle "Gratuit" | SHOULD | 2100 | S2 |
| US-018 | Voir activités sur une carte | Google Maps SDK, clustering, info window | SHOULD | 1900 | S3 |
| US-019 | Voir activités populaires | Section "Trending", tri par vues/inscrits 24h | SHOULD | 2000 | S2 |
| US-020 | Sauvegarder en favoris | Coeur, liste "Mes Favoris", persistance | SHOULD | 1500 | S3 |
| US-021 | Voir prix sur la card | Affichage clair, "à partir de", devise locale | MUST | 3000 | S2 |
| US-022 | Pull-to-refresh | Animation loading, mise à jour données | MUST | 2500 | S2 |
| US-023 | Voir distance par rapport à ma position | Haversine, "à 2km", permission GPS | SHOULD | 1800 | S3 |
| US-024 | Partager une activité | Native share, deep link, prévisualisation | MUST | 2400 | S3 |

### EPIC 3 — DÉTAIL ACTIVITÉ

| ID | User Story | Critères d'acceptation | Priorité | RICE | Sprint |
|----|-----------|------------------------|----------|------|--------|
| US-029 | Voir description complète | Texte riche, "voir plus", structure claire | MUST | 3100 | S3 |
| US-030 | Voir galerie photos | Slider, zoom, indicateur photos | MUST | 2900 | S3 |
| US-031 | Voir programme détaillé (Timeline) | Liste séquentielle, icônes étapes | MUST | 2800 | S3 |
| US-032 | Voir qui participe | Avatars, amis en premier, count | MUST | 3300 | S3 |
| US-033 | Voir places restantes | Compteur temps réel, "Plus que 3 places" (FOMO) | MUST | 3400 | S3 |
| US-034 | Voir localisation sur carte | Mini-map, bouton "Y aller" (GMaps/Waze) | MUST | 2700 | S3 |
| US-035 | Voir profil organisateur | Nom, photo, note, nb activités | MUST | 2600 | S3 |

### EPIC 4 — RÉSERVATION

| ID | User Story | Critères d'acceptation | Priorité | RICE | Sprint |
|----|-----------|------------------------|----------|------|--------|
| US-043 | Réserver une place | CTA sticky, flow modal | MUST | 4000 | S3 |
| US-044 | Sélectionner nombre de places | Sélecteur +/−, total dynamique, max places | MUST | 3800 | S3 |
| US-045 | Avoir un ticket numérique (QR Code) | QR unique, écran "Mon Ticket", offline | MUST | 3900 | S3 |
| US-046 | Voir mes réservations à venir | Onglet "Tickets", chronologique, statut | MUST | 3700 | S3 |
| US-047 | Annuler ma réservation | Conditions, raison, MAJ stock | MUST | 3200 | S3 |

### EPIC 5 — PAIEMENT

| ID | User Story | Critères d'acceptation | Priorité | RICE | Sprint |
|----|-----------|------------------------|----------|------|--------|
| US-059 | Payer via Orange Money | API, saisie numéro, push USSD/OTP | MUST | 3600 | S4 |
| US-060 | Payer via MTN Mobile Money | API, saisie numéro, validation | MUST | 3500 | S4 |
| US-061 | Payer via Wave | Deep link, retour app auto, confirmation | MUST | 3800 | S4 |
| US-062 | Voir historique transactions | Liste, dates, montants, statuts, ID | SHOULD | 2400 | S4 |
| US-063 | Recevoir un reçu | Email auto, PDF téléchargeable | SHOULD | 2000 | S4 |
| US-064 | Commission prélevée auto (organisateur) | Split payment, net vs brut | MUST | 3000 | S4 |

### EPIC 6 — DASHBOARD ORGANISATEUR

| ID | User Story | Critères d'acceptation | Priorité | RICE | Sprint |
|----|-----------|------------------------|----------|------|--------|
| US-089 | Créer une nouvelle activité | Formulaire multi-étapes, validation, prévisualisation | MUST | 3300 | S5 |

### EPIC 7 — CATÉGORIES & BILLETTERIE MULTI-TIER

Refonte des modèles `Activity` et `Booking` avant l'intégration paiement.
Le total à payer dépend des forfaits choisis ; refactorer après le
paiement = migration dangereuse.

| ID | User Story | Critères d'acceptation | Priorité | RICE | Sprint |
|----|-----------|------------------------|----------|------|--------|
| US-100 | (Organisateur) Choisir une catégorie à la création | Liste fixe : Culture, Formation, Soirée, Tourisme, Sport, Festival, Science, Gastronomie, Business. Champ obligatoire. Migration V10 ajoute la colonne. | MUST | 3500 | S3.5 |
| US-101 | (Participant) Filtrer le feed par catégorie | Chips multi-sélection, indicateur du nombre d'activités par catégorie | MUST | 3200 | S3.5 |
| US-102 | (Organisateur) Définir N forfaits par activité | Au moins 1 forfait par activité. Chaque forfait : code (GRAND_PUBLIC / ECO / VIP / VVIP / ENFANT / ADULTE / libre), label affiché, prix en XOF, capacité, places vendues | MUST | 4200 | S3.5 |
| US-103 | (Organisateur) Modifier un forfait existant | Prix éditable tant qu'aucune vente, capacité éditable tant que ≥ vendues, suppression refusée si vendues > 0 | MUST | 2800 | S3.5 |
| US-104 | (Participant) Voir tous les forfaits sur le détail | Liste verticale : label + prix + places restantes par forfait. "À partir de X XOF" sur la card du feed (US-021). | MUST | 3800 | S3.5 |
| US-105 | (Participant) Réserver un mix de forfaits | Sélecteur +/− par forfait (ex. 2 VIP + 1 ECO). Total dynamique. Cap global du compte appliqué sur la somme des qty. | MUST | 4000 | S3.5 |
| US-106 | (Participant) Recevoir N billets QR | 1 QR par billet (pas 1 QR pour le booking complet). Écran "Mes billets" déplie tous les QR du booking. | MUST | 3700 | S3.5 |
| US-107 | (Organisateur) Voir les ventes par forfait | Tableau de bord : chaque forfait → vendu / capacité / revenu | SHOULD | 2400 | S3.5 |
| US-108 | (Organisateur) Tier "Enfant" avec date de naissance | Bookings sur le tier Enfant exigent la DOB du participant et le système refuse si > 17 ans | COULD | 1100 | S4 |

### EPIC 8 — APP MOBILE FLUTTER (PARTICIPANTS)

L'app native iOS + Android pour l'expérience participant. Le web SPA reste
pour les organisateurs et les admins. **Backend identique** — aucune
modification API, juste un nouveau client.

| ID | User Story | Critères d'acceptation | Priorité | RICE | Sprint |
|----|-----------|------------------------|----------|------|--------|
| US-150 | Bootstrap repo Flutter | `linkup-mobile-flutter` créé, structure Clean Arch (presentation / domain / data), CI Codemagic ou GitHub Actions Fastlane | MUST | 1500 | S6.0 |
| US-151 | Auth Keycloak PKCE mobile | flutter_appauth + secure storage du refresh, biométrie en option pour ré-ouvrir l'app | MUST | 2800 | S6.1 |
| US-152 | Feed d'activités mobile | Cartes scrollables, image cover, prix "à partir de", pull-to-refresh, filtres bottom-sheet | MUST | 3300 | S6.2 |
| US-153 | Détail activité mobile | Hero scroll, programme, forfaits, CTA sticky "Réserver", carte (flutter_map / google_maps_flutter) | MUST | 3100 | S6.2 |
| US-154 | Booking flow mobile (multi-tier) | Sélecteur +/− par forfait, total live, paiement Mobile Money (deep links Wave, USSD Orange/MTN), QR généré offline-first | MUST | 4000 | S6.3 |
| US-155 | Mes billets QR offline | Cache local (Isar / Drift), affichage QR sans réseau, tap = zoom plein écran | MUST | 3500 | S6.3 |
| US-156 | Push notifications | Firebase Cloud Messaging, rappel J-1 / H-2 avant activité, confirmation paiement, annulation organisateur | MUST | 2800 | S6.4 |
| US-157 | Scan QR participants (organisateur) | Mode "Door check" : caméra scanne le QR, valide côté backend (`POST /api/v1/bookings/{id}/check-in`), affiche ✅ / ❌ | SHOULD | 2400 | S6.5 |
| US-158 | Mode invité (browsing sans login) | Lire le feed + détail sans compte. Login forcé au moment de réserver. | SHOULD | 1900 | S6.2 |
| US-159 | Deep links activité partagée | Universal links iOS + App links Android sur `linkup.io/a/{id}` → ouvre l'app sur le détail | SHOULD | 1700 | S6.4 |

---

## 6. Mapping User Story → Architecture

Référence pour traçabilité ticket → code.

| US | Module(s) backend | Services dépendants | Event(s) publié(s) | API |
|----|-------------------|---------------------|--------------------|----|
| US-001 | `auth` | `notification` | `user.registered` | délégué à Keycloak |
| US-003 | `auth` | — | — | `GET /api/v1/auth/me` |
| US-007 | `profile` | `auth` (`UserDirectory`) | `profile.updated` | `PUT /api/v1/profile` |
| US-013 | `activity` | `profile` (intérêts), `recommendation` (V2) | — | `GET /api/v1/activities` |
| US-033 | `activity` | — | — | inclus dans `GET /api/v1/activities/{id}` |
| US-043 | `booking` | `activity` (`ActivitySeatService`) | `booking.created` | `POST /api/v1/bookings` |
| US-045 | `booking` | `notification` | `booking.ticket-issued` | `GET /api/v1/bookings/{id}/ticket` |
| US-047 | `booking` | `activity`, `payment` | `booking.cancelled` | `DELETE /api/v1/bookings/{id}` |
| US-059..061 | `payment` | `booking`, `notification` | `payment.completed`, `payment.failed` | `POST /api/v1/payments` |
| US-064 | `payment` | `organizer` | `payment.commission-debited` | interne |
| US-089 | `activity` | `auth` | `activity.created` | `POST /api/v1/activities` |
| US-100 | `activity` (refonte) | — | — | `POST /api/v1/activities` (champ `category`) |
| US-101 | `activity` | — | — | `GET /api/v1/activities?categories=…` |
| US-102 | `activity` (nouveau aggregate `TicketTier`) | — | `activity.tier-added` | `POST /api/v1/activities/{id}/tiers`, `PUT /…/tiers/{code}` |
| US-104 | `activity` | — | — | `ActivityResponse.tiers[]` |
| US-105 | `booking` (refonte ligne items) | `activity.tier`, `payment` | `booking.created` | `POST /api/v1/bookings` body devient `{ activityId, lines: [{tierCode, qty}] }` |
| US-106 | `booking` | `notification` | `booking.ticket-issued` × N | `GET /api/v1/bookings/{id}/tickets` (liste de QR) |
| US-150..159 | (mobile only — backend inchangé) | — | — | mêmes endpoints, nouveau client Flutter |

---

## 7. Glossaire

- **Activity** : événement publié par un organisateur, avec une **catégorie**, un lieu, des horaires, et une ou plusieurs **billetteries (forfaits)**.
- **ActivityCategory** : enum fixe (Culture / Formation / Soirée / Tourisme / Sport / Festival / Science / Gastronomie / Business). Permet le filtrage et le matching avec les centres d'intérêt participant.
- **TicketTier** (forfait) : palier tarifaire d'une activity. Codes standards : `GRAND_PUBLIC`, `ECO`, `VIP`, `VVIP`, `ENFANT`, `ADULTE`. Chaque tier porte son prix (XOF), sa capacité, son nombre de vendus.
- **Booking** : réservation d'un participant sur une activity ; composée de N **BookingLine** (un par forfait acheté).
- **BookingLine** : `{ tierCode, qty, unitPriceAtPurchase }`. Le snapshot du prix au moment de l'achat est conservé pour la facturation, même si le prix du tier change ensuite.
- **Ticket** : preuve d'**une place** vendue, matérialisée par un QR unique. Un booking de 3 places génère 3 tickets (3 QR distincts).
- **Organizer** : user avec le rôle `organizer` ; peut publier des activities et gérer leurs forfaits.
- **Mobile Money** : Orange Money, MTN MoMo, Wave — moyens de paiement principaux ciblés.
- **Discovery Feed** : flux personnalisé d'activities affiché à l'ouverture de l'app.
- **LinkUp Mobile** : app Flutter (iOS + Android) destinée aux **participants** — booking + QR + push. Sortira après S3.5 + S4.
