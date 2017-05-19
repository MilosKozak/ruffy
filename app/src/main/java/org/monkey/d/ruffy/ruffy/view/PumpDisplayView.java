package org.monkey.d.ruffy.ruffy.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import org.monkey.d.ruffy.ruffy.driver.DisplayUpdater;

public class PumpDisplayView extends SurfaceView implements DisplayUpdater {

    private SurfaceHolder surfaceHolder;
    private final Paint black;
    private final Paint white;

    public PumpDisplayView(Context c, AttributeSet s) {
        super(c,s);
        surfaceHolder = getHolder();
        //surfaceHolder.setFixedSize(96,32);
        black = new Paint();
        black.setColor(Color.BLACK);
        white = new Paint();
        white.setColor(Color.WHITE);
    }

    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec)
    {
        int originalWidth = View.MeasureSpec.getSize(widthMeasureSpec);

        int originalHeight = View.MeasureSpec.getSize(heightMeasureSpec);

        int calculatedHeight = originalWidth * 32 / 96;

        int finalWidth, finalHeight;

        if (calculatedHeight > originalHeight)
        {
            finalWidth = originalHeight * 32 / 96;
            finalHeight = originalHeight;
        }
        else
        {
            finalWidth = originalWidth;
            finalHeight = calculatedHeight;
        }

        super.onMeasure(
                MeasureSpec.makeMeasureSpec(finalWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY));
    }

    @Override
    public void update(boolean[][] pixel, int which)
    {
        int w = getWidth();
        float x = ((float)w)/96f;
        int h = getHeight();
        float y = ((float)h) / 32f;

        Canvas tempCanvas = surfaceHolder.lockCanvas(new Rect(0,(int)(y*(which*8)),(int)(x*96),(int)(y*((which+1)*8))));

        if(tempCanvas==null)
            return;

        tempCanvas.drawColor(Color.WHITE);

        for(int r = 0; r < 8; r++)
            for(int c = 0; c < 96; c++)

                if(pixel[r][c])
                    tempCanvas.drawRect(x*c,y*((which*8)+r),x*(c+1),y*((which*8)+r+1),black);

        surfaceHolder.unlockCanvasAndPost(tempCanvas);
    }

    @Override
    public void clear() {
        Canvas tempCanvas = surfaceHolder.lockCanvas();

        if(tempCanvas==null)
            return;

        tempCanvas.drawColor(Color.WHITE);
        surfaceHolder.unlockCanvasAndPost(tempCanvas);
    }
}