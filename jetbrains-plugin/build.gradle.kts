import org.gradle.api.tasks.Sync
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.streams.toList

plugins {
    id("java")
    kotlin("jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.5"
}

group = "com.souche.mindmap"
version = "0.1.0"

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
        pycharmProfessional("2025.3.3")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "253"
            untilBuild = "253.*"
        }
    }
}

val bundledWebUiOutput = layout.buildDirectory.dir("generated-resources/bundled-webui")

val syncBundledWebUi by tasks.registering(Sync::class) {
    val sourceWebUi = file("../webui")
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
    into(bundledWebUiOutput)

    doLast {
        val outputPath = bundledWebUiOutput.get().asFile.toPath()
        if (!Files.exists(outputPath)) {
            return@doLast
        }

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
