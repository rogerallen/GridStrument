package com.gmail.rallen.gridstrument;

// TODO: single event with multiple fingers down may not send multiple notes ???

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;

import de.humatic.nmj.NetworkMidiOutput;

/**
 * GridGLSurfaceView
 */
public class GridGLSurfaceView extends GLSurfaceView {
    private static final int MAX_NOTES = 16;
    private static final float FINGER_SIZE_INCHES = 0.6f;

    // FIXME as GUI settings
    private int mRange = 12;  // how far to stretch? 1 grid unit?  12?
    float mBaseNote = 48.0f;  // which note is in lower-left corner
    // C.D.EF.G.A.BC
    // 0123456789012
    float mStride = 7.0f;     // moving up one column is this many notes
    float mMinPressureDomain = 0.10f;
    float mMaxPressureDomain = 0.45f;
    float mMinPressureRange  = 0.2f;
    float mMaxPressureRange  = 1.0f;

    private final GridGLRenderer mRenderer;
    private GridLines gridLines, touchLines, curLines, pressLines;
    private GridRects[] mLightRects = {
            new GridRects(0.5f, 0.5f, 0.5f, 1.0f),
            new GridRects(0.5f, 0.5f, 0.5f, 1.0f),
            new GridRects(0.5f, 0.5f, 0.5f, 1.0f),
            new GridRects(0.5f, 0.5f, 0.5f, 1.0f),
            new GridRects(0.5f, 0.5f, 0.5f, 1.0f),
            new GridRects(0.5f, 0.5f, 0.5f, 1.0f),
            new GridRects(0.5f, 0.5f, 0.5f, 1.0f),
            new GridRects(0.5f, 0.5f, 0.5f, 1.0f),
            new GridRects(0.5f, 0.5f, 0.5f, 1.0f),
            new GridRects(0.5f, 0.5f, 0.5f, 1.0f),
            new GridRects(0.5f, 0.5f, 0.5f, 1.0f),
            new GridRects(0.5f, 0.5f, 0.5f, 1.0f),
            new GridRects(0.5f, 0.5f, 0.5f, 1.0f),
            new GridRects(0.5f, 0.5f, 0.5f, 1.0f),
            new GridRects(0.5f, 0.5f, 0.5f, 1.0f),
            new GridRects(0.5f, 0.5f, 0.5f, 1.0f)
    };
    private NetworkMidiOutput mMidiOut = null;

    private float mXdpi, mYdpi;

    private float mCellWidth, mCellHeight = 200f;
    private float[] mTouchXs = new float[MAX_NOTES];
    private float[] mTouchYs = new float[MAX_NOTES];
    private float[] mCurXs = new float[MAX_NOTES];
    private float[] mCurYs = new float[MAX_NOTES];
    private float[] mCurPressures = new float[MAX_NOTES];

    private float[] mTouchMatrix = new float[16];
    private float[] mCurMatrix = new float[16];
    private float[] mPressMatrix = new float[16];
    private float[][] mLightMatrices = {
            new float[16], new float[16], new float[16], new float[16],
            new float[16], new float[16], new float[16], new float[16],
            new float[16], new float[16], new float[16], new float[16],
            new float[16], new float[16], new float[16], new float[16]
    };

    private byte[][] mNoteOns = { // 0x90 Note On
            new byte[]{(byte) 0x90, (byte) 0x3c, (byte) 100},
            new byte[]{(byte) 0x91, (byte) 0x3c, (byte) 100},
            new byte[]{(byte) 0x92, (byte) 0x3c, (byte) 100},
            new byte[]{(byte) 0x93, (byte) 0x3c, (byte) 100},
            new byte[]{(byte) 0x94, (byte) 0x3c, (byte) 100},
            new byte[]{(byte) 0x95, (byte) 0x3c, (byte) 100},
            new byte[]{(byte) 0x96, (byte) 0x3c, (byte) 100},
            new byte[]{(byte) 0x97, (byte) 0x3c, (byte) 100},
            new byte[]{(byte) 0x98, (byte) 0x3c, (byte) 100},
            new byte[]{(byte) 0x99, (byte) 0x3c, (byte) 100},
            new byte[]{(byte) 0x9a, (byte) 0x3c, (byte) 100},
            new byte[]{(byte) 0x9b, (byte) 0x3c, (byte) 100},
            new byte[]{(byte) 0x9c, (byte) 0x3c, (byte) 100},
            new byte[]{(byte) 0x9d, (byte) 0x3c, (byte) 100},
            new byte[]{(byte) 0x9e, (byte) 0x3c, (byte) 100},
            new byte[]{(byte) 0x9f, (byte) 0x3c, (byte) 100}
    };
    // Aha!  Sending multiple midiOut packets back-to-back results in dropped packets.
    // So, packing multiple packets into one array will be necessary.
    // Note off will also reset the pitch-bend & mod wheel.
    private byte[][] mNoteOffs = { // 0x80 Note Off (0x90 + velocity=0 also works)
            new byte[]{(byte) 0xb0, (byte) 0x01, (byte) 0x00, (byte) 0xe0, (byte) 0x00, (byte) 0x40, (byte) 0x90, (byte) 0x3c, (byte) 0},
            new byte[]{(byte) 0xb1, (byte) 0x01, (byte) 0x00, (byte) 0xe1, (byte) 0x00, (byte) 0x40, (byte) 0x91, (byte) 0x3c, (byte) 0},
            new byte[]{(byte) 0xb2, (byte) 0x01, (byte) 0x00, (byte) 0xe2, (byte) 0x00, (byte) 0x40, (byte) 0x92, (byte) 0x3c, (byte) 0},
            new byte[]{(byte) 0xb3, (byte) 0x01, (byte) 0x00, (byte) 0xe3, (byte) 0x00, (byte) 0x40, (byte) 0x93, (byte) 0x3c, (byte) 0},
            new byte[]{(byte) 0xb4, (byte) 0x01, (byte) 0x00, (byte) 0xe4, (byte) 0x00, (byte) 0x40, (byte) 0x94, (byte) 0x3c, (byte) 0},
            new byte[]{(byte) 0xb5, (byte) 0x01, (byte) 0x00, (byte) 0xe5, (byte) 0x00, (byte) 0x40, (byte) 0x95, (byte) 0x3c, (byte) 0},
            new byte[]{(byte) 0xb6, (byte) 0x01, (byte) 0x00, (byte) 0xe6, (byte) 0x00, (byte) 0x40, (byte) 0x96, (byte) 0x3c, (byte) 0},
            new byte[]{(byte) 0xb7, (byte) 0x01, (byte) 0x00, (byte) 0xe7, (byte) 0x00, (byte) 0x40, (byte) 0x97, (byte) 0x3c, (byte) 0},
            new byte[]{(byte) 0xb8, (byte) 0x01, (byte) 0x00, (byte) 0xe8, (byte) 0x00, (byte) 0x40, (byte) 0x98, (byte) 0x3c, (byte) 0},
            new byte[]{(byte) 0xb9, (byte) 0x01, (byte) 0x00, (byte) 0xe9, (byte) 0x00, (byte) 0x40, (byte) 0x99, (byte) 0x3c, (byte) 0},
            new byte[]{(byte) 0xba, (byte) 0x01, (byte) 0x00, (byte) 0xea, (byte) 0x00, (byte) 0x40, (byte) 0x9a, (byte) 0x3c, (byte) 0},
            new byte[]{(byte) 0xbb, (byte) 0x01, (byte) 0x00, (byte) 0xeb, (byte) 0x00, (byte) 0x40, (byte) 0x9b, (byte) 0x3c, (byte) 0},
            new byte[]{(byte) 0xbc, (byte) 0x01, (byte) 0x00, (byte) 0xec, (byte) 0x00, (byte) 0x40, (byte) 0x9c, (byte) 0x3c, (byte) 0},
            new byte[]{(byte) 0xbd, (byte) 0x01, (byte) 0x00, (byte) 0xed, (byte) 0x00, (byte) 0x40, (byte) 0x9d, (byte) 0x3c, (byte) 0},
            new byte[]{(byte) 0xbe, (byte) 0x01, (byte) 0x00, (byte) 0xee, (byte) 0x00, (byte) 0x40, (byte) 0x9e, (byte) 0x3c, (byte) 0},
            new byte[]{(byte) 0xbf, (byte) 0x01, (byte) 0x00, (byte) 0xef, (byte) 0x00, (byte) 0x40, (byte) 0x9f, (byte) 0x3c, (byte) 0}
    };
    // 0xE0 Pitch bend
    // [1] = 1 // mod wheel (coarse), 33 mod wheel (fine)
    // [1] = 2 // breath control (coarse), 34 bc (fine)
    private byte[][] mModulates = {
            new byte[]{(byte) 0xb0, (byte) 0x01, (byte) 0x00, (byte) 0xe0, (byte) 0x00, (byte) 0x40},
            new byte[]{(byte) 0xb0, (byte) 0x01, (byte) 0x00, (byte) 0xe1, (byte) 0x00, (byte) 0x40},
            new byte[]{(byte) 0xb0, (byte) 0x01, (byte) 0x00, (byte) 0xe2, (byte) 0x00, (byte) 0x40},
            new byte[]{(byte) 0xb0, (byte) 0x01, (byte) 0x00, (byte) 0xe3, (byte) 0x00, (byte) 0x40},
            new byte[]{(byte) 0xb0, (byte) 0x01, (byte) 0x00, (byte) 0xe4, (byte) 0x00, (byte) 0x40},
            new byte[]{(byte) 0xb0, (byte) 0x01, (byte) 0x00, (byte) 0xe5, (byte) 0x00, (byte) 0x40},
            new byte[]{(byte) 0xb0, (byte) 0x01, (byte) 0x00, (byte) 0xe6, (byte) 0x00, (byte) 0x40},
            new byte[]{(byte) 0xb0, (byte) 0x01, (byte) 0x00, (byte) 0xe7, (byte) 0x00, (byte) 0x40},
            new byte[]{(byte) 0xb0, (byte) 0x01, (byte) 0x00, (byte) 0xe8, (byte) 0x00, (byte) 0x40},
            new byte[]{(byte) 0xb0, (byte) 0x01, (byte) 0x00, (byte) 0xe9, (byte) 0x00, (byte) 0x40},
            new byte[]{(byte) 0xb0, (byte) 0x01, (byte) 0x00, (byte) 0xea, (byte) 0x00, (byte) 0x40},
            new byte[]{(byte) 0xb0, (byte) 0x01, (byte) 0x00, (byte) 0xeb, (byte) 0x00, (byte) 0x40},
            new byte[]{(byte) 0xb0, (byte) 0x01, (byte) 0x00, (byte) 0xec, (byte) 0x00, (byte) 0x40},
            new byte[]{(byte) 0xb0, (byte) 0x01, (byte) 0x00, (byte) 0xed, (byte) 0x00, (byte) 0x40},
            new byte[]{(byte) 0xb0, (byte) 0x01, (byte) 0x00, (byte) 0xee, (byte) 0x00, (byte) 0x40},
            new byte[]{(byte) 0xb0, (byte) 0x01, (byte) 0x00, (byte) 0xef, (byte) 0x00, (byte) 0x40}
    };
    // Other potential controls
    // 0xA0     Aftertouch + 1 byte for note + 1 byte for value
    // 0xD0     Channel Pressure + 1 byte for value (not per-note)
    private boolean[] mActiveNotes = new boolean[]{
            false, false, false, false,
            false, false, false, false,
            false, false, false, false,
            false, false, false, false
    };

    public GridGLSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        mRenderer = new GridGLRenderer();
        setRenderer(mRenderer);
        gridLines = new GridLines(0.9f, 0.9f, 0.9f, 1.0f);
        touchLines = new GridLines(0.9f, 0.5f, 0.5f, 1.0f);
        curLines = new GridLines(0.5f, 0.9f, 0.5f, 1.0f);
        pressLines = new GridLines(0.9f, 0.9f, 0.9f, 1.0f);
        for (int i = 0; i < 16; i++) {
            mRenderer.addItem(mLightRects[i]);
        }
        mRenderer.addItem(gridLines);
        //mRenderer.addItem(touchLines);
        //mRenderer.addItem(curLines);
        //mRenderer.addItem(pressLines);
        mXdpi = mYdpi = 200;
    }

    public void setDPI(float xdpi, float ydpi) {
        mXdpi = xdpi;
        mYdpi = ydpi;
    }

    public void setMidiOutput(NetworkMidiOutput midiOut) {
        mMidiOut = midiOut;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        mCellWidth = FINGER_SIZE_INCHES * mXdpi;
        mCellHeight = FINGER_SIZE_INCHES * mYdpi;
        int N = (int) Math.ceil((right - left) / mCellWidth) + 1;
        gridLines.reset();
        for (int i = 0; i <= N; i++) {
            gridLines.add((float) i * mCellWidth, 0.0f, 0.0f, (float) i * mCellWidth, (float) N * mCellHeight, 0.0f);
            gridLines.add(0.0f, (float) i * mCellHeight, 0.0f, (float) N * mCellWidth, (float) i * mCellHeight, 0.0f);
        }

        touchLines.reset();
        touchLines.add(-mCellWidth, 0.0f, 0.0f, mCellWidth, 0.0f, 0.0f);
        touchLines.add(0.0f, -mCellHeight, 0.0f, 0.0f, mCellHeight, 0.0f);

        curLines.reset();
        curLines.add(-mCellWidth, 0.0f, 0.0f, mCellWidth, 0.0f, 0.0f);
        curLines.add(0.0f, -mCellHeight, 0.0f, 0.0f, mCellHeight, 0.0f);

        pressLines.reset();
        pressLines.add(-mCellWidth, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);

        for (int i = 0; i < 16; i++) {
            mLightRects[i].reset();
            mLightRects[i].add(-mCellWidth / 2, mCellHeight / 2, 0.0f, mCellWidth / 2, -mCellHeight / 2, 0.0f);
            Matrix.setIdentityM(mLightMatrices[i], 0);
            Matrix.translateM(mLightMatrices[i], 0, -mCellWidth / 2, -mCellHeight / 2, 0.0f); // offscreen
            mLightRects[i].setModelMatrix(mLightMatrices[i]);
        }
    }

    static float clamp(float min, float max, float x) {
        return ((x > max) ? max : ((x < min) ? min : x));
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        int pointerCount = ev.getPointerCount();
        //final long eventTime = ev.getEventTime();

         for (int p = 0; p < pointerCount; p++) {
             int pointerId = ev.getPointerId(p);
             mCurXs[pointerId] = ev.getX(p);
             mCurYs[pointerId] = this.getHeight() - ev.getY(p); // Y-invert for OpenGL
             // scale (minDomain,maxDomain) value to (minRange,maxRange)
             //Log.d("press","!"+ev.getPressure());
             mCurPressures[pointerId] = (ev.getPressure(p) - mMinPressureDomain) / (mMaxPressureDomain-mMinPressureDomain);
             mCurPressures[pointerId] = clamp(mMinPressureRange, mMaxPressureRange, mCurPressures[pointerId]);
             //Log.d("press","->"+mCurPressures[pointerId]);

         }

        Matrix.setIdentityM(mCurMatrix,0);
        Matrix.translateM(mCurMatrix, 0, mCurXs[0], mCurYs[0], 0.0f);
        curLines.setModelMatrix(mCurMatrix);

        Matrix.setIdentityM(mPressMatrix, 0);
        Matrix.translateM(mPressMatrix, 0, mCurPressures[0] * mCellWidth, mCellHeight / 2, 0.0f);
        pressLines.setModelMatrix(mPressMatrix);

        final int historySize = ev.getHistorySize();
        if(historySize > 0) {
            Log.e("samp","UNEXPECTED HISTORY!");
        }
        //debugSamples(ev);
        switch (ev.getActionMasked()) {
            // ignoring ACTION_HOVER_*, ACTION_SCROLL
            case MotionEvent.ACTION_POINTER_DOWN:
                eventDown(ev.getPointerId(ev.getActionIndex()));
                break;
            case MotionEvent.ACTION_POINTER_UP:
                eventUp(ev.getPointerId(ev.getActionIndex()));
                break;
            case MotionEvent.ACTION_DOWN:
                eventDown(ev.getPointerId(0));
                this.requestUnbufferedDispatch(ev); // move events will not be buffered (Android L & later)
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                eventUp(ev.getPointerId(0));
                break;
            case MotionEvent.ACTION_MOVE:
                for (int p = 0; p < pointerCount; p++) {
                    eventMove(ev.getPointerId(p));
                }
                break;
            case MotionEvent.ACTION_OUTSIDE:
                Log.d("onTouch", "Outside? what to do?");
                break;
        }
        return true;
    }

    private void eventMove(int pointerId) {
        float dx = mCurXs[pointerId] - mTouchXs[pointerId];
        float dy = mCurYs[pointerId] - mTouchYs[pointerId];
        //Log.d("onTouch", String.format("cx: %.2f tx: %.2f p: %d id: %d", mCurXs[pointerId], mTouchXs[pointerId], p, pointerId));
        sendModulate(pointerId, dx, dy);
    }

    private void eventDown(int pointerId) {
        //Log.d("eventDown","down:"+pointerId);
        mTouchXs[pointerId] = mCurXs[pointerId];
        mTouchYs[pointerId] = mCurYs[pointerId];

        // light rect
        float x = (float)Math.floor(mTouchXs[pointerId]/mCellWidth)*mCellWidth + mCellWidth/2;
        float y = (float)Math.floor(mTouchYs[pointerId]/mCellHeight)*mCellHeight + mCellHeight/2;
        Matrix.setIdentityM(mLightMatrices[pointerId],0);
        Matrix.translateM(mLightMatrices[pointerId], 0, x, y, 0.0f);
        mLightRects[pointerId].setModelMatrix(mLightMatrices[pointerId]);
        float color = mCurPressures[pointerId];
        mLightRects[pointerId].setColor(color,color,color,1.0f);

        if(pointerId == 0) {
            Matrix.setIdentityM(mTouchMatrix, 0);
            Matrix.translateM(mTouchMatrix, 0, mTouchXs[pointerId], mTouchYs[pointerId], 0.0f);
            touchLines.setModelMatrix(mTouchMatrix);
        }
        updateNote(pointerId);
        sendNoteOn(pointerId);
    }

    private void eventUp(int pointerId) {
        //Log.d("eventUp","up:"+pointerId);

        // light rect offscreen
        Matrix.setIdentityM(mLightMatrices[pointerId],0);
        Matrix.translateM(mLightMatrices[pointerId], 0, -mCellWidth/2, -mCellHeight/2, 0.0f);
        mLightRects[pointerId].setModelMatrix(mLightMatrices[pointerId]);

        mCurPressures[pointerId] = 0.0f;
        if(pointerId == 0) {
            Matrix.setIdentityM(mPressMatrix, 0);
            Matrix.translateM(mPressMatrix, 0, mCurPressures[pointerId] * mCellWidth, mCellHeight / 2, 0.0f);
            pressLines.setModelMatrix(mPressMatrix);
        }
        sendNoteOff(pointerId);
    }

    private void sendMidi(byte[] m) {
        try{
            mMidiOut.sendMidiOnThread(m);
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private void updateNote(int channel) {
        byte curNote = (byte)clamp(0f,127f,(float)(mBaseNote +
                                                   Math.floor(mTouchXs[channel]/ mCellWidth) +
                                                   mStride*Math.floor(mTouchYs[channel]/ mCellHeight)));
        byte curPressure = (byte)clamp(0f,127f,(float)Math.floor(mCurPressures[channel]*127 + 0.5f));
        mNoteOns[channel][1] = curNote;
        mNoteOns[channel][2] = curPressure;
        mNoteOffs[channel][6+1] = curNote;
    }

    private void sendNoteOn(int channel) {
        //Log.d("sendNoteOn",String.format("ch=%d", channel));
        if(mActiveNotes[channel]) {
            Log.e("sendNoteOn",String.format("MISSED NOTE OFF on Channel %d", channel));
        }
        mActiveNotes[channel] = true;
        sendMidi(mNoteOns[channel]);
    }

    private void sendNoteOff(int channel) {
        //Log.d("sendNoteOff",String.format("ch=%d", channel));
        if(!(mActiveNotes[channel])) {
            Log.e("sendNoteOff",String.format("MISSED NOTE ON on Channel %d", channel));
        }
        mActiveNotes[channel] = false;
        sendMidi(mNoteOffs[channel]);
    }

    private void sendModulate(int channel, float deltax, float deltay) {
        // normalize, clamp & pack into bytes each delta modulation
        float pitchDelta = 0x2000 + 0x2000*(deltax/(mRange*mCellWidth));
        pitchDelta = (pitchDelta > 0x3fff) ? 0x3ffff : ((pitchDelta < 0) ? 0 : pitchDelta);
        byte pitchHighByte = (byte)((((int)pitchDelta)>>7) & 0x7f);
        byte pitchLowByte = (byte)(((int)pitchDelta) & 0x7f);

        float modDelta = 0x7f*(Math.abs(deltay)/ mCellHeight);
        modDelta = (modDelta > 0x7f) ? 0x7f : ((modDelta < 0) ? 0 : modDelta);
        byte modByte = (byte)(((int)modDelta) & 0x7f);

        boolean changed = !((mModulates[channel][3+1] == pitchLowByte) &&
                            (mModulates[channel][3+2] == pitchHighByte) &&
                            (mModulates[channel][2] == modByte));
        if(changed) {
            //Log.d("sendModulate",String.format("ch=%d, dlt=%f %02x%02x", channel, delta, ((d>>7) & 0x7f), (d & 0x7f)));
            mModulates[channel][3+1] = pitchLowByte;
            mModulates[channel][3+2] = pitchHighByte;
            mModulates[channel][2] = modByte;
            sendMidi(mModulates[channel]);
        }
    }

    // for debug
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

}
