plugins {
  `java-library`
  id("com.github.johnrengelman.shadow") version "7.1.2"
  id("net.minecrell.plugin-yml.bukkit") version "0.5.2"
  id("xyz.jpenilla.run-paper") version "2.0.1"
  id("net.kyori.indra.license-header") version "3.0.1"
}

group = "xyz.jpenilla"
version = "0.0.1-SNAPSHOT"

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
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
  implementation("cloud.commandframework:cloud-minecraft-extras") {
    isTransitive = false
  }
  implementation("org.spongepowered:configurate-yaml:4.1.2")
  compileOnly("com.github.oshi:oshi-core:6.2.2")
}

license {
  header.set(resources.text.fromFile(rootProject.file("LICENSE_HEADER")))
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
  compileJava {
    options.encoding = Charsets.UTF_8.name()
    options.release.set(17)
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
    exclude("log4j.properties", "logback.xml")
  }
}

bukkit {
  name = "ChessCraft"
  author = "jmp"
  main = "xyz.jpenilla.chesscraft.ChessCraft"
  apiVersion = "1.19"
}
