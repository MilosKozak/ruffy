package org.monkey.d.ruffy.ruffy.driver;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.monkey.d.ruffy.ruffy.driver.display.DisplayParser;
import org.monkey.d.ruffy.ruffy.driver.display.DisplayParserHandler;
import org.monkey.d.ruffy.ruffy.driver.display.Menu;
import org.monkey.d.ruffy.ruffy.driver.display.MenuAttribute;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by fishermen21 on 25.05.17.
 */

public class Ruffy extends Service  {

    public static class Key {
        public static byte NO_KEY				=(byte)0x00;
        public static byte MENU					=(byte)0x03;
        public static byte CHECK				=(byte)0x0C;
        public static byte UP					=(byte)0x30;
        public static byte DOWN					=(byte)0xC0;
        public static byte BACK                 =(byte)0x33;
        public static byte COPY                 =(byte)0xF0;
    }

    private Set<IRTHandler> rtHandlers = new HashSet<>();
    private BTConnection btConn;
    private PumpData pumpData;

    private boolean rtModeRunning = false;
    private long lastRtMessageSent = 0;

    private final Object rtSequenceSemaphore = new Object();
    private short rtSequence = 0;

    private int modeErrorCount = 0;
    private int step = 0;

    private boolean synRun=false;//With set to false, write process is started at first time
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool( 3 );

    private Display display;

    private final IRuffyService.Stub serviceBinder = new IRuffyService.Stub(){

        @Override
        public void addHandler(IRTHandler handler) throws RemoteException {
            rtHandlers.add(handler);
            if(rtModeRunning)
                handler.rtStarted();
        }

        @Override
        public void removeHandler(IRTHandler handler) throws RemoteException {
            rtHandlers.remove(handler);
            if(rtHandlers.size()==0 && rtModeRunning)
                doRTDisconnect(null);
        }

        @Override
        public int doRTConnect(IRTHandler startingHandler) throws RemoteException {
            Log.d("Ruffy","doRTConnect");
            if(isConnected() && rtModeRunning)
            {
                startingHandler.rtStarted();
                return 0;
            }
            step= 0;
            if(Ruffy.this.rtHandlers.size()==0)
            {
                return -2;//FIXME make errors
            }
            if(pumpData==null)
            {
                pumpData = PumpData.loadPump(Ruffy.this,rtHandlers);
            }
            if(pumpData != null) {
                btConn = new BTConnection(rtBTHandler);
                step=0;
                rtModeRunning = true;
                btConn.connect(pumpData, 10);
                return 0;
            }
            return -1;
        }

        public void doRTDisconnect(IRTHandler handler)
        {
            if(!rtModeRunning)
                try{handler.rtStopped();}catch(Exception e){}
            boolean doDisco = true;

            String discoIdent = null;
            try{discoIdent = handler.getServiceIdentifier();}catch(Exception e){}
            for(IRTHandler h : new LinkedList<>(rtHandlers))
            {
                String ident = null;
                try {ident= h.getServiceIdentifier();}catch(Exception e){rtHandlers.remove(h);}

                if(ident!=null && !ident.equals(discoIdent)) {
                    try {
                        doDisco = h.canDisconnect();
                    } catch (Exception e) {
                        rtHandlers.remove(h);
                    }
                    if (!doDisco) {
                        Log.d("Ruffy", "doRTDisconnect interupted by: " + ident);

                        break;
                    }
                }
            }
            if(doDisco) {
                Log.d("Ruffy", "doRTDisconnect");
                step = 300;

                stopRT();
            }
            else if(handler!=null)
            {
                try{handler.rtStopped();}catch(Exception e){};
            }
        }

        public void rtSendKey(byte keyCode, boolean changed)
        {
            Log.d("Ruffy","rtSendKey");
            lastRtMessageSent = System.currentTimeMillis();
            synchronized (rtSequenceSemaphore) {
                rtSequence = Application.rtSendKey(keyCode, changed, rtSequence, btConn);
                Log.v("RUFFY_KEY","send a key with sequence: "+(rtSequence-1)+" and value "+String.format("%02X",keyCode));
            }
        }

        public void resetPairing()
        {
            Log.d("Ruffy","resetPairing");
            SharedPreferences prefs = Ruffy.this.getSharedPreferences("pumpdata", Activity.MODE_PRIVATE);
            prefs.edit().putBoolean("paired",false).apply();
            synRun=false;
            rtModeRunning =false;
        }

        public boolean isConnected()
        {
            Log.d("Ruffy","isConnected");
            if (btConn!=null) {
                return btConn.isConnected();
            } else {
                return false;
            }
        }

        @Override
        public boolean isBoundToPump() throws RemoteException {
            Log.d("Ruffy","isBoundToPump");
            return PumpData.isPumpBound(Ruffy.this);
        }
    };

    @Override
    public void onCreate() {
        Log.d("Ruffy","onCreate");
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("Ruffy","onBind");
        return serviceBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d("Ruffy","onRebind");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v("Ruffy","onUnbind");
        return true;
    }

    @Override
    public void onDestroy() {

        Log.d("Ruffy","onDestroy");
        super.onDestroy();
    }

    private BTHandler rtBTHandler = new BTHandler() {
        @Override
        public void deviceConnected() {
            log("connected to pump");
            if(synRun==false) {
                synRun = true;
                log("start synThread");
                scheduler.execute(synThread);
            }
        }

        @Override
        public void log(String s) {
            Ruffy.this.log(s);
            if(s.equals("got error in read") && step < 200)
            {
                synRun=false;
                btConn.connect(pumpData,4);
            }
        }

        @Override
        public void fail(String s) {
            log("failed: "+s);
            synRun=false;
            if(step < 200)
                btConn.connect(pumpData,4);
            else
                Ruffy.this.fail(s);
        }

        @Override
        public void deviceFound(BluetoothDevice bd) {
            log("not be here!?!");
        }

        @Override
        public void handleRawData(byte[] buffer, int bytes) {
            log("got data from pump");
            synRun=false;
            //step=0;
            Packet.handleRawData(buffer,bytes, rtPacketHandler);
        }

        @Override
        public void requestBlueTooth() {
            for(IRTHandler handler : new LinkedList<>(rtHandlers))
            {
                try
                {
                    handler.requestBluetooth();
                }catch(Exception e1)
                {
                    rtHandlers.remove(handler);
                }
            }
        }
    };

    private void stopRT()
    {
        rtModeRunning = false;
    }

    private void startRT() {
        log("starting RT keepAlive");
        new Thread(){
            @Override
            public void run() {
                rtSequence = 0;
                lastRtMessageSent = System.currentTimeMillis();
                rtModeRunning = true;
                display = new Display(new DisplayUpdater() {
                    @Override
                    public void clear() {
                        for(IRTHandler handler : new LinkedList<>(rtHandlers))
                        {
                            try
                            {
                                handler.rtClearDisplay();
                            }catch(Exception e1)
                            {
                                rtHandlers.remove(handler);
                            }
                        }
                    }

                    @Override
                    public void update(byte[] quarter, int which) {
                        for(IRTHandler handler : new LinkedList<>(rtHandlers))
                        {
                            try
                            {
                                handler.rtUpdateDisplay(quarter,which);
                            }catch(Exception e1)
                            {
                                rtHandlers.remove(handler);
                            }
                        }
                    }
                });
                display.setCompletDisplayHandler(new CompleteDisplayHandler() {
                    @Override
                    public void handleCompleteFrame(byte[][] pixels, final short seq) {
                        DisplayParser.findMenu(pixels, new DisplayParserHandler() {
                            @Override
                            public void menuFound(Menu menu) {
                                Log.v("RUFFY_KEY", "found menu: " + menu.getType());
                                if (menu.getAttribute(MenuAttribute.BASAL_RATE)!=null)
                                {
                                    Log.v("RUFFY_KEY", "TBR: " + menu.getAttribute(MenuAttribute.BASAL_RATE));
                                }
                                if (menu.getAttribute(MenuAttribute.RUNTIME)!=null)
                                {
                                    Log.v("RUFFY_KEY", "RUNTIME: " + menu.getAttribute(MenuAttribute.RUNTIME));
                                }
                                for(IRTHandler handler : new LinkedList<>(rtHandlers))
                                {
                                    try
                                    {
                                        handler.rtDisplayHandleMenu(menu,seq);
                                    }catch(Exception e1)
                                    {
                                        rtHandlers.remove(handler);
                                    }
                                }
                            }

                            @Override
                            public void noMenuFound() {
                                for(IRTHandler handler : new LinkedList<>(rtHandlers))
                                {
                                    try
                                    {
                                        handler.rtDisplayHandleNoMenu(seq);
                                    }catch(Exception e1)
                                    {
                                        rtHandlers.remove(handler);
                                    }
                                }
                            }
                        });

                    }
                });
                for(IRTHandler handler : new LinkedList<>(rtHandlers))
                {
                    try
                    {
                        handler.rtStarted();
                    }catch(Exception e1)
                    {
                        rtHandlers.remove(handler);
                    }
                }
                while(rtModeRunning)
                {
                    if(System.currentTimeMillis() > lastRtMessageSent +1000L) {
                        log("sending keep alive");
                        synchronized (rtSequenceSemaphore) {
                            rtSequence = Application.sendRTKeepAlive(rtSequence, btConn);
                            lastRtMessageSent = System.currentTimeMillis();
                        }
                    }
                    try{Thread.sleep(500);}catch(Exception e){/*ignore*/}
                }

                synRun=false;
                Application.sendAppDisconnect(btConn);
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                btConn.disconnect();

                for(IRTHandler handler : new LinkedList<>(rtHandlers))
                {
                    try
                    {
                        handler.rtStopped();
                    }catch(Exception e1) {rtHandlers.remove(handler);}
                }
                rtSequence =0;

            }
        }.start();
    }

    private Runnable synThread = new Runnable(){
        @Override
        public void run() {
            while(synRun && rtModeRunning)
            {
                Protocol.sendSyn(btConn);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private AppHandler rtAppHandler = new AppHandler() {
        @Override
        public void log(String s) {
            Ruffy.this.log(s);
        }

        @Override
        public void connected() {
            Application.sendAppCommand(Application.Command.RT_MODE, btConn);
        }

        @Override
        public void rtModeActivated() {
            startRT();
        }

        @Override
        public void modeDeactivated() {
            rtModeRunning = false;
            Application.sendAppCommand(Application.Command.RT_MODE, btConn);
        }

        @Override
        public void addDisplayFrame(ByteBuffer b) {
            //FIXME this short has to be removed in Display:47 if this line is removed
            short seq = b.getShort();
            Log.v("RUFFY_KEY","display with sequence: "+(seq));
            display.addDisplayFrame(b,seq);
        }

        @Override
        public void modeError() {
            modeErrorCount++;

            if (modeErrorCount > Application.MODE_ERROR_TRESHHOLD) {
                stopRT();
                log("wrong mode, deactivate");

                modeErrorCount = 0;
                Application.sendAppCommand(Application.Command.DEACTIVATE_ALL, btConn);
            }
        }

        @Override
        public void sequenceError() {
            Application.sendAppCommand(Application.Command.RT_DEACTIVATE, btConn);
        }

        @Override
        public void error(short error, String desc) {
            switch (error)
            {
                case (short) 0xF056:
                    PumpData d = btConn.getPumpData();
                    btConn.disconnect();
                    btConn.connect(d,4);
                    break;
                default:
                    log(desc);
            }
        }

        @Override
        public void keySent(ByteBuffer b) {
            short seq = b.getShort();
            Log.v("RUFFY_KEY","key ack with sequence: "+(seq));
            for(IRTHandler h : new LinkedList<>(rtHandlers))
            {
                try {h.keySent(seq);}catch(Exception e){rtHandlers.remove(h);}
            }
        }
    };

    public void log(String s) {
        Log.d("Ruffy-log",s);
        for(IRTHandler handler : new LinkedList<>(rtHandlers))
        {
            try
            {
                handler.log(s);
            }catch(Exception e1)
            {
                rtHandlers.remove(handler);
            }
        }
    }

    public void fail(String s) {
        Log.e("Ruffy-fail",s);
        for(IRTHandler handler : new LinkedList<>(rtHandlers))
        {
            try
            {
                handler.fail(s);
            }catch(Exception e1)
            {
                rtHandlers.remove(handler);
            }
        }
    }

    private PacketHandler rtPacketHandler = new PacketHandler(){
        @Override
        public void sendImidiateAcknowledge(byte sequenceNumber) {
            Protocol.sendAck(sequenceNumber,btConn);
        }

        @Override
        public void log(String s) {
            Ruffy.this.log(s);
        }

        @Override
        public void handleResponse(Packet.Response response,boolean reliableFlagged, byte[] payload) {
            switch (response)
            {
                case ID:
                    Protocol.sendSyn(btConn);
                    break;
                case SYNC:
                    btConn.seqNo = 0x00;

                    if(step<201)
                        Application.sendAppConnect(btConn);
                    else
                    {
                        Application.sendAppDisconnect(btConn);
                        step = 300;
                    }
                    break;
                case UNRELIABLE_DATA:
                case RELIABLE_DATA:
                    Application.processAppResponse(payload, reliableFlagged, rtAppHandler);
                    break;
            }
        }

        @Override
        public void handleErrorResponse(byte errorCode, String errDecoded, boolean reliableFlagged, byte[] payload) {
            log(errDecoded);
        }

        @Override
        public Object getToDeviceKey() {
            return pumpData.getToDeviceKey();
        }
    };
}
