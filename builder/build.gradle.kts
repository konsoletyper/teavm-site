plugins {
  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  implementation(libs.freemarker)
  implementation(libs.commonmark)
  implementation(libs.jackson.yaml)
}
