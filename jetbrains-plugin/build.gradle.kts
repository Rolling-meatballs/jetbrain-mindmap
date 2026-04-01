import org.gradle.api.file.FileSystemOperations
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.stream.Collectors
import javax.inject.Inject

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

abstract class SyncBundledWebUiTask @Inject constructor(
    private val fs: FileSystemOperations
) : DefaultTask() {
    @get:Internal
    abstract val outputDir: org.gradle.api.file.DirectoryProperty

    @get:Internal
    abstract val sourceWebUi: org.gradle.api.file.DirectoryProperty

    @TaskAction
    fun sync() {
        val outDir = outputDir.get().asFile
        if (outDir.exists()) outDir.deleteRecursively()

        fs.copy {
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
            into(outDir)
        }

        val outputPath = outDir.toPath()
        val manifest = outputPath.resolve("manifest.txt")
        val lines = Files.walk(outputPath).use { walk ->
            walk
                .filter { Files.isRegularFile(it) }
                .map { outputPath.relativize(it).toString().replace('\\', '/') }
                .filter { it != "manifest.txt" }
                .sorted()
                .collect(Collectors.toList())
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

val syncBundledWebUi by tasks.registering(SyncBundledWebUiTask::class) {
    doNotTrackState("webui source tree contains large bower_components/node_modules that Gradle 9 cannot snapshot reliably")
    outputDir = bundledWebUiOutput
    sourceWebUi = layout.projectDirectory.dir("../webui")
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
