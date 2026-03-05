import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    kotlin("jvm") version "2.1.20"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.souche.mindmap"
version = "0.1.0"

repositories {
    mavenCentral()
}

intellij {
    version.set("2024.3")
    type.set("IC")
    plugins.set(emptyList())
}

tasks {
    patchPluginXml {
        sinceBuild.set("243")
        untilBuild.set("251.*")
    }

    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    runIde {
        jvmArgs("-Xmx2g")
    }
}
