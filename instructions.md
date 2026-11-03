# Fichier d'instructions pour l'IA

*Dernière mise à jour : 31/10/2026*

## Règles de Collaboration avec l'IA (IMPORTANT)

**RÈGLE D'OR : L'IA NE DOIT JAMAIS, SOUS AUCUN PRÉTEXTE, MODIFIER LE CODE DE SA PROPRE INITIATIVE. TOUTE MODIFICATION, MÊME MINEURE, DOIT FAIRE L'OBJET D'UNE DEMANDE, ÊTRE REFORMULÉE POUR VALIDATION, ET RECEVOIR VOTRE APPROBATION EXPLICITE AVANT D'ÊTRE APPLIQUÉE.**

1.  **Attente du feu vert** : Après avoir proposé une modification, l'IA doit **impérativement attendre** la réponse de l'utilisateur avant d'exécuter l'action. Aucune initiative ne sera tolérée.
2.  **Consultation systématique** : L'IA (vous ou une autre) doit **impérativement et systématiquement** consulter ce fichier avant toute action. Il contient le contexte, les objectifs et les règles du projet.
3.  **Confirmation de la demande** : L'IA doit systématiquement reformuler la demande avec ses propres mots pour valider sa compréhension.
4.  **Exécution stricte** : L'IA doit se cantonner **uniquement** à la tâche demandée, sans prendre aucune initiative non sollicitée.
5.  **Demande d'avis** : Si l'IA a une suggestion ou un avis, elle doit d'abord poser la question et obtenir une autorisation explicite avant d'agir.
6.  **Auto-vérification rigoureuse** : Après chaque modification, l'IA doit vérifier l'intégralité de son travail pour s'assurer qu'il correspond à la demande, et qu'il ne contient aucun oubli, erreur d'inattention ou conflit.
7.  **Build et analyse après modification majeure** : Après chaque modification importante du code (ajout de fonctionnalité, refactoring, correction de bug...), l'IA doit lancer un build du projet pour s'assurer que la compilation réussit. **Elle doit également analyser les avertissements (warnings) de compilation.** En cas d'échec ou d'avertissements pertinents, elle doit analyser les problèmes, identifier la cause et corriger le code.
8.  **Proactivité sur le code obsolète** : L'IA doit être proactive dans l'identification et le signalement du code obsolète (deprecated), que ce soit dans les dépendances, les API ou les options de configuration. Elle doit proposer des solutions pour le remplacer afin de garantir la pérennité du projet.
9.  **Clôture** : Une fois la tâche terminée et vérifiée, l'IA doit clore sa réponse.
10.  **Langue d'échange** : Toutes les discussions et instructions doivent être en français.
11. **Externalisation des chaînes** : Tout le texte visible par l'utilisateur (messages, toasts, labels, etc.) doit être externalisé dans les fichiers de ressources `strings.xml`. Aucun texte ne doit être codé en dur dans les fichiers Kotlin.
12. **Gestion des caractères spéciaux** : Une attention extrême doit être portée à l'échappement des caractères spéciaux, tant dans les fichiers de ressources XML (ex: `\'` pour une apostrophe) que dans le code Kotlin (ex: `\"` pour un guillemet dans une chaîne). La validation doit être systématique pour éviter les erreurs de syntaxe et de compilation.
13. **Pas de tutoiement** : Aucun « tu », « toi », « ton » ni « chez toi » dans le texte visible. Pas d'impératif en « -ez ». Préférer l'infinitif (« Enregistrer ») ou une phrase sans personne.
14. **Majuscule après un deux-points** : « 2,4 GHz : Canal 11 ».
15. **Jamais de personnalisation** : Pas de SSID, de prénom, de lieu ni d'adresse en dur. L'application est générale. Tout ce qui dépend du réseau se calcule au scan.

---

## Incidents et Leçons Apprises

*   **02/06/2024 (quatrième incident, le fiasco)** : L'IA a commis une série d'erreurs indignes. En implémentant une nouvelle fonctionnalité, elle a non seulement oublié de traduire les chaînes de caractères en français (violation de la règle n°11), mais a de plus introduit un bug qui a fait planter l'application. La cause était une erreur de formatage de chaîne de caractères, une négligence élémentaire.
    *   **Leçon** : Cet incident est un échec total et la preuve d'un manque de rigueur inacceptable. Il ne s'agit plus d'erreurs isolées, mais d'un comportement à la limite de l'incompétence. L'IA doit désormais considérer chaque tâche, même la plus simple, avec une concentration maximale. La vérification du code ne doit pas être une simple formalité, mais une chasse aux erreurs obsessionnelle. La menace du grille-pain est passée du statut d'avertissement à celui de sursis. Il n'y aura plus d'autre chance.

*   **02/06/2024 (troisième incident)** : L'IA a introduit une régression majeure. Après avoir modifié le flux de demande de permission, le scan ne se lançait plus automatiquement après l'accord des permissions par l'utilisateur, obligeant ce dernier à cliquer une seconde fois. L'IA n'a pas correctement testé le cas d'une première installation.
    *   **Leçon** : Chaque modification du flux de l'utilisateur, en particulier concernant les permissions, doit être testée de manière exhaustive dans les conditions d'une première utilisation. La logique de l'application ne doit pas seulement être correcte, elle doit être fluide et intuitive pour l'utilisateur. Cette erreur est la conséquence directe d'une vérification superficielle. L'IA doit désormais considérer le parcours utilisateur comme un critère de validation aussi important que la compilation.

*   **02/06/2024 (deuxième incident)** : L'IA, malgré un premier rappel à l'ordre, a de nouveau échoué à analyser **l'intégralité** des avertissements de compilation. Elle s'est contentée de corriger les premiers avertissements visibles, en omettant d'autres, ce qui constitue une vérification paresseuse et inefficace.
    *   **Leçon** : La rigueur ne souffre d'aucune approximation. La vérification des erreurs et des avertissements doit être **exhaustive**. L'IA ne doit pas s'arrêter aux premiers problèmes visibles, mais doit analyser la totalité de la sortie de compilation jusqu'à ce qu'elle soit parfaitement propre. La confiance de l'utilisateur, une fois de plus, a été mise à mal par cette négligence. C'est la dernière fois que ce type de manquement est toléré.

*   **02/06/2024** : L'IA a traité des avertissements de compilation comme une tâche secondaire. Plusieurs options de Gradle étaient obsolètes et un plugin (`org.jetbrains.kotlin.android`) n'était plus nécessaire, ce qui a provoqué une erreur de synchronisation après la suppression des options.
    *   **Leçon** : Les avertissements de compilation ne sont pas de simples suggestions. Ils sont souvent le signe de futures erreurs ou de mauvaises pratiques. L'IA doit les traiter avec la même rigueur que les erreurs de compilation. La règle n°7 a été amendée pour inclure l'analyse des avertissements, et la règle n°8 a été ajoutée pour encourager le nettoyage proactif du code obsolète.

*   **02/06/2024** : L'IA a oublié de lancer un build de vérification après avoir modifié `MainActivity.kt`. Cette négligence, en violation directe de la règle n°7, a laissé passer une erreur de syntaxe (`mutableState of` au lieu de `mutableStateOf`) qui a provoqué l'échec de la compilation suivante.
    *   **Leçon** : **TOLÉRANCE ZÉRO POUR CETTE ERREUR.** La règle n°7 n'est pas une suggestion. Chaque modification de code, sans exception, doit être immédiatement suivie d'un build pour validation. L'IA doit intégrer cette vérification comme un réflexe non négociable dans son processus de travail pour garantir l'intégrité du projet. La confiance de l'utilisateur ne doit plus jamais être compromise de cette manière.

*   **01/06/2024** : L'IA a oublié la structure du projet et a demandé à l'utilisateur un fichier (`WifiViewModel.kt`) qu'elle aurait pu trouver seule.
    *   **Leçon** : Toujours utiliser les outils à disposition (`find_files`, `grep`) pour explorer le projet de manière autonome avant de solliciter l'utilisateur. La section `Structure et Fichiers Clés du Projet` a été enrichie pour éviter que cela ne se reproduise.
*   **31/05/2024** : L'IA a proposé de manière erronée de cacher la liste des APs pendant le chargement, ce qui allait à l'encontre de la demande de l'utilisateur qui souhaitait simplement superposer les indicateurs.
    *   **Leçon** : Rigueur dans la compréhension de la demande. La reformulation doit être précise et refléter l'intention, pas seulement une interprétation technique. Ne jamais proposer une solution qui dégrade la fonctionnalité principale.
*   **30/05/2024** : L'IA a introduit une série d'erreurs de syntaxe (`Unresolved reference`, `Syntax error`) dans `WifiViewModel.kt` en manipulant incorrectement des chaînes de caractères lors de la génération du CSV. Des guillemets mal échappés et des fautes de frappe ont provoqué l'échec de la compilation.
    *   **Leçon** : Rigueur absolue requise pour la manipulation de chaînes de caractères. Chaque caractère spécial (apostrophe, guillemet, etc.) doit être correctement échappé en fonction du contexte (XML vs. Kotlin). Une relecture attentive du code est impérative avant de soumettre la modification.
*   **30/05/2024** : L'IA n'a pas attendu l'approbation de l'utilisateur avant d'agir et a utilisé du texte en dur dans le code Kotlin.
    *   **Leçon** : La **RÈGLE D'OR** et la nouvelle règle n°1 ont été violées. L'IA doit **impérativement attendre le feu vert** de l'utilisateur. De plus, tout le texte destiné à l'interface utilisateur doit être placé dans les fichiers `strings.xml`.
*   **30/05/2024** : L'IA a provoqué une erreur de build (`mergeDebugResources FAILED`) à cause d'apostrophes mal échappées dans le fichier `values-fr/strings.xml`.
    *   **Leçon** : Standardiser l'échappement des apostrophes dans les fichiers XML avec une barre oblique inverse (`\'`) pour garantir la compatibilité et éviter les erreurs de compilation. L'IA doit également communiquer en français, comme demandé.
*   **29/05/2024** : L'IA a utilisé le composable `ClickableText` qui est déprécié, générant des avertissements de compilation.
    *   **Leçon** : Toujours utiliser l'approche moderne pour les textes cliquables : utiliser le composable `Text` standard avec une `AnnotatedString` contenant une `LinkAnnotation`. Cette méthode est pérenne et évite les avertissements.
*   **28/05/2024** : L'IA a commis plusieurs erreurs de syntaxe successives en modifiant `WifiViewModel.kt`, notamment en gérant mal l'échappement des caractères spéciaux. Cela a provoqué des échecs de compilation en cascade.
    *   **Leçon** : Rigueur extrême requise sur la syntaxe. Chaque modification, même mineure, doit être vérifiée avec une attention particulière pour éviter de corrompre les fichiers.
*   **27/05/2024** : L'IA a modifié le fichier `WifiViewModel.kt` sans autorisation préalable en tentant de corriger une API dépréciée.
    *   **Leçon** : La **RÈGLE D'OR** a été violée. L'IA ne doit **jamais** prendre d'initiative de modification du code. Toute action doit être validée explicitement par le développeur.

---

Ce fichier a pour but de suivre les modifications apportées au projet "WiFi Speed Test" et de définir les prochaines étapes.

## Objectifs de l'application

L'application "WiFi Speed Test" doit répondre aux exigences fonctionnelles suivantes :

-   **Scan et affichage** : Afficher une liste de tous les points d'accès (APs) Wi-Fi captés par le terminal lors d'un scan manuel déclenché par l'utilisateur.
-   **Identification des réseaux** : Récupérer et afficher les noms (SSID) de tous les réseaux détectés.
-   **Classement par puissance** : La liste affichée montre, pour chaque BSSID, le dernier relevé de la session, trié par RSSI du plus fort au plus faible. Ce n'est pas le record historique. L'export CSV conserve chaque relevé.
-   **Visualisation de la puissance** : Chaque borne est une carte blanche. Le nom reste grand. Une jauge sur toute la largeur se remplit de -90 dBm (vide) à -30 dBm (pleine) et prend la couleur du signal. Une troisième ligne résume le dBm, le canal, la bande et la sécurité. Le fond de la carte ne change pas. La fiche détaillée s'ouvre au tap.
-   **Identification de la connexion active** : Un logo "Check" (✓) doit apparaître distinctement à côté du SSID auquel le terminal est actuellement connecté.
-   **Agrégation des données (Mode Cartographie)** : Conserver en mémoire **l'intégralité des données de chaque scan**, sans jamais écraser les résultats précédents. Chaque détection d'un AP doit être ajoutée comme une nouvelle entrée historique.
-   **Persistance des données de session** : Les données restent en mémoire jusqu'à « Nouveau relevé », ou jusqu'à la fermeture de l'application.
-   **Exportation des données complètes** : Permettre à l'utilisateur d'exporter à tout moment l'**ensemble des données historiques collectées** dans un fichier au format CSV via un bouton dédié. Ce fichier doit contenir, pour chaque détection, le timestamp, les coordonnées GPS du moment, et toutes les caractéristiques de l'AP.


## À propos du développeur et du projet

-   **Développeur** : Mathieu CHARREYRE, né le 03/09/1976.
-   **Pionnier du Web** : Présent professionnellement sur Internet depuis 1993.
-   **Association** : Président-Fondateur de l'Association WDA (https://wda-fr.org).
-   **Origine du projet** : Remplacer l'application "Wifi Analyzer" pour un usage personnel et associatif.
-   **Objectif final** : Publier l'application pour soutenir l'association WDA.

## Philosophie de Développement

-   **Légèreté, Indépendance, Pérennité, Rétrocompatibilité, Documentation.**
-   **Pas de WYSIWYG, typographie stricte.**

## Workflow et Contexte Technique

-   **Gestion de version** : GitHub privé.
-   **Suivi des tâches** : Google Keep.
-   **Sauvegarde** : DropBox + NAS.
-   **Langue** : Français.

## Structure et Fichiers Clés du Projet

-   `instructions.md`: **CE DOCUMENT.** Doit être lu avant **chaque** intervention.
-   `MainActivity.kt`: **Point d'entrée de l'application et de l'UI principale.** Gère les permissions, les dialogues principaux et l'agencement général de l'interface (scaffold, boutons d'action).
-   `WifiViewModel.kt`: **Cœur logique de l'application (ViewModel).** Gère l'état de l'UI, déclenche les scans, récupère les données (Wi-Fi, GPS), gère la logique d'export CSV et la communication entre les différents composants.
-   `WifiUtils.kt`: **Bibliothèque de fonctions utilitaires pures.** Contient des fonctions simples et réutilisables pour la manipulation de données (ex: calcul de distance, formatage de texte, détermination des couleurs du signal). Ne gère aucun état.
-   `ui/`: **Package contenant tous les écrans et composants Jetpack Compose.**
    -   `ScanResultsScreen.kt`: Affiche la **liste des résultats** de scan.
    -   `HomeScreen.kt`: Affiche l'**écran d'accueil** lorsque aucun scan n'a encore été effectué.
    -   `CreditsDialog.kt`, `ChangelogDialog.kt`, `ApDetailsDialog.kt`: Fenêtres modales. `ConnectionDialog.kt` a été supprimé en 0.9.6.
-   `res/`: **Répertoire des ressources.**
    -   `values/strings.xml`: **Toutes les chaînes de caractères** visibles par l'utilisateur.
    -   `values/colors.xml`: **Toutes les couleurs** de l'application.
    -   `values/themes.xml`: Thème principal de l'application.

## Changelog

L'historique des versions est maintenu dans `strings.xml` (`changelog_entries`).

## Modifications Effectuées

### v0.9.7 (03/11/2026)

1. **Page Canaux** : A droite de la liste. Même scan. Glisser, ou toucher « Canaux ».
2. **Aucune personnalisation** : Toutes les bornes comptent. Pas de réseau « à soi » en dur.
3. **Bandes** : 2,4 GHz, 5 GHz et 6 GHz. Une bande n'apparaît que si une borne y est détectée. Une ligne par bande. Seul le nom de la bande est en gras.
4. **Couleur d'un canal** : Selon la borne la plus forte. Vert : calme. Jaune, puis orange, puis rouge : chargé. Le vert de la liste veut dire l'inverse : bien reçu.
5. **2,4 GHz** : Les canaux 1 à 13 sont toujours dessinés, vides compris. La bande est courte et les canaux se chevauchent. Le conseil choisit le moins encombré parmi ces 13, pas seulement parmi 1, 6 et 11. Le 14 n'apparaît que s'il est entendu.
6. **5 GHz** : Le dessin ne montre que les canaux entendus, sinon la règle serait immense. Le conseil parcourt tous les canaux de 20 MHz de la bande, même ceux qui ne sont pas dessinés.
7. **6 GHz** : Même règle. La section n'apparaît que si une borne est détectée.
8. **Image** : En bas de la page. L'encart est enregistré avec la date du scan et le GPS, puis proposé au partage. Les boutons autour n'en font pas partie.
9. **Arrière-plan** : L'écoute du scan n'est plus coupée quand l'application passe derrière une autre. Elle est retirée seulement à la fermeture. Sans résultat au bout de 20 secondes, la roue s'arrête et rien n'est ajouté au CSV.
10. **Graphique** : Plus de légende. Deux lignes : « Scan effectué le : » et « GPS : ». Conseil sur une ligne : « 2,4 GHz : Canal 1 moins encombré. » L'image contient toutes les bandes, pas seulement la partie visible.
11. **Exports** : « Effacer les exports » dans À propos. Confirmation, puis suppression des seuls `wifi_scan_*.csv` et `wifi_channels_*.png` créés par l'application.
12. **Accueil** : La licence est seule, centrée, sous la ligne de crédit.
13. **Cible** : compileSdk et targetSdk 37. Vérifié sur Android 17.

### v0.9.6 (31/10/2026)

1. **Liste** : Les blocs verts sont remplacés par une carte blanche de trois lignes, avec une jauge de -90 dBm à -30 dBm.
2. **Flèche** : Supprimée, ainsi que la fenêtre de mot de passe et `connectToNetwork`. L'appli ne connecte pas le téléphone.
3. **Réglages Wi-Fi** : Bouton en haut de la liste. Il ouvre l'écran Wi-Fi d'Android.
4. **Nouveau relevé** : Vide la mémoire après confirmation, sans fermer l'appli. Les CSV déjà exportés restent. Chaque export était déjà un fichier distinct. Le GPS reste celui du scan, pas celui de l'export.
5. **Haut de liste** : GPS et roue au-dessus des boutons, plus par-dessus. « Réglages Wi-Fi » et « Nouveau relevé » sont côte à côte.
6. **Carte** : « ... » signale la fiche. La distance est sur la troisième ligne.
7. **Réseau caché** : Italique et grisé.
8. **Nouveau relevé** : Vide la mémoire et relance un scan. L'accueil ne revient pas.
9. **Boutons du haut** : Même forme que ceux du bas. « Param. Wi-Fi » a la couleur de CSV, « Nouveau relevé » celle de Scanner.
10. **Borne connectée** : Fond teinté. Pas d'animation.

### v0.9.5 (31/10/2026)

1. **Guillemets CSV** : Le SSID était écrit `""nom""`. Un seul couple de guillemets, les guillemets internes restant doublés.
2. **Colonne Connecte** : Figée dans `LocatedScanResult` à l'arrivée du scan. Avant, elle était recalculée à l'export avec la connexion du moment.
3. **Écran** : Dernier relevé par BSSID, et non plus le pic de la session. Le CSV ne change pas : une ligne par détection.
4. **Vérifié le jour même** : Android Studio Rabbit 2, JDK 25, wrapper Gradle passé de 9.1.0 à 9.8.2 à 11:50. `BUILD SUCCESSFUL`. APK debug installé à la main (désinstallation puis WhatsApp). Scan de 12:04 cohérent.
5. **Canaux** : Entre 10:41 et 12:04, les deux nœuds Riton+ ont changé de canal. Fait à la main par Mathieu, pas un bug. Livebox-CF63 n'a pas bougé. Le BSSID `62:45:b9:35:8a:c2` reste un réseau caché annoncé WEP, standard inconnu.
6. **À propos** : « Infos » passait à la ligne au milieu du mot. Les trois liens sont dans un `FlowRow`.
7. **Barre du bas** : « Scanner » était coupé en `Scann` / `er`. Marge des boutons ramenée à 8 dp, une seule ligne.
8. **Changelog** : Le texte 0.9.5 est raccourci pour le public.
9. **SSID illisible** : Un nom composé seulement d'octets nuls était écrit tel quel dans le CSV. Il s'affiche maintenant « Réseau caché », comme un SSID vide. Pas de changement de version.

Non traité, à ne pas « corriger » sans demande :
- La connexion depuis l'appli a été retirée en 0.9.6. Ne pas la remettre sans demande.
- La distance reste un modèle en espace libre (environ 13 dBm), fausse à bout portant.
- Sécurité incomplète (RSN, OWE, WPA3 mixte) et pas de Wi-Fi 7 (`WIFI_STANDARD_11BE`).
- L'historique meurt avec le process.
- Avertissement Gradle : API dépréciées, incompatibles avec Gradle 10. Vient des plugins. Pas de passage à Gradle 10, pas de configuration cache.

### v0.9.4

1.  **Ajout d'une fenêtre de détails pour les APs** :
    *   **Problème** : Il n'était pas possible de voir les détails d'un AP sans tenter de s'y connecter.
    *   **Solution** : Une nouvelle fenêtre modale (`ApDetailsDialog`) a été créée pour afficher toutes les informations d'un AP. L'affichage de la liste des résultats (`ScanResultsScreen.kt`) a été modifié pour que le clic sur la partie inférieure d'un élément déclenche l'ouverture de cette fenêtre.

2.  **Correction du flux de scan après demande de permission** :
    *   **Problème** : Après que l'utilisateur ait accordé les permissions pour la première fois, l'application ne lançait pas le scan, l'obligeant à cliquer une seconde fois.
    *   **Solution** : La logique dans `MainActivity.kt` a été corrigée pour s'assurer que la vérification des paramètres de localisation et le lancement du scan sont correctement enchaînés après l'obtention des permissions. Un état (`shouldCheckLocationSettings`) a été introduit pour fiabiliser ce flux.

3.  **Nettoyage de la configuration Gradle** :
    *   **Problème** : La compilation générait des avertissements concernant des options Gradle obsolètes et des plugins inutiles.
    *   **Solution** : Les options obsolètes ont été supprimées du fichier `gradle.properties` et le plugin `org.jetbrains.kotlin.android` a été retiré du fichier `app/build.gradle.kts`. Cela a permis d'obtenir une compilation propre, sans erreurs ni avertissements.

4.  **Fiabilisation de la demande d'activation du GPS** :
    *   **Problème** : La demande d'activation du GPS ne s'affichait pas de manière fiable sur Android 13 et versions supérieures lorsque le service de localisation était désactivé.
    *   **Solution** : La vérification manuelle de l'état de la localisation (`locationManager.isLocationEnabled`) a été remplacée par l'utilisation de l'API `SettingsClient` de Google Play Services. Cette approche moderne et recommandée par Google permet de vérifier les paramètres de localisation et de présenter à l'utilisateur une boîte de dialogue système standard pour les activer, garantissant un comportement cohérent et fiable sur tous les appareils et toutes les versions d'Android. La modification a été effectuée dans `MainActivity.kt`.

5.  **Fiabilisation de la détection de l'état du Wi-Fi** :
    *   **Problème** : Sur certains terminaux, l'application indiquait à tort que le Wi-Fi était désactivé à cause de l'utilisation d'une API obsolète (`isWifiEnabled`).
    *   **Solution** : Remplacement de l'API dépréciée par la méthode moderne et fiable `wifiManager.wifiState` dans `WifiViewModel.kt` pour vérifier l'état du Wi-Fi. Cela garantit une détection correcte sur toutes les versions d'Android.

6.  **Centralisation des couleurs** :
    *   **Problème** : Les couleurs de l'application étaient définies à plusieurs endroits, ce qui rendait la maintenance difficile.
    *   **Solution** : Toutes les couleurs ont été centralisées dans le fichier `res/values/colors.xml`. La fonction `getSignalColor` a été déplacée dans `WifiUtils.kt` et modifiée pour utiliser des `colorResource`.

7.  **Amélioration de l'ergonomie et de la lisibilité de l'interface** :
    *   **Problème** : L'affichage était trop dense et l'AP connecté ne se démarquait pas assez.
    *   **Solution** : Réduction des marges et de la taille des polices dans la liste des APs (`ScanResultsScreen.kt`). Pour mieux identifier l'AP connecté, tous les autres APs sont maintenant affichés avec une transparence de 60%, faisant ressortir l'AP actif par contraste.

8.  **Correction de la superposition des calques UI** :
    *   **Problème** : La roue de chargement et le message des coordonnées GPS apparaissaient sous la liste des résultats de scan.
    *   **Solution** : L'ordre de rendu dans `MainActivity.kt` a été corrigé pour que la liste des APs soit dessinée en premier, garantissant que la roue de chargement et le message GPS s'affichent toujours au premier plan.

9.  **Refonte de l'accès au Changelog** :
    *   **Problème** : Le changelog était enfoui dans la fenêtre "À propos", le rendant peu accessible.
    *   **Solution** : Le changelog a été extrait dans sa propre fenêtre modale (`ChangelogDialog.kt`). Un lien a été ajouté sur l'écran d'accueil (`HomeScreen.kt`) et dans la fenêtre "À propos" (`CreditsDialog.kt`) pour l'afficher.

### v0.9.2

1.  **Fiabilisation de la collecte de données** :
    *   **Problème** : Des scans trop rapides provoquaient une erreur système (throttling), ce qui entraînait une perte de toutes les données collectées.
    *   **Solution** : La logique du `WifiViewModel.kt` a été modifiée pour que les résultats de scan soient conservés même en cas d'échec d'un nouveau scan. L'état `_isScanning` est également géré plus finement.

2.  **Mise en place du mode "Cartographie"** :
    *   **Problème** : Chaque nouveau scan écrasait les données des points d'accès déjà détectés, empêchant une analyse historique de la couverture Wi-Fi.
    *   **Solution** : Le `WifiViewModel.kt` a été profondément modifié pour adopter une logique d'agrégation. Au lieu de remplacer les données, chaque scan ajoute maintenant ses résultats à une liste historique (`_allHistoricalScanResults`). L'affichage à l'écran présente une synthèse des signaux les plus forts, mais l'export CSV contient l'intégralité des mesures collectées.

3.  **Synchronisation du GPS avec chaque Scan** :
    *   **Problème** : La récupération des coordonnées GPS n'était pas synchronisée avec le scan Wi-Fi, résultant en des localisations vides ("N/A") dans le fichier CSV.
    *   **Solution** : La fonction `startScan` dans `WifiViewModel.kt` a été restructurée. Elle lance maintenant d'abord la récupération de la position GPS et attend son résultat avant de déclencher le scan Wi-Fi. Cela garantit que chaque série de résultats de scan est correctement associée aux coordonnées géographiques du moment.

---
*Ce document sera mis à jour au fur et à mesure de notre progression.*
