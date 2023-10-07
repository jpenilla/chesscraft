import io.papermc.hangarpublishplugin.model.Platforms

plugins {
  id("com.github.johnrengelman.shadow") version "8.1.1"
  id("xyz.jpenilla.run-paper") version "2.2.0"
  val indraVer = "3.1.3"
  id("net.kyori.indra") version indraVer
  id("net.kyori.indra.git") version indraVer
  id("net.kyori.indra.licenser.spotless") version indraVer
  id("io.papermc.hangar-publish-plugin") version "0.1.0"
  id("com.modrinth.minotaur") version "2.7.5"
}

decorateVersion()

indra {
  javaVersions().target(17)
}

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
  compileOnly("io.papermc.paper", "paper-api", "1.19.4-R0.1-SNAPSHOT") {
    exclude("org.yaml", "snakeyaml")
  }
  implementation("xyz.niflheim:stockfish-java:4.0.0-SNAPSHOT")
  implementation(platform("cloud.commandframework:cloud-bom:1.8.4"))
  implementation("cloud.commandframework:cloud-paper")
  compileOnly("com.mojang", "brigadier", "1.0.18")
  implementation("cloud.commandframework:cloud-minecraft-extras") {
    isTransitive = false
  }
  implementation("org.spongepowered:configurate-yaml:4.1.2")
  runtimeOnly("io.papermc:paper-trail:0.0.1-SNAPSHOT")
  implementation("org.bstats", "bstats-bukkit", "3.0.2")

  val cpuFeaturesJniVersion = "1.0.1"
  implementation("io.github.aecsocket:cpu-features-jni:$cpuFeaturesJniVersion")
  runtimeOnly("io.github.aecsocket:cpu-features-jni-natives-linux:$cpuFeaturesJniVersion")
  runtimeOnly("io.github.aecsocket:cpu-features-jni-natives-windows:$cpuFeaturesJniVersion")
  runtimeOnly("io.github.aecsocket:cpu-features-jni-natives-macos:$cpuFeaturesJniVersion")
  runtimeOnly("io.github.aecsocket:cpu-features-jni-natives-macos-arm64:$cpuFeaturesJniVersion")
}

indraSpotlessLicenser {
  licenseHeaderFile(rootProject.file("LICENSE_HEADER"))
}

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
    minecraftVersion("1.20.2")
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
  shadowJar {
    fun reloc(pkg: String) = relocate(pkg, "xyz.jpenilla.chesscraft.dependency.$pkg")
    reloc("cloud.commandframework")
    reloc("io.leangen.geantyref")
    reloc("xyz.niflheim")
    reloc("org.spongepowered.configurate")
    reloc("org.yaml.snakeyaml")
    reloc("io.github.aecsocket.jniglue")
    reloc("io.papermc.papertrail")
    reloc("org.bstats")
    exclude("log4j.properties", "logback.xml")
    dependencies {
      exclude(dependency("com.google.code.findbugs:jsr305"))
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
  platforms.register(Platforms.PAPER) {
    jar.set(shadowJar)
    platformVersions.set(versions)
  }
}

modrinth {
  projectId.set("PYmT3jyX")
  versionType.set("release")
  file.set(shadowJar)
  gameVersions.set(versions)
  loaders.set(listOf("paper"))
  changelog.set(releaseNotes)
  token.set(providers.environmentVariable("MODRINTH_TOKEN"))
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
