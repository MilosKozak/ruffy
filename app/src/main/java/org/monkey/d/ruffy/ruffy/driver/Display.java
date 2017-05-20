package org.monkey.d.ruffy.ruffy.driver;

import android.util.Log;
import android.view.View;

import java.nio.ByteBuffer;

public class Display {

    private final DisplayUpdater updater;
    private boolean pixels[][][] = new boolean[4][8][96];
    private int index = -1;
    private CompleteDisplayHandler completeHandler;
    private boolean[] complete = {false,false,false,false};

    public Display(DisplayUpdater updater)
    {
        this.updater = updater;
    }

    public void setCompletDisplayHandler(CompleteDisplayHandler completeHandler) {this.completeHandler = completeHandler;}
    public void clear()
    {
        pixels = new boolean[4][8][96];
        updater.clear();
    }

    private void update(boolean quarter[][], int which, int index)
    {
        pixels[which] = quarter;
        updater.update(quarter,which);
        if(this.index==index)
        {
            complete[which]=true;
            if(this.completeHandler != null && complete[0] && complete[1] && complete[2] && complete[3])
                completeHandler.handleCompleteFrame(pixels);

        }
        else
        {
            this.index = index;
            complete = new boolean[]{false,false,false,false};
            complete[which] = true;
        }
    }

    public void addDisplayFrame(ByteBuffer b)
    {
        //discard first 3
        b.getShort();
        b.get();
        int index = (int)(b.get() & 0xFF);
        byte row = b.get();

        byte[] map = new byte[96];		//New array
        b.get(map);						//Read in array from packet

        boolean[][] quarter = new boolean[8][96];

        int column = 96;
        for(byte d:map)
        {
            column--;

            quarter[0][column] = ((d & 0x01) == 0x01);
            quarter[1][column] = ((d & 0x02) == 0x02);
            quarter[2][column] = ((d & 0x04) == 0x04);
            quarter[3][column] = ((d & 0x08) == 0x08);
            quarter[4][column] = ((d & 0x10) == 0x10);
            quarter[5][column] = ((d & 0x20) == 0x20);
            quarter[6][column] = ((d & 0x40) == 0x40);
            quarter[7][column] = ((d & 0x80) == 0x80);
        }

        switch(row)
        {
            case 0x47:
                update(quarter,0,index);
                break;
            case 0x48:
                update(quarter,1,index);
                break;
            case (byte)0xB7:
                update(quarter,2,index);
                break;
            case (byte)0xB8:
                update(quarter,3,index);
                break;
        }
    }
}