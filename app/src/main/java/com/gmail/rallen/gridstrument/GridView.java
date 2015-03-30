package com.gmail.rallen.gridstrument;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class GridView extends View {
    private ShapeDrawable mGridDrawable;
    private Path mIntersectPath, mIntersectPath1;
    private Paint mIntersectPaint, mIntersectPaint1;
    private float touchX, touchY, curX, curY, curPressure;

    public GridView(Context context) {
        super(context);
    }

    public GridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // in the constructor we don't have the width and height of the view, yet
        init();
    }

    private void init() {
        Log.d("init", String.format("width: %d; height: %d", this.getWidth(), this.getHeight()));
        Path gridPath = new Path();
        int N = 4;
        float dN = 200f;
        for(int i = 0; i <= N; i++) {
            gridPath.moveTo(i * dN, 0);
            gridPath.lineTo(i * dN, N*dN);
            gridPath.moveTo(0, i * dN);
            gridPath.lineTo(N*dN, i * dN);
        }
        gridPath.close();

        mGridDrawable = new ShapeDrawable(new PathShape(gridPath, this.getWidth(), this.getHeight()));
        mGridDrawable.getPaint().setColor(0xffeeeeee);//0xff74AC23);
        mGridDrawable.getPaint().setStyle(Paint.Style.STROKE);
        mGridDrawable.getPaint().setStrokeWidth(2.0f);
        mGridDrawable.setBounds(0, 0, this.getWidth(), this.getHeight());

        mIntersectPath = new Path();
        mIntersectPaint = new Paint();
        mIntersectPaint.setColor(0xff74AC23);
        mIntersectPaint.setStyle(Paint.Style.STROKE);
        mIntersectPaint.setStrokeWidth(3.0f);
        mIntersectPath1 = new Path();
        mIntersectPaint1 = new Paint();
        mIntersectPaint1.setColor(0xffAC2374);
        mIntersectPaint1.setStyle(Paint.Style.STROKE);
        mIntersectPaint1.setStrokeWidth(1.0f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mGridDrawable.draw(canvas);

        mIntersectPath.reset();
        mIntersectPath.moveTo(-this.getWidth(), touchY);
        mIntersectPath.lineTo(this.getWidth(), touchY);
        mIntersectPath.moveTo(touchX, -this.getHeight());
        mIntersectPath.lineTo(touchX, this.getHeight());
        mIntersectPath.close();
        canvas.drawPath(mIntersectPath, mIntersectPaint);

        mIntersectPath1.reset();
        mIntersectPath1.moveTo(-this.getWidth(), curY);
        mIntersectPath1.lineTo(this.getWidth(), curY);
        mIntersectPath1.moveTo(curX, -this.getHeight());
        mIntersectPath1.lineTo(curX, this.getHeight());
        mIntersectPath1.close();
        canvas.drawPath(mIntersectPath1, mIntersectPaint1);

        canvas.drawCircle(100, 100, 100*curPressure, mIntersectPaint1);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        float x = ev.getX();
        float y = ev.getY();
        curX = x;
        curY = y;
        float p = ev.getPressure();
        curPressure = p;
        final int nHistory = ev.getHistorySize();
        final int nPointers = ev.getPointerCount();

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //Log.d("onTouch", String.format("down x: %.2f y: %.2f p: %.2f nHist: %d, nPtr: %d", x, y, p, nHistory, nPointers));
                touchX = x;
                touchY = y;
                this.invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                //Log.d("onTouch", String.format("..move x: %.2f y: %.2f p: %.2f nHist: %d, nPtr: %d", x-touchX, y-touchY, p, nHistory, nPointers));
                this.invalidate();
                break;
            case MotionEvent.ACTION_UP:
                //Log.d("onTouch", String.format("..up   x: %.2f y: %.2f p: %.2f nHist: %d, nPtr: %d", x-touchX, y-touchY, p, nHistory, nPointers));
                curPressure = 0.0f;
                this.invalidate();
                break;
        }
        return true;
    }

}
