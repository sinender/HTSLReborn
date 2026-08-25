import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.0"
    id("dev.kikugie.loom-back-compat")
    id("com.google.devtools.ksp") version "2.3.4"
    `maven-publish`
}

group = "llc.redstone"
version = "${property("mod.version")}+${stonecutter.current.version}"
base.archivesName = property("mod.id") as String

val requiredJava: JavaVersion = when {
    stonecutter.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    stonecutter.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    stonecutter.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    stonecutter.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}


repositories {
    mavenLocal()
    maven("https://repo.redstone.llc/snapshots")
    maven("https://repo.redstone.llc/releases")
    maven("https://maven.kosmx.dev") //IDK why I couldnt make this a strict maven :shrug:
    maven { url = uri("https://jitpack.io") }

    /**
     * Restricts dependency search of the given [groups] to the [maven URL][url],
     * improving the setup speed.
     */
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }

    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    strictMaven("https://maven.terraformersmc.com/", "Terraformers", "com.terraformersmc")
    strictMaven("https://maven.isxander.dev/releases", "xanderRepoReleases", "dev.isxander", "org.quiltmc.parsers")
    strictMaven("https://maven.wispforest.io/releases", "wispForestReleases", "io.wispforest", "io.wispforest.endec")
    strictMaven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1", "DevAuth", "me.djtheredstoner")
}

dependencies {
    minecraft("com.mojang:minecraft:${stonecutter.current.version}")
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("deps.fabric_language_kotlin")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

    modImplementation("io.wispforest:owo-lib:${property("deps.owo")}")
    ksp("dev.kosmx.kowoconfig:ksp-owo-config:0.2.0")
    modImplementation("llc.redstone:SystemsAPI:${property("deps.systemsapi")}") {
        exclude(module = "dynamic-fps")
    }

    implementation(include("org.mozilla:rhino:1.9.0")!!)
    implementation(include("guru.zoroark.tegral:tegral-niwen-lexer:0.0.4")!!)
    implementation(include("guru.zoroark.tegral:tegral-core:0.0.4")!!)

    implementation(include("llc.redstone:SystemsData:1.2.1")!!)

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.2")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(requiredJava.majorVersion))
    }
    sourceSets {
        val test by getting {
            kotlin.srcDir("../../src/test/kotlin")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "HTSLReborn"

            version = if (hasProperty("commit")) "${property("commit")}+${stonecutter.current.version}" else project.version.toString()
//            version = "dev"
        }
    }
    repositories {
        maven {
            name = "releasesRepo"
            url = uri("https://repo.redstone.llc/releases")
            credentials {
                username = findProperty("releasesRepoUsername") as? String
                password = findProperty("releasesRepoPassword") as? String
            }
        }
        maven {
            name = "snapshotsRepo"
            url = uri("https://repo.redstone.llc/snapshots")
            credentials {
                username = findProperty("releasesRepoUsername") as? String
                password = findProperty("releasesRepoPassword") as? String
            }
        }
    }
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
            "fabric_api" to project.property("deps.fabric_api"),
            "systemsapi" to project.property("deps.systemsapi"),
            "owo_lib" to project.property("deps.owo")
        )

        filesMatching("fabric.mod.json") { expand(props) }
    }

    test {
        useJUnitPlatform()
    }
}
