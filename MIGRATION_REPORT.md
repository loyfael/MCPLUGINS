# Migration Report

## Scope

Migration complete des projets traites vers:

- Paper API `26.1.2.build.72-stable`
- Java `25`
- Gradle Wrapper `9.4.1`
- Shadow `9.4.3`

## Versions retenues

- Kotlin Gradle Plugin: `2.4.0`
- Triumph GUI: `3.1.11`
- PlaceholderAPI: `2.11.5` ou `2.11.6` selon le projet existant
- HikariCP: `5.0.1`
- SQLite JDBC: `3.46.0.0`
- MySQL Connector/J: `8.0.33` ou version conservee du projet si deja figee
- Lands API: `7.27.1`

## Projets migres et validates

- LoyChatFillCommand
- LoyLecternFirstPage
- LoyVoidBounce
- LoyFlightImproved
- AntiVillagerLag
- KotlinPaperMCPluginBoilerPlate
- LandBlockLimiter
- LoyCompassMenu
- AetherSafeMode
- LoyCustomMobs
- NuvaPeoples
- TriumphGUI-Kotlin-Tutorial
- AetherPlayerDelivery
- AetherPlayerShop2
- AetherPlayerShop
- KrakenLevels2
- LoyFishing
- PaperMC-Plugin-Boilerplate
- NuvaPlayerSync

## Travaux realises

- remplacement des builds Maven par Gradle ou Gradle Kotlin DSL selon le projet
- passage des toolchains Java et compilations sur Java 25
- mise a jour des dependances Paper vers `26.1.2.build.72-stable`
- mise a jour des placeholders `plugin.yml` vers les variables Gradle (`${version}`, `${description}`, `${url}` selon les cas)
- mise a jour des `api-version` vers `1.21` lorsque necessaire
- conservation des comportements existants, des fichiers de configuration et des donnees du plugin
- suppression des `pom.xml`, `dependency-reduced-pom.xml` et dossiers `target/` sur les projets rebuild avec succes
- verification finale: plus aucun `pom.xml` ni `dependency-reduced-pom.xml` restant dans le workspace

## Adaptations de code notables

### LandBlockLimiter

- migration de l'identification des lands vers les ULID de Lands API `7.27.1`
- remplacement des anciens acces `getId()` et `getLandById(int)`

### LoyFishing

- remplacement des constantes `Particle` obsoletes par les noms Paper modernes:
  - `VILLAGER_HAPPY` -> `HAPPY_VILLAGER`
  - `WATER_SPLASH` -> `SPLASH`
  - `SPELL_WITCH` -> `WITCH`
  - `ENCHANTMENT_TABLE` -> `ENCHANT`

## Validation effectuee

Chaque projet migre a ete valide par:

1. `gradlew.bat clean build`
2. inspection du JAR final dans `build/libs`
3. verification de la presence du `plugin.yml` et de la classe principale
4. verification de l'absence de shading des APIs serveur en `compileOnly`
5. verification des relocations pour les bibliotheques embarquees quand applicable

## Relocations confirmees

- AntiVillagerLag: bStats relocalise
- NuvaPeoples: Triumph GUI relocalise
- TriumphGUI-Kotlin-Tutorial: Triumph GUI relocalise
- AetherPlayerDelivery: Triumph GUI relocalise
- AetherPlayerShop2: Triumph GUI relocalise
- KrakenLevels2: MongoDB, BSON, SLF4J, XSeries et Triumph GUI relocalises

## Avertissements residuels

- plusieurs projets Kotlin ou Java compilent avec warnings de deprecation mineurs sans bloquer la build
- LoyFishing compile avec warnings sur l'API `Biome` marquee pour suppression future, mais sans erreur de compilation sur Paper `26.1.2`

## Resultat

Les projets listes dans ce rapport compilent maintenant sous Gradle avec Java 25 et Paper 26.1.2, et produisent un JAR final dans `build/libs/`.
## Mise a jour Minecraft 26.2

- Paper API `26.1.2.build.72-stable` -> `26.2.build.133-stable` (AetherialShop: `26.2.build.+`)
- `api-version` des `plugin.yml` passe de `1.21` a `26.2` (les plugins ne se chargent plus sur un serveur < 26.2)
- Java 25, Gradle 9.4.1, Shadow 9.4.3 et Kotlin 2.4.0 inchanges (deja compatibles)
- Code audite contre les suppressions de Paper 26.2 / Adventure 5 (`BookMeta` qui n'etend plus `Book`,
  `MessageType`, `sendMessage(Identity, ...)`, `PaperComponents`, serializers de `UnsafeValues`,
  PDC sur les lits) : aucun usage trouve
- AetherialShop : ajout de la toolchain Java 25 (la compilation utilisait le JDK de Gradle et echouait)
- Compilation verifiee : les 20 projets passent `./gradlew build` sur Paper `26.2.build.133-stable`
- API marquees pour suppression remplacees (plus aucun avertissement `[removal]`) :
  - LoyFishing : `Biome.valueOf/values` et `name()` remplaces par `BiomeUtils` (registre Paper), memes noms en config
  - AetherPlayerShop : suppression de `PriceChangePrompt` (API `org.bukkit.conversations`), code mort remplace par `ShopEditMenuGUI`
