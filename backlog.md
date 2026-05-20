# Backlog Produit — LinkUp

Extraction des User Stories depuis le cahier des charges.
**Total user stories extraites : 44**

## EPIC 1 — AUTH & ONBOARDING EPIC 1 — AUTH & ONBOARDING EPIC 1 — AUTH & ONBOARDING EPIC 1 — AUTH & ONBOARDING EPIC 1 — AUTH & ONBOARDING EPIC 1 — AUTH & ONBOARDING

| ID | User Story | Critères d'acceptation | Priorité | RICE | Sprint |
|---|---|---|---:|---:|---|
| US-001 | En tant que visiteur, je veux m'inscrire avec mon email | Email unique, confirmation email envoyée, mot de passe > 8 chars, regex validation email | MUST | 2400 | S1 |
| US-002 | En tant que visiteur, je veux m'inscrire avec mon numéro de téléphone | Validation format intl, unicité, OTP envoyé par SMS, timeout 60s | MUST | 2600 | S1 |
| US-003 | En tant qu'utilisateur, je veux me connecter avec email/mdp | Feedback erreur clair, token JWT généré, persistance session | MUST | 2800 | S1 |
| US-004 | En tant qu'utilisateur, je veux réinitialiser mon mot de passe | Lien deep-link email, expiration 15min, nouveau mot de passe sécurisé | MUST | 1800 | S1 |
| US-005 | En tant que nouvel utilisateur, je veux choisir mes centres d'intérêt | Liste prédéfinie (min 5), sélection multiple, impact sur le feed initial | MUST | 3200 | S1 |
| US-006 | En tant qu'utilisateur, je veux me connecter via Google OAuth | Flux OAuth2 standard, récupération auto email/photo/nom | SHOULD | 1600 | S3 |
| US-007 | En tant qu'utilisateur, je veux compléter mon profil | Upload photo (crop), Bio (max 150 chars), Ville (liste), Date naissance | MUST | 2200 | S2 |
| US-008 | En tant qu'utilisateur, je veux modifier mes centres d'intérêt | Accès via paramètres, mise à jour immédiate du feed | SHOULD | 1400 | S2 |
| US-009 | En tant qu'utilisateur, je veux gérer mes notifications | Toggles séparés (push/email), catégories (marketing/transactionnel) | SHOULD | 1200 | S3 |
| US-010 | En tant qu'utilisateur, je veux voir un tutoriel d'onboarding | 3 slides max, skip button, ne s'affiche qu'une fois | COULD | 800 | S4 |
| US-011 | En tant qu'utilisateur, je veux me déconnecter de tous mes appareils | Invalidation tokens JWT, redirection login | SHOULD | 900 | S3 |
| US-012 | En tant qu'utilisateur, je veux supprimer mon compte | Double confirmation, soft delete 30j, raison suppression (feedback) | MUST | 1100 | S2 |
| US-013 | En tant qu'utilisateur, je veux voir un feed d'activités personnalisé | Algo basé sur intérêts et localisation, pagination infinie | MUST | 3500 | S2 |
| US-014 | En tant qu'utilisateur, je veux rechercher une activité par mot-clé | Recherche full-text (Elasticsearch), autocomplétion, historique récent | MUST | 2800 | S2 |
| US-015 | En tant qu'utilisateur, je veux filtrer par date (Ce soir, Demain, Week-end) | Filtres rapides (chips), sélecteur de date custom | MUST | 2900 | S2 |
| US-016 | En tant qu'utilisateur, je veux filtrer par catégorie | Liste catégories icônes, multi-sélection | MUST | 2700 | S2 |
| US-017 | En tant qu'utilisateur, je veux filtrer par prix (Max price) | Slider prix, toggle "Gratuit" | SHOULD | 2100 | S2 |
| US-018 | En tant qu'utilisateur, je veux voir les activités sur une carte | Google Maps SDK, clustering markers, info window au clic | SHOULD | 1900 | S3 |
| US-019 | En tant qu'utilisateur, je veux voir les activités populaires | Section "Trending", tri par nb vues/inscrits 24h | SHOULD | 2000 | S2 |
| US-020 | En tant qu'utilisateur, je veux sauvegarder des activités en favoris | Icône coeur, liste "Mes Favoris", persistance | SHOULD | 1500 | S3 |
| US-021 | En tant qu'utilisateur, je veux voir les détails de prix sur la card | Affichage clair prix, mention "à partir de", devise locale | MUST | 3000 | S2 |
| US-022 | En tant qu'utilisateur, je veux rafraîchir le feed (Pull-to-refresh) | Animation loading, mise à jour données | MUST | 2500 | S2 |
| US-023 | En tant qu'utilisateur, je veux voir la distance par rapport à ma position | Calcul haversine, affichage "à 2km", permission GPS | SHOULD | 1800 | S3 |
| US-024 | En tant qu'utilisateur, je veux partager une activité | Native share sheet, lien profond (deep link), prévisualisation image | MUST | 2400 | S3 |
| US-029 | En tant qu'utilisateur, je veux voir la description complète | Texte riche, "voir plus", structure claire | MUST | 3100 | S3 |
| US-030 | En tant qu'utilisateur, je veux voir la galerie photos | Slider images, zoom, indicateur nombre photos | MUST | 2900 | S3 |
| US-031 | En tant qu'utilisateur, je veux voir le programme détaillé (Timeline) | Liste horaire séquentielle, icônes étapes | MUST | 2800 | S3 |
| US-032 | En tant qu'utilisateur, je veux voir qui participe | Avatars participants, amis en premier, count total | MUST | 3300 | S3 |
| US-033 | En tant qu'utilisateur, je veux voir le nombre de places restantes | Compteur temps réel, label "Plus que 3 places" (FOMO) | MUST | 3400 | S3 |
| US-034 | En tant qu'utilisateur, je veux voir la localisation exacte sur une carte | Mini-map statique, bouton "Y aller" (ouvre GMaps/Waze) | MUST | 2700 | S3 |
| US-035 | En tant qu'utilisateur, je veux voir le profil de l'organisateur | Nom, photo, note moyenne, nombre d'activités organisées | MUST | 2600 | S3 |
| US-043 | En tant qu'utilisateur, je veux réserver une place | Bouton CTA sticky, flow réservation modale | MUST | 4000 | S3 |
| US-044 | En tant qu'utilisateur, je veux sélectionner le nombre de places | Sélecteur +/- , calcul total prix dynamique, limite max places | MUST | 3800 | S3 |
| US-045 | En tant qu'utilisateur, je veux avoir un ticket numérique (QR Code) | Génération QR unique, écran "Mon Ticket", accès offline | MUST | 3900 | S3 |
| US-046 | En tant qu'utilisateur, je veux voir mes réservations à venir | Onglet "Tickets", tri chronologique, status (confirmé/attente) | MUST | 3700 | S3 |
| US-047 | En tant qu'utilisateur, je veux annuler ma réservation | Conditions annulation, raison, mise à jour stock | MUST | 3200 | S3 |
| US-059 | En tant qu'utilisateur, je veux payer via Orange Money | Intégration API, saisie numéro, push USSD ou OTP | MUST | 3600 | S4 |
| US-060 | En tant qu'utilisateur, je veux payer via MTN Mobile Money | Intégration API, saisie numéro, validation paiement | MUST | 3500 | S4 |
| US-061 | En tant qu'utilisateur, je veux payer via Wave | Deep link app Wave, retour app auto, confirmation instantanée | MUST | 3800 | S4 |
| US-062 | En tant qu'utilisateur, je veux voir l'historique de mes transactions | Liste paiements, dates, montants, statuts, ID transaction | SHOULD | 2400 | S4 |
| US-063 | En tant qu'utilisateur, je veux recevoir un reçu de paiement | Email auto, PDF téléchargeable depuis app | SHOULD | 2000 | S4 |
| US-064 | En tant qu'organisateur, je veux que la commission soit prélevée auto | Split payment logic, net perçu vs brut | MUST | 3000 | S4 |
| US-089 | En tant qu'organisateur, je veux créer une nouvelle activité | Formulaire multi-étapes, validation champs, prévisualisation | MUST | 3300 | S5 |
| US-001 | Inscription Email | auth-service | user-service, notification-service | user.registered | POST /api/auth/register |