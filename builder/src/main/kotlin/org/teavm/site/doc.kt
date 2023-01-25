package org.teavm.site

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls

class Document @JsonCreator constructor(
  @JsonProperty("title")
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  val title: String,
  @JsonProperty("path")
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  val path: String,
  @JsonProperty("synthesized")
  val synthesized: Boolean = false,
  @JsonProperty("children")
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  val children: List<Document> = emptyList()
)

class DocumentNavigation(
  val title: String,
  val path: String,
  val selected: Boolean,
  val children: List<DocumentNavigation> = emptyList()
)

class DocumentRenderModel(
  val contents: List<DocumentNavigation>,
  val path: String,
  val synthesized: Boolean,
  val title: String,
  val text: String
) {
  var previous: String? = null
  var next: String? = null
}