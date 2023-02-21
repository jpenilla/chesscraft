import io.papermc.hangarpublishplugin.model.Platforms
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
  id("com.github.johnrengelman.shadow") version "7.1.2"
  id("net.minecrell.plugin-yml.bukkit") version "0.5.2"
  id("xyz.jpenilla.run-paper") version "2.0.1"
  val indraVer = "3.0.1"
  id("net.kyori.indra") version indraVer
  id("net.kyori.indra.git") version indraVer
  id("net.kyori.indra.license-header") version indraVer
  id("io.papermc.hangar-publish-plugin") version "0.0.3"
  id("com.modrinth.minotaur") version "2.7.2"
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
  compileOnly("io.papermc.paper", "paper-api", "1.19.3-R0.1-SNAPSHOT") {
    exclude("org.yaml", "snakeyaml")
  }
  implementation("xyz.niflheim:stockfish-java:4.0.0-SNAPSHOT")
  implementation(platform("cloud.commandframework:cloud-bom:1.8.0"))
  implementation("cloud.commandframework:cloud-paper")
  compileOnly("com.mojang", "brigadier", "1.0.18")
  implementation("cloud.commandframework:cloud-minecraft-extras") {
    isTransitive = false
  }
  implementation("org.spongepowered:configurate-yaml:4.1.2")
  implementation("io.papermc:paperlib:1.0.8")
  implementation("org.bstats", "bstats-bukkit", "3.0.1")

  val cpuFeaturesJniVersion = "1.0.1"
  implementation("io.github.aecsocket:cpu-features-jni:$cpuFeaturesJniVersion")
  runtimeOnly("io.github.aecsocket:cpu-features-jni-natives-linux:$cpuFeaturesJniVersion")
  runtimeOnly("io.github.aecsocket:cpu-features-jni-natives-windows:$cpuFeaturesJniVersion")
  runtimeOnly("io.github.aecsocket:cpu-features-jni-natives-macos:$cpuFeaturesJniVersion")
  runtimeOnly("io.github.aecsocket:cpu-features-jni-natives-macos-arm64:$cpuFeaturesJniVersion")
}

license {
  header.set(resources.text.fromFile(rootProject.file("LICENSE_HEADER")))
}

tasks {
  withType<Jar> {
    from(layout.projectDirectory.file("LICENSE")) {
      rename { "LICENSE_chesscraft" }
    }
    from(generateBukkitPluginDescription.flatMap { it.outputDirectory }) {
      rename { "paper-plugin.yml" }
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
    minecraftVersion("1.19.3")
  }
  shadowJar {
    fun reloc(pkg: String) = relocate(pkg, "xyz.jpenilla.chesscraft.dependency.$pkg")
    reloc("cloud.commandframework")
    reloc("io.leangen.geantyref")
    reloc("xyz.niflheim")
    reloc("org.spongepowered.configurate")
    reloc("org.yaml.snakeyaml")
    reloc("io.github.aecsocket.jniglue")
    reloc("io.papermc.lib")
    reloc("org.bstats")
    exclude("log4j.properties", "logback.xml")
    dependencies {
      exclude(dependency("com.google.code.findbugs:jsr305"))
    }
  }
}

bukkit {
  name = "ChessCraft"
  author = "jmp"
  main = "xyz.jpenilla.chesscraft.ChessCraft"
  apiVersion = "1.19"
  permissions {
    val defaultTrue = listOf(
      "chesscraft.command.help",
      "chesscraft.command.challenge.cpu",
      "chesscraft.command.challenge.player",
      "chesscraft.command.accept",
      "chesscraft.command.deny",
      "chesscraft.command.next_promotion",
      "chesscraft.command.forfeit",
      "chesscraft.command.show_legal_moves"
    )
    defaultTrue.forEach {
      register(it) {
        default = BukkitPluginDescription.Permission.Default.TRUE
      }
    }
  }
}

val releaseNotes = providers.environmentVariable("RELEASE_NOTES")
val versions = listOf("1.19.3")
val shadowJar = tasks.shadowJar.flatMap { it.archiveFile }

hangarPublish.publications.register("plugin") {
  version.set(project.version as String)
  owner.set("jmp")
  slug.set("ChessCraft")
  channel.set("Release")
  changelog.set(releaseNotes)
  apiKey.set(providers.environmentVariable("HANGAR_UPLOAD_KEY"))
  platforms {
    register(Platforms.PAPER) {
      jar.set(shadowJar)
      platformVersions.set(versions)
    }
  }
}

modrinth {
  projectId.set("PYmT3jyX")
  versionType.set("release")
  file.set(shadowJar)
  gameVersions.set(versions)
  loaders.set(listOf("paper"))
  changelog.set(releaseNotes)
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
