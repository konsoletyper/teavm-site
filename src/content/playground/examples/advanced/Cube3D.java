import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Int16Array;
import org.teavm.jso.webgl.WebGLBuffer;
import org.teavm.jso.webgl.WebGLProgram;
import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGLUniformLocation;

public class Main {
    private static final String VERTEX_SHADER_SOURCE =
            """
                attribute vec3 aVertexPosition;
                attribute vec4 aVertexColor;
                attribute vec3 aVertexNormal;
                uniform mat4 uMVMatrix;
                uniform mat4 uPMatrix;
                uniform vec3 uLightPosition;
                uniform vec3 uViewPosition;
                varying vec4 vColor;
                varying vec3 vNormal;
                varying vec3 vLightDirection;
                varying vec3 vViewDirection;
                void main(void) {
                  vec4 vertexPosition = uMVMatrix * vec4(aVertexPosition, 1.0);
                  gl_Position = uPMatrix * vertexPosition;
                  vColor = aVertexColor;
                  vNormal = mat3(uMVMatrix) * aVertexNormal;
                  vLightDirection = uLightPosition - vertexPosition.xyz;
                  vViewDirection = uViewPosition - vertexPosition.xyz;
                }
            """;

    private static final String FRAGMENT_SHADER_SOURCE =
            """
                precision mediump float;
                varying vec4 vColor;
                varying vec3 vNormal;
                varying vec3 vLightDirection;
                varying vec3 vViewDirection;
                uniform vec3 uAmbientColor;
                uniform vec3 uLightColor;
                uniform float uShininess;
                void main(void) {
                  // Normalize vectors
                  vec3 normal = normalize(vNormal);
                  vec3 lightDir = normalize(vLightDirection);
                  vec3 viewDir = normalize(vViewDirection);

                  // Ambient component
                  vec3 ambient = uAmbientColor * vColor.rgb;

                  // Diffuse component
                  float diff = max(dot(normal, lightDir), 0.0);
                  vec3 diffuse = diff * uLightColor * vColor.rgb;

                  // Specular component
                  vec3 reflectDir = reflect(-lightDir, normal);
                  float spec = pow(max(dot(viewDir, reflectDir), 0.0), uShininess);
                  vec3 specular = spec * uLightColor;

                  // Combine all lighting components
                  vec3 result = ambient + diffuse + specular;
                  gl_FragColor = vec4(result, vColor.a);
                }
            """;

    private static HTMLCanvasElement canvas;
    private static WebGLRenderingContext gl;
    private static WebGLProgram shaderProgram;
    private static WebGLBuffer vertexPositionBuffer;
    private static WebGLBuffer vertexColorBuffer;
    private static WebGLBuffer vertexNormalBuffer;
    private static WebGLBuffer indexBuffer;
    private static WebGLUniformLocation pMatrixUniform;
    private static WebGLUniformLocation mvMatrixUniform;
    private static WebGLUniformLocation lightPositionUniform;
    private static WebGLUniformLocation viewPositionUniform;
    private static WebGLUniformLocation ambientColorUniform;
    private static WebGLUniformLocation lightColorUniform;
    private static WebGLUniformLocation shininessUniform;
    private static float[] pMatrix;
    private static float rotationAngle;

    public static void main(String[] args) {
        var document = HTMLDocument.current();
        document.getDocumentElement().getStyle().setProperty("padding", "0");
        document.getDocumentElement().getStyle().setProperty("margin", "0");
        document.getDocumentElement().getStyle().setProperty("width", "100%");
        document.getDocumentElement().getStyle().setProperty("height", "100%");
        document.getBody().getStyle().setProperty("padding", "0");
        document.getBody().getStyle().setProperty("margin", "0");
        document.getBody().getStyle().setProperty("width", "100%");
        document.getBody().getStyle().setProperty("height", "100%");
        canvas = (HTMLCanvasElement) document.createElement("canvas");
        canvas.getStyle().setProperty("width", "100%");
        canvas.getStyle().setProperty("height", "100%");
        canvas.getStyle().setProperty("display", "block");
        canvas.getStyle().setProperty("padding", "0");
        canvas.getStyle().setProperty("margin", "0");
        canvas.getStyle().setProperty("box-sizing", "border-box");

        document.getBody().appendChild(canvas);

        gl = (WebGLRenderingContext) canvas.getContext("webgl");
        if (gl == null) {
            gl = (WebGLRenderingContext) canvas.getContext("experimental-webgl");
        }

        if (gl != null) {
            updateCanvasSize();
            Window.current().onEvent("resize", event -> updateCanvasSize());
            gl.clearColor(0.0f, 0.0f, 0.0f, 1.0f);
            gl.enable(WebGLRenderingContext.DEPTH_TEST);
            gl.depthFunc(WebGLRenderingContext.LEQUAL);
            gl.clear(WebGLRenderingContext.COLOR_BUFFER_BIT | WebGLRenderingContext.DEPTH_BUFFER_BIT);

            initShaders();
            initBuffers();
            startAnimation();
        }
    }

    private static void updateCanvasSize() {
        var rect = canvas.getBoundingClientRect();
        canvas.setWidth(rect.getWidth());
        canvas.setHeight(rect.getHeight());
        gl.viewport(0, 0, canvas.getWidth(), canvas.getHeight());
        pMatrix = perspectiveMatrix(45, (float) rect.getWidth() / rect.getHeight(), 0.1f, 100.0f);
    }

    private static void initShaders() {
        // Create vertex shader
        var vertexShader = gl.createShader(WebGLRenderingContext.VERTEX_SHADER);
        gl.shaderSource(vertexShader, VERTEX_SHADER_SOURCE);
        gl.compileShader(vertexShader);

        // Create fragment shader
        var fragmentShader = gl.createShader(WebGLRenderingContext.FRAGMENT_SHADER);
        gl.shaderSource(fragmentShader, FRAGMENT_SHADER_SOURCE);
        gl.compileShader(fragmentShader);

        // Create shader program
        shaderProgram = gl.createProgram();
        gl.attachShader(shaderProgram, vertexShader);
        gl.attachShader(shaderProgram, fragmentShader);
        gl.linkProgram(shaderProgram);
        gl.useProgram(shaderProgram);

        // Get attribute locations
        var vertexPositionAttribute = gl.getAttribLocation(shaderProgram, "aVertexPosition");
        gl.enableVertexAttribArray(vertexPositionAttribute);

        var vertexColorAttribute = gl.getAttribLocation(shaderProgram, "aVertexColor");
        gl.enableVertexAttribArray(vertexColorAttribute);

        var vertexNormalAttribute = gl.getAttribLocation(shaderProgram, "aVertexNormal");
        gl.enableVertexAttribArray(vertexNormalAttribute);

        // Get uniform locations
        pMatrixUniform = gl.getUniformLocation(shaderProgram, "uPMatrix");
        mvMatrixUniform = gl.getUniformLocation(shaderProgram, "uMVMatrix");
        lightPositionUniform = gl.getUniformLocation(shaderProgram, "uLightPosition");
        viewPositionUniform = gl.getUniformLocation(shaderProgram, "uViewPosition");
        ambientColorUniform = gl.getUniformLocation(shaderProgram, "uAmbientColor");
        lightColorUniform = gl.getUniformLocation(shaderProgram, "uLightColor");
        shininessUniform = gl.getUniformLocation(shaderProgram, "uShininess");
    }

    private static void initBuffers() {
        // Create vertex position buffer
        vertexPositionBuffer = gl.createBuffer();
        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, vertexPositionBuffer);

        // Define cube vertices (24 vertices for a cube, 4 for each face)
        float[] vertices = {
                // Front face
                -1.0f, -1.0f,  1.0f,
                1.0f, -1.0f,  1.0f,
                1.0f,  1.0f,  1.0f,
                -1.0f,  1.0f,  1.0f,

                // Back face
                -1.0f, -1.0f, -1.0f,
                -1.0f,  1.0f, -1.0f,
                1.0f,  1.0f, -1.0f,
                1.0f, -1.0f, -1.0f,

                // Left face
                -1.0f, -1.0f, -1.0f,
                -1.0f, -1.0f,  1.0f,
                -1.0f,  1.0f,  1.0f,
                -1.0f,  1.0f, -1.0f,

                // Right face
                1.0f, -1.0f,  1.0f,
                1.0f, -1.0f, -1.0f,
                1.0f,  1.0f, -1.0f,
                1.0f,  1.0f,  1.0f,

                // Top face
                -1.0f,  1.0f,  1.0f,
                1.0f,  1.0f,  1.0f,
                1.0f,  1.0f, -1.0f,
                -1.0f,  1.0f, -1.0f,

                // Bottom face
                -1.0f, -1.0f, -1.0f,
                1.0f, -1.0f, -1.0f,
                1.0f, -1.0f,  1.0f,
                -1.0f, -1.0f,  1.0f
        };

        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, Float32Array.copyFromJavaArray(vertices),
                WebGLRenderingContext.STATIC_DRAW);

        // Create vertex color buffer
        vertexColorBuffer = gl.createBuffer();
        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, vertexColorBuffer);

        // Define colors for each vertex
        float[] colors = {
                // Front face (red)
                1.0f, 0.0f, 0.0f, 1.0f,
                1.0f, 0.0f, 0.0f, 1.0f,
                1.0f, 0.0f, 0.0f, 1.0f,
                1.0f, 0.0f, 0.0f, 1.0f,

                // Back face (green)
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,

                // Left face (blue)
                0.0f, 0.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                // Right face (yellow)
                1.0f, 1.0f, 0.0f, 1.0f,
                1.0f, 1.0f, 0.0f, 1.0f,
                1.0f, 1.0f, 0.0f, 1.0f,
                1.0f, 1.0f, 0.0f, 1.0f,

                // Top face (cyan)
                0.0f, 1.0f, 1.0f, 1.0f,
                0.0f, 1.0f, 1.0f, 1.0f,
                0.0f, 1.0f, 1.0f, 1.0f,
                0.0f, 1.0f, 1.0f, 1.0f,

                // Bottom face (magenta)
                1.0f, 0.0f, 1.0f, 1.0f,
                1.0f, 0.0f, 1.0f, 1.0f,
                1.0f, 0.0f, 1.0f, 1.0f,
                1.0f, 0.0f, 1.0f, 1.0f
        };

        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, Float32Array.copyFromJavaArray(colors),
                WebGLRenderingContext.STATIC_DRAW);

        // Create vertex normal buffer
        vertexNormalBuffer = gl.createBuffer();
        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, vertexNormalBuffer);

        // Define normals for each vertex (each face has the same normal for all vertices)
        float[] normals = {
                // Front face - normal pointing towards positive Z
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                // Back face - normal pointing towards negative Z
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,

                // Left face - normal pointing towards negative X
                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,

                // Right face - normal pointing towards positive X
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,

                // Top face - normal pointing towards positive Y
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,

                // Bottom face - normal pointing towards negative Y
                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f
        };

        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, Float32Array.copyFromJavaArray(normals),
                WebGLRenderingContext.STATIC_DRAW);

        // Create index buffer (for drawing the cube faces)
        indexBuffer = gl.createBuffer();
        gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, indexBuffer);

        // Define indices for the cube faces (6 faces, 2 triangles per face)
        short[] indices = {
                0, 1, 2,      0, 2, 3,    // Front face
                4, 5, 6,      4, 6, 7,    // Back face
                8, 9, 10,     8, 10, 11,  // Left face
                12, 13, 14,   12, 14, 15, // Right face
                16, 17, 18,   16, 18, 19, // Top face
                20, 21, 22,   20, 22, 23  // Bottom face
        };

        gl.bufferData(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, Int16Array.copyFromJavaArray(indices),
                WebGLRenderingContext.STATIC_DRAW);
    }

    private static void drawScene() {
        gl.clear(WebGLRenderingContext.COLOR_BUFFER_BIT | WebGLRenderingContext.DEPTH_BUFFER_BIT);

        var mat = createRotationMatrix(rotationAngle, 1, 1, 0.5f);
        mat = multiplyMatrices(mat, createTranslationMatrix(0.0f, 0.0f, -5.0f));

        // Bind vertex position buffer
        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, vertexPositionBuffer);
        gl.vertexAttribPointer(gl.getAttribLocation(shaderProgram, "aVertexPosition"), 3,
                WebGLRenderingContext.FLOAT, false, 0, 0);

        // Bind vertex color buffer
        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, vertexColorBuffer);
        gl.vertexAttribPointer(gl.getAttribLocation(shaderProgram, "aVertexColor"), 4,
                WebGLRenderingContext.FLOAT, false, 0, 0);

        // Bind vertex normal buffer
        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, vertexNormalBuffer);
        gl.vertexAttribPointer(gl.getAttribLocation(shaderProgram, "aVertexNormal"), 3,
                WebGLRenderingContext.FLOAT, false, 0, 0);

        // Bind index buffer
        gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, indexBuffer);

        // Set matrix uniforms
        gl.uniformMatrix4fv(pMatrixUniform, false, Float32Array.copyFromJavaArray(pMatrix));
        gl.uniformMatrix4fv(mvMatrixUniform, false, Float32Array.copyFromJavaArray(mat));

        // Set lighting uniforms
        // Light position in world space
        gl.uniform3f(lightPositionUniform, 5.0f, 5.0f, 5.0f);

        // View position (camera position) in world space
        gl.uniform3f(viewPositionUniform, 0.0f, 0.0f, 0.0f);

        // Ambient light color (low intensity white)
        gl.uniform3f(ambientColorUniform, 0.2f, 0.2f, 0.2f);

        // Light color (white)
        gl.uniform3f(lightColorUniform, 1.0f, 1.0f, 1.0f);

        // Shininess for specular component
        gl.uniform1f(shininessUniform, 32.0f);

        // Draw the cube
        gl.drawElements(WebGLRenderingContext.TRIANGLES, 36, WebGLRenderingContext.UNSIGNED_SHORT, 0);
    }

    private static void startAnimation() {
        Window.requestAnimationFrame(timestamp -> {
            rotationAngle += 0.01f;
            drawScene();
            startAnimation();
        });
    }

    // Matrix utility functions
    private static void identityMatrix(float[] matrix) {
        matrix[0] = 1.0f;  matrix[1] = 0.0f;  matrix[2] = 0.0f;  matrix[3] = 0.0f;
        matrix[4] = 0.0f;  matrix[5] = 1.0f;  matrix[6] = 0.0f;  matrix[7] = 0.0f;
        matrix[8] = 0.0f;  matrix[9] = 0.0f;  matrix[10] = 1.0f; matrix[11] = 0.0f;
        matrix[12] = 0.0f; matrix[13] = 0.0f; matrix[14] = 0.0f; matrix[15] = 1.0f;
    }

    private static float[] createTranslationMatrix(float x, float y, float z) {
        float[] matrix = new float[16];
        identityMatrix(matrix);
        matrix[12] = x;
        matrix[13] = y;
        matrix[14] = z;
        return matrix;
    }

    private static float[] createRotationMatrix(float angle, float x, float y, float z) {
        float[] matrix = new float[16];
        float c = (float) Math.cos(angle);
        float s = (float) Math.sin(angle);
        float t = 1.0f - c;

        // Normalize rotation vector
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len > 0.00001f) {
            x /= len;
            y /= len;
            z /= len;
        }

        matrix[0] = t * x * x + c;
        matrix[1] = t * x * y + s * z;
        matrix[2] = t * x * z - s * y;
        matrix[3] = 0.0f;

        matrix[4] = t * x * y - s * z;
        matrix[5] = t * y * y + c;
        matrix[6] = t * y * z + s * x;
        matrix[7] = 0.0f;

        matrix[8] = t * x * z + s * y;
        matrix[9] = t * y * z - s * x;
        matrix[10] = t * z * z + c;
        matrix[11] = 0.0f;

        matrix[12] = 0.0f;
        matrix[13] = 0.0f;
        matrix[14] = 0.0f;
        matrix[15] = 1.0f;

        return matrix;
    }

    // Multiply two matrices
    private static float[] multiplyMatrices(float[] a, float[] b) {
        var result = new float[16];
        for (var i = 0; i < 4; i++) {
            for (var j = 0; j < 4; j++) {
                result[i * 4 + j] = 0.0f;
                for (var k = 0; k < 4; k++) {
                    result[i * 4 + j] += a[i * 4 + k] * b[k * 4 + j];
                }
            }
        }
        return result;
    }

    private static float[] perspectiveMatrix(float fovy, float aspect, float zNear, float zFar) {
        var matrix = new float[16];
        var f = (float) (1.0 / Math.tan(fovy * Math.PI / 360.0));

        matrix[0] = f / aspect;
        matrix[1] = 0.0f;
        matrix[2] = 0.0f;
        matrix[3] = 0.0f;

        matrix[4] = 0.0f;
        matrix[5] = f;
        matrix[6] = 0.0f;
        matrix[7] = 0.0f;

        matrix[8] = 0.0f;
        matrix[9] = 0.0f;
        matrix[10] = (zFar + zNear) / (zNear - zFar);
        matrix[11] = -1.0f;

        matrix[12] = 0.0f;
        matrix[13] = 0.0f;
        matrix[14] = (2.0f * zFar * zNear) / (zNear - zFar);
        matrix[15] = 0.0f;

        return matrix;
    }
}
