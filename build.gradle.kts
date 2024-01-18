import me.modmuss50.mpp.ReleaseType
import xyz.jpenilla.gremlin.gradle.ShadowGremlin
import xyz.jpenilla.runpaper.task.RunServer

plugins {
  id("com.github.johnrengelman.shadow") version "8.1.1"
  id("xyz.jpenilla.run-paper") version "2.2.2"
  val indraVer = "3.1.3"
  id("net.kyori.indra") version indraVer
  id("net.kyori.indra.git") version indraVer
  id("net.kyori.indra.licenser.spotless") version indraVer
  id("io.papermc.hangar-publish-plugin") version "0.1.1"
  id("me.modmuss50.mod-publish-plugin") version "0.4.5"
  id("net.kyori.blossom") version "2.1.0"
  id("xyz.jpenilla.gremlin-gradle") version "0.0.3"
}

decorateVersion()

indra {
  javaVersions().target(17)
}

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
  sonatype.s01Snapshots()
}

dependencies {
  compileOnly("io.papermc.paper", "paper-api", "1.20.2-R0.1-SNAPSHOT") {
    exclude("org.yaml", "snakeyaml")
  }
  implementation("xyz.niflheim:stockfish-java:4.0.0-SNAPSHOT")
  implementation(platform("cloud.commandframework:cloud-bom:1.8.4"))
  implementation("cloud.commandframework:cloud-paper")
  compileOnly("com.mojang", "brigadier", "1.0.500")
  implementation("cloud.commandframework:cloud-minecraft-extras") {
    isTransitive = false
  }
  implementation("org.spongepowered:configurate-yaml:4.1.2")
  runtimeOnly("io.papermc:paper-trail:0.0.1-SNAPSHOT")
  implementation("org.bstats", "bstats-bukkit", "3.0.2")

  val commonsCompress = "org.apache.commons:commons-compress:1.25.0"
  runtimeDownload(commonsCompress)
  compileOnly(commonsCompress)

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
}

indraSpotlessLicenser {
  licenseHeaderFile(rootProject.file("LICENSE_HEADER"))
}

val runVersions = listOf(
  "19.4",
  "20.1",
  "20.2"
)

tasks {
  withType<Jar> {
    from(layout.projectDirectory.file("LICENSE")) {
      rename { "LICENSE_chesscraft" }
    }
  }
  jar {
    manifest {
      attributes("Multi-Release" to true)
    }
  }
  assemble {
    dependsOn(shadowJar)
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
    reloc("cloud.commandframework")
    reloc("io.leangen.geantyref")
    reloc("xyz.niflheim")
    reloc("org.spongepowered.configurate")
    reloc("org.yaml.snakeyaml")
    reloc("io.papermc.papertrail")
    reloc("org.bstats")
    reloc("xyz.jpenilla.gremlin")
    exclude("log4j.properties", "logback.xml")
    dependencies {
      exclude(dependency("com.google.code.findbugs:jsr305"))
    }
  }
  writeDependencies {
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
val versions = listOf("1.19.4", "1.20.2")
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
