<#macro page title category site>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="description" content="Java bytecode AOT compiler">
  <title>${site.name} <#if title != ""> &mdash; ${title}</#if></title>
  <link rel="stylesheet" href="/css/main.css">
  <link rel="icon" type="image/png" href="/favicon.png" />
</head>
<body>
  <nav class="site-navigation-container">
    <div class="site-navigation">
      <div class="site-navigation-header">
        <button type="button" class="navigation-toggle" aria-expanded="false" aria-controls="navigation">
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
          <li><a href="${site.forum}" title="Discord" class="with-icon forum"></a></li>
          <li><a href="${site.issues}" title="Issue tracker" class="with-icon issue-tracker"></a></li>
          <li><a href="${site.vcs}" title="Source code" class="with-icon source-code"></a></li>
          <li><a href="${site.donate}" title="Donate" class="with-icon donate"></a></li>
        </ul>
      </div>
    </div>
  </nav>

  <#nested>

  <footer class="footer">
    ${site.name} ${site.year?c}
  </footer>
  <script>
    (function () {
      var toggle = document.querySelector('.navigation-toggle');
      var nav = document.getElementById('navigation');
      if (toggle && nav) {
        toggle.addEventListener('click', function () {
          var open = nav.classList.toggle('open');
          toggle.setAttribute('aria-expanded', String(open));
        });
      }
      var docToggle = document.querySelector('.doc-menu-toggle');
      var docMenu = document.querySelector('.documentation-menu');
      if (docToggle && docMenu) {
        docToggle.addEventListener('click', function () {
          var open = docMenu.classList.toggle('open');
          docToggle.setAttribute('aria-expanded', String(open));
        });
      }
    })();
  </script>
</body>
</html>
</#macro>