plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

includeBuild("work/Stockfish-Java")

rootProject.name = "chesscraft"
