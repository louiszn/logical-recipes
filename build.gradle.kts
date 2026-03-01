import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar

plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.3.0"
}

repositories {
    mavenCentral()
}

group = project.properties["group"] as String
version = project.properties["version"] as String
description = project.properties["description"] as String

val minecraftVersion = project.properties["minecraft-version"] as String

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.REOBF_PRODUCTION

tasks.assemble {
    dependsOn(tasks.reobfJar)
}

dependencies {
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
    implementation("de.exlll:configlib-paper:4.8.1")
    implementation("org.incendo:cloud-paper:2.0.0-beta.10")
    implementation("org.incendo:cloud-minecraft-extras:2.0.0-beta.10")
    implementation("org.incendo:cloud-annotations:2.0.0")
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    processResources {
        filteringCharset = "UTF-8"

        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }

    compileJava {
        options.release = 21
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    shadowJar {
        configurations = listOf(project.configurations.runtimeClasspath.get())
        relocate("de.exlll.configlib", "xyz.louiszn.logicalrecipes.libs.configlib")
        archiveClassifier.set("")
    }

    runServer {
        minecraftVersion(minecraftVersion)
    }
}
