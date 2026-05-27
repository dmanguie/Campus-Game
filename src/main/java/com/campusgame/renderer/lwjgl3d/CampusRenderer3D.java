package com.campusgame.renderer.lwjgl3d;

import com.campusgame.map.data.BuildingData;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import java.util.HashMap;
import java.util.Map;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * CampusRenderer3D — Phase 7.1
 * ─────────────────────────────────────────────────────────────────────────
 * Added: ray-pick building selection (Tab = toggle pick/fly, left-click = pick).
 * Selected building is brightened + gets a yellow wireframe outline.
 * Selection is read-only — no data is modified or saved.
 */
public class CampusRenderer3D {

    // ── Window ────────────────────────────────────────────────────────
    private static final int    WIN_W = 1280;
    private static final int    WIN_H = 720;
    private static final String TITLE = "Campus Quest — 3D Prototype (LWJGL)";

    // ── World scale ───────────────────────────────────────────────────
    private static final float SCALE           = 0.05f;
    private static final float FLOOR_HEIGHT_3D = 3.5f;   // metres per floor in 3-D

    // ── OpenGL handles ────────────────────────────────────────────────
    private long window;
    private int  shaderProgram;
    private int minimapShader;
    private int  cubeVao;
    private int  groundVao;
    private int  groundIbo;
    private int  groundIndexCount;
    private boolean debugPrinted = false;
    private boolean minimapVisible  = true;        // M key toggle
    private float   minimapZoom     = 1.0f;        // scroll to zoom
    private boolean minimapNorthUp  = true;        // false = camera-relative (future)
    private int     minimapLineVao  = -1;          // VAO for path/polygon lines
    private int mmLineVao = 0;
    private int mmLineVbo = 0;
    private int mmPolyVao = 0;
    private int mmPolyVbo = 0;
    private final java.util.List<float[]> mmPathSegments = new java.util.ArrayList<>();
    private final List<com.campusgame.map.data.PathData> paths;
    private final Map<BuildingData, Integer> polygonVaos        = new HashMap<>();
    private final Map<BuildingData, Integer> polygonIndexCounts = new HashMap<>();

    // ── Minimap ───────────────────────────────────────────────────────────
    private static final int   MM_X      = 10;      // screen pixels from left
    private static final int   MM_Y      = 10;      // screen pixels from top
    private static final int   MM_W      = 220;     // minimap width in pixels
    private static final int   MM_H      = 180;     // minimap height in pixels
    private static final float MM_WORLD_W = 3000 * SCALE;  // world width in 3D units
    private static final float MM_WORLD_H = 2400 * SCALE;  // world height in 3D units

    private int mmQuadVao;

    // ── Camera ────────────────────────────────────────────────────────
    private final FlyCamera camera = new FlyCamera();

    // ── Scene data ────────────────────────────────────────────────────
    private final List<BuildingData> buildings;

    // ── Selection (Phase 7.1) ─────────────────────────────────────────
    private BuildingData selectedBuilding = null;

    // ── Matrices ──────────────────────────────────────────────────────
    private final Matrix4f model      = new Matrix4f();
    private final Matrix4f view       = new Matrix4f();
    private final Matrix4f projection = new Matrix4f();

    // ── Cached framebuffer size (updated each frame) ──────────────────
    private int currentW = WIN_W;
    private int currentH = WIN_H;

    // ─────────────────────────────────────────────────────────────────

    public CampusRenderer3D(List<BuildingData> buildings,
                            List<com.campusgame.map.data.PathData> paths) {
        this.buildings = buildings;
        this.paths     = paths != null ? paths : java.util.Collections.emptyList();
    }

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void buildMmLineBuffers() {
        // Line VAO — 2 vertices × 2 floats, updated each draw
        mmLineVao = glGenVertexArrays();
        mmLineVbo = glGenBuffers();
        glBindVertexArray(mmLineVao);
        glBindBuffer(GL_ARRAY_BUFFER, mmLineVbo);
        glBufferData(GL_ARRAY_BUFFER, 4 * Float.BYTES, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);
        glBindVertexArray(0);

        // Poly VAO — up to 256 triangles, updated each draw
        mmPolyVao = glGenVertexArrays();
        mmPolyVbo = glGenBuffers();
        glBindVertexArray(mmPolyVao);
        glBindBuffer(GL_ARRAY_BUFFER, mmPolyVbo);
        glBufferData(GL_ARRAY_BUFFER, 256 * 3 * 2 * Float.BYTES, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);
        glBindVertexArray(0);
    }

    // ── Init ──────────────────────────────────────────────────────────

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!GLFW.glfwInit())
            throw new IllegalStateException("GLFW init failed");

        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, 4);

        window = GLFW.glfwCreateWindow(WIN_W, WIN_H, TITLE, 0L, 0L);
        if (window == 0L) throw new RuntimeException("GLFW window creation failed");

        var vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        if (vidMode != null)
            GLFW.glfwSetWindowPos(window,
                    (vidMode.width()  - WIN_W) / 2,
                    (vidMode.height() - WIN_H) / 2);

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);

        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_MULTISAMPLE);
        glClearColor(0.52f, 0.74f, 0.84f, 1f);

        shaderProgram = ShaderUtil.buildProgram(VERT_SRC, FRAG_SRC);
        minimapShader = ShaderUtil.buildProgram(MM_VERT_SRC, MM_FRAG_SRC);
        cubeVao       = buildCubeVao();
        buildGroundBuffers();
        mmQuadVao = buildMmQuadVao();
        buildMmLineBuffers();


        // Pre-build extruded VAOs for polygon buildings
        for (BuildingData b : buildings) {
            if (!b.isPolygon() || b.polygonX.length < 3) continue;
            int[] result = buildPolygonExtrusionVao(b);
            polygonVaos.put(b, result[0]);
            polygonIndexCounts.put(b, result[1]);
        }

        camera.attachTo(window);
        bakeMmPathSegments();

        // ── Key callback ──────────────────────────────────────────────
        GLFW.glfwSetKeyCallback(window, (win, key, sc, action, mods) -> {
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS)
                GLFW.glfwSetWindowShouldClose(win, true);

            if (key == GLFW.GLFW_KEY_TAB && action == GLFW.GLFW_PRESS) {
                camera.togglePickMode(win);
                System.out.println("[3D] " + (camera.isPickMode()
                        ? "PICK MODE  — click a building to select it"
                        : "FLY MODE   — mouse look active"));
            }

            if (key == GLFW.GLFW_KEY_M && action == GLFW.GLFW_PRESS) {
                minimapVisible = !minimapVisible;
                System.out.println("[3D] Minimap " + (minimapVisible ? "ON" : "OFF"));
            }
        });

        // ── Mouse button callback (Phase 7.1: ray pick) ───────────────
        GLFW.glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS
                    && camera.isPickMode()) {

                int[] fw = new int[1], fh = new int[1];
                GLFW.glfwGetFramebufferSize(win, fw, fh);

                BuildingData hit = pickBuilding(
                        camera.getMouseX(), camera.getMouseY(), fw[0], fh[0]);

                if (hit != selectedBuilding) {
                    selectedBuilding = hit;
                    if (hit != null) {
                        System.out.printf("[3D] Selected: \"%s\"  (%d floor%s, tag=%s)%n",
                                hit.name, hit.floors,
                                hit.floors == 1 ? "" : "s",
                                hit.tag != null ? hit.tag : "—");
                    } else {
                        System.out.println("[3D] Deselected");
                    }
                }
            }
        });

        // Initial camera position — look over campus centre
        camera.position.set(1500 * SCALE, 60f, 1200 * SCALE + 120f);
        camera.pitch = -20f;
        camera.yaw   = 180f;

        System.out.println("[3D] Tab = toggle pick / fly mode  |  ESC = quit");
    }

    private void bakeMmPathSegments() {
        mmPathSegments.clear();
        for (com.campusgame.map.data.PathData p : paths) {
            if (!p.isValid()) continue;
            for (int i = 0; i < p.points.size() - 1; i++) {
                float x1 = p.points.get(i    )[0] * SCALE;
                float z1 = p.points.get(i    )[1] * SCALE;
                float x2 = p.points.get(i + 1)[0] * SCALE;
                float z2 = p.points.get(i + 1)[1] * SCALE;
                mmPathSegments.add(new float[]{x1, z1, x2, z2});
            }
        }
    }

    private static final String MM_VERT_SRC = """
    #version 330 core
    layout(location = 0) in vec2 aPos;
    uniform mat4 uProj;
    uniform vec2 uPos;
    uniform vec2 uSize;
    uniform int uRawCoords;
    void main() {
        vec2 world = (uRawCoords == 1) ? aPos : (uPos + aPos * uSize);
        gl_Position = uProj * vec4(world, 0.0, 1.0);
    }
    """;

    private static final String MM_FRAG_SRC = """
        #version 330 core
        uniform vec3 uColor;
        out vec4 FragColor;
        void main() {
            FragColor = vec4(uColor, 0.88);
        }
    """;


    private int buildMmQuadVao() {
        // Unit quad [0,1]×[0,1] — shader scales via uPos/uSize
        float[] verts = { 0f,0f,  1f,0f,  0f,1f,  1f,1f };
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        glBindVertexArray(vao);
        FloatBuffer vb = BufferUtils.createFloatBuffer(verts.length);
        vb.put(verts).flip();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vb, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);
        glBindVertexArray(0);
        return vao;
    }

    // ── Minimap ───────────────────────────────────────────────────────

    private void drawMinimap() {
        if (!minimapVisible) return;

        float worldW = MM_WORLD_W / minimapZoom;
        float worldH = MM_WORLD_H / minimapZoom;

        float camX = camera.position.x;
        float camZ = camera.position.z;
        float viewX0 = (minimapZoom > 1.0f) ? camX - worldW * 0.5f : 0f;
        float viewZ0 = (minimapZoom > 1.0f) ? camZ - worldH * 0.5f : 0f;

        // ── DEBUG: print first few building positions ─────────────────
        if (!debugPrinted) {
            System.out.printf("[MM] worldW=%.2f worldH=%.2f viewX0=%.2f viewZ0=%.2f%n",
                    worldW, worldH, viewX0, viewZ0);
            int count = 0;
            for (BuildingData b : buildings) {
                if (count++ > 5) break;
                if (b.isPolygon()) {
                    System.out.printf("[MM] polygon '%s' px[0]=%.0f pz[0]=%.0f → mmX=%.2f mmZ=%.2f%n",
                            b.name, (float)b.polygonX[0], (float)b.polygonZ[0],
                            b.polygonX[0] * SCALE, b.polygonZ[0] * SCALE);
                } else {
                    System.out.printf("[MM] rect '%s' x=%.0f z=%.0f w=%.0f d=%.0f → mmX=%.2f mmZ=%.2f mmW=%.2f mmD=%.2f%n",
                            b.name, b.x, b.z, b.width, b.depth,
                            b.x * SCALE, b.z * SCALE, b.width * SCALE, b.depth * SCALE);
                }
            }
        }

        int[] vp = new int[4];
        glGetIntegerv(GL_VIEWPORT, vp);

        int mmLeft   = MM_X;
        int mmBottom = currentH - MM_Y - MM_H;
        glEnable(GL_SCISSOR_TEST);
        glScissor(mmLeft, mmBottom, MM_W, MM_H);
        glViewport(mmLeft, mmBottom, MM_W, MM_H);

        glClearColor(0.08f, 0.08f, 0.10f, 0.85f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glDisable(GL_DEPTH_TEST);

        glUseProgram(minimapShader);

        // Store locations once
        int projLoc  = glGetUniformLocation(minimapShader, "uProj");
        int colorLoc = glGetUniformLocation(minimapShader, "uColor");
        int posLoc   = glGetUniformLocation(minimapShader, "uPos");
        int sizeLoc  = glGetUniformLocation(minimapShader, "uSize");
        int rawLoc   = glGetUniformLocation(minimapShader, "uRawCoords");

        if (!debugPrinted) {
            System.out.printf("[MM] uniform locs: proj=%d color=%d pos=%d size=%d raw=%d%n",
                    projLoc, colorLoc, posLoc, sizeLoc, rawLoc);
        }

        Matrix4f ortho = new Matrix4f().ortho(
                viewX0, viewX0 + worldW,
                viewZ0 + worldH, viewZ0,
                -1f, 1f);

        // Helper: always call this after any line/polygon draw to restore state
        FloatBuffer projBuf = BufferUtils.createFloatBuffer(16);

        // Upload projection
        ortho.get(projBuf);
        glUniformMatrix4fv(projLoc, false, projBuf);
        glUniform1i(rawLoc, 0);

        // ── Ground ───────────────────────────────────────────────────────
        glUniform3f(colorLoc, 0.20f, 0.35f, 0.16f);
        glUniform2f(posLoc,  viewX0, viewZ0);
        glUniform2f(sizeLoc, worldW, worldH);
        glBindVertexArray(mmQuadVao);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glBindVertexArray(0);

        // ── Paths ────────────────────────────────────────────────────────
        for (float[] seg : mmPathSegments) {
            drawMinimapLine(seg[0], seg[1], seg[2], seg[3], 0.86f, 0.84f, 0.78f, 2.5f);
            // Restore after each raw draw
            glUseProgram(minimapShader);
            projBuf.clear(); ortho.get(projBuf);
            glUniformMatrix4fv(projLoc, false, projBuf);
            glUniform1i(rawLoc, 0);
        }



        // ── Buildings ────────────────────────────────────────────────────
        for (BuildingData b : buildings) {
            boolean sel = (b == selectedBuilding);

            float r  = ((b.colorARGB >> 16) & 0xFF) / 255f;
            float g  = ((b.colorARGB >>  8) & 0xFF) / 255f;
            float bv =  (b.colorARGB        & 0xFF) / 255f;
            if (r == 0f && g == 0f && bv == 0f) { r = 0.55f; g = 0.55f; bv = 0.55f; }
            if (sel) { r = 1f; g = 0.9f; bv = 0.1f; }

            if (b.isPolygon() && b.polygonX.length >= 3) {
                drawMinimapPolygon(b, r, g, bv);
                glUseProgram(minimapShader);
                projBuf.clear(); ortho.get(projBuf);
                glUniformMatrix4fv(projLoc, false, projBuf);
                glUniform1i(rawLoc, 0);

                if (sel) {
                    for (int i = 0; i < b.polygonX.length; i++) {
                        int next = (i + 1) % b.polygonX.length;
                        drawMinimapLine(
                                b.polygonX[i]    * SCALE, b.polygonZ[i]    * SCALE,
                                b.polygonX[next] * SCALE, b.polygonZ[next] * SCALE,
                                1f, 0.9f, 0.1f, 3.0f);
                        glUseProgram(minimapShader);
                        projBuf.clear(); ortho.get(projBuf);
                        glUniformMatrix4fv(projLoc, false, projBuf);
                        glUniform1i(rawLoc, 0);
                    }
                }

            } else {
                float bx = b.x * SCALE, bz = b.z * SCALE;
                float bw = b.width * SCALE, bd = b.depth * SCALE;
                if (bw <= 0 || bd <= 0) continue;

                glUniform1i(rawLoc, 0);
                glUniform3f(colorLoc, r, g, bv);
                glUniform2f(posLoc,  bx, bz);
                glUniform2f(sizeLoc, bw, bd);
                glBindVertexArray(mmQuadVao);
                glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
                glBindVertexArray(0);

                if (sel) {
                    drawMinimapLine(bx,      bz,      bx + bw, bz,      1f, 0.9f, 0.1f, 2.5f);
                    drawMinimapLine(bx + bw, bz,      bx + bw, bz + bd, 1f, 0.9f, 0.1f, 2.5f);
                    drawMinimapLine(bx + bw, bz + bd, bx,      bz + bd, 1f, 0.9f, 0.1f, 2.5f);
                    drawMinimapLine(bx,      bz + bd, bx,      bz,      1f, 0.9f, 0.1f, 2.5f);
                    glUseProgram(minimapShader);
                    projBuf.clear(); ortho.get(projBuf);
                    glUniformMatrix4fv(projLoc, false, projBuf);
                    glUniform1i(rawLoc, 0);
                }
            }
        }

        // ── Camera dot ───────────────────────────────────────────────────
        float dotR = worldW * 0.008f;
        glUniform1i(rawLoc, 0);
        glUniform3f(colorLoc, 1f, 1f, 1f);
        glUniform2f(posLoc,  camX - dotR, camZ - dotR);
        glUniform2f(sizeLoc, dotR * 2f,   dotR * 2f);
        glBindVertexArray(mmQuadVao);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glBindVertexArray(0);

        // ── Camera direction cone ─────────────────────────────────────────
        float yawR    = (float) Math.toRadians(camera.yaw);
        float fwdX    = (float) Math.sin(yawR);
        float fwdZ    = (float) Math.cos(yawR);
        float coneLen = worldW * 0.045f;
        float coneW   = coneLen * 0.45f;
        float perpX   = -fwdZ, perpZ = fwdX;

        float tipX = camX + fwdX * coneLen,  tipZ = camZ + fwdZ * coneLen;
        float lbX  = camX + perpX * coneW,   lbZ  = camZ + perpZ * coneW;
        float rbX  = camX - perpX * coneW,   rbZ  = camZ - perpZ * coneW;

        FloatBuffer coneBuf = BufferUtils.createFloatBuffer(6);
        coneBuf.put(tipX).put(tipZ).put(lbX).put(lbZ).put(rbX).put(rbZ).flip();

        int coneVao = glGenVertexArrays(), coneVbo = glGenBuffers();
        glBindVertexArray(coneVao);
        glBindBuffer(GL_ARRAY_BUFFER, coneVbo);
        glBufferData(GL_ARRAY_BUFFER, coneBuf, GL_STREAM_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);
        glUniform1i(rawLoc, 1);
        glUniform3f(colorLoc, 1f, 0.65f, 0.1f);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);
        glDeleteVertexArrays(coneVao);
        glDeleteBuffers(coneVbo);

        // ── Border ────────────────────────────────────────────────────────
        projBuf.clear(); ortho.get(projBuf);
        glUniformMatrix4fv(projLoc, false, projBuf);
        glUniform1i(rawLoc, 0);
        glUniform3f(colorLoc, 0.7f, 0.7f, 0.7f);

        float bw2 = worldW * 0.004f, bh2 = worldH * 0.005f;
        glUniform2f(posLoc, viewX0, viewZ0);
        glUniform2f(sizeLoc, worldW, bh2);
        glBindVertexArray(mmQuadVao); glDrawArrays(GL_TRIANGLE_STRIP, 0, 4); glBindVertexArray(0);

        glUniform2f(posLoc, viewX0, viewZ0 + worldH - bh2);
        glBindVertexArray(mmQuadVao); glDrawArrays(GL_TRIANGLE_STRIP, 0, 4); glBindVertexArray(0);

        glUniform2f(posLoc, viewX0, viewZ0);
        glUniform2f(sizeLoc, bw2, worldH);
        glBindVertexArray(mmQuadVao); glDrawArrays(GL_TRIANGLE_STRIP, 0, 4); glBindVertexArray(0);

        glUniform2f(posLoc, viewX0 + worldW - bw2, viewZ0);
        glBindVertexArray(mmQuadVao); glDrawArrays(GL_TRIANGLE_STRIP, 0, 4); glBindVertexArray(0);

        // ── Restore ───────────────────────────────────────────────────────
        glDisable(GL_SCISSOR_TEST);
        glViewport(vp[0], vp[1], vp[2], vp[3]);
        glClearColor(0.52f, 0.74f, 0.84f, 1f);
        glEnable(GL_DEPTH_TEST);   // ← add this
        glUseProgram(shaderProgram);
    }
    /** Draws a screen-aligned rectangle using the minimap shader's uPos/uSize uniforms. */
    private void drawMinimapRect() {
        glUniform1i(glGetUniformLocation(minimapShader, "uRawCoords"), 0);
        glBindVertexArray(mmQuadVao);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glBindVertexArray(0);
    }

    private void drawMinimapLine(float x1, float z1, float x2, float z2,
                                 float r, float g, float b, float lineW) {
        glUniform1i(glGetUniformLocation(minimapShader, "uRawCoords"), 1);
        glUniform3f(glGetUniformLocation(minimapShader, "uColor"), r, g, b);
        glLineWidth(lineW);

        FloatBuffer pts = BufferUtils.createFloatBuffer(4);
        pts.put(x1).put(z1).put(x2).put(z2).flip();

        glBindVertexArray(mmLineVao);
        glBindBuffer(GL_ARRAY_BUFFER, mmLineVbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0L, pts);
        glDrawArrays(GL_LINES, 0, 2);
        glBindVertexArray(0);

        glLineWidth(1.0f);
    }

    private void drawMinimapPolygon(BuildingData b, float r, float g, float bv) {
        int n = b.polygonX.length;
        if (n < 3) return;

        glUniform1i(glGetUniformLocation(minimapShader, "uRawCoords"), 1);
        glUniform3f(glGetUniformLocation(minimapShader, "uColor"), r, g, bv);

        int triCount = n - 2;
        FloatBuffer fb = BufferUtils.createFloatBuffer(triCount * 3 * 2);
        for (int i = 1; i < n - 1; i++) {
            fb.put(b.polygonX[0]     * SCALE).put(b.polygonZ[0]     * SCALE);
            fb.put(b.polygonX[i]     * SCALE).put(b.polygonZ[i]     * SCALE);
            fb.put(b.polygonX[i + 1] * SCALE).put(b.polygonZ[i + 1] * SCALE);
        }
        fb.flip();

        glBindVertexArray(mmPolyVao);
        glBindBuffer(GL_ARRAY_BUFFER, mmPolyVbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0L, fb);
        glDrawArrays(GL_TRIANGLES, 0, triCount * 3);
        glBindVertexArray(0);
    }


    // ── Main loop ─────────────────────────────────────────────────────

    private void loop() {
        long last = System.nanoTime();
        while (!GLFW.glfwWindowShouldClose(window)) {
            long  now = System.nanoTime();
            float dt  = (now - last) / 1_000_000_000f;
            last      = now;

            camera.update(window, dt);

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            int[] w = new int[1], h = new int[1];
            GLFW.glfwGetFramebufferSize(window, w, h);
            if (h[0] == 0) h[0] = 1;
            currentW = w[0];
            currentH = h[0];
            glViewport(0, 0, currentW, currentH);

            glUseProgram(shaderProgram);

            projection.identity().perspective(
                    (float) Math.toRadians(60f),
                    (float) currentW / currentH, 0.1f, 5000f);
            setUniformMat4("uProj", projection);

            camera.buildViewMatrix(view);
            setUniformMat4("uView", view);

            // ── Ground ────────────────────────────────────────────────
            model.identity();
            setUniformMat4("uModel", model);
            setUniformVec3("uColor", 0.42f, 0.63f, 0.33f);
            glBindVertexArray(groundVao);
            glDrawElements(GL_TRIANGLES, groundIndexCount, GL_UNSIGNED_INT, 0L);

            // ── Buildings ─────────────────────────────────────────────
            // ── Buildings ─────────────────────────────────────────────
            glBindVertexArray(cubeVao);
            for (BuildingData b : buildings) {

                float bx, bz, bw, bd;
                if (b.isPolygon()) {
                    float minX = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
                    float maxX = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
                    for (int i = 0; i < b.polygonX.length; i++) {
                        minX = Math.min(minX, b.polygonX[i]);
                        minZ = Math.min(minZ, b.polygonZ[i]);
                        maxX = Math.max(maxX, b.polygonX[i]);
                        maxZ = Math.max(maxZ, b.polygonZ[i]);
                    }
                    bx = minX * SCALE; bz = minZ * SCALE;
                    bw = (maxX - minX) * SCALE; bd = (maxZ - minZ) * SCALE;
                } else {
                    bx = b.x * SCALE; bz = b.z * SCALE;
                    bw = b.width * SCALE; bd = b.depth * SCALE;
                }

                if (bw <= 0 || bd <= 0) continue;

                int   effectiveFloors = Math.max(1, b.floors);
                float bh              = effectiveFloors * FLOOR_HEIGHT_3D;
                boolean sel           = (b == selectedBuilding);

                // ── Resolve color BEFORE using it ─────────────────────
                float r  = ((b.colorARGB >> 16) & 0xFF) / 255f;
                float g  = ((b.colorARGB >>  8) & 0xFF) / 255f;
                float bv =  (b.colorARGB        & 0xFF) / 255f;

                if (r == 0f && g == 0f && bv == 0f) { r = 0.55f; g = 0.55f; bv = 0.55f; }

                if (sel) {
                    r  = Math.min(1f, r  * 1.4f + 0.15f);
                    g  = Math.min(1f, g  * 1.4f + 0.15f);
                    bv = Math.min(1f, bv * 1.4f + 0.15f);
                }

                if (b.isPolygon() && !sel) { bv = Math.min(1f, bv + 0.25f); }

                // ── Draw ──────────────────────────────────────────────
                if (b.isPolygon() && polygonVaos.containsKey(b)) {
                    model.identity();
                    setUniformMat4("uModel", model);
                    setUniformVec3("uColor", r, g, bv);
                    glBindVertexArray(polygonVaos.get(b));
                    glDrawElements(GL_TRIANGLES, polygonIndexCounts.get(b),
                            GL_UNSIGNED_INT, 0L);

                    if (sel) {
                        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                        glLineWidth(3.0f);
                        setUniformVec3("uColor", 1f, 0.9f, 0.1f);
                        glDrawElements(GL_TRIANGLES, polygonIndexCounts.get(b),
                                GL_UNSIGNED_INT, 0L);
                        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
                        glLineWidth(1.0f);
                    }
                    glBindVertexArray(cubeVao);
                } else {
                    model.identity().translate(bx, 0f, bz).scale(bw, bh, bd);
                    setUniformMat4("uModel", model);
                    setUniformVec3("uColor", r, g, bv);
                    glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0L);

                    if (sel) {
                        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                        glLineWidth(3.0f);
                        float e = 0.15f;
                        model.identity()
                                .translate(bx - e, -e, bz - e)
                                .scale(bw + e * 2, bh + e * 2, bd + e * 2);
                        setUniformMat4("uModel", model);
                        setUniformVec3("uColor", 1f, 0.9f, 0.1f);
                        glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0L);
                        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
                        glLineWidth(1.0f);
                    }
                }
            }

            glBindVertexArray(0);
            glUseProgram(0);

            drawMinimap();
            debugPrinted = true;
            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    // ── Ray picking (Phase 7.1) ───────────────────────────────────────

    /**
     * Cast a ray from screen pixel (mx, my) and return the nearest
     * rectangular building intersected, or null if none.
     *
     * Uses the slab method (Amy Williams, 2004) against each building's AABB.
     */
    private BuildingData pickBuilding(double mx, double my, int screenW, int screenH) {
        if (screenW == 0 || screenH == 0) return null;

        // 1. Screen → NDC
        float ndcX = (float)(2.0 * mx / screenW  - 1.0);
        float ndcY = (float)(1.0 - 2.0 * my / screenH);

        // 2. NDC → eye space (undo projection)
        Matrix4f invProj = new Matrix4f(projection).invert();
        Vector4f rayEye  = invProj.transform(new Vector4f(ndcX, ndcY, -1f, 1f));
        rayEye.z = -1f;
        rayEye.w =  0f;  // direction, not position

        // 3. Eye → world space (undo view)
        Matrix4f invView  = new Matrix4f(view).invert();
        Vector4f rayWorld = invView.transform(rayEye);
        Vector3f rayDir   = new Vector3f(rayWorld.x, rayWorld.y, rayWorld.z).normalize();
        Vector3f rayOrig  = new Vector3f(camera.position);

        // 4. Test each building AABB; keep nearest hit
        BuildingData nearest  = null;
        float        nearestT = Float.MAX_VALUE;

        for (BuildingData b : buildings) {
            float bx, bz, bw, bd;
            if (b.isPolygon()) {
                float minX = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
                float maxX = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
                for (int i = 0; i < b.polygonX.length; i++) {
                    minX = Math.min(minX, b.polygonX[i]);
                    minZ = Math.min(minZ, b.polygonZ[i]);
                    maxX = Math.max(maxX, b.polygonX[i]);
                    maxZ = Math.max(maxZ, b.polygonZ[i]);
                }
                bx = minX * SCALE; bz = minZ * SCALE;
                bw = (maxX - minX) * SCALE; bd = (maxZ - minZ) * SCALE;
            } else {
                bx = b.x * SCALE; bz = b.z * SCALE;
                bw = b.width * SCALE; bd = b.depth * SCALE;
            }
            if (bw <= 0 || bd <= 0) continue;
            if (!debugPrinted && b.isPolygon()) {
                System.out.printf("[3D DEBUG] polygon '%s' bbox: bx=%.2f bz=%.2f bw=%.2f bd=%.2f bh=%.2f%n",
                        b.name, bx, bz, bw, bd, Math.max(1, b.floors) * FLOOR_HEIGHT_3D);
            }
            float bh = Math.max(1, b.floors) * FLOOR_HEIGHT_3D;

            float t = rayAABB(rayOrig, rayDir,
                    new Vector3f(bx,      0f, bz),
                    new Vector3f(bx + bw, bh, bz + bd));

            if (t > 0f && t < nearestT) {
                nearestT = t;
                nearest  = b;
            }
        }
        return nearest;
    }

    /**
     * Slab-method ray vs axis-aligned bounding box.
     * Returns the entry distance t along the ray, or -1 if no intersection.
     */
    private float rayAABB(Vector3f orig, Vector3f dir, Vector3f min, Vector3f max) {
        float tmin = Float.NEGATIVE_INFINITY;
        float tmax = Float.POSITIVE_INFINITY;

        // X slab
        if (Math.abs(dir.x) > 1e-6f) {
            float tx1 = (min.x - orig.x) / dir.x;
            float tx2 = (max.x - orig.x) / dir.x;
            tmin = Math.max(tmin, Math.min(tx1, tx2));
            tmax = Math.min(tmax, Math.max(tx1, tx2));
        } else if (orig.x < min.x || orig.x > max.x) return -1f;

        // Y slab
        if (Math.abs(dir.y) > 1e-6f) {
            float ty1 = (min.y - orig.y) / dir.y;
            float ty2 = (max.y - orig.y) / dir.y;
            tmin = Math.max(tmin, Math.min(ty1, ty2));
            tmax = Math.min(tmax, Math.max(ty1, ty2));
        } else if (orig.y < min.y || orig.y > max.y) return -1f;

        // Z slab
        if (Math.abs(dir.z) > 1e-6f) {
            float tz1 = (min.z - orig.z) / dir.z;
            float tz2 = (max.z - orig.z) / dir.z;
            tmin = Math.max(tmin, Math.min(tz1, tz2));
            tmax = Math.min(tmax, Math.max(tz1, tz2));
        } else if (orig.z < min.z || orig.z > max.z) return -1f;

        if (tmax < 0f || tmin > tmax) return -1f;
        return tmin >= 0f ? tmin : tmax;
    }

    // ── Cleanup ───────────────────────────────────────────────────────

    private void cleanup() {
        glDeleteVertexArrays(cubeVao);
        glDeleteVertexArrays(groundVao);
        glDeleteVertexArrays(mmQuadVao);   // ADD
        glDeleteBuffers(groundIbo);
        for (int vao : polygonVaos.values()) glDeleteVertexArrays(vao);
        glDeleteProgram(minimapShader);
        glDeleteProgram(shaderProgram);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
        glDeleteVertexArrays(mmLineVao);
        glDeleteVertexArrays(mmPolyVao);
        glDeleteBuffers(mmLineVbo);
        glDeleteBuffers(mmPolyVbo);
    }

    // ── Cube VAO ──────────────────────────────────────────────────────

    // ── Polygon extrusion VAO ─────────────────────────────────────────
    /**
     * Builds an extruded 3D mesh from a polygon footprint.
     * Vertices are baked in world-scale coordinates.
     * Model matrix should be identity when drawing.
     *
     * Returns int[]{vaoHandle, indexCount}.
     */
    private int[] buildPolygonExtrusionVao(BuildingData b) {
        int   n  = b.polygonX.length;
        float bh = Math.max(1, b.floors) * FLOOR_HEIGHT_3D;

        // 2 rings of n vertices (bottom + top), 3 floats each
        float[] verts = new float[n * 2 * 3];
        for (int i = 0; i < n; i++) {
            float wx = b.polygonX[i] * SCALE;
            float wz = b.polygonZ[i] * SCALE;
            // bottom ring
            verts[i * 3    ] = wx;
            verts[i * 3 + 1] = 0f;
            verts[i * 3 + 2] = wz;
            // top ring
            verts[(n + i) * 3    ] = wx;
            verts[(n + i) * 3 + 1] = bh;
            verts[(n + i) * 3 + 2] = wz;
        }

        // Side walls:  2 triangles × n edges  = 6n indices
        // Top cap:     fan triangulation       = 3(n-2) indices
        // Bottom cap:  fan triangulation (CW)  = 3(n-2) indices
        int[] idx = new int[6 * n + 6 * (n - 2)];
        int ip = 0;

        // Side walls
        for (int i = 0; i < n; i++) {
            int next = (i + 1) % n;
            int b0 = i,     b1 = next;
            int t0 = n + i, t1 = n + next;
            idx[ip++] = b0; idx[ip++] = t0; idx[ip++] = t1;
            idx[ip++] = b0; idx[ip++] = t1; idx[ip++] = b1;
        }

        // Top cap — fan from top vertex 0
        for (int i = 1; i < n - 1; i++) {
            idx[ip++] = n;
            idx[ip++] = n + i;
            idx[ip++] = n + i + 1;
        }

        // Bottom cap — fan from bottom vertex 0, reversed winding
        for (int i = 1; i < n - 1; i++) {
            idx[ip++] = 0;
            idx[ip++] = i + 1;
            idx[ip++] = i;
        }

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
        return new int[]{ vao, ip };
    }

    private int buildCubeVao() {
        float[] verts = {
                0,0,0,  1,0,0,  1,1,0,  0,1,0,
                0,0,1,  1,0,1,  1,1,1,  0,1,1,
        };
        int[] idx = {
                0,2,1,  0,3,2,
                4,5,6,  4,6,7,
                0,4,7,  0,7,3,
                1,2,6,  1,6,5,
                0,1,5,  0,5,4,
                3,7,6,  3,6,2,
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

    // ── Ground plane ──────────────────────────────────────────────────

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

    // ── Uniform helpers ───────────────────────────────────────────────

    private void setUniformMat4(String name, Matrix4f m) {
        int loc = glGetUniformLocation(shaderProgram, name);
        FloatBuffer buf = BufferUtils.createFloatBuffer(16);
        m.get(buf);
        glUniformMatrix4fv(loc, false, buf);
    }

    private void setUniformVec3(String name, float x, float y, float z) {
        glUniform3f(glGetUniformLocation(shaderProgram, name), x, y, z);
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
            vec3 lightDir = normalize(vec3(0.6, 1.0, 0.4));
            float diff    = max(dot(normalize(vNormal), lightDir), 0.0);
            float ambient = 0.35;
            float light   = ambient + diff * 0.65;
            float dist    = length(vWorldPos - vec3(75.0, 0.0, 60.0));
            float fog     = clamp(dist / 600.0, 0.0, 1.0);
            vec3  fogColor = vec3(0.52, 0.74, 0.84);
            vec3  lit     = uColor * light;
            vec3  finalC  = mix(lit, fogColor, fog * fog);
            FragColor = vec4(finalC, 1.0);
        }
        """;
}