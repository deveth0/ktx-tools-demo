import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `kotlin-dsl`
}

dependencies {
  implementation("com.squareup:kotlinpoet:1.7.2")
}

gradlePlugin {
  plugins {
    register("ktxtoolsplugin") {
      id = "de.dev.eth0.libgdx.plugins.ktx"
      implementationClass = "de.dev.eth0.libgdx.tools.KtxToolsPlugin"
    }
  }
}

repositories {
  jcenter()
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
  languageVersion = "1.4"
}