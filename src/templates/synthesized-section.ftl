<ul>
  <#list children as child>
    <li>
      <a href="${child.path}.html">${child.title}</a>
    </li>
  </#list>
</ul>