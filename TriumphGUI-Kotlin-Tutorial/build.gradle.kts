import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.0"
    id("com.gradleup.shadow") version "9.4.3"
}

group = "com.exemple"
version = "1.0.0-SNAPSHOT"
description = "TriumphGUI Kotlin tutorial for Paper 26.1.2"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget("25"))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.triumphteam.dev/releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.72-stable")

    implementation(kotlin("stdlib"))
    implementation("dev.triumphteam:triumph-gui:3.1.11")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

sourceSets {
    main {
        java.setSrcDirs(emptyList<String>())
        kotlin.setSrcDirs(listOf("src/main/kotlin"))
        resources.setSrcDirs(listOf("src/main/resources"))
    }
    test {
        java.setSrcDirs(emptyList<String>())
        kotlin.setSrcDirs(listOf("src/test/kotlin"))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(mapOf("version" to project.version))
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<ShadowJar>().configureEach {
    archiveBaseName.set("TriumphGUI-Kotlin-Tutorial")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())

    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("META-INF/MANIFEST.MF")
    exclude("META-INF/versions/9/module-info.class")
    exclude("module-info.class")

    mergeServiceFiles()
    relocate("dev.triumphteam.gui", "loyfael.libs.triumphgui")
}

tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}