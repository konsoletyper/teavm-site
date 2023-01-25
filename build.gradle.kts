plugins {
  base
  war
  id("org.gretty") version "4.0.3"
}

configurations {
  create("builder")
}

dependencies {
  "builder"(project(":builder"))
}

val buildSite = tasks.register<JavaExec>("buildSite") {
  val inputDir = File(projectDir, "src")
  val outputDir = File(buildDir, "site")
  inputs.dir(inputDir)
  outputs.dir(outputDir)
  classpath += files(provider { configurations["builder"] })
  mainClass.set("org.teavm.site.GeneratorKt")
  args(listOf(inputDir, outputDir))
}

tasks.assemble {
  dependsOn(buildSite)
}

gretty {
  contextPath = "/"
  fastReload = false
  extraResourceBase(File(buildDir, "site"))
}

tasks.configureEach {
  if (name == "prepareInplaceWebApp") {
    dependsOn(buildSite)
  }
}