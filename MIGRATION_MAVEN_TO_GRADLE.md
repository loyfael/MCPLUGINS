# Guide générique : migration Maven -> Gradle

> **Objectif** : convertir un projet Java (mono-module) de Maven vers Gradle de façon propre, reproductible, et publiable dans un repository.

---

## Table des matières

1. [Portée et prérequis](#1-portée-et-prérequis)
2. [Checklist rapide](#2-checklist-rapide)
3. [Étape 1 - Créer les fichiers Gradle](#3-etape-1---creer-les-fichiers-gradle)
4. [Étape 2 - Convertir le `pom.xml` vers `build.gradle`](#4-etape-2---convertir-le-pomxml-vers-buildgradle)
5. [Étape 3 - Générer le wrapper Gradle](#5-etape-3---generer-le-wrapper-gradle)
6. [Étape 4 - Vérifier le build](#6-etape-4---verifier-le-build)
7. [Étape 5 - Nettoyer Maven](#7-etape-5---nettoyer-maven)
8. [Étape 6 - Forcer IntelliJ à utiliser Gradle](#8-etape-6---forcer-intellij-a-utiliser-gradle)
9. [Dépannage courant](#9-depannage-courant)
10. [Templates prêts à copier](#10-templates-prets-a-copier)
11. [Structure finale attendue](#11-structure-finale-attendue)

---

## 1. Portée et prérequis

Ce guide couvre :
- projet Java standard (`src/main/java`, `src/main/resources`)
- migration de dépendances et plugins Maven vers Gradle
- génération d'un wrapper pour un build reproductible
- bascule IntelliJ de Maven vers Gradle

Prérequis minimaux :
- un `pom.xml` fonctionnel
- un JDK installé
- IntelliJ IDEA (optionnel mais recommandé)

---

## 2. Checklist rapide

- [ ] Créer `settings.gradle`
- [ ] Créer `build.gradle` en convertissant dépendances + plugins
- [ ] Générer `gradlew` / `gradlew.bat`
- [ ] Lancer un build Gradle (`build` ou `shadowJar`)
- [ ] Supprimer les fichiers Maven (`pom.xml`, `dependency-reduced-pom.xml`, `target/`)
- [ ] Recharger le projet dans IntelliJ via Gradle
- [ ] Mettre à jour la doc du repo

---

## 3. Etape 1 - Creer les fichiers Gradle

Créer `settings.gradle` :

```groovy
rootProject.name = '<NOM_DU_PROJET>'
```

Créer `build.gradle` (version minimale) :

```groovy
plugins {
    id 'java'
}

group = '<GROUP_ID>'
version = '<VERSION>'

repositories {
    mavenCentral()
}

dependencies {
    // Exemple
    testImplementation 'org.junit.jupiter:junit-jupiter:5.11.4'
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
    // Ajustez selon votre projet
    options.release = 21
}
```

---

## 4. Etape 2 - Convertir le `pom.xml` vers `build.gradle`

### Mapping des scopes

| Maven | Gradle |
|---|---|
| `provided` | `compileOnly` |
| `compile` | `implementation` |
| `runtime` | `runtimeOnly` |
| `test` | `testImplementation` |

### Mapping des plugins les plus courants

| Maven Plugin | Gradle |
|---|---|
| `maven-compiler-plugin` | config `JavaCompile` / `java { toolchain { ... } }` |
| `maven-surefire-plugin` | `test { useJUnitPlatform() }` |
| `maven-shade-plugin` | `com.gradleup.shadow` |

### Cas fat JAR (équivalent `maven-shade-plugin`)

Ajouter le plugin Shadow :

```groovy
plugins {
    id 'java'
    id 'com.gradleup.shadow' version '9.4.3'
}
```

Configurer la tâche :

```groovy
shadowJar {
    archiveClassifier.set('')
    mergeServiceFiles()

    // Exemple de relocation
    // relocate 'com.google.gson', 'my.project.libs.gson'
}

build.dependsOn shadowJar
```

---

## 5. Etape 3 - Generer le wrapper Gradle

Depuis la racine du projet :

```powershell
gradle wrapper --gradle-version 9.4.1
```

Fichiers générés :
- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradle/wrapper/gradle-wrapper.jar`

---

## 6. Etape 4 - Verifier le build

Sous Windows :

```powershell
.\gradlew clean build
```

Si vous utilisez Shadow :

```powershell
.\gradlew clean shadowJar
```

Sous Linux/macOS :

```bash
./gradlew clean build
./gradlew clean shadowJar
```

---

## 7. Etape 5 - Nettoyer Maven

Supprimer ces éléments (après validation du build Gradle) :
- `pom.xml`
- `dependency-reduced-pom.xml` (si présent)
- `target/`

Pourquoi :
- éviter une double source de vérité sur le build
- éviter des builds différents entre développeurs
- clarifier l'outillage du repository

---

## 8. Etape 6 - Forcer IntelliJ a utiliser Gradle

### Symptômes

- IntelliJ continue d'afficher Maven
- les dépendances ne correspondent pas à `build.gradle`
- pas de panneau Gradle actif

### Actions recommandées

1. Recharger le projet Gradle :

```text
Gradle Tool Window -> Reload All Gradle Projects
```

2. Ou via menu :

```text
File -> Sync Project with Gradle Files
```

3. Si besoin, fermer puis rouvrir le projet.

### Nettoyage IntelliJ optionnel (si cache Maven persistant)

- retirer le composant `MavenProjectsManager` de `.idea/misc.xml`
- supprimer `.idea/jarRepositories.xml` (si présent)
- s'assurer que `.idea/gradle.xml` existe après import Gradle

---

## 9. Depannage courant

### Erreur de compatibilité Java/Gradle

Exemple : `Unsupported class file major version ...`

Solution :
- mettre à jour la version de Gradle Wrapper
- aligner la version Java de compilation (`options.release`)

```powershell
gradle wrapper --gradle-version <VERSION_COMPATIBLE>
```

### Conflit de dépendances transitives

Gradle peut être plus strict que Maven (capabilities, variantes).

Solution type : exclusion ciblée.

```groovy
implementation('<GROUP>:<ARTIFACT>:<VERSION>') {
    exclude group: '<GROUP_A_EXCLURE>', module: '<MODULE_A_EXCLURE>'
}
```

### Problème avec plugin Shadow

Si erreur ASM/Groovy sur anciennes versions :
- mettre à jour vers une version récente de `com.gradleup.shadow`

---

## 10. Templates prets a copier

### `settings.gradle`

```groovy
rootProject.name = '<NOM_DU_PROJET>'
```

### `build.gradle` (base générique)

```groovy
plugins {
    id 'java'
    // Décommentez si vous voulez un fat JAR
    // id 'com.gradleup.shadow' version '9.4.3'
}

group = '<GROUP_ID>'
version = '<VERSION>'

repositories {
    mavenCentral()
    // maven { url = '<URL_DEPOT_SUPPLEMENTAIRE>' }
}

dependencies {
    // compileOnly '<G>:<A>:<V>'
    // implementation '<G>:<A>:<V>'
    // runtimeOnly '<G>:<A>:<V>'
    testImplementation 'org.junit.jupiter:junit-jupiter:5.11.4'
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
    options.release = 21
}

test {
    useJUnitPlatform()
}

// Si Shadow est activé :
// shadowJar {
//     archiveClassifier.set('')
//     mergeServiceFiles()
// }
// build.dependsOn shadowJar
```

### `.gitignore` recommandé

```gitignore
# Gradle
.gradle/
build/

# Anciennes sorties Maven (si historique)
target/

# IntelliJ
.idea/
*.iml
```

---

## 11. Structure finale attendue

```text
<PROJECT_ROOT>/
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── src/
│   └── main/
│       ├── java/
│       └── resources/
└── build/   (généré)
```

Fichiers/dossiers Maven retirés :
- `pom.xml`
- `dependency-reduced-pom.xml` (si existant)
- `target/`

---

## Notes pour publication en repository

- Garder ce guide à la racine sous un nom explicite (`MIGRATION_MAVEN_TO_GRADLE.md`)
- Ajouter un lien depuis `README.md`
- Indiquer la version minimale de Java et de Gradle supportées
- Documenter clairement la commande de build officielle (`./gradlew build` ou `./gradlew shadowJar`)

