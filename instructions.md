# Fichier d'instructions pour l'IA

*Dernière mise à jour : 31/05/2024*

## Règles de Collaboration avec l'IA (IMPORTANT)

**RÈGLE D'OR : L'IA NE DOIT JAMAIS, SOUS AUCUN PRÉTEXTE, MODIFIER LE CODE DE SA PROPRE INITIATIVE. TOUTE MODIFICATION, MÊME MINEURE, DOIT FAIRE L'OBJET D'UNE DEMANDE, ÊTRE REFORMULÉE POUR VALIDATION, ET RECEVOIR VOTRE APPROBATION EXPLICITE AVANT D'ÊTRE APPLIQUÉE.**

1.  **Attente du feu vert** : Après avoir proposé une modification, l'IA doit **impérativement attendre** la réponse de l'utilisateur avant d'exécuter l'action. Aucune initiative ne sera tolérée.
2.  **Consultation systématique** : L'IA (vous ou une autre) doit **impérativement et systématiquement** consulter ce fichier avant toute action. Il contient le contexte, les objectifs et les règles du projet.
3.  **Confirmation de la demande** : L'IA doit systématiquement reformuler la demande avec ses propres mots pour valider sa compréhension.
4.  **Exécution stricte** : L'IA doit se cantonner **uniquement** à la tâche demandée, sans prendre aucune initiative non sollicitée.
5.  **Demande d'avis** : Si l'IA a une suggestion ou un avis, elle doit d'abord poser la question et obtenir une autorisation explicite avant d'agir.
6.  **Auto-vérification rigoureuse** : Après chaque modification, l'IA doit vérifier l'intégralité de son travail pour s'assurer qu'il correspond à la demande, et qu'il ne contient aucun oubli, erreur d'inattention ou conflit.
7.  **Build après modification majeure** : Après chaque modification importante du code (ajout de fonctionnalité, refactoring, correction de bug...), l'IA doit lancer un build du projet pour s'assurer que la compilation réussit. En cas d'échec, elle doit analyser les erreurs, identifier la cause et corriger le problème.
8.  **Clôture** : Une fois la tâche terminée et vérifiée, l'IA doit clore sa réponse.
9.  **Langue d'échange** : Toutes les discussions et instructions doivent être en français.
10. **Externalisation des chaînes** : Tout le texte visible par l'utilisateur (messages, toasts, labels, etc.) doit être externalisé dans les fichiers de ressources `strings.xml`. Aucun texte ne doit être codé en dur dans les fichiers Kotlin.
11. **Gestion des caractères spéciaux** : Une attention extrême doit être portée à l'échappement des caractères spéciaux, tant dans les fichiers de ressources XML (ex: `\'` pour une apostrophe) que dans le code Kotlin (ex: `\"` pour un guillemet dans une chaîne). La validation doit être systématique pour éviter les erreurs de syntaxe et de compilation.

---

## Incidents et Leçons Apprises

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
-   **Classement par puissance** : La liste des réseaux affichée doit être une synthèse triée par puissance de signal reçu (RSSI), du plus fort au plus faible, en ne montrant que la meilleure détection pour chaque AP.
-   **Visualisation de la puissance** : La couleur de chaque élément de la liste doit refléter la qualité du signal (par exemple, du vert pour un signal fort au rouge pour un signal faible).
-   **Identification de la connexion active** : Un logo "Check" (✓) doit apparaître distinctement à côté du SSID auquel le terminal est actuellement connecté.
-   **Agrégation des données (Mode Cartographie)** : Conserver en mémoire **l'intégralité des données de chaque scan**, sans jamais écraser les résultats précédents. Chaque détection d'un AP doit être ajoutée comme une nouvelle entrée historique.
-   **Persistance des données de session** : Les données agrégées ne doivent pas être effacées tant que l'application n'est pas explicitement fermée par l'utilisateur.
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

-   `instructions.md`: Ce document.
-   `MainActivity.kt`: Point d'entrée de l'UI.
-   `WifiViewModel.kt`: Cœur logique (gestion des scans, data, GPS, export).
-   `WifiUtils.kt`: Fonctions utilitaires pures.
-   `ui/ScanResultsScreen.kt`: Affichage de la liste des scans.
-   Autres fichiers UI et ressources.

## Changelog

L'historique des versions est maintenu dans `strings.xml` (`changelog_entries`).

## Modifications Effectuées

### v0.9.3 (en cours)

1.  **Amélioration de l'ergonomie et de la lisibilité de l'interface** :
    *   **Problème** : L'affichage était trop dense et l'AP connecté ne se démarquait pas assez.
    *   **Solution** : Réduction des marges et de la taille des polices dans la liste des APs (`ScanResultsScreen.kt`). Pour mieux identifier l'AP connecté, tous les autres APs sont maintenant affichés avec une transparence de 60%, faisant ressortir l'AP actif par contraste.

2.  **Correction de la superposition des calques UI** :
    *   **Problème** : La roue de chargement et le message des coordonnées GPS apparaissaient sous la liste des résultats de scan.
    *   **Solution** : L'ordre de rendu dans `MainActivity.kt` a été corrigé pour que la liste des APs soit dessinée en premier, garantissant que la roue de chargement et le message GPS s'affichent toujours au premier plan.

3.  **Refonte de l'accès au Changelog** :
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
