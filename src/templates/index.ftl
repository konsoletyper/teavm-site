<#import "page.ftl" as page>
<@page.page title="" category="index" site=site>
  <div class="product-header-container">
    <div class="product-header">
      <h1>TeaVM: Build Fast, Modern Web Apps in Java</h1>
      <p>A powerful tool for Java developers who want to develop
        web applications without the difficulties of a JavaScript
        development stack.</p>
      <p class="learn-more">
        <a class="button-like-link" href="/docs/intro/overview.html" role="button">Learn More</a>
      </p>
    </div>
  </div>

  <div class="content-container">
    <div class="content">

      <div class="product-what-is">
        <h2>What is TeaVM?</h2>
        <blockquote>
          <p>TeaVM is an ahead-of-time compiler for Java bytecode
            that emits JavaScript and WebAssembly that runs in
            a browser.  Its close relative is the well-known GWT.
            The main difference is that TeaVM does not require
            source code, only compiled class files.
            Moreover, the source code is not required to be Java,
            so TeaVM successfully compiles Kotlin and Scala.
          </p>
        </blockquote>
      </div>

      <ul class="product-strong-parts">
        <li>
          <h1 class="with-icon easy-to-use"></h1>
          <h2>Easy to use</h2>
          <p>Create a new project from a Maven archetype OR apply Gradle plugin and develop with pleasure
            You don't need a complicated setup with npm, Webpack, UglifyJS, Babel, etc.
            TeaVM does everything for you.
          </p>
        </li>
        <li>
          <h1 class="with-icon efficient"></h1>
          <h2>Efficient</h2>
          <p>TeaVM is quite efficient, you won't wait for hours while it's compiling.
            Also, TeaVM produces fast, small JavaScript code for web apps that start quickly, even
            on mobile devices.
          </p>
        </li>
      </ul>

      <div class="product-sponsors">
        <h2>Sponsors</h2>
        <ul>
          <li>
            <a href="http://reportmill.com/">
              <img src="sponsor-badges/reportmill.png" alt="ReportMill Software">
            </a>
          </li>
        </ul>

        <div class="product-become-sponsor">
          <iframe src="https://github.com/sponsors/konsoletyper/button" title="Want to become a sponsor?" height="32" width="114" style="border: 0; border-radius: 6px;"></iframe>
        </div>
      </div>

    </div>
  </div>

</@page.page>