package com.gmail.rallen.gridstrument;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

public class GridDrawables {
    protected final String vertexShaderCode =
            "uniform mat4 uVPMatrix;" +
                    "uniform mat4 uMMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    "  gl_Position = uVPMatrix * uMMatrix * vPosition;" +
                    "}";

    protected final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    protected FloatBuffer mVertexBuffer;
    protected ShortBuffer mIndexBuffer;
    protected int mIndexBufferSize;
    protected int mProgram;

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    static final int BYTES_PER_FLOAT = 4;
    static final int BYTES_PER_SHORT = 2;
    static final int VERTEX_STRIDE = COORDS_PER_VERTEX * BYTES_PER_FLOAT;
    protected ArrayList<Float> mCoords = new ArrayList<>();

    protected float mColor[] = new float[4]; // initialize this in constructor.
    protected float mMMatrix[] = new float[16]; // initialize this in constructor.

    protected boolean dirty = false;

    /**
     * Sets up the drawing object data for use in an OpenGL ES context.
     */
    public GridDrawables(float r, float g, float b, float a) {
        mColor[0] = r;
        mColor[1] = g;
        mColor[2] = b;
        mColor[3] = a;
        Matrix.setIdentityM(mMMatrix, 0);
    }

    public void reset() {
        dirty = false;
        mCoords.clear();
    }

    /*public void add() {
        // I do nothing.  Typically you would add coordinates & make it dirty
    }*/

    private void setup() {
        if(!dirty) {
            return;
        }
        dirty = false;

        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(mCoords.size() * BYTES_PER_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        mVertexBuffer = bb.asFloatBuffer();
        for (Float f : mCoords) {
            mVertexBuffer.put(f != null ? f : Float.NaN);
        }
        mVertexBuffer.position(0);

        // initialize byte buffer for the draw list
        mIndexBufferSize = (int)Math.floor(mCoords.size()/COORDS_PER_VERTEX);
        short drawOrder[] = new short[mIndexBufferSize];
        for(short i = 0; i < mIndexBufferSize; i++) {
            drawOrder[i] = i;
        }
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * BYTES_PER_SHORT);
        dlb.order(ByteOrder.nativeOrder());
        mIndexBuffer = dlb.asShortBuffer();
        mIndexBuffer.put(drawOrder);
        mIndexBuffer.position(0);

        // prepare shaders and OpenGL program
        int vertexShader = GridGLRenderer.loadShader(
                GLES20.GL_VERTEX_SHADER,
                vertexShaderCode);
        int fragmentShader = GridGLRenderer.loadShader(
                GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // create OpenGL program executables
        GridGLRenderer.checkGlError("glLinkProgram");
    }

    public void setModelMatrix(float[] mMatrix) {
        mMMatrix = mMatrix;
    }

    public void setColor(float r, float g, float b, float a) {
        mColor[0] = r;
        mColor[1] = g;
        mColor[2] = b;
        mColor[3] = a;
    }

    public void draw(float[] vpMatrix) {
        int positionHandle;
        int colorHandle;
        int vpMatrixHandle;
        int mMatrixHandle;

        if(mCoords.size() == 0) {
            return;
        }
        setup();

        GLES20.glUseProgram(mProgram);
        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(
                positionHandle,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                VERTEX_STRIDE, mVertexBuffer);

        colorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        GridGLRenderer.checkGlError("glGetUniformLocation");
        GLES20.glUniform4fv(colorHandle, 1, mColor, 0);
        GridGLRenderer.checkGlError("glUniform4fv");

        vpMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uVPMatrix");
        GridGLRenderer.checkGlError("glGetUniformLocation");
        GLES20.glUniformMatrix4fv(vpMatrixHandle, 1, false, vpMatrix, 0);
        GridGLRenderer.checkGlError("glUniformMatrix4fv");

        mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMMatrix");
        GridGLRenderer.checkGlError("glGetUniformLocation");
        GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMMatrix, 0);
        GridGLRenderer.checkGlError("glUniformMatrix4fv");

        drawElements(); // override in sub-classes

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    public void drawElements() {
        // I do nothing, add details in the sub-class
    }

}
