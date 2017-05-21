package org.monkey.d.ruffy.ruffy.driver.display;

import android.util.Log;

import org.monkey.d.ruffy.ruffy.driver.display.parser.LargeTextParser;
import org.monkey.d.ruffy.ruffy.driver.display.parser.SmallTextParser;

import java.util.LinkedList;

/**
 * Created by fishermen21 on 20.05.17.
 */

public class DisplayParser {
    private static boolean busy = false;

    public static Menu findMenu(byte[][] pixels) {
        if(busy)
        {
            Log.v("Tokens","skipping frame, busy…");
            return null;
        }
        busy = true;

        long t1 = System.currentTimeMillis();

        try {
            byte[][] display = new byte[4][96];
            for(int i = 0; i < 4;i++)
                for(int c = 0;c < 96;c++)
                    display[i][c]=pixels[i][95-c];

            Log.v("Tokens", "----------------------------------");
            //print(display,"analyze");
            LinkedList<Token> tokens = new LinkedList<>();
            for(int i = 0; i< 4;i++)
            {
                for(int x = 0; x < 92;)//no token is supposed to be smaller then 5 columns
                {
                    Token t = null;

                    t = LargeTextParser.findToken(display,i,x);
                    if(t==null)
                    {
                        t = SmallTextParser.findToken(display, i, x);
                    }

                    if (t != null) {
                        tokens.add(t);
                        x += t.getWidth()-1;
                    } else {
                        x++;
                    }
                }

            }
            if(tokens.size()>0)
            {
                for(Token t : tokens)
                {
                    Log.v("Tokens",t+" found");
                }
            }
            long s = 0;
            for(int i = 0; i< 4;i++) {
                for (int x = 0; x < 96; x++) {
                    s+=display[i][x];
                }
            }
            if(s>0)
            {
                print(display,"not empty");
            }
            return null;
        }catch(Throwable e){e.printStackTrace();Log.e("Tokens","error...",e);}
        finally {
            Log.v("Tokens",(((double)(System.currentTimeMillis()-t1))/1000d)+" secs needed for frame");
            busy=false;
        }
        return null;
    }

    private static String[] makeStrings(boolean[][][] pixels) {
        String[] display = new String[32];
        for (int w = 0; w < 4; w++) {
            for (int r = 0; r < 8; r++) {
                String line = "";
                for (int c = 0; c < 96; c++) {
                    line += pixels[w][r][c] ? "█" : " ";
                }
                display[(w*8)+r]=line;
            }
        }
        return display;
    }

    public static boolean match(String[] display, String[] symbol, int startx, int starty) {
        for(int y = starty; y < 32 && y < starty+symbol.length;y++)
        {
            try {
                int l = Math.min(display[y].length(), startx + symbol[y - starty].length());
                if(l>0 && startx+l<display[y].length()) {
                    String c = display[y].substring(startx, l);
                    if (!c.equals(symbol[y - starty])) {
                        return false;
                    }
                }
                else
                {
                    //Log.d("display","wtf?!?!");
                }
            }catch(Exception e)
            {e.printStackTrace();}
        }
        //Debug
        for(int y = starty;y<starty+symbol.length;y++)
        {
            String ds = display[y];
            int s = startx;
            String before = "";
            if(s>1)
                 before = ds.substring(0,startx-1);
            String after = "";
            s=startx+symbol[y-starty].length();
            if(ds.length()>s)
                after = ds.substring(startx+symbol[y-starty].length());
            String sym = "|";
            for(int x = 0; x < symbol[y-starty].length()-1;x++)
                sym+="+";
            sym+="|";
            display[y]=before+sym+after;
        }
        return true;
    }

    public static void print(byte[][] display, String text) {
        Log.d("DisplayParser","////////////////////////////////////////////////////////////////////////////////////////////////");
        Log.d("DisplayParser",text);

        for (int i = 0; i < 4; i++) {
            String[] lines = new String[]{"","","","","","","",""};
            for(int c = 0;c < 96;c++)
            {
                for(int r = 0; r < 8; r++) {
                    lines[r] += (display[i][c] & ((1 << r) & 0xFF)) != 0 ? "█" : " ";
                }
            }
            for(int r = 0; r < 8; r++) {
                Log.d("DisplayParser", lines[r]);
            }
        }
        Log.d("DisplayParser","////////////////////////////////////////////////////////////////////////////////////////////////");
    }
}

