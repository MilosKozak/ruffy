package org.monkey.d.ruffy.ruffy.driver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.LinearLayout;

public class PumpDisplay extends SurfaceView implements SurfaceHolder.Callback {

    Thread thread = null;
    SurfaceHolder surfaceHolder;
    volatile boolean running = false;

    public PumpDisplay(Context c, AttributeSet s) {
        super(c,s);
        surfaceHolder = getHolder();
        surfaceHolder.setFixedSize(96,32);
        surfaceHolder.addCallback(this);
    }



    public void draw(rtFrame frame) {
        Canvas canvas = surfaceHolder.lockCanvas();

        if(canvas==null)return;
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        int blockWidth = 1;//w / 96;
        int blockHeight = 1;// h / 32;

        Paint p = new Paint();
        p.setColor(Color.BLACK);
        canvas.drawColor(Color.WHITE);
        for(int i = 0; i < 96; i++)
        {
            for(int j= 0; j < 8; j++)
            {
                if(frame.row1[j].charAt(i)=='8')
                {
                    canvas.drawRect(i*blockWidth,j*blockHeight,(i+1)*blockWidth,(j+1)*blockHeight,p);
                }
            }
        }
        for(int i = 0; i < 96; i++)
        {
            for(int j= 0; j < 8; j++)
            {
                if(frame.row2[j].charAt(i)=='8')
                {
                    canvas.drawRect(i*blockWidth,8+(j*blockHeight),(i+1)*blockWidth,8+((j+1)*blockHeight),p);
                }
            }
        }
        for(int i = 0; i < 96; i++)
        {
            for(int j= 0; j < 8; j++)
            {
                if(frame.row3[j].charAt(i)=='8')
                {
                    canvas.drawRect(i*blockWidth,16+(j*blockHeight),(i+1)*blockWidth,16+((j+1)*blockHeight),p);
                }
            }
        }
        for(int i = 0; i < 96; i++)
        {
            for(int j= 0; j < 8; j++)
            {
                if(frame.row4[j].charAt(i)=='8')
                {
                    canvas.drawRect(i*blockWidth,24+(j*blockHeight),(i+1)*blockWidth,24+((j+1)*blockHeight),p);
                }
            }
        }

        surfaceHolder.unlockCanvasAndPost(canvas);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.v("surface","surface created");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.v("surface","surface changed");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v("surface","surface destroyed");
    }

   /* @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
       // if(widthMeasureSpec== LinearLayout.LayoutParams.MATCH_PARENT)
            widthMeasureSpec = getParent().
        float xspec = ((float)widthMeasureSpec) / 96.0f;
        int h = (int)(xspec*32);
        setMeasuredDimension(widthMeasureSpec,h);
    }*/

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
}