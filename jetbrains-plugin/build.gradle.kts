import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.stream.Collectors

plugins {
    id("java")
    kotlin("jvm") version "2.3.10"
    // Pinned to 2.11.0: 2.12.0+ requires Gradle 9.0, but the Kotlin plugin
    // (2.3.10) publishes no Gradle 9 variant, so Gradle 9 + Kotlin fails to
    // resolve. 2.11.0 supports Gradle 8.13 and still targets 2026.1 (build 261).
    id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "com.souche.mindmap"
version = "0.1.7"

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

abstract class SyncBundledWebUiTask : DefaultTask() {
    @get:Internal
    abstract val outputDir: org.gradle.api.file.DirectoryProperty

    @get:Internal
    abstract val sourceWebUi: org.gradle.api.file.DirectoryProperty

    @TaskAction
    fun sync() {
        val outputPath = outputDir.get().asFile.toPath()

        // Robust clean: nio bottom-up delete. Kotlin's File.deleteRecursively()
        // fails silently/partially, leaving stale entries that then collide with
        // the copy below (FileAlreadyExistsException on re-run over a populated
        // dir). Deleting deepest-first empties dirs before removing them, and
        // surfaces any real delete failure instead of a confusing collision.
        if (Files.exists(outputPath)) {
            Files.walk(outputPath).use { stream ->
                stream.sorted { a, b -> b.compareTo(a) }.forEach { Files.deleteIfExists(it) }
            }
        }
        Files.createDirectories(outputPath)

        // Plain nio copy WITHOUT COPY_ATTRIBUTES. Gradle's Copy/fs.copy mirrors
        // each source file's POSIX mode onto the destination, which fails under
        // Gradle 8.13 on macOS ("Could not set file mode 644 on ..."). The
        // plugin only needs the file bytes in its resources, so we copy content
        // and let the default umask set permissions. We walk only the included
        // subtrees (not all of node_modules) to keep this fast.
        val srcRoot = sourceWebUi.get().asFile.toPath()
        if (Files.exists(srcRoot)) {
            val includeFiles = listOf("mindmap.html", "main.js", "favicon.ico")
            val includeDirs = listOf(
                "dist", "ui", "server", "src",
                "bower_components", "node_modules/kity", "node_modules/kityminder-core"
            )
            val roots = (includeFiles + includeDirs).map { srcRoot.resolve(it) }
            for (root in roots) {
                if (!Files.exists(root)) continue
                Files.walk(root).use { walk ->
                    walk.filter { Files.isRegularFile(it) }.forEach { file ->
                        val dest = outputPath.resolve(srcRoot.relativize(file).toString())
                        Files.createDirectories(dest.parent)
                        Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
        }

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

// React editor (JCEF scheme handler): bundle the webui-react build into plugin
// resources under react-poc/ so MindmapReactSchemeHandlerFactory can serve it.
// Source is the formal webui-react project (the react-spike was M0's seed).
val reactPocOutput = layout.buildDirectory.dir("generated-resources/react-poc")
val syncReactPoc by tasks.registering(Copy::class) {
    // Clear stale hashed chunks from prior builds so only the current dist ships.
    doFirst { delete(reactPocOutput) }
    from(layout.projectDirectory.dir("../webui-react/dist"))
    into(reactPocOutput)
}

tasks {
    processResources {
        dependsOn(syncBundledWebUi)
        from(bundledWebUiOutput) {
            into("bundled-webui")
        }
        dependsOn(syncReactPoc)
        from(reactPocOutput) {
            into("react-poc")
        }
    }

    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    withType<KotlinCompile> {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
        compilerOptions.freeCompilerArgs.add("-jvm-default=no-compatibility")
    }

    runIde {
        jvmArgs("-Xmx2g")
        // PoC: route .km/.xmind opens through the React (scheme handler) editor
        // path in the sandbox IDE. Absent in production installs -> file:// webui.
        systemProperty("mindmap.react.editor", "true")
    }

    test {
        useJUnitPlatform()
    }
}
