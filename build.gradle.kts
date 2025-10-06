plugins {
  base
  war
  id("org.gretty") version "4.1.10"
}

configurations {
  create("builder")
}

dependencies {
  "builder"(project(":builder"))
}

val buildSite = tasks.register<JavaExec>("buildSite") {
  val inputDir = File(projectDir, "src")
  val outputDir = layout.buildDirectory.dir("site").get().asFile
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
  setFastReload(false)
  extraResourceBase(layout.buildDirectory.dir("site"))
}

tasks.configureEach {
  if (name == "prepareInplaceWebApp") {
    dependsOn(buildSite)
  }
}