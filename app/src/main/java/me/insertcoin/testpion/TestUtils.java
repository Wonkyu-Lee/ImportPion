package me.insertcoin.testpion;

import android.graphics.Color;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import me.insertcoin.lib.pion.gl.GlUtils;

/**
 * Created by blazeq on 16. 4. 12..
 */
public class TestUtils {
    public static float[] convertColor(int color) {
        float[] colorArray = new float[4];
        colorArray[0] = Color.red(color)/255.0f;
        colorArray[1] = Color.green(color)/255.0f;
        colorArray[2] = Color.blue(color)/255.0f;
        colorArray[3] = Color.alpha(color)/255.0f;
        return colorArray;
    }

    public static class Triangle {
        public static final int COORDS_PER_VERTEX = 3;

        private final FloatBuffer mVertexBuffer;

        private static float mTriangleCoords[] = {
                0.0f,  0.622008459f, 0.0f,  // top
                -0.5f, -0.311004243f, 0.0f, // bottom left
                0.5f, -0.311004243f, 0.0f   // bottom right
        };

        public Triangle() {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(mTriangleCoords.length * 4);
            byteBuffer.order(ByteOrder.nativeOrder());
            mVertexBuffer = byteBuffer.asFloatBuffer();
            mVertexBuffer.put(mTriangleCoords);
            mVertexBuffer.position(0);
        }

        public FloatBuffer getVertexBuffer() {
            return mVertexBuffer;
        }
    }

    public static class Square {
        public static final int COORDS_PER_VERTEX = 3;

        private final FloatBuffer mVertexBuffer;
        private final ShortBuffer mDrawListBuffer;

        private static float mSquareCoords[] = {
                -0.5f,  0.5f, 0.0f,   // top left
                -0.5f, -0.5f, 0.0f,   // bottom left
                0.5f, -0.5f, 0.0f,    // bottom right
                0.5f,  0.5f, 0.0f };  // top right

        private short mDrawOrder[] = { 0, 1, 2, 0, 2, 3 };

        public Square() {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(mSquareCoords.length * 4);
            byteBuffer.order(ByteOrder.nativeOrder());
            mVertexBuffer = byteBuffer.asFloatBuffer();
            mVertexBuffer.put(mSquareCoords);
            mVertexBuffer.position(0);

            ByteBuffer dlb = ByteBuffer.allocateDirect(mDrawOrder.length * 2);
            dlb.order(ByteOrder.nativeOrder());
            mDrawListBuffer = dlb.asShortBuffer();
            mDrawListBuffer.put(mDrawOrder);
            mDrawListBuffer.position(0);
        }

        public FloatBuffer getVertexBuffer() {
            return mVertexBuffer;
        }

        public ShortBuffer getDrawListBuffer() {
            return mDrawListBuffer;
        }
    }

    public static class Shader {
        private final int mProgram;

        private static final String vertexShaderCode =
                "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "void main() {" +
                "  gl_Position = uMVPMatrix * vPosition;" +
                "}";

        private static final String fragmentShaderCode =
                "precision mediump float;" +
                "uniform vec4 vColor;" +
                "void main() {" +
                "  gl_FragColor = vColor;" +
                "}";

        public Shader() {
            mProgram = createProgram();
        }

        public void release() {
            GLES20.glDeleteProgram(mProgram);
        }

        public static int createProgram() {
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
            int program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            GLES20.glLinkProgram(program);
            return program;
        }

        private static int loadShader(int type, String shaderCode){
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            return shader;
        }

        public void draw(Triangle triangle, float[] mvpMatrix, int color) {
            int vertexCount = 3;
            int vertexStride = Triangle.COORDS_PER_VERTEX * 4;

            GLES20.glUseProgram(mProgram);

            int mvpMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

            int positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(
                    positionHandle,
                    Triangle.COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT,
                    false,
                    vertexStride,
                    triangle.getVertexBuffer());

            int colorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
            GLES20.glUniform4fv(colorHandle, 1, convertColor(color), 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
            GLES20.glDisableVertexAttribArray(positionHandle);
        }

        public void draw(Square square, float[] mvpMatrix, int color) {
            GlUtils.checkGlError("debug");

            int vertexStride = Square.COORDS_PER_VERTEX * 4;

            GLES20.glUseProgram(mProgram);
            GlUtils.checkGlError("debug");

            int mvpMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
            GlUtils.checkGlError("debug");

            int positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
            GlUtils.checkGlError("debug");

            GLES20.glEnableVertexAttribArray(positionHandle);
            GlUtils.checkGlError("debug");

            GLES20.glVertexAttribPointer(
                    positionHandle,
                    Square.COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT,
                    false,
                    vertexStride,
                    square.getVertexBuffer());
            GlUtils.checkGlError("debug");

            int colorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
            GLES20.glUniform4fv(colorHandle, 1, convertColor(color), 0);
            GlUtils.checkGlError("debug");

            GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, square.getDrawListBuffer());
            GlUtils.checkGlError("debug");

            GLES20.glDisableVertexAttribArray(positionHandle);
            GlUtils.checkGlError("debug");
        }

    }
}
