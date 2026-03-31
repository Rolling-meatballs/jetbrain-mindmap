import org.gradle.api.tasks.Sync
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.streams.toList

plugins {
    id("java")
    kotlin("jvm") version "2.3.10"
    id("org.jetbrains.intellij.platform") version "2.13.1"
}

group = "com.souche.mindmap"
version = "0.1.5"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("org.json:json:20240303")
    testImplementation(kotlin("test"))
    // Required by IntelliJ JUnit5 test environment initializer at runtime.
    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        intellijIdea("2026.1")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "261"
            untilBuild = "261.*"
        }
    }

    publishing {
        token = providers.environmentVariable("JETBRAINS_MARKETPLACE_TOKEN")
    }
}

val bundledWebUiOutput = layout.buildDirectory.dir("generated-resources/bundled-webui")

val syncBundledWebUi by tasks.registering {
    doNotTrackState("webui source tree contains large bower_components/node_modules that Gradle 9 cannot snapshot reliably")
    outputs.dir(bundledWebUiOutput)

    doLast {
        val outputDir = bundledWebUiOutput.get().asFile
        if (outputDir.exists()) outputDir.deleteRecursively()

        val sourceWebUi = file("../webui")
        project.copy {
            from(sourceWebUi) {
                include("mindmap.html")
                include("main.js")
                include("favicon.ico")
                include("dist/**")
                include("ui/**")
                include("server/**")
                include("src/**")
                include("bower_components/**")
                include("node_modules/kity/**")
                include("node_modules/kityminder-core/**")
            }
            into(outputDir)
        }

        val outputPath = outputDir.toPath()
        val manifest = outputPath.resolve("manifest.txt")
        val lines = Files.walk(outputPath).use { walk ->
            walk
                .filter { Files.isRegularFile(it) }
                .map { outputPath.relativize(it).toString().replace('\\', '/') }
                .filter { it != "manifest.txt" }
                .sorted()
                .toList()
        }

        Files.write(
            manifest,
            lines,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        )
    }
}

tasks {
    processResources {
        dependsOn(syncBundledWebUi)
        from(bundledWebUiOutput) {
            into("bundled-webui")
        }
    }

    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    withType<KotlinCompile> {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
    }

    runIde {
        jvmArgs("-Xmx2g")
    }

    test {
        useJUnitPlatform()
    }
}
