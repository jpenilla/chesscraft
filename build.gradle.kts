import me.modmuss50.mpp.ReleaseType
import xyz.jpenilla.gremlin.gradle.ShadowGremlin
import xyz.jpenilla.runpaper.task.RunServer

plugins {
  id("io.github.goooler.shadow") version "8.1.8"
  id("xyz.jpenilla.run-paper") version "2.3.0"
  val indraVer = "3.1.3"
  id("net.kyori.indra") version indraVer
  id("net.kyori.indra.git") version indraVer
  id("net.kyori.indra.licenser.spotless") version indraVer
  id("io.papermc.hangar-publish-plugin") version "0.1.2"
  id("me.modmuss50.mod-publish-plugin") version "0.5.1"
  id("net.kyori.blossom") version "2.1.0"
  id("xyz.jpenilla.gremlin-gradle") version "0.0.6"
}

decorateVersion()

indra {
  javaVersions().target(21)
}

repositories {
  mavenCentral()
  sonatype.ossSnapshots()
  sonatype.s01Snapshots()
  maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
  compileOnly(libs.paperApi) {
    exclude("org.yaml", "snakeyaml")
  }
  implementation("xyz.niflheim:stockfish-java:4.0.0-SNAPSHOT") // from included build
  implementation(platform(libs.cloud.bom))
  implementation(platform(libs.cloud.minecraft.bom))
  implementation(platform(libs.cloud.translations.bom))
  implementation(libs.cloud.paper)
  implementation(libs.cloud.translations.core)
  implementation(libs.cloud.translations.bukkit)
  implementation(libs.cloud.translations.minecraft.extras)
  compileOnly(libs.brigadier)
  implementation(libs.cloud.minecraft.extras) {
    isTransitive = false
  }
  implementation(libs.configurate.yaml)
  implementation(libs.adventure.serializer.configurate4) {
    isTransitive = false
  }
  runtimeOnly(libs.paperTrail)
  implementation(libs.bstatsBukkit)

  runtimeDownload(libs.commonsCompress)
  compileOnly(libs.commonsCompress)

  val cpuFeatures = "org.bytedeco:cpu_features:0.7.0-1.5.8"
  runtimeDownload(cpuFeatures)
  compileOnly(cpuFeatures)

  fun cpuFeaturesNatives(platform: String, onlyJavaCpp: Boolean = false) {
    if (!onlyJavaCpp) {
      runtimeDownload("org.bytedeco", "cpu_features", "0.7.0-1.5.8", classifier = platform)
    }
    runtimeDownload("org.bytedeco", "javacpp", "1.5.8", classifier = platform)
  }

  cpuFeaturesNatives("linux-x86_64")
  cpuFeaturesNatives("macosx-arm64", true)
  cpuFeaturesNatives("macosx-x86_64")
  cpuFeaturesNatives("windows-x86_64")

  compileOnly(libs.hikariCP)
  runtimeDownload(libs.hikariCP)
  compileOnly(libs.flyway)
  runtimeDownload(libs.flyway)
  runtimeDownload(libs.mysql)
  runtimeDownload(libs.mariadb)
  runtimeDownload(libs.flywayMysql)
  compileOnly(libs.jdbiCore)
  runtimeDownload(libs.jdbiCore)
  runtimeDownload(libs.h2)
  compileOnly(libs.caffeine)
  runtimeDownload(libs.caffeine)
}

configurations.runtimeDownload {
  exclude("org.checkerframework", "checker-qual")
  exclude("org.slf4j")
  exclude("com.google.code.gson")
}

indraSpotlessLicenser {
  licenseHeaderFile(rootProject.file("LICENSE_HEADER"))
}

val runVersions = listOf(
  "19.4",
  "20.1",
  "20.2",
  "20.4",
  "20.6",
  "21",
)

tasks {
  withType<Jar> {
    from(layout.projectDirectory.file("LICENSE")) {
      rename { "LICENSE_chesscraft" }
    }
  }
  jar {
    manifest {
      attributes(
        "Multi-Release" to true,
        "paperweight-mappings-namespace" to "mojang",
      )
    }
  }
  assemble {
    dependsOn(shadowJar)
  }
  compileJava {
    options.compilerArgs.add("-Xlint:-classfile")
  }
  runServer {
    minecraftVersion("1.${runVersions.last()}")
  }
  runVersions.take(runVersions.size - 1).forEach { ver ->
    val n = ver.replace(".", "_")
    register("run$n", RunServer::class) {
      minecraftVersion("1.$ver")
      runDirectory.set(layout.projectDirectory.dir("run$n"))
      pluginJars.from(shadowJar.flatMap { it.archiveFile })
    }
  }
  processResources {
    val props = mapOf(
      "version" to project.version
    )
    inputs.properties(props)
    filesMatching("*.yml") {
      expand(props)
    }
  }
  fun Task.reloc(pkg: String) = ShadowGremlin.relocate(this, pkg, "xyz.jpenilla.chesscraft.dependency.$pkg")
  shadowJar {
    reloc("org.incendo")
    reloc("io.leangen.geantyref")
    reloc("org.apache.commons")
    reloc("xyz.niflheim")
    reloc("org.spongepowered.configurate")
    reloc("net.kyori.adventure.serializer.configurate4")
    reloc("org.yaml.snakeyaml")
    reloc("io.papermc.papertrail")
    reloc("org.bstats")
    reloc("xyz.jpenilla.gremlin")
    exclude("log4j.properties", "logback.xml")
    dependencies {
      exclude(dependency("com.google.code.findbugs:jsr305"))
      exclude(dependency("io.leangen.geantyref:geantyref:.*"))
    }
  }
  writeDependencies {
    reloc("io.leangen.geantyref")
    reloc("org.apache.commons")
    repos.set(listOf(
      "https://repo.papermc.io/repository/maven-public/",
      "https://repo.maven.apache.org/maven2/",
    ))
  }
}

sourceSets {
  main {
    blossom {
      javaSources {
        variants("int", "double")
        variants.all {
          properties {
            put("abv", this@all.name.take(1))
            put("parse", if (this@all.name == "int") "Integer.parseInt" else {
              val cap = this@all.name.replaceFirstChar(Char::uppercase)
              "$cap.parse$cap"
            })
          }
        }
      }
    }
  }
}

val releaseNotes = providers.environmentVariable("RELEASE_NOTES")
val versions = listOf("1.19.4", "1.20.6", "1.21")
val shadowJar = tasks.shadowJar.flatMap { it.archiveFile }

hangarPublish.publications.register("plugin") {
  version.set(project.version as String)
  id.set("ChessCraft")
  channel.set("Release")
  changelog.set(releaseNotes)
  apiKey.set(providers.environmentVariable("HANGAR_UPLOAD_KEY"))
  platforms.paper {
    jar.set(shadowJar)
    platformVersions.set(versions)
  }
}

publishMods.modrinth {
  projectId = "PYmT3jyX"
  type = ReleaseType.STABLE
  file = shadowJar
  minecraftVersions = versions
  modLoaders.add("paper")
  changelog = releaseNotes
  accessToken = providers.environmentVariable("MODRINTH_TOKEN")
}

fun lastCommitHash(): String = indraGit.commit()?.name?.substring(0, 7)
  ?: error("Could not determine commit hash")

fun decorateVersion() {
  val versionString = version as String
  version = if (versionString.endsWith("-SNAPSHOT")) {
    "$versionString+${lastCommitHash()}"
  } else {
    versionString
  }
}
