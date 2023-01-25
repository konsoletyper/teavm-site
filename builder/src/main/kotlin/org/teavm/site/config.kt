package org.teavm.site

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls

class Config @JsonCreator constructor(
  @JsonProperty("site")
  val site: Site,

  @JsonProperty("documents")
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  val documents: List<Document>,

  @JsonProperty("gallery")
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  val gallery: List<GalleryItem>
)