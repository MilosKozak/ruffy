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
            Ruffy.this.rtHandlers.add(handler);
        }

        @Override
        public void removeHandler(IRTHandler handler) throws RemoteException {
            Ruffy.this.rtHandlers.remove(handler);
        }

        @Override
        public int doRTConnect() throws RemoteException {
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
                btConn.connect(pumpData, 10);
                return 0;
            }
            return -1;
        }

        public void doRTDisconnect()
        {
            step = 200;
            stopRT();
            btConn.disconnect();
        }

        public void rtSendKey(byte keyCode, boolean changed)
        {
            lastRtMessageSent = System.currentTimeMillis();
            synchronized (rtSequenceSemaphore) {
                rtSequence = Application.rtSendKey(keyCode, changed, rtSequence, btConn);
            }
        }

        public void resetPairing()
        {
            SharedPreferences prefs = Ruffy.this.getSharedPreferences("pumpdata", Activity.MODE_PRIVATE);
            prefs.edit().putBoolean("paired",false).apply();
            synRun=false;
            rtModeRunning =false;
        }

        public boolean isConnected()
        {
            if (btConn!=null) {
                return btConn.isConnected();
            } else {
                return false;
            }
        }

        @Override
        public boolean isBoundToPump() throws RemoteException {
            return PumpData.isPumpBound(Ruffy.this);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v("Ruffy","++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        Log.v("Ruffy","got ounbind");
        Log.v("Ruffy","++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        return true;
    }

    @Override
    public void onDestroy() {
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
            step=0;
            Packet.handleRawData(buffer,bytes, rtPacketHandler);
        }

        @Override
        public void requestBlueTooth() {
            for(IRTHandler handler : new LinkedList<>(rtHandlers))
            {
                try
                {
                    handler.requestBluetooth();
                }catch(RemoteException e1)
                {
                    rtHandlers.remove(handler);
                }
            }
        }
    };

    private void stopRT()
    {
        rtModeRunning = false;
        rtSequence =0;
    }

    private void startRT() {
        log("starting RT keepAlive");
        new Thread(){
            @Override
            public void run() {
                rtModeRunning = true;
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
                                }catch(RemoteException e1)
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
                                }catch(RemoteException e1)
                                {
                                    rtHandlers.remove(handler);
                                }
                            }
                        }
                    });
                    display.setCompletDisplayHandler(new CompleteDisplayHandler() {
                        @Override
                        public void handleCompleteFrame(byte[][] pixels) {
                            DisplayParser.findMenu(pixels, new DisplayParserHandler() {
                                @Override
                                public void menuFound(Menu menu) {
                                    for(IRTHandler handler : new LinkedList<>(rtHandlers))
                                    {
                                        try
                                        {
                                            handler.rtDisplayHandleMenu(menu);
                                        }catch(RemoteException e1)
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
                                            handler.rtDisplayHandleNoMenu();
                                        }catch(RemoteException e1)
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
                        }catch(RemoteException e1)
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
                for(IRTHandler handler : new LinkedList<>(rtHandlers))
                {
                    try
                    {
                        handler.rtStopped();
                    }catch(RemoteException e1)
                    {
                        rtHandlers.remove(handler);
                    }
                }

            }
        }.start();
    }

    private Runnable synThread = new Runnable(){
        @Override
        public void run() {
            while(synRun)
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
            display.addDisplayFrame(b);
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
    };

    public void log(String s) {
        for(IRTHandler handler : new LinkedList<>(rtHandlers))
        {
            try
            {
                handler.log(s);
            }catch(RemoteException e1)
            {
                rtHandlers.remove(handler);
            }
        }
    }

    public void fail(String s) {
        for(IRTHandler handler : new LinkedList<>(rtHandlers))
        {
            try
            {
                handler.fail(s);
            }catch(RemoteException e1)
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

                    if(step<100)
                        Application.sendAppConnect(btConn);
                    else
                    {
                        Application.sendAppDisconnect(btConn);
                        step = 200;
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
