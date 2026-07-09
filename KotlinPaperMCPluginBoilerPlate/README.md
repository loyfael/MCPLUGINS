# Kotlin PaperMC Plugin Boilerplate

Boilerplate Kotlin pour plugins Paper migre sur Gradle 9.4.1, Java 25 et Paper 26.1.2.

## Versions

- Java: 25
- Kotlin: 2.4.0
- Paper API: 26.1.2.build.72-stable
- Gradle Wrapper: 9.4.1
- Shadow: 9.4.3
- PlaceholderAPI: 2.11.5
- HikariCP: 5.0.1
- MySQL Connector/J: 8.0.33
- SQLite JDBC: 3.46.0.0

## Contenu

Le template inclut:

- une classe principale Kotlin pour Paper
- un gestionnaire de configuration YAML
- un exemple de commandes et listeners
- un gestionnaire de base de donnees MySQL ou SQLite
- le shading des dependances runtime utiles
- des placeholders `plugin.yml` compatibles Gradle

## Build Windows

```powershell
.\gradlew.bat clean build
```

Le JAR final est produit dans `build/libs/`.

## Structure

```text
KotlinPaperMCPluginBoilerPlate/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
├── gradlew
├── gradlew.bat
└── src/main/
    ├── kotlin/loyfael/
    └── resources/
```

## Personnalisation rapide

1. Modifiez `group`, `version` et `description` dans `build.gradle.kts`.
2. Adaptez `name`, `main`, `author` et les permissions dans `src/main/resources/plugin.yml`.
3. Renommez le package `loyfael` selon votre plugin.
4. Ajustez `config.yml` et les options de base de donnees si necessaire.

## Dependances embarquees

Le build embarque et relocalise les bibliotheques runtime necessaires:

- Kotlin Standard Library
- HikariCP
- MySQL Connector/J
- SQLite JDBC

Paper API et PlaceholderAPI restent en `compileOnly` et ne sont pas inclus dans le JAR final.

## Notes de migration

- Le projet n'utilise plus Maven.
- Les placeholders `plugin.yml` attendent `${version}`, `${description}` et `${url}` cotes Gradle.
- Le toolchain Java et la compilation Kotlin ciblent Java 25.
