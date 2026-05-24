package com.campusgame.renderer.api;

import com.campusgame.core.GameWorld;
import com.campusgame.entities.Player;
import com.campusgame.map.data.BuildingData;
import com.campusgame.map.data.MapData;
import com.campusgame.renderer.Camera;

/**
 * LWJGL 3D RENDERER — STUB (renderer/api/LwjglRenderer3D.java)
 * --------------------------------------------------------------
 * Phase 3 renderer stub. Implement this for Roblox-style 3D view.
 *
 * HOW THE 3D CAMPUS WILL WORK:
 *
 *   Each BuildingData becomes a 3D cuboid (box mesh):
 *     - Position: (b.x, b.y, b.z) in world space
 *     - Size:     (b.width, b.height, b.depth)
 *     - Height:   b.floors * BuildingData.METERS_PER_FLOOR
 *
 *   The camera becomes a perspective 3D camera:
 *     - FOV ~60°, near=0.1, far=5000
 *     - Can be first-person or third-person (follow cam)
 *
 *   Rendering pipeline:
 *     1. Build VAO/VBO for each building box (done once at init)
 *     2. Each frame: clear, set view/projection matrix, draw all boxes
 *     3. Apply simple directional lighting (diffuse + ambient)
 *     4. Draw player capsule mesh
 *     5. 2D HUD pass on top (minimap, labels — reuse Camera math)
 *
 * SETUP STEPS (when ready for Phase 3):
 *   1. Add LWJGL to pom.xml:
 *        org.lwjgl:lwjgl:3.3.3
 *        org.lwjgl:lwjgl-opengl:3.3.3
 *        org.lwjgl:lwjgl-glfw:3.3.3
 *        (+ natives for your OS)
 *   2. Implement the TODOs below
 *   3. In GameLoop, change:
 *        IRenderer activeRenderer = new SwingRenderer2D(...)
 *      to:
 *        IRenderer activeRenderer = new LwjglRenderer3D(...)
 *
 * Everything else — Player, GameWorld, MapData, CollisionManager — unchanged.
 *
 * VERTEX SHADER (assets/shaders/campus.vert) — future file:
 *   #version 330 core
 *   layout(location=0) in vec3 aPos;
 *   uniform mat4 mvp;
 *   void main() { gl_Position = mvp * vec4(aPos, 1.0); }
 *
 * FRAGMENT SHADER (assets/shaders/campus.frag) — future file:
 *   #version 330 core
 *   uniform vec4 uColor;
 *   out vec4 fragColor;
 *   void main() { fragColor = uColor; }
 */
public class LwjglRenderer3D implements IRenderer {

    private int screenWidth;
    private int screenHeight;

    // TODO Phase 3: store GLFW window handle, shader program IDs, VAO/VBO IDs

    public LwjglRenderer3D(int screenWidth, int screenHeight) {
        this.screenWidth  = screenWidth;
        this.screenHeight = screenHeight;
    }

    @Override
    public void init() {
        // TODO Phase 3:
        //   glfwInit()
        //   Create GLFW window (replace JFrame entirely)
        //   glfwMakeContextCurrent(window)
        //   GL.createCapabilities()
        //   glEnable(GL_DEPTH_TEST)
        //   Compile vertex + fragment shaders
        //   Build VAO/VBO for each BuildingData box in MapData.BUILDINGS
        //   Build player capsule VAO/VBO

        System.out.println("[LwjglRenderer3D] STUB — not yet implemented.");
        System.out.println("  → Switch back to SwingRenderer2D for Phase 1.");
    }

    @Override
    public void render(GameWorld world, Player player, Camera camera) {
        // TODO Phase 3:
        //   glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
        //
        //   // Set up 3D perspective camera
        //   Matrix4f proj = new Matrix4f().perspective(FOV, aspect, 0.1f, 5000f)
        //   Matrix4f view = new Matrix4f().lookAt(camPos, camTarget, UP)
        //
        //   // Draw buildings
        //   for (BuildingData b : MapData.BUILDINGS) {
        //       Matrix4f model = new Matrix4f().translate(b.x, b.y, b.z)
        //                                      .scale(b.width, b.height, b.depth)
        //       glUniformMatrix4fv(mvpLoc, false, (proj * view * model).toBuffer())
        //       glUniform4f(colorLoc, b.red()/255f, b.green()/255f, b.blue()/255f, 1f)
        //       glBindVertexArray(cubeVAO)
        //       glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0)
        //   }
        //
        //   // Draw player capsule at (player.x, player.y, player.z)
        //   // Draw 2D HUD pass (minimap etc.)
        //   glfwSwapBuffers(window)
    }

    @Override
    public void onResize(int width, int height) {
        this.screenWidth  = width;
        this.screenHeight = height;
        // TODO: glViewport(0, 0, width, height); update projection matrix
    }

    @Override
    public void dispose() {
        // TODO: glDeleteVertexArrays, glDeleteBuffers, glDeleteProgram
        //       glfwDestroyWindow, glfwTerminate
    }

    @Override
    public String getBackendName() {
        return "LWJGL OpenGL 3D (Phase 3 — STUB)";
    }

    // ---------------------------------------------------------------
    // 3D MATH HELPERS — ready for Phase 3
    // ---------------------------------------------------------------

    /**
     * Builds the 24 vertices (4 per face × 6 faces) of a unit cube.
     * Scale with a model matrix in the shader to match BuildingData dimensions.
     * Call once in init() and store as a VAO.
     */
    public static float[] buildUnitCubeVertices() {
        return new float[] {
                // positions          // normals (for lighting)
                // Front face
                0,0,1,  0,0,1,   1,0,1,  0,0,1,   1,1,1,  0,0,1,   0,1,1,  0,0,1,
                // Back face
                1,0,0,  0,0,-1,  0,0,0,  0,0,-1,  0,1,0,  0,0,-1,  1,1,0,  0,0,-1,
                // Left face
                0,0,0,  -1,0,0,  0,0,1,  -1,0,0,  0,1,1,  -1,0,0,  0,1,0,  -1,0,0,
                // Right face
                1,0,1,  1,0,0,   1,0,0,  1,0,0,   1,1,0,  1,0,0,   1,1,1,  1,0,0,
                // Top face
                0,1,1,  0,1,0,   1,1,1,  0,1,0,   1,1,0,  0,1,0,   0,1,0,  0,1,0,
                // Bottom face
                0,0,0,  0,-1,0,  1,0,0,  0,-1,0,  1,0,1,  0,-1,0,  0,0,1,  0,-1,0,
        };
    }

    /** Index buffer for a unit cube (6 faces × 2 triangles × 3 indices = 36). */
    public static int[] buildUnitCubeIndices() {
        int[] idx = new int[36];
        for (int face = 0; face < 6; face++) {
            int base = face * 4;
            int i    = face * 6;
            idx[i]   = base;     idx[i+1] = base+1; idx[i+2] = base+2;
            idx[i+3] = base+2;   idx[i+4] = base+3; idx[i+5] = base;
        }
        return idx;
    }
}