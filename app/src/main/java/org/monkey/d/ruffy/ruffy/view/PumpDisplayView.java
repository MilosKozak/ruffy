package org.monkey.d.ruffy.ruffy.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
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
    public void update(byte[] pixel, int which)
    {
        int w = getWidth();
        float xf = ((float)w)/96f;
        int h = getHeight();
        float yf = ((float)h) / 32f;

        int x = (int)(Math.floor(xf));
        int y = (int)(Math.floor(yf));
        Canvas tempCanvas = surfaceHolder.lockCanvas(new Rect(0,y*(which*8),x*96,y*((which+1)*8)));

        if(tempCanvas==null)
            return;

        tempCanvas.drawColor(Color.WHITE);

        for(int c = 0; c < 96; c++)
            for(int r = 0; r < 8; r++)
                if((pixel[95-c] & (1<<r))!=0)
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