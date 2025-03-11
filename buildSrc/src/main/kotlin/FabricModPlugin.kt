import com.modrinth.minotaur.ModrinthExtension
import com.modrinth.minotaur.dependencies.DependencyType.EMBEDDED
import com.modrinth.minotaur.dependencies.DependencyType.REQUIRED
import com.modrinth.minotaur.dependencies.ModDependency
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.api.fabricapi.FabricApiExtension
import net.fabricmc.loom.task.RemapJarTask
import net.swiftzer.semver.SemVer
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.utils.extendsFrom

private val fabricModulesDefault = listOf(
    "fabric-api-base",
    "fabric-networking-api-v1",
    "fabric-resource-loader-v0",
)

private val fabricDependencyModules = listOf(
    FabricModuleDependency(
        "fabric-api",
        "P7dR8mSH",
    ),
    FabricModuleDependency(
        "fabric-language-kotlin",
        "Ha28R6CL",
    ),
)

abstract class FabricModExtension(
    private val project: Project
) {
    @get:Input
    abstract val supportedVersions: ListProperty<String>

    @get:Input
    val fabricModules: ListProperty<String> = project.objects.listProperty()

    fun module(name: String): Dependency {
        val fabricApi = project.extensions.getByName<FabricApiExtension>("fabricApi")
        val module = fabricApi.module(name, project.fabricVersion())
        fabricModules.add(name)
        return module
    }
}

abstract class WriteFabricModJsonTask : DefaultTask() {
    @get:Input
    val version: Property<String> = project.objects.property(String::class).convention(
        project.provider {
            project.version.toString()
        }
    )

    @get:Input
    val minecraftVersion: Property<String> = project.objects.property(String::class).convention(
        project.provider {
            project.properties["minecraft_version"] as String
        }
    )

    @get:Input
    val supportedVersions: ListProperty<String> = project.objects.listProperty<String>().convention(
        project.extensions.getByName<FabricModExtension>("fabricMod").supportedVersions
    )

    @get:Input
    val javaVersion: Property<String> = project.objects.property(String::class).convention(
        project.provider {
            project.extensions.getByName<JavaPluginExtension>("java")
                .targetCompatibility
                .majorVersion
        }
    )

    @get:Input
    val fabricModules: ListProperty<String> = project.objects.listProperty<String>().convention(
        project.provider {
            val v = mutableListOf<String>()
            if (project.modIncludeAll()) {
                v.addAll(fabricModulesDefault)
            } else {
                v.addAll(fabricDependencyModules.map { it.module })
            }
            v.addAll(
                project.extensions.getByName<FabricModExtension>("fabricMod")
                    .fabricModules.get()
            )
            v
        }
    )

    @get:InputFile
    val inputFile: RegularFileProperty = project.objects.fileProperty().convention(
        project.objects.fileProperty().fileValue(
            project.rootDir.resolve("fabric/fabric.mod-v1.json")
        )
    )

    @get:OutputFile
    val outputFile: RegularFileProperty = project.objects.fileProperty().convention(
        project.layout.buildDirectory.file("resources/main/fabric.mod.json")
    )

    @OptIn(ExperimentalSerializationApi::class)
    @TaskAction
    fun write() {
        val modJson = inputFile.asFile.get().inputStream().use { stream ->
            Json.decodeFromStream<JsonObject>(stream).toMutableMap()
        }
        modJson["version"] = JsonPrimitive(buildVersionStr(
            version.get(),
            project.versionSuffix().get(),
        ))
        modJson["depends"] = buildJsonObject {
            put("minecraft", supportedVersions.get().run {
                when (size) {
                    0 -> minecraftVersion.get()
                    1 -> get(0)
                    else -> ">=${first()} <=${last()}"
                }
            })
            put("java", ">=${javaVersion.get()}")
            fabricModules.get().forEach {
                put(it, "*")
            }
        }
        outputFile.asFile.get().outputStream().use { stream ->
            Json.encodeToStream(modJson, stream)
        }
    }
}

@Suppress("UnstableApiUsage")
class FabricModPlugin : Plugin<Project> {
    override fun apply(p: Project) {
        p.run {
            applyPlugins()
            createExtension()
            createTasks()
            setupConfigurations()
            setupDependencies()
            setupLoom()
            setupModrinth()
            setupTasks()
        }
    }

    private fun Project.applyPlugins() {
        plugins.apply("kotlin-common")
        plugins.apply("com.modrinth.minotaur")
        plugins.apply("fabric-loom")
    }

    private fun Project.createExtension() {
        extensions.create(
            "fabricMod",
            FabricModExtension::class,
            project,
        )
    }

    private fun Project.createTasks() {
        tasks.register<WriteFabricModJsonTask>(
            "writeFabricModJson",
        ) {
            listOf(
                "classes",
                "jar",
            ).forEach {
                tasks.named(it) {
                    dependsOn(this@register)
                }
            }
            mustRunAfter("processResources")
        }
    }

    private fun Project.setupConfigurations() {
        configurations {
            maybeInclude().let {
                modImpl().extendsFrom(it)
                if (modIncludeAll()) {
                    include().extendsFrom(it)
                }
            }
            named("includeable").let {
                impl().extendsFrom(it)
                include().extendsFrom(it)
            }
            adventure().let {
                modImpl().extendsFrom(it)
                include().extendsFrom(it)
            }
        }
    }

    private fun Project.setupDependencies() {
        val prop = properties
        val fabricApi = extensions.getByName<FabricApiExtension>("fabricApi")

        dependencies {
            fabricModulesDefault.map { id ->
                maybeInclude()(fabricApi.module(id, fabricVersion()))
            }
            adventure().let {
                it("net.kyori:adventure-key")
                it("net.kyori:examination-api")
                it("net.kyori:examination-string")
                it("net.kyori:adventure-text-serializer-gson")
            }

            impl()(project(":common"))
            config("minecraft")("com.mojang:minecraft:${minecraftVersion()}")
            config("mappings")("net.fabricmc:yarn:${prop["yarn_mappings"]}:v2")
            modImpl()("net.fabricmc:fabric-loader:${prop["loader_version"]}")
            modImpl()("net.fabricmc.fabric-api:fabric-api:${prop["fabric_version"]}")
            maybeInclude()("net.fabricmc:fabric-language-kotlin:1.10.19+kotlin.1.9.23")
        }
    }

    private fun Project.setupLoom() {
        extensions.configure<LoomGradleExtensionAPI>("loom") {
            accessWidenerPath = file("$projectDir/src/main/resources/pwam.accesswidener")
            enableTransitiveAccessWideners = true
            mixin {
                // Use uniform refmap entry name
                add(
                    extensions.getByType<JavaPluginExtension>()
                        .sourceSets
                        .named("main")
                        .get(),
                    "pwma.refmap.json"
                )
            }
        }
    }

    private fun Project.setupModrinth() {
        extensions.configure<ModrinthExtension>("modrinth") {
            token = System.getenv("MODRINTH_TOKEN")
            projectId = "PrkvGEnM"
            versionNumber = versionSuffix().map { suffix ->
                buildVersionStr(
                    project.version.toString(),
                    suffix,
                )
            }
            changelog = provider {
                """
                    Changelog is available on
                    [GitHub](https://github.com/turikhay/passwordmanager-for-authme/releases/tag/v${project.version})
                """.trimIndent()
            }
            versionType = provider {
                val preRelease = SemVer.parse(project.version as String).preRelease
                if (preRelease != null) {
                    if (preRelease.contains("beta")) {
                        "beta"
                    } else {
                        "alpha"
                    }
                } else {
                    "release"
                }
            }
            file = tasks.named("remapJar").map { it.outputs.files.singleFile }
            gameVersions.addAll(provider {
                extensions.getByType<FabricModExtension>().supportedVersions.get()
            })
            dependencies = fabricDependencyModules.map {
                ModDependency(
                    it.modrinthId,
                    if (modIncludeAll()) EMBEDDED else REQUIRED,
                )
            }
            syncBodyFrom = provider {
                rootDir.resolve("README.md").readText()
            }
        }
    }

    private fun Project.setupTasks() {
        tasks {
            // Merge common.jar into mod jar directly, because
            // loom fails when using include(project(...))
            val commonJar = project(":common").tasks.named("jar")
            named<Jar>("jar") {
                dependsOn(commonJar)
                from(commonJar.map { zipTree(it.outputs.files.singleFile) })
            }
            named<RemapJarTask>("remapJar") {
                archiveBaseName = "PasswordManagerAuthMe"
                archiveClassifier = versionSuffix()
            }
        }
    }

    private fun Project.config(name: String) =
        configurations.named(name)

    private fun Project.maybeInclude() =
        config("maybeInclude")

    private fun Project.include() =
        config("include")

    private fun Project.impl() =
        config("implementation")

    private fun Project.modImpl() =
        config("modImplementation")

    private fun Project.adventure() =
        config("adventure")
}

private fun Project.minecraftVersion() =
    properties["minecraft_version"]

private fun Project.fabricVersion() =
    properties["fabric_version"].toString()

private fun Project.modIncludeAll() =
    properties["fabric_modIncludeAll"] == "true"

private fun Project.versionSuffix(): Provider<String> = provider {
    var v = ""
    if (modIncludeAll()) {
        v += "ALL-"
    }
    v += "fabric-${minecraftVersion()}"
    v
}

private fun buildVersionStr(version: String, suffix: String): String {
    // 0.0.1-SNAPSHOT+mc1.20.4
    // 0.0.1-alpha.1+test-mc1.20.4
    var v = version
    v += if (v.contains("+")) { "-" } else { "+" }
    v += suffix
    return v
}