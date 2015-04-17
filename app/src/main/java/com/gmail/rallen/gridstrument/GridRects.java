package com.gmail.rallen.gridstrument;

import android.opengl.GLES20;

public class GridRects extends GridDrawables {

     public GridRects(float r, float g, float b, float a) {
        super(r,g,b,a);
    }

    // add V0, V1 for a rectangle upper-left & lower-right verts
    public void add(float x0, float y0, float z0,  // FIXME remove z0,z1
                    float x1, float y1, float z1) {
        dirty = true;
        mCoords.add(x0);
        mCoords.add(y0);
        mCoords.add(z0);

        mCoords.add(x1);
        mCoords.add(y0);
        mCoords.add(z0);

        mCoords.add(x0);
        mCoords.add(y1);
        mCoords.add(z0);

        mCoords.add(x1);
        mCoords.add(y1);
        mCoords.add(z1);
    }

    public void drawElements() {
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLE_STRIP, mIndexBufferSize,
                GLES20.GL_UNSIGNED_SHORT, mIndexBuffer);
    }
}
