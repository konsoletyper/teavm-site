<#import "page.ftl" as page>
<#import "doc-navigation.ftl" as nav>
<@page.page title="${content.title}" category="docs" site=site>
  <div class="content-container">
    <div class="content documentation-content">
      <nav class="documentation-menu">
        <ul>
            <#list content.contents as section>
              <@nav.node section content.path/>
            </#list>
        </ul>
      </nav>
      <div class="article-container">
        <article>
          <h1>${content.title}</h1>
          <div id="markdown-content-container">${content.text?no_esc}</div>
        </article>
        <nav class="edit-article">
          <a target="_blank" class="with-icon"
            <#if !content.synthesized>href="${site.githubPath}/${content.path}.md"</#if>>Improve this page</a>
        </nav>
        <nav class="pager">
          <a class="previous-page" <#if content.previous??>href="/docs/${content.previous}.html"</#if>>Previous</a>
          <a class="next-page" <#if content.next??> href="/docs/${content.next}.html"</#if>>Next</a>
        </nav>
      </div>
    </div>
  </div>
</@page.page>

