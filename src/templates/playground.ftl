<#import "page.ftl" as page>
<@page.page title="Playground" category="playground" site=site>
  <script type="text/javascript" src="/playground/codemirror.min.js"></script>
  <script type="text/javascript" src="/playground/codemirror-clike.min.js"></script>
  <script type="module">
      import { load } from "/playground/wasm-gc/ui.wasm-runtime.js";

      document.body.onload = async () => {
          let teavm = await load("/playground/wasm-gc/ui.wasm", {
              stackDeobfuscator: {
                  enabled: true
              }
          });
          teavm.exports.setupUI({
              workerLocation: "/playground/worker.js",
              stdlibLocation: "/playground/compile-classlib-teavm.bin",
              runtimeStdlibLocation: "/playground/runtime-classlib-teavm.bin",
              examplesLocation: "/playground/examples/",
              frameLocation: "/playground/run-frame.html"
          });
      }
      let link = document.createElement("link");
      link.setAttribute("href", "/playground/codemirror.min.css")
      link.setAttribute("rel", "stylesheet")
      link.setAttribute("type", "text/css")
      document.head.appendChild(link);
  </script>
  <div class="playground">
    <div class="playground-toolbar">
      <button id="compile-button" disabled>
        <span class="run-action">Run</span>
      </button>
      <button id="choose-example" disabled>
        <span class="examples-action">Examples</span>
      </button>
      <div class="playground-toolbar-right">
        <a href="https://github.com/konsoletyper/teavm-javac" title="Source code"
           class="with-icon playground-github-link"></a>
      </div>
    </div>
    <div class="playground-content">
      <div class="playground-editor-container">
          <textarea cols="80" rows="20" id="source-code">public class Main {
      public static void main(String[] args) {
          System.out.println("Hello, world!");
      }
  }</textarea>
        <span class="playground-panel-label">java</span>
      </div>
      <div class="playground-out-container">
        <div class="playground-result-container" id="result-container">
          <iframe id="result" class="playground-result"></iframe>
          <span class="playground-panel-label">result</span>
        </div>
        <div class="playground-stdout-container">
          <div id="stdout" class="playground-stdout"></div>
          <span class="playground-panel-label">console</span>
        </div>
      </div>
    </div>

    <div class="modal" tabindex="-1" id="examples">
      <div class="modal-dialog" role="document">
        <div class="modal-content">
          <div class="modal-header">
            <h4 class="modal-title">Code examples</h4>
            <button type="button" class="close" data-dismiss="modal" aria-label="Close" id="cancel-example-selection">
              <span aria-hidden="true">&times;</span>
            </button>
          </div>
          <div class="modal-body examples-container">
            <div id="examples-content" class="playground-examples-content">
            </div>
            <div id="examples-content-progress" class="loading">
              Loading... Please, wait.
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</@page.page>
