package com.campusgame.renderer.lwjgl3d;

import static org.lwjgl.opengl.GL20.*;

/**
 * ShaderUtil
 * ──────────────────────────────────────────────────────────────────────────
 * Minimal helper that compiles + links a GLSL shader program from source
 * strings.  Throws RuntimeException with the info log on any error.
 *
 * Usage:
 *   int prog = ShaderUtil.buildProgram(vertSrc, fragSrc);
 *   glUseProgram(prog);
 *   // ...
 *   glDeleteProgram(prog);
 */
public final class ShaderUtil {

    private ShaderUtil() {}

    /**
     * Compile and link vertex + fragment shader sources into a GL program.
     *
     * @param vertSrc GLSL 330 core vertex shader source
     * @param fragSrc GLSL 330 core fragment shader source
     * @return GL program handle
     */
    public static int buildProgram(String vertSrc, String fragSrc) {
        int vert = compile(GL_VERTEX_SHADER,   vertSrc);
        int frag = compile(GL_FRAGMENT_SHADER, fragSrc);

        int prog = glCreateProgram();
        glAttachShader(prog, vert);
        glAttachShader(prog, frag);
        glLinkProgram(prog);

        if (glGetProgrami(prog, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(prog);
            glDeleteProgram(prog);
            throw new RuntimeException("Shader link failed:\n" + log);
        }

        glDeleteShader(vert);
        glDeleteShader(frag);
        return prog;
    }

    private static int compile(int type, String src) {
        int id = glCreateShader(type);
        glShaderSource(id, src);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(id);
            glDeleteShader(id);
            String typeName = (type == GL_VERTEX_SHADER) ? "vertex" : "fragment";
            throw new RuntimeException(typeName + " shader compile failed:\n" + log);
        }
        return id;
    }
}