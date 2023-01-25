<#import "page.ftl" as page>
<@page.page title="Gallery" category="gallery" site=site>
<div class="content-container">
  <div class="content gallery-content">
    <article>
      <ul class="thumbnail-list">
        <#list content as item>
          <li class="thumbnail">
            <div class="thumbnail-body">
              <a href="${item.path}">
                <img src="${item.image}" alt="${item.title} thumbnail">
              </a>
            </div>
            <div  class="thumbnail-footer">
              ${item.title}
              <#if item.source??>
                <a href="${item.source}">(source&nbsp;code)</a>
              </#if>
            </div>
          </li>
        </#list>
      </ul>
    </article>
  </div>
</div>
</@page.page>

