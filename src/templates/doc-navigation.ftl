<#macro node data current>
  <li>
    <#if data.path??>
      <a href="/docs/${data.path}.html" <#if data.path == current>class="current-article"</#if>>${data.title}</a>
    <#else>
      <span>${data.title}</span>
    </#if>
    <#if data.children?size gt 0 && data.selected && data.level lt 1>
        <ul>
          <#list data.children as child>
            <@node child current/>
          </#list>
        </ul>
    </#if>
  </li>
</#macro>