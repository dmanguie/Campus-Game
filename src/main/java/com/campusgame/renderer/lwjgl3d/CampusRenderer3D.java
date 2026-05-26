package com.campusgame.renderer.lwjgl3d;

import com.campusgame.map.data.BuildingData;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.opengl.GL;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * CampusRenderer3D
 * ─────────────────────────────────────────────────────────────────────────────
 * Standalone LWJGL 3.3 prototype.  Reads existing BuildingData and renders
 * the campus as coloured 3-D boxes on a grass plane.
 *
 * DOES NOT touch GameLoop, Swing renderer, or any other game systems.
 *
 * Run from main():  new CampusRenderer3D(buildings).run();
 *
 * Controls
 * ─────────
 *   W / S          – move camera forward / back
 *   A / D          – strafe left / right
 *   Mouse drag     – look around (click-drag)
 *   Scroll wheel   – zoom (move along look direction)
 *   ESC            – close window
 */
public class CampusRenderer3D {

    // ── Window ────────────────────────────────────────────────────────
    private static final int   WIN_W  = 1280;
    private static final int   WIN_H  = 720;
    private static final String TITLE = "Campus Quest — 3D Prototype (LWJGL)";

    // ── World scale ───────────────────────────────────────────────────
    /**
     * Campus world units are pixels (~3000×2400).  We scale them down
     * so they feel like metres:  1 world unit = SCALE metres.
     */
    private static final float SCALE          = 0.05f;
    private static final float PIXELS_PER_FLOOR = BuildingData.METERS_PER_FLOOR; // 30
    private static final float FLOOR_HEIGHT_3D  = 3.5f;   // metres per floor in 3-D

    // ── OpenGL handles ────────────────────────────────────────────────
    private long window;
    private int  shaderProgram;
    private int  cubeVao;
    private int  groundVao;
    private int  groundIbo;
    private int  groundIndexCount;

    // ── Camera ────────────────────────────────────────────────────────
    private final FlyCamera camera = new FlyCamera();

    // ── Scene data ────────────────────────────────────────────────────
    private final List<BuildingData> buildings;

    // Cached GL buffer for box draws (reuse per building)
    private final Matrix4f model      = new Matrix4f();
    private final Matrix4f view       = new Matrix4f();
    private final Matrix4f projection = new Matrix4f();

    // ─────────────────────────────────────────────────────────────────

    public CampusRenderer3D(List<BuildingData> buildings) {
        this.buildings = buildings;
    }

    // ── Entry point ───────────────────────────────────────────────────

    public void run() {
        init();
        loop();
        cleanup();
    }

    // ── Initialisation ────────────────────────────────────────────────

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!GLFW.glfwInit())
            throw new IllegalStateException("GLFW init failed");

        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE); // macOS
        GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, 4); // MSAA

        window = GLFW.glfwCreateWindow(WIN_W, WIN_H, TITLE, 0L, 0L);
        if (window == 0L) throw new RuntimeException("GLFW window creation failed");

        // Centre on primary monitor
        var vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        if (vidMode != null)
            GLFW.glfwSetWindowPos(window,
                    (vidMode.width()  - WIN_W) / 2,
                    (vidMode.height() - WIN_H) / 2);

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1); // vsync

        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_MULTISAMPLE);

        // Semi-transparent sky colour (light blue)
        glClearColor(0.52f, 0.74f, 0.84f, 1f);

        shaderProgram  = ShaderUtil.buildProgram(VERT_SRC, FRAG_SRC);
        cubeVao        = buildCubeVao();
        buildGroundBuffers();

        // Input
        camera.attachTo(window);
        GLFW.glfwSetKeyCallback(window, (win, key, sc, action, mods) -> {
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS)
                GLFW.glfwSetWindowShouldClose(win, true);
        });

        // Position camera above campus centre
        float worldCX = 1500 * SCALE;
        float worldCZ = 1200 * SCALE;
        camera.position.set(worldCX, 60f, worldCZ + 120f);
        camera.pitch = -20f;
        camera.yaw   = 180f;
    }

    // ── Main loop ─────────────────────────────────────────────────────

    private void loop() {
        long last = System.nanoTime();
        while (!GLFW.glfwWindowShouldClose(window)) {
            long now   = System.nanoTime();
            float dt   = (now - last) / 1_000_000_000f;
            last       = now;

            camera.update(window, dt);

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            int[] w = new int[1], h = new int[1];
            GLFW.glfwGetFramebufferSize(window, w, h);
            if (h[0] == 0) h[0] = 1;
            glViewport(0, 0, w[0], h[0]);

            glUseProgram(shaderProgram);

            // Projection + view (update every frame)
            projection.identity().perspective(
                    (float) Math.toRadians(60f),
                    (float) w[0] / h[0], 0.1f, 5000f);
            setUniformMat4("uProj", projection);

            camera.buildViewMatrix(view);
            setUniformMat4("uView", view);

            // ── Ground plane ──────────────────────────────────────────
            model.identity();
            setUniformMat4("uModel", model);
            setUniformVec3("uColor", 0.42f, 0.63f, 0.33f); // grass green
            glBindVertexArray(groundVao);
            glDrawElements(GL_TRIANGLES, groundIndexCount, GL_UNSIGNED_INT, 0L);

            // ── Buildings ─────────────────────────────────────────────
            glBindVertexArray(cubeVao);
            for (BuildingData b : buildings) {
                if (b.isPolygon()) continue; // skip polygons (first prototype)

                float bx  = b.x * SCALE;
                float bz  = b.z * SCALE;
                float bw  = b.width  * SCALE;
                float bd  = b.depth  * SCALE;
                float bh  = b.floors * FLOOR_HEIGHT_3D;

                model.identity()
                        .translate(bx, 0f, bz)
                        .scale(bw, bh, bd);
                setUniformMat4("uModel", model);

                float r = ((b.colorARGB >> 16) & 0xFF) / 255f;
                float g = ((b.colorARGB >>  8) & 0xFF) / 255f;
                float bv=  (b.colorARGB        & 0xFF) / 255f;
                setUniformVec3("uColor", r, g, bv);

                glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0L);
            }

            glBindVertexArray(0);
            glUseProgram(0);

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────────

    private void cleanup() {
        glDeleteVertexArrays(cubeVao);
        glDeleteVertexArrays(groundVao);
        glDeleteBuffers(groundIbo);
        glDeleteProgram(shaderProgram);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    // ── Cube VAO (unit cube 0,0,0 → 1,1,1) ───────────────────────────

    private int buildCubeVao() {
        // 8 unique corners; we use an index buffer for 36 triangle indices
        float[] verts = {
                // x, y, z
                0,0,0,  1,0,0,  1,1,0,  0,1,0,  // back face   (z=0)
                0,0,1,  1,0,1,  1,1,1,  0,1,1,  // front face  (z=1)
        };
        int[] idx = {
                // back  (z=0, normal 0,0,-1)
                0,2,1, 0,3,2,
                // front (z=1, normal 0,0,+1)
                4,5,6, 4,6,7,
                // left  (x=0, normal -1,0,0)
                0,4,7, 0,7,3,
                // right (x=1, normal +1,0,0)
                1,2,6, 1,6,5,
                // bottom(y=0, normal 0,-1,0)
                0,1,5, 0,5,4,
                // top   (y=1, normal 0,+1,0)
                3,7,6, 3,6,2,
        };

        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        int ibo = glGenBuffers();
        glBindVertexArray(vao);

        FloatBuffer vb = BufferUtils.createFloatBuffer(verts.length);
        vb.put(verts).flip();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vb, GL_STATIC_DRAW);

        IntBuffer ib = BufferUtils.createIntBuffer(idx.length);
        ib.put(idx).flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);

        glBindVertexArray(0);
        return vao;
    }

    // ── Ground plane (covers full campus extent) ──────────────────────

    private void buildGroundBuffers() {
        float w = 3000 * SCALE;
        float d = 2400 * SCALE;
        float[] verts = { 0,0,0,  w,0,0,  w,0,d,  0,0,d };
        int[]   idx   = { 0,1,2,  0,2,3 };

        groundVao = glGenVertexArrays();
        int vbo   = glGenBuffers();
        groundIbo = glGenBuffers();
        glBindVertexArray(groundVao);

        FloatBuffer vb = BufferUtils.createFloatBuffer(verts.length);
        vb.put(verts).flip();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vb, GL_STATIC_DRAW);

        IntBuffer ib = BufferUtils.createIntBuffer(idx.length);
        ib.put(idx).flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, groundIbo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);

        glBindVertexArray(0);
        groundIndexCount = idx.length;
    }

    // ── Shader uniform helpers ────────────────────────────────────────

    private void setUniformMat4(String name, Matrix4f m) {
        int loc = glGetUniformLocation(shaderProgram, name);
        FloatBuffer buf = BufferUtils.createFloatBuffer(16);
        m.get(buf);
        glUniformMatrix4fv(loc, false, buf);
    }

    private void setUniformVec3(String name, float x, float y, float z) {
        int loc = glGetUniformLocation(shaderProgram, name);
        glUniform3f(loc, x, y, z);
    }

    // ── Shaders ───────────────────────────────────────────────────────

    private static final String VERT_SRC = """
        #version 330 core
        layout(location = 0) in vec3 aPos;
        uniform mat4 uModel;
        uniform mat4 uView;
        uniform mat4 uProj;
        out vec3 vWorldPos;
        out vec3 vNormal;
        void main() {
            vec4 worldPos = uModel * vec4(aPos, 1.0);
            vWorldPos = worldPos.xyz;
            // Approximate face normal from vertex position in model space
            // (good enough for flat-shaded cubes)
            vNormal = normalize(mat3(transpose(inverse(uModel))) * (aPos - vec3(0.5)));
            gl_Position = uProj * uView * worldPos;
        }
        """;

    private static final String FRAG_SRC = """
        #version 330 core
        in vec3 vWorldPos;
        in vec3 vNormal;
        uniform vec3 uColor;
        out vec4 FragColor;
        void main() {
            // Simple directional light from above-right
            vec3 lightDir = normalize(vec3(0.6, 1.0, 0.4));
            float diff    = max(dot(normalize(vNormal), lightDir), 0.0);
            float ambient = 0.35;
            float light   = ambient + diff * 0.65;
            // Subtle fog based on distance
            float dist    = length(vWorldPos - vec3(75.0, 0.0, 60.0));
            float fog     = clamp(dist / 600.0, 0.0, 1.0);
            vec3  fogColor = vec3(0.52, 0.74, 0.84);
            vec3  lit     = uColor * light;
            vec3  final_c = mix(lit, fogColor, fog * fog);
            FragColor = vec4(final_c, 1.0);
        }
        """;
}