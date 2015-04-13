package com.gmail.rallen.gridstrument;

// TODO: pitch wheel starts as if it were never reset.
// TODO: check mod wheel
// TODO: single event with multiple fingers down does not send multiple notes

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;

import de.humatic.nmj.NetworkMidiOutput;

/**
 * GridGLSurfaceView
 */
public class GridGLSurfaceView extends GLSurfaceView {
    private static final int MAX_NOTES = 16;
    private static final float FINGER_SIZE_INCHES = 0.6f;
    private float dNx, dNy = 200f;

    private final GridGLRenderer mRenderer;
    private final GridLines gridLines, touchLines, curLines, pressLines;
    private NetworkMidiOutput mMidiOut = null;

    private float mXdpi, mYdpi;

    private float[] mTouchXs = new float[MAX_NOTES];
    private float[] mTouchYs = new float[MAX_NOTES];
    private float[] mCurXs = new float[MAX_NOTES];
    private float[] mCurYs = new float[MAX_NOTES];
    private float[] mCurPressures = new float[MAX_NOTES];

    private float[] mTouchMatrix = new float[16];
    private float[] mCurMatrix = new float[16];
    private float[] mPressMatrix = new float[16];

    private byte[][] mNoteOns = { // 0x90 Note On
            new byte[]{(byte)0x90, (byte)0x3c, (byte)100},
            new byte[]{(byte)0x91, (byte)0x3c, (byte)100},
            new byte[]{(byte)0x92, (byte)0x3c, (byte)100},
            new byte[]{(byte)0x93, (byte)0x3c, (byte)100},
            new byte[]{(byte)0x94, (byte)0x3c, (byte)100},
            new byte[]{(byte)0x95, (byte)0x3c, (byte)100},
            new byte[]{(byte)0x96, (byte)0x3c, (byte)100},
            new byte[]{(byte)0x97, (byte)0x3c, (byte)100},
            new byte[]{(byte)0x98, (byte)0x3c, (byte)100},
            new byte[]{(byte)0x99, (byte)0x3c, (byte)100},
            new byte[]{(byte)0x9a, (byte)0x3c, (byte)100},
            new byte[]{(byte)0x9b, (byte)0x3c, (byte)100},
            new byte[]{(byte)0x9c, (byte)0x3c, (byte)100},
            new byte[]{(byte)0x9d, (byte)0x3c, (byte)100},
            new byte[]{(byte)0x9e, (byte)0x3c, (byte)100},
            new byte[]{(byte)0x9f, (byte)0x3c, (byte)100}
    };
    private byte[][] mNoteOffs = { // 0x80 Note Off
            new byte[]{(byte)0x90, (byte)0x3c, (byte)0},
            new byte[]{(byte)0x91, (byte)0x3c, (byte)0},
            new byte[]{(byte)0x92, (byte)0x3c, (byte)0},
            new byte[]{(byte)0x93, (byte)0x3c, (byte)0},
            new byte[]{(byte)0x94, (byte)0x3c, (byte)0},
            new byte[]{(byte)0x95, (byte)0x3c, (byte)0},
            new byte[]{(byte)0x96, (byte)0x3c, (byte)0},
            new byte[]{(byte)0x97, (byte)0x3c, (byte)0},
            new byte[]{(byte)0x98, (byte)0x3c, (byte)0},
            new byte[]{(byte)0x99, (byte)0x3c, (byte)0},
            new byte[]{(byte)0x9a, (byte)0x3c, (byte)0},
            new byte[]{(byte)0x9b, (byte)0x3c, (byte)0},
            new byte[]{(byte)0x9c, (byte)0x3c, (byte)0},
            new byte[]{(byte)0x9d, (byte)0x3c, (byte)0},
            new byte[]{(byte)0x9e, (byte)0x3c, (byte)0},
            new byte[]{(byte)0x9f, (byte)0x3c, (byte)0}
    };
    // 0xA0     Aftertouch + 1 byte for note + 1 byte for value
    // 0xD0     Channel Pressure + 1 byte for value (not per-note)
    private byte[][] mAftertouches = {
            new byte[]{(byte)0xa0, (byte)0x3c, (byte)0},
            new byte[]{(byte)0xa1, (byte)0x3c, (byte)0},
            new byte[]{(byte)0xa2, (byte)0x3c, (byte)0},
            new byte[]{(byte)0xa3, (byte)0x3c, (byte)0},
            new byte[]{(byte)0xa4, (byte)0x3c, (byte)0},
            new byte[]{(byte)0xa5, (byte)0x3c, (byte)0},
            new byte[]{(byte)0xa6, (byte)0x3c, (byte)0},
            new byte[]{(byte)0xa7, (byte)0x3c, (byte)0},
            new byte[]{(byte)0xa8, (byte)0x3c, (byte)0},
            new byte[]{(byte)0xa9, (byte)0x3c, (byte)0},
            new byte[]{(byte)0xaa, (byte)0x3c, (byte)0},
            new byte[]{(byte)0xab, (byte)0x3c, (byte)0},
            new byte[]{(byte)0xac, (byte)0x3c, (byte)0},
            new byte[]{(byte)0xad, (byte)0x3c, (byte)0},
            new byte[]{(byte)0xae, (byte)0x3c, (byte)0},
            new byte[]{(byte)0xaf, (byte)0x3c, (byte)0}
    };
    private byte[][] mPitchBends = { // 0xE0 Pitch bend
            new byte[]{(byte)0xe0, (byte)0x00, (byte)0x40},
            new byte[]{(byte)0xe1, (byte)0x00, (byte)0x40},
            new byte[]{(byte)0xe2, (byte)0x00, (byte)0x40},
            new byte[]{(byte)0xe3, (byte)0x00, (byte)0x40},
            new byte[]{(byte)0xe4, (byte)0x00, (byte)0x40},
            new byte[]{(byte)0xe5, (byte)0x00, (byte)0x40},
            new byte[]{(byte)0xe6, (byte)0x00, (byte)0x40},
            new byte[]{(byte)0xe7, (byte)0x00, (byte)0x40},
            new byte[]{(byte)0xe8, (byte)0x00, (byte)0x40},
            new byte[]{(byte)0xe9, (byte)0x00, (byte)0x40},
            new byte[]{(byte)0xea, (byte)0x00, (byte)0x40},
            new byte[]{(byte)0xeb, (byte)0x00, (byte)0x40},
            new byte[]{(byte)0xec, (byte)0x00, (byte)0x40},
            new byte[]{(byte)0xed, (byte)0x00, (byte)0x40},
            new byte[]{(byte)0xee, (byte)0x00, (byte)0x40},
            new byte[]{(byte)0xef, (byte)0x00, (byte)0x40}
    };
    // [1] = 1 // mod wheel (coarse), 33 mod wheel (fine)
    // [1] = 2 // breath control (coarse), 34 bc (fine)
    private byte[][] mContinuousControllers = { // 0xB0 Continuous controller
            new byte[]{(byte)0xb0, (byte)0x01, (byte)0x00},
            new byte[]{(byte)0xb1, (byte)0x01, (byte)0x00},
            new byte[]{(byte)0xb2, (byte)0x01, (byte)0x00},
            new byte[]{(byte)0xb3, (byte)0x01, (byte)0x00},
            new byte[]{(byte)0xb4, (byte)0x01, (byte)0x00},
            new byte[]{(byte)0xb5, (byte)0x01, (byte)0x00},
            new byte[]{(byte)0xb6, (byte)0x01, (byte)0x00},
            new byte[]{(byte)0xb7, (byte)0x01, (byte)0x00},
            new byte[]{(byte)0xb8, (byte)0x01, (byte)0x00},
            new byte[]{(byte)0xb9, (byte)0x01, (byte)0x00},
            new byte[]{(byte)0xba, (byte)0x01, (byte)0x00},
            new byte[]{(byte)0xbb, (byte)0x01, (byte)0x00},
            new byte[]{(byte)0xbc, (byte)0x01, (byte)0x00},
            new byte[]{(byte)0xbd, (byte)0x01, (byte)0x00},
            new byte[]{(byte)0xbe, (byte)0x01, (byte)0x00},
            new byte[]{(byte)0xbf, (byte)0x01, (byte)0x00},
    };
    private boolean[] mActiveNotes = new boolean[]{
            false, false, false, false,
            false, false, false, false,
            false, false, false, false,
            false, false, false, false
    };
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
        mXdpi = mYdpi = 200;
    }

    public void setDPI(float xdpi,float ydpi) {
        mXdpi = xdpi;
        mYdpi = ydpi;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        dNx = FINGER_SIZE_INCHES*mXdpi;
        dNy = FINGER_SIZE_INCHES*mYdpi;
        int N = (int)Math.ceil((right-left)/dNx)+1;
        gridLines.reset();
        for(int i = 0; i <= N; i++) {
            gridLines.add((float)i * dNx, 0.0f, 0.0f,   (float)i * dNx, (float)N*dNy, 0.0f);
            gridLines.add(0.0f, (float)i * dNy, 0.0f,   (float)N*dNx, (float)i * dNy, 0.0f);
        }

        touchLines.reset();
        touchLines.add(-dNx, 0.0f, 0.0f,    dNx, 0.0f, 0.0f);
        touchLines.add(0.0f, -dNy, 0.0f,   0.0f, dNy, 0.0f);

        curLines.reset();
        curLines.add(-dNx, 0.0f, 0.0f,    dNx, 0.0f, 0.0f);
        curLines.add(0.0f, -dNy, 0.0f,   0.0f, dNy, 0.0f);

        pressLines.reset();
        pressLines.add(-dNx, 0.0f, 0.0f,    0.0f, 0.0f, 0.0f);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int pointerCount = ev.getPointerCount();
        //final long eventTime = ev.getEventTime();
        for (int p = 0; p < pointerCount; p++) {
            final int pointerId = ev.getPointerId(p);
            mCurXs[pointerId] = ev.getX(p);
            mCurYs[pointerId] = this.getHeight() - ev.getY(p); // Y-invert for OpenGL
            mCurPressures[pointerId] = ev.getPressure(p);
        }

        Matrix.setIdentityM(mCurMatrix,0);
        Matrix.translateM(mCurMatrix, 0, mCurXs[0], mCurYs[0], 0.0f);
        curLines.setModelMatrix(mCurMatrix);

        Matrix.setIdentityM(mPressMatrix, 0);
        Matrix.translateM(mPressMatrix, 0, mCurPressures[0]*dNx, dNy/2, 0.0f);
        pressLines.setModelMatrix(mPressMatrix);

        //debugSamples(ev);
        switch (ev.getActionMasked()) {
            // ignoring ACTION_HOVER_*, ACTION_SCROLL
            case MotionEvent.ACTION_POINTER_DOWN:
                //Log.d("onTouch", "Pointer Down");
                eventDown(ev.getPointerId(ev.getActionIndex()));
                break;
            case MotionEvent.ACTION_POINTER_UP:
                //Log.d("onTouch", "Pointer Up");
                eventUp(ev.getPointerId(ev.getActionIndex()));
                break;
            case MotionEvent.ACTION_DOWN:
                //Log.d("onTouch", String.format("down x: %.2f y: %.2f p: %.2f nHist: %d, nPtr: %d", mCurXs, mCurYs, mCurPressures, 0, 0));//nHistory, nPointers));
                eventDown(ev.getPointerId(0));
                this.requestUnbufferedDispatch(ev); // move events will not be buffered (Android L & later)
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                //Log.d("onTouch", String.format("..up   x: %.2f y: %.2f p: %.2f nHist: %d, nPtr: %d", x-mTouchXs, y-mTouchYs, p, nHistory, nPointers));
                eventUp(ev.getPointerId(0));
                break;
            case MotionEvent.ACTION_MOVE:
                //Log.d("onTouch", String.format("..move x: %.2f y: %.2f p: %.2f", mCurXs-mTouchXs, mCurYs-mTouchYs, mCurPressures));
                for (int p = 0; p < pointerCount; p++) {
                    final int pointerId = ev.getPointerId(p);
                    float dx = mCurXs[pointerId] - mTouchXs[pointerId];
                    float dy = mCurYs[pointerId] - mTouchYs[pointerId];
                    sendPitchBend(pointerId,dx);
                    sendModWheel(pointerId,dy);
                }
                break;
            case MotionEvent.ACTION_OUTSIDE:
                Log.d("onTouch", "Outside? what to do?");
                break;
        }
        return true;
    }

    void eventDown(int pointerId) {
        //Log.d("eventDown","down:"+pointerId);
        mTouchXs[pointerId] = mCurXs[pointerId];
        mTouchYs[pointerId] = mCurYs[pointerId];
        if(pointerId == 0) {
            Matrix.setIdentityM(mTouchMatrix, 0);
            Matrix.translateM(mTouchMatrix, 0, mTouchXs[pointerId], mTouchYs[pointerId], 0.0f);
            touchLines.setModelMatrix(mTouchMatrix);
        }
        updateNote(pointerId);
        sendNoteOn(pointerId);
        //sendPitchBend(pointerId,0);
        //sendModWheel(pointerId,0);
    }

    void eventUp(int pointerId) {
        //Log.d("eventUp","up:"+pointerId);
        mCurPressures[pointerId] = 0.0f;
        if(pointerId == 0) {
            Matrix.setIdentityM(mPressMatrix, 0);
            Matrix.translateM(mPressMatrix, 0, mCurPressures[pointerId] * dNx, dNy / 2, 0.0f);
            pressLines.setModelMatrix(mPressMatrix);
        }
        sendPitchBend(pointerId,0);
        //sendModWheel(pointerId,0);
        sendNoteOff(pointerId);
    }

    // for debug
    void debugSamples(MotionEvent ev) {
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

    private void updateNote(int channel) {
        byte curNote = (byte)(48.0 + Math.floor(mTouchXs[channel]/dNx) + 5*Math.floor(mTouchYs[channel]/dNy));
        mNoteOns[channel][1] = curNote;
        mNoteOffs[channel][1] = curNote;
    }

    private void sendMidi(byte[] m) {
        try{
            mMidiOut.sendMidiOnThread(m);
        } catch (Exception ex){
            ex.printStackTrace();
        }
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

    private void sendPitchBend(int channel, float delta) {
        //Log.d("sendPitchBend",String.format("ch=%d, dlt=%f", channel, delta));
        // FIXME multiple notes will need to go on multiple channels
        // FIXME pitchBend is not working perfectly...
        delta = 0x2000 + 0x2000*(delta/dNx); // normalize
        delta = (delta > 0x3fff) ? 0x3ffff : ((delta < 0) ? 0 : delta);
        int d = (int)delta;
        boolean changed = !((mPitchBends[channel][1] == (byte)(d & 0x7f)) &&
                            (mPitchBends[channel][2] == (byte)((d>>7) & 0x7f)));
        if(changed) {
            mPitchBends[channel][1] = (byte)(d & 0x7f);
            mPitchBends[channel][2] = (byte)((d>>7) & 0x7f);
            sendMidi(mPitchBends[channel]);
        }
    }

    private void sendModWheel(int channel, float delta) {
        delta = 0x7f*(delta/dNy); // normalize
        delta = (delta > 0x7f) ? 0x7f : ((delta < 0) ? 0 : delta);
        int d = (int)delta;
        boolean changed = !(mContinuousControllers[channel][2] == (byte)(d & 0x7f));
        if(changed) {
            mContinuousControllers[channel][2] = (byte)(d & 0x7f);
            sendMidi(mContinuousControllers[channel]);
        }
    }

    public void setMidiOutput(NetworkMidiOutput midiOut) {
         mMidiOut = midiOut;
    }

}
