<#macro page title category site>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="description" content="Java bytecode AOT compiler">
  <title>${site.name} <#if title != ""> &mdash; ${title}</#if></title>
  <link rel="stylesheet" href="/css/main.css">
  <link rel="icon" type="image/png" href="/favicon.png" />
</head>
<body>
  <nav class="site-navigation-container">
    <div class="site-navigation">
      <div class="site-navigation-header">
        <button type="button" class="navigation-toggle collapsed" aria-expanded="false" aria-controls="navbar">
          Toggle navigation
        </button>
        <a class="site-title" href="/">${site.name}</a>
      </div>
      <div id="navigation" class="site-navigation-items">
        <ul class="site-navigation-list site-navigation-main">
          <#list site.pages as page>
            <li <#if category == page.category> class="active"</#if>><a href="${page.path}">${page.name}</a></li>
          </#list>
        </ul>
        <ul class="site-navigation-list site-navigation-aux">
          <li><a href="mailto:${site.email}" title="Contact us" class="with-icon contact-us"></a></li>
          <li><a href="${site.forum}" title="Google Groups" class="with-icon forum"></a></li>
          <li><a href="${site.issues}" title="Issue tracker" class="with-icon issue-tracker"></a></li>
          <li><a href="${site.vcs}" title="Source code" class="with-icon source-code"></a></li>
        </ul>
      </div>
    </div>
  </nav>

  <#nested>

  <footer class="footer">
    ${site.name} ${site.year?c}
  </footer>
</body>
</html>
</#macro>