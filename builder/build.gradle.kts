import org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  implementation(libs.freemarker)
  implementation(libs.commonmark)
  implementation(libs.jackson.yaml)
}

tasks.withType<KotlinJvmCompile>().configureEach {
  jvmTargetValidationMode.set(JvmTargetValidationMode.WARNING)
}