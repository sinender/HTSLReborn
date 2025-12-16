plugins {
    kotlin("jvm") version "2.2.10"
    id("fabric-loom")
    `maven-publish`
}

version = "${property("mod.version")}+${stonecutter.current.version}"
base.archivesName = property("mod.id") as String

repositories {
    mavenLocal()
    /**
     * Restricts dependency search of the given [groups] to the [maven URL][url],
     * improving the setup speed.
     */
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }

    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    strictMaven("https://maven.terraformersmc.com/", "Terraformers")
    strictMaven("https://maven.isxander.dev/releases", "Xander Maven")
    strictMaven("https://maven.siphalor.de/", "Siphalor's Maven")

    strictMaven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1", "DevAuth")
}

dependencies {
    /**
     * Fetches only the required Fabric API modules to not waste time downloading all of them for each version.
     * @see <a href="https://github.com/FabricMC/fabric">List of Fabric API modules</a>
     */
    fun fapi(vararg modules: String) {
        for (it in modules) modImplementation(fabricApi.module(it, property("deps.fabric_api") as String))
    }

    minecraft("com.mojang:minecraft:${stonecutter.current.version}")
    mappings("net.fabricmc:yarn:${property("deps.yarn")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")

    modImplementation("net.fabricmc:fabric-language-kotlin:${property("deps.fabric_language_kotlin")}")
    modImplementation("com.terraformersmc:modmenu:${property("deps.modmenu")}")
    modImplementation("dev.isxander:yet-another-config-lib:${property("deps.yacl")}")
    modImplementation("llc.redstone:SystemsAPI:0.0.14+1.21.9")

    implementation("com.github.shynixn.mccoroutine:mccoroutine-fabric-api:2.22.0")
    implementation("com.github.shynixn.mccoroutine:mccoroutine-fabric-core:2.22.0")

    implementation(tegralLibs.niwen.lexer)

    fapi("fabric-lifecycle-events-v1", "fabric-resource-loader-v0", "fabric-content-registries-v0", "fabric-command-api-v2")

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.1")

}

loom {
    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        ideConfigGenerated(true)
        vmArgs("-Dmixin.debug.export=true") // Exports transformed classes for debugging
        runDir = "../../run" // Shares the run directory between versions
    }
}

java {
    withSourcesJar()
    val javaVersion: JavaVersion = JavaVersion.VERSION_21
    targetCompatibility = javaVersion
    sourceCompatibility = javaVersion
}

tasks {
    processResources {
        val props = mapOf(
            "id" to project.property("mod.id"),
            "name" to project.property("mod.name"),
            "version" to project.property("mod.version"),
            "minecraft" to project.property("mod.mc_dep"),
            "fabric_loader" to project.property("deps.fabric_loader"),
            "fabric_language_kotlin" to project.property("deps.fabric_language_kotlin"),
            "yacl" to project.property("deps.yacl")
        )

        filesMatching("fabric.mod.json") { expand(props) }
    }

    // Builds the version into a shared folder in `build/libs/${mod version}/`
    register<Copy>("buildAndCollect") {
        group = "build"
        from(remapJar.map { it.archiveFile }, remapSourcesJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}
