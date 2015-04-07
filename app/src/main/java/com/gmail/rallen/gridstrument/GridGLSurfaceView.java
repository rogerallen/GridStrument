package com.gmail.rallen.gridstrument;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;

import de.humatic.nmj.NetworkMidiOutput;

/**
 * GridGLSurfaceView
 */
public class GridGLSurfaceView extends GLSurfaceView {
    static float dN = 200f;

    private float mTouchX, mTouchY, mCurX, mCurY, mCurPressure;
    private float[] mTouchMatrix = new float[16];
    private float[] mCurMatrix = new float[16];
    private float[] mPressMatrix = new float[16];

    private final GridGLRenderer mRenderer;
    private final GridLines gridLines, touchLines, curLines, pressLines;

    private NetworkMidiOutput mMidiOut = null;
    private byte[] mNoteOn    = new byte[]{(byte)0x90, (byte)0x44, (byte)100};
    private byte[] mNoteOff   = new byte[]{(byte)0x80, (byte)0x44, (byte)0};
    private byte[] mPitchBend = new byte[]{(byte)0xe0, (byte)0x00, (byte)0x40};
    private byte[] mModWheel  = new byte[]{(byte)0xb0, (byte)0x01, (byte)0x00};

    public GridGLSurfaceView(Context context){
        super(context);
        setEGLContextClientVersion(2);
        mRenderer = new GridGLRenderer();
        setRenderer(mRenderer);
        gridLines  = new GridLines(0.9f, 0.9f, 0.5f, 1.0f);
        touchLines = new GridLines(0.9f, 0.5f, 0.5f, 1.0f);
        curLines   = new GridLines(0.5f, 0.9f, 0.5f, 1.0f);
        pressLines = new GridLines(0.9f, 0.9f, 0.9f, 1.0f);
        mRenderer.addItem(gridLines);
        mRenderer.addItem(touchLines);
        mRenderer.addItem(curLines);
        mRenderer.addItem(pressLines);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        float width = right-left;
        float height = top-bottom;
        int N = 4;
        gridLines.reset();
        for(int i = 0; i <= N; i++) {
            gridLines.add((float)i * dN, 0.0f, 0.0f,   (float)i * dN, (float)N*dN, 0.0f);
            gridLines.add(0.0f, (float)i * dN, 0.0f,   (float)N*dN, (float)i * dN, 0.0f);
        }

        touchLines.reset();
        touchLines.add(-width, 0.0f, 0.0f,    width, 0.0f, 0.0f);
        touchLines.add(0.0f, -height, 0.0f,   0.0f, height, 0.0f);

        curLines.reset();
        curLines.add(-width, 0.0f, 0.0f,    width, 0.0f, 0.0f);
        curLines.add(0.0f, -height, 0.0f,   0.0f, height, 0.0f);

        pressLines.reset();
        pressLines.add(-dN, 0.0f, 0.0f,    0.0f, 0.0f, 0.0f);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mCurX = ev.getX();
        mCurY = this.getHeight() - ev.getY(); // Y-invert for OpenGL
        Matrix.setIdentityM(mCurMatrix,0);
        Matrix.translateM(mCurMatrix, 0, mCurX, mCurY, 0.0f);
        curLines.setModelMatrix(mCurMatrix);

        mCurPressure = ev.getPressure();
        Matrix.setIdentityM(mPressMatrix,0);
        Matrix.translateM(mPressMatrix, 0, mCurPressure*dN, dN/2, 0.0f);
        pressLines.setModelMatrix(mPressMatrix);

        //final int nHistory = ev.getHistorySize();
        //final int nPointers = ev.getPointerCount();

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d("onTouch", String.format("down x: %.2f y: %.2f p: %.2f nHist: %d, nPtr: %d", mCurX, mCurY, mCurPressure, 0, 0));//nHistory, nPointers));
                mTouchX = mCurX;
                mTouchY = mCurY;
                Matrix.setIdentityM(mTouchMatrix,0);
                Matrix.translateM(mTouchMatrix, 0, mTouchX, mTouchY, 0.0f);
                touchLines.setModelMatrix(mTouchMatrix);
                this.requestUnbufferedDispatch(ev); // move events will not be buffered (Android L & later)
                updateNote();
                try{
                    mMidiOut.sendMidiOnThread(mNoteOn);
                } catch (Exception ex){
                    ex.printStackTrace();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                //Log.d("onTouch", String.format("..move x: %.2f y: %.2f p: %.2f", mCurX-mTouchX, mCurY-mTouchY, mCurPressure));
                float dx = mCurX-mTouchX;
                float dy = mCurY-mTouchY;
                updatePitchBend(dx);
                updateModWheel(dy);
                try {
                    mMidiOut.sendMidiOnThread(mPitchBend);
                    mMidiOut.sendMidiOnThread(mModWheel);
                } catch (Exception ex){
                    ex.printStackTrace();
                }
                break;
            case MotionEvent.ACTION_UP:
                //Log.d("onTouch", String.format("..up   x: %.2f y: %.2f p: %.2f nHist: %d, nPtr: %d", x-mTouchX, y-mTouchY, p, nHistory, nPointers));
                mCurPressure = 0.0f;
                Matrix.setIdentityM(mPressMatrix,0);
                Matrix.translateM(mPressMatrix, 0, mCurPressure*dN, dN/2, 0.0f);
                pressLines.setModelMatrix(mPressMatrix);
                updatePitchBend(0);
                updateModWheel(0);
                try {
                    mMidiOut.sendMidiOnThread(mPitchBend);
                    mMidiOut.sendMidiOnThread(mModWheel);
                    mMidiOut.sendMidiOnThread(mNoteOff);
                } catch (Exception ex){
                    ex.printStackTrace();
                }
                break;
        }
        return true;
    }

    private void updateNote() {
        byte curNote = (byte)(40.0 + Math.floor(mTouchX/dN) + 5*Math.floor(mTouchY/dN));
        mNoteOn[1] = curNote;
        mNoteOff[1] = curNote;
    }
    private void updatePitchBend(float delta) {
        delta = 0x2000 + 0x1000*(delta/dN); // normalize
        delta = (delta > 0x3fff) ? 0x3ffff : ((delta < 0) ? 0 : delta);
        int d = (int)delta;
        mPitchBend[1] = (byte)(d & 0x7f);
        mPitchBend[2] = (byte)((d>>7) & 0x7f);
    }
    private void updateModWheel(float delta) {
        delta = 0x7f*(delta/dN); // normalize
        delta = (delta > 0x7f) ? 0x7f : ((delta < 0) ? 0 : delta);
        int d = (int)delta;
        mModWheel[2] = (byte)(d & 0x7f);
    }

    public void setMidiOutput(NetworkMidiOutput midiOut) {
         mMidiOut = midiOut;
    }

}
