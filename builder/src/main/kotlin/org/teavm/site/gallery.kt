package org.teavm.site

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

class GalleryItem @JsonCreator constructor(
  @JsonProperty("title")
  val title: String,

  @JsonProperty("image")
  val image: String,

  @JsonProperty("path")
  val path: String,

  @JsonProperty("source")
  val source: String?
)