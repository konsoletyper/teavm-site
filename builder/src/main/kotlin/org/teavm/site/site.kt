package org.teavm.site

import java.util.*

class Site {
  var email: String = ""
  var name: String = ""
  var forum: String = ""
  var issues: String = ""
  var vcs: String = ""
  val donate: String = ""
  var pages: List<Page> = emptyList()
  var year: Int = Calendar.getInstance().get(Calendar.YEAR)
  var githubPath: String = "file://"
  var teavmVersion = "0.1.0-SNAPSHOT"
  var teavmDevVersion = "0.1.0-SNAPSHOT"
  var teavmBranch = "snapshot"
}

class Page {
  var category: String = ""
  var name: String = ""
  var path: String = ""
}