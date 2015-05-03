package com.gmail.rallen.gridstrument;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * GridGLSurfaceView (really a ViewController)
 */
public class GridGLSurfaceView extends GLSurfaceView {
    private static final int MAX_NOTES = 16;  // maybe 2 players can drive?

    // configuration options
    private int mPitchBendRange       = 12;    // how far to stretch? 1 grid unit?  12?
    private float mBaseNote           = 48.0f; // which note is in lower-left corner
    // C.D.EF.G.A.BC
    // 0123456789012
    private float mStride             = 5.0f;  // 5 matches the LinnStrument default
    private int   mModulationYControl = 1;     // 1=mod wheel, 2=breath control, etc
    private float mMinPressureDomain  = 0.10f; // linear region to map from...
    private float mMaxPressureDomain  = 0.45f;
    private float mMinPressureRange   = 0.2f;  // linear region to map to.
    private float mMaxPressureRange   = 1.0f;
    private float mFingerSizeInches   = 0.6f;

    // rendering vars...
    private final GridGLRenderer mRenderer;
    private GridLines mGridLines;
    private ArrayList<GridRects> mNoteRects;
    private float mXdpi, mYdpi;
    private float mCellWidth, mCellHeight;

    private OSCPortOut mOSCPortOut;
    private GridOSCController mOSC = new GridOSCController();

    private float[][] mNoteColors = { // modulo 12 color keys
        new float[] {0.0f, 0.9f, 0.0f, 1.0f}, // C
        new float[] {0.0f, 0.0f, 0.0f, 1.0f}, // C#
        new float[] {0.0f, 0.9f, 0.9f, 1.0f}, // D
        new float[] {0.0f, 0.0f, 0.0f, 1.0f}, // D#
        new float[] {0.0f, 0.9f, 0.9f, 1.0f}, // E
        new float[] {0.0f, 0.9f, 0.9f, 1.0f}, // F
        new float[] {0.0f, 0.0f, 0.0f, 1.0f}, // F#
        new float[] {0.0f, 0.9f, 0.9f, 1.0f}, // G
        new float[] {0.0f, 0.0f, 0.0f, 1.0f}, // G#
        new float[] {0.0f, 0.9f, 0.9f, 1.0f}, // A
        new float[] {0.0f, 0.0f, 0.0f, 1.0f}, // A#
        new float[] {0.0f, 0.9f, 0.9f, 1.0f}  // B
    };

    private GridFinger[] mFingers = {
            new GridFinger(0), new GridFinger(1), new GridFinger(2), new GridFinger(3),
            new GridFinger(4), new GridFinger(5), new GridFinger(6), new GridFinger(7),
            new GridFinger(8), new GridFinger(9), new GridFinger(10), new GridFinger(11),
            new GridFinger(12), new GridFinger(13), new GridFinger(14), new GridFinger(15)
    };

    static float clamp(float min, float max, float x) {
        return ((x > max) ? max : ((x < min) ? min : x));
    }

    // ======================================================================
    public GridGLSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        mRenderer = new GridGLRenderer();
        setRenderer(mRenderer);
        mXdpi = mYdpi = 200;
        mCellWidth = mCellHeight = 200f;
    }

    public void setDPI(float xdpi, float ydpi) {
        mXdpi = xdpi;
        mYdpi = ydpi;
    }
    public void setPitchBendRange(int n) {
        mPitchBendRange = n;
    }
    public void setBaseNote(int n) {
        mBaseNote = n;
    }

    public void setOSCPortOut(OSCPortOut aOSCPortOut) {
        mOSCPortOut = aOSCPortOut;
    }
    public int xyToNote(float x, float y) {
        return (int)clamp(0f, 127f,
                (float)(mBaseNote + Math.floor(x/mCellWidth) +  mStride*Math.floor(y/mCellHeight)));
    }

    private void resetRenderer() {
        mRenderer.clearItems();
        for(GridRects g: mNoteRects) {
            mRenderer.addItem(g);
        }
        for (int i = 0; i < 16; i++) {
            mRenderer.addItem(mFingers[i].lightRect);
        }
        mRenderer.addItem(mGridLines);
    }
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        mCellWidth = mFingerSizeInches * mXdpi;
        mCellHeight = mFingerSizeInches * mYdpi;
        int numHorizCells = (int) Math.ceil((right - left) / mCellWidth) + 1;
        int numVertCells = (int) Math.ceil((bottom - top) / mCellHeight) + 1;
        mGridLines = new GridLines(0.9f, 0.9f, 0.9f, 1.0f);
        mNoteRects = new ArrayList<GridRects>();
        //mGridLines.reset();
        for (int i = 0; i <= numHorizCells; i++) {
            mGridLines.add((float) i * mCellWidth, 0.0f, 0.0f, (float) i * mCellWidth, (float) numHorizCells * mCellHeight, 0.0f);
            mGridLines.add(0.0f, (float) i * mCellHeight, 0.0f, (float) numHorizCells * mCellWidth, (float) i * mCellHeight, 0.0f);
            for(int j = 0; j <= numVertCells; j++) {
                int note = xyToNote(i*mCellWidth+1,j*mCellHeight+1);
                float[] curColor = mNoteColors[note % 12];
                GridRects curRect = new GridRects(curColor[0],curColor[1],curColor[2],curColor[3]);
                curRect.add(-mCellWidth/4f, mCellHeight/6f, 0f, mCellWidth/6f, -mCellHeight/4f, 0f);
                float[] curMatrix = new float[16];
                Matrix.setIdentityM(curMatrix, 0);
                Matrix.translateM(curMatrix, 0, i * mCellWidth + mCellWidth / 2, j * mCellHeight + mCellHeight / 2, 0.0f);
                curRect.setModelMatrix(curMatrix);
                mNoteRects.add(curRect);
            }
        }

        for (int i = 0; i < 16; i++) {
            mFingers[i].lightRect.reset();
            mFingers[i].lightRect.add(-mCellWidth / 2, mCellHeight / 2, 0.0f, mCellWidth / 2, -mCellHeight / 2, 0.0f);
            Matrix.setIdentityM(mFingers[i].lightMatrix, 0);
            Matrix.translateM(mFingers[i].lightMatrix, 0, -mCellWidth / 2, -mCellHeight / 2, 0.0f); // offscreen
            mFingers[i].lightRect.setModelMatrix(mFingers[i].lightMatrix);
        }

        resetRenderer();
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        int pointerCount = ev.getPointerCount();
        //final long eventTime = ev.getEventTime();

        for (int p = 0; p < pointerCount; p++) {
            int pointerId = ev.getPointerId(p);
            mFingers[pointerId].current.set(ev.getX(p), this.getHeight() - ev.getY(p)); // Y-invert for OpenGL
            mFingers[pointerId].setPressure(ev.getPressure(p));
        }

        final int historySize = ev.getHistorySize();
        if(historySize > 0) {
            Log.e("samp","UNEXPECTED HISTORY!");
        }
        //debugSamples(ev);
        switch (ev.getActionMasked()) {
            // ignoring ACTION_HOVER_*, ACTION_SCROLL
            case MotionEvent.ACTION_POINTER_DOWN:
                mFingers[ev.getPointerId(ev.getActionIndex())].eventDown();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mFingers[ev.getPointerId(ev.getActionIndex())].eventUp();
                break;
            case MotionEvent.ACTION_DOWN:
                mFingers[ev.getPointerId(0)].eventDown();
                this.requestUnbufferedDispatch(ev); // move events will not be buffered (Android L & later)
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mFingers[ev.getPointerId(0)].eventUp();
                break;
            case MotionEvent.ACTION_MOVE:
                for (int p = 0; p < pointerCount; p++) {
                    mFingers[ev.getPointerId(p)].eventMove();
                }
                break;
            case MotionEvent.ACTION_OUTSIDE:
                Log.e("onTouch", "Outside? what to do?");
                break;
        }
        return true;
    }

    // ======================================================================
    private class OSCSendMessageTask extends AsyncTask<Object, Void, Boolean> {
        private String mAddress;
        OSCSendMessageTask(String address) {
            mAddress = address;
        }
        protected Boolean doInBackground(Object... objs) {
            //Log.d("dib","len="+objs.length+" "+objs);
            try {
                OSCMessage message = new OSCMessage(mAddress, Arrays.asList(objs));
                mOSCPortOut.send(message);
            } catch (Exception e) {
                Log.e("OSCSendMessageTask","Unknown exception "+e);
            }
            return true;
        }
    }

    // ======================================================================
    private class GridOSCController {
        private int lastPressure = -1, lastModulationX = -1, lastModulationY = -1;
        public void sendPressure(int channel, float p) {
            int pi = (int)clamp(0f, 127f, p);
            if(pi != lastPressure) {
                //Log.d("sendPressure", String.format("/vkb_midi/%d/channelpressure=%d", channel, pi));
                new OSCSendMessageTask(String.format("/vkb_midi/%d/channelpressure",channel)).execute(pi);
                lastPressure = pi;
            }
        }
        public void sendModulationX(int channel, float mx) {
            int mxi = (int)clamp(0f,1f*0x3fff,0x2000 + (0x2000*(mx/(mPitchBendRange*mCellWidth))));
            if(mxi != lastModulationX) {
                //Log.d("sendModulationX", String.format("/vkb_midi/%d/pitch=%d", channel, mxi));
                new OSCSendMessageTask(String.format("/vkb_midi/%d/pitch",channel)).execute(mxi);
                lastModulationX = mxi;
            }
        }
        public void sendModulationY(int channel, float my) {
            int myi = (int)clamp(0f, 127f, 127f*(Math.abs(my)/mCellHeight));
            if(myi != lastModulationY) {
                //Log.d("sendModulationY",String.format("/vkb_midi/%d/cc/%d=%d",channel,mModulationYControl,myi));
                new OSCSendMessageTask(String.format("/vkb_midi/%d/cc/%d",channel,mModulationYControl)).execute(myi);
                lastModulationY = myi;
            }
        }
        public void sendNote(int channel, int note, float velocity) {
            int vi = (int)clamp(0f, 127f, (float)Math.floor(velocity*127 + 0.5f));
            //Log.d("sendNote",String.format("/vkb_midi/%d/note/%d=%d",channel,note,vi));
            new OSCSendMessageTask(String.format("/vkb_midi/%d/note/%d",channel,note)).execute(vi);
        }

    }

    // ======================================================================
    private class GridFinger {
        public int       channel      = 0;
        public PointF    touch        = new PointF();
        public PointF    current      = new PointF();
        public float     pressure     = 0.0f;
        public float[]   lightMatrix  = new float[16];
        public GridRects lightRect    = new GridRects(0.5f, 0.5f, 0.5f, 1.0f);
        public boolean   active       = false;
        public int       note         = 0;

        GridFinger(int c) {
            channel = c;
        }
        public float getModulationX() {
            return current.x - touch.x;
        }
        public float getModulationY() {
            return current.y - touch.y;
        }
        public void setPressure(float p) {
            // scale (minDomain,maxDomain) value to (minRange,maxRange)
            pressure = clamp(mMinPressureRange, mMaxPressureRange,
                    (p - mMinPressureDomain) / (mMaxPressureDomain-mMinPressureDomain));
        }
        private void eventDown() {
            touch.set(current);
            note = xyToNote(touch.x, touch.y);
            if(active) {
                Log.e("eventDown",String.format("MISSED NOTE OFF on Channel %d", channel));
            }
            active = true;
            // light rect
            float x = (float)Math.floor(touch.x/mCellWidth)*mCellWidth + mCellWidth/2;
            float y = (float)Math.floor(touch.y/mCellHeight)*mCellHeight + mCellHeight / 2;
            Matrix.setIdentityM(lightMatrix, 0);
            Matrix.translateM(lightMatrix, 0, x, y, 0.0f);
            lightRect.setModelMatrix(lightMatrix);
            lightRect.setColor(pressure, pressure, pressure, 1.0f);

            mOSC.sendPressure(channel, 0f);
            mOSC.sendModulationX(channel, 0f);
            mOSC.sendModulationY(channel, 0f);
            mOSC.sendNote(channel, note, pressure);
        }
        private void eventUp() {
            if(!active) {
                Log.e("eventUp",String.format("MISSED NOTE ON on Channel %d", channel));
            }
            active = false;
            // light rect offscreen
            Matrix.setIdentityM(lightMatrix, 0);
            Matrix.translateM(lightMatrix, 0, -mCellWidth / 2, -mCellHeight / 2, 0.0f);
            lightRect.setModelMatrix(lightMatrix);

            mOSC.sendNote(channel, note, 0.0f);
        }
        private void eventMove() {
            if(!active) {
                if(getModulationX() + getModulationY() != 0) {
                    Log.e("eventMove", String.format("MISSING NOTE ON on Channel %d", channel));
                }
            }
            lightRect.setColor(pressure, pressure, pressure, 1.0f);

            mOSC.sendModulationX(channel, getModulationX());
            mOSC.sendModulationY(channel, getModulationY());
        }
    }

    /* for debug
    private void debugSamples(MotionEvent ev) {
        int pointerActionIndex = -1;
        String action = "?";
        boolean unhandledAction = false;
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_CANCEL:
                action = "CANCEL";
                break;
            case MotionEvent.ACTION_DOWN:
                action = "DOWN";
                break;
            case MotionEvent.ACTION_HOVER_ENTER:
                action = "HOVER_ENTER";
                unhandledAction = true;
                break;
            case MotionEvent.ACTION_HOVER_EXIT:
                action = "HOVER_EXIT";
                unhandledAction = true;
                break;
            case MotionEvent.ACTION_HOVER_MOVE:
                action = "HOVER_MOVE";
                unhandledAction = true;
                break;
            case MotionEvent.ACTION_MOVE:
                action = "MOVE";
                break;
            case MotionEvent.ACTION_OUTSIDE:
                action = "OUTSIDE";
                unhandledAction = true;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                action = "POINTER_DOWN";
                pointerActionIndex = ev.getActionIndex();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                action = "POINTER_UP";
                pointerActionIndex = ev.getActionIndex();
                break;
            case MotionEvent.ACTION_SCROLL:
                action = "SCROLL";
                unhandledAction = true;
                break;
            case MotionEvent.ACTION_UP:
                action = "UP";
                break;
        }
        if(unhandledAction) {
            Log.e("samp", String.format("UNHANDLED %s",action));
        }
        final int historySize = ev.getHistorySize();
        if(historySize > 0) {
            Log.e("samp","UNEXPECTED HISTORY!");
        }
        final int pointerCount = ev.getPointerCount();
        Log.d("samp",String.format("act %s %d %d %d", action, pointerActionIndex, pointerCount, historySize));
        for (int h = 0; h < historySize; h++) {
            final long historicalTime = ev.getHistoricalEventTime(h);
            for (int p = 0; p < pointerCount; p++) {
                final int pointerId = ev.getPointerId(p);
                final float pointerX = ev.getHistoricalX(p, h);
                final float pointerY = ev.getHistoricalY(p, h);
                Log.d("samp",String.format("%08d: hst (%d,%d) %d (%.1f,%.1f)",historicalTime, h, p,
                        pointerId, pointerX, pointerY));
            }
        }
        final long eventTime = ev.getEventTime();
        for (int p = 0; p < pointerCount; p++) {
            final int pointerId = ev.getPointerId(p);
            final float pointerX = ev.getX(p);
            final float pointerY = ev.getY(p);
            Log.d("samp",String.format("%08d: ptr (%d) %d (%.1f,%.1f)",eventTime, p,
                    pointerId, pointerX, pointerY));
        }
    }
    */
}
