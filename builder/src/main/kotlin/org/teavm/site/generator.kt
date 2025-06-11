package org.teavm.site

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import freemarker.core.HTMLOutputFormat
import freemarker.core.OutputFormat
import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.Image
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import java.io.File
import java.io.FileWriter
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

fun main(args: Array<out String>) {
  val inputDir = File(args[0])
  val outputDir = File(args[1])

  val config = readConfig(File(inputDir, "config.yaml"))
  val processor = Processor(
    config.site,
    File(inputDir, "templates"),
    outputDir
  )
  processor.processPage("index.ftl", "index.html")
  processor.processPage("gallery.ftl", "gallery.html", config.gallery)
  processor.processPage("playground.ftl", "playground.html", config.gallery)
  processor.processPage("404.ftl", "404.html", config.gallery)

  val docs = DocumentGenerator(config.site, inputDir, processor, config.documents).apply { generate() }

  val contentDir = File(inputDir, "content")
  for (file in contentDir.walkTopDown()) {
    if (file.isFile) {
      val outputFile = File(outputDir, file.toRelativeString(contentDir))
      file.copyTo(outputFile, overwrite = true)
    }
  }
  for (relPath in docs.filesToCopy) {
    val sourceFile = File(inputDir, "docs/$relPath")
    if (sourceFile.exists()) {
      val targetFile = File(outputDir, "docs/$relPath")
      sourceFile.copyTo(targetFile, overwrite = true)
    }
  }
}

private fun readConfig(file: File): Config {
  val mapper = ObjectMapper(YAMLFactory())
  mapper.findAndRegisterModules()
  return file.inputStream().use {
    mapper.readValue(it, Config::class.java)
  }
}

private class DocumentGenerator(
  private val site: Site,
  private val inputDir: File,
  private val processor: Processor,
  private val rootDocuments: List<Document>
) {
  private val filesToCopyImpl = mutableSetOf<String>()

  val filesToCopy: Set<String> get() = filesToCopyImpl

  fun generate() {
    val documentsToRender = mutableListOf<DocumentRenderModel>()
    for (document in rootDocuments) {
      process(document, documentsToRender)
    }
    for ((a, b) in documentsToRender.asSequence().zipWithNext()) {
      a.next = b.path
      b.previous = a.path
    }
    for (document in documentsToRender) {
      processor.processPage("document.ftl", "docs/${document.path}.html", document)
    }
  }

  private fun process(document: Document, result: MutableList<DocumentRenderModel>) {
    result += processContent(document)
    for (child in document.children) {
      process(child, result)
    }
  }

  private fun processContent(document: Document): DocumentRenderModel {
    val path = document.path
    val text = if (document.synthesized) {
      processor.processPageInMemory("synthesized-section.ftl", document)
    } else {
      val parser = Parser.builder().build()
      val inputText = File(inputDir, "docs/$path.md").readText()
      val markdownDocument = parser.parse(processProperties(path, inputText))
      val pathPrefix = path.substring(0, path.lastIndexOf('/') + 1)
      markdownDocument.accept(object : AbstractVisitor() {
        override fun visit(image: Image) {
          if (listOf("http://", "https://", "/").none { image.destination.startsWith(it) }) {
            val filePath = "docs/$pathPrefix${image.destination}"
            val file = File(inputDir, filePath)
            if (!file.exists()) {
              error("Input document 'docs/$path.md' references nonexistent file 'docs/$filePath'")
            }
            filesToCopyImpl += pathPrefix + image.destination
          }
        }
      })
      val renderer = HtmlRenderer.builder().build()
      renderer.render(markdownDocument)
    }
    return DocumentRenderModel(
      contents = createNavigation(document),
      path = path,
      title = document.title,
      synthesized = document.synthesized,
      text = text
    )
  }

  private fun createNavigation(selectedDocument: Document): List<DocumentNavigation> {
    return rootDocuments.map { createNavigation(it, selectedDocument, 0) }
  }

  private fun createNavigation(document: Document, selectedDocument: Document, level: Int): DocumentNavigation {
    val children = document.children.map { createNavigation(it, selectedDocument, level + 1) }
    return DocumentNavigation(
      title = document.title,
      path = document.path,
      level = level,
      selected = document == selectedDocument || children.any { it.selected },
      children = children
    )
  }

  private fun processProperties(docPath: String, text: String): String {
    return pattern.matcher(text).replaceAll {
      when (val prop = it.group(1)) {
        "teavm_version" -> site.teavmVersion
        "teavm_branch" -> site.teavmBranch
        "teavm_dev_version" -> site.teavmDevVersion
        else -> {
          println("Unexpected property $prop as $docPath")
          "***"
        }
      }
    }.replace("\\\${", "\${")
  }

  companion object {
    private val pattern = Pattern.compile("(?<!\\\\)\\\$\\{(.*?)}")
  }
}

private class Processor(
  private val site: Site,
  inputDir: File,
  private val outputDir: File
) {
  private val config = Configuration(Configuration.VERSION_2_3_32).apply {
    setDirectoryForTemplateLoading(inputDir)
    defaultEncoding = "UTF-8"
    templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
    outputFormat = HTMLOutputFormat.INSTANCE
    logTemplateExceptions = false
    wrapUncheckedExceptions = true
    fallbackOnNullLoopVariable = false
  }

  fun processPage(name: String, output: String, data: Any? = null) {
    val template = config.getTemplate(name)
    val model = PageModel(
      site = site,
      content = data
    )
    val outputFile = File(outputDir, output)
    outputFile.parentFile.mkdirs()
    FileWriter(outputFile, StandardCharsets.UTF_8).use {
      template.process(model, it)
    }
  }

  fun processPageInMemory(name: String, data: Any? = null): String {
    val template = config.getTemplate(name)
    return StringWriter().also { template.process(data, it) }.toString()
  }
}
