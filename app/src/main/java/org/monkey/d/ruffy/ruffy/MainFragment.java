package org.monkey.d.ruffy.ruffy;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.monkey.d.ruffy.ruffy.driver.AppHandler;
import org.monkey.d.ruffy.ruffy.driver.Application;
import org.monkey.d.ruffy.ruffy.driver.BTConnection;
import org.monkey.d.ruffy.ruffy.driver.BTHandler;
import org.monkey.d.ruffy.ruffy.driver.CompleteDisplayHandler;
import org.monkey.d.ruffy.ruffy.driver.LogHandler;
import org.monkey.d.ruffy.ruffy.driver.Packet;
import org.monkey.d.ruffy.ruffy.driver.PacketHandler;
import org.monkey.d.ruffy.ruffy.driver.Protokoll;
import org.monkey.d.ruffy.ruffy.driver.PumpData;
import org.monkey.d.ruffy.ruffy.driver.display.DisplayParser;
import org.monkey.d.ruffy.ruffy.driver.display.Menu;
import org.monkey.d.ruffy.ruffy.view.PumpDisplayView;
import org.monkey.d.ruffy.ruffy.driver.Display;

import java.nio.ByteBuffer;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainFragment extends Fragment implements View.OnClickListener {

    private TextView connectLog;

    private BTConnection btConn;
    private PumpDisplayView displayView;
    private LinearLayout displayLayout;
    private Display display;
    private Button connect;
    private PumpData pumpData;
    private TextView frameCounter;

    public MainFragment() {

    }

    private int upRunning = 0;
    private Thread upThread = new Thread()
    {
        @Override
        public void run() {
            while(upRunning >0)
            {
                if(upRunning==1) {
                    lastRtMessageSent = System.currentTimeMillis();
                    upRunning++;
                    synchronized (rtSequenceSemaphore) {
                        rtSequence = Application.rtSendKey(Application.UP, true, rtSequence, btConn);
                    }
                }
                else
                {
                    lastRtMessageSent = System.currentTimeMillis();
                    synchronized (rtSequenceSemaphore) {
                        rtSequence = Application.rtSendKey(Application.UP, false, rtSequence, btConn);
                    }
                }
                try{sleep(200);}catch(Exception e){}
            }
            synchronized (rtSequenceSemaphore) {
                rtSequence = Application.rtSendKey(Application.NO_KEY, true, rtSequence, btConn);
            }
        }
    };
    private int downRunning = 0;
    private Thread downThread = new Thread()
    {
        @Override
        public void run() {
            while(downRunning >0)
            {
                if(downRunning==1) {
                    lastRtMessageSent = System.currentTimeMillis();
                    downRunning++;
                    synchronized (rtSequenceSemaphore) {
                        rtSequence = Application.rtSendKey(Application.DOWN, true, rtSequence, btConn);
                    }
                }
                else
                {
                    lastRtMessageSent = System.currentTimeMillis();
                    synchronized (rtSequenceSemaphore) {
                        rtSequence = Application.rtSendKey(Application.DOWN, false, rtSequence, btConn);
                    }
                }
                try{sleep(200);}catch(Exception e){}
            }
            synchronized (rtSequenceSemaphore) {
                rtSequence = Application.rtSendKey(Application.NO_KEY, true, rtSequence, btConn);
            }
        }
    };
    private void sleep(long millis)
    {
        try{Thread.sleep(millis);}catch(Exception e){/*ignore*/}
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main, container, false);
        connect = (Button) v.findViewById(R.id.main_connect);
        connect.setOnClickListener(this);

        Button reset = (Button) v.findViewById(R.id.main_reset);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getContext()).setTitle("remove bonding?").setMessage("Really delete bonding informations with pump?").setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences prefs = getActivity().getSharedPreferences("pumpdata", Activity.MODE_PRIVATE);
                        prefs.edit().putBoolean("paired",false).apply();
                        synRun=false;
                        rtModeRunning =false;
                        getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.container,new SetupFragment()).addToBackStack("Start").commit();

                    }
                }).setNegativeButton("NO",null).show();
            }
        });

        connectLog = (TextView) v.findViewById(R.id.main_log);
        connectLog.setMovementMethod(new ScrollingMovementMethod());

        displayLayout= (LinearLayout) v.findViewById(R.id.pumpPanel);
        displayView = (PumpDisplayView) displayLayout.findViewById(R.id.pumpView);

        display = new Display(displayView);
        
        frameCounter = (TextView) v.findViewById(R.id.frameCounter);
        display.setCompletDisplayHandler(displayCompletHandle);
        Button menu = (Button) displayLayout.findViewById(R.id.pumpMenu);
        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lastRtMessageSent = System.currentTimeMillis();
                synchronized (rtSequenceSemaphore) {
                    rtSequence = Application.rtSendKey(Application.MENU, true, rtSequence, btConn);
                }
                sleep(100);
                synchronized (rtSequenceSemaphore) {
                    rtSequence = Application.rtSendKey(Application.NO_KEY, true, rtSequence, btConn);
                }
            }
        });
        Button check = (Button) displayLayout.findViewById(R.id.pumpCheck);
        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lastRtMessageSent = System.currentTimeMillis();
                synchronized (rtSequenceSemaphore) {
                    rtSequence = Application.rtSendKey(Application.CHECK, true, rtSequence, btConn);
                }
                sleep(100);
                synchronized (rtSequenceSemaphore) {
                    rtSequence = Application.rtSendKey(Application.NO_KEY, true, rtSequence, btConn);
                }
            }
        });
        Button up = (Button) displayLayout.findViewById(R.id.pumpUp);
        up.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        upRunning=1;
                        upThread.start();
                    break;

                    case MotionEvent.ACTION_UP:
                        upRunning=0;
                        break;
                }

                return false;
            }
        });
        Button down= (Button) displayLayout.findViewById(R.id.pumpDown);
        down.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        downRunning=1;
                        downThread.start();
                        break;

                    case MotionEvent.ACTION_UP:
                        downRunning=0;
                        break;
                }

                return false;
            }
        });
        return v;
    }

    boolean synRun= true;
    Thread synThread = new Thread(){
        @Override
        public void run() {
            while(synRun)
            {
                Protokoll.sendSyn(btConn);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    BTHandler rtBTHandler = new BTHandler() {
        @Override
        public void deviceConnected() {
            appendLog("connected to pump");
            synRun=true;
            synThread.start();
        }

        @Override
        public void log(String s) {
            appendLog(s);
            if(s.equals("got error in read") && step < 200)
            {
                btConn.connect(pumpData,4);
            }
        }

        @Override
        public void fail(String s) {
            appendLog("failed: "+s);
            synRun=false;
            if(step < 200)
                btConn.connect(pumpData,4);
            else
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connect.setText("Try Connect again!");
                        connect.setEnabled(true);
                    }
                });
        }

        @Override
        public void deviceFound(BluetoothDevice bd) {
            appendLog("not be here!?!");
        }

        @Override
        public void handleRawData(byte[] buffer, int bytes) {
            appendLog("got data from pump");
            synRun=false;
            step=0;
            Packet.handleRawData(buffer,bytes, rtPacketHandler);
        }

        @Override
        public void requestBlueTooth() {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            getActivity().startActivityForResult(enableBtIntent, 1);
        }
    };

    AppHandler rtAppHandler = new AppHandler() {
        @Override
        public void log(String s) {
            appendLog(s);
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
            if (connectLog.getVisibility() != View.GONE)
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectLog.setVisibility(View.GONE);
                    }
                });
        }

        @Override
        public void modeError() {
            modeErrorCount++;

            if (modeErrorCount > Application.MODE_ERROR_TRESHHOLD) {
                stopRT();
                appendLog("wrong mode, deactivate");

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
                    appendLog(desc);
            }
        }
    };
    PacketHandler rtPacketHandler = new PacketHandler(){
        @Override
        public void sendImidiateAcknowledge(byte sequenceNumber) {
            Protokoll.sendAck(sequenceNumber,btConn);
        }

        @Override
        public void log(String s) {
            appendLog(s);
        }

        @Override
        public void handleResponse(Packet.Response response,boolean reliableFlagged, byte[] payload) {
            switch (response)
            {
                case ID:
                    Protokoll.sendSyn(btConn);
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
            appendLog(errDecoded);
        }

        @Override
        public Object getToDeviceKey() {
            return pumpData.getToDeviceKey();
        }
    };

    private LogHandler logHandler = new LogHandler() {
        @Override
        public void log(String s) {
            appendLog(s);
        }

        @Override
        public void fail(String s) {
            appendLog(s);
        }
    };

    CompleteDisplayHandler displayCompletHandle = new CompleteDisplayHandler() {
        @Override
        public void handleCompleteFrame(byte[][] display) {
            appendLog("got display");

            final Menu menu = DisplayParser.findMenu(display);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(menu!=null)
                        {
                            String ats = "";
                            for(String k : menu.attributes())
                            {
                                ats+="\n"+k+": "+menu.getAttribute(k);
                            }
                            frameCounter.setText("found menu: "+menu.getName()+ ats);
                        }
                        else
                        {
                            frameCounter.setText("menu not recognized");
                        }
                    }
                });

        }
    };
    @Override
    public void onClick(final View view) {
        if(connect.getText().toString().startsWith("Disco"))
        {
            step = 200;
            stopRT();
            btConn.disconnect();
            connect.setText("Connect!");
            return;
        }
        step= 0;
        view.setEnabled(false);

        pumpData = PumpData.loadPump(getActivity(),logHandler);
        if(pumpData != null) {
            btConn = new BTConnection(rtBTHandler);
            btConn.connect(pumpData, 10);
        }

    }

    private void appendLog(final String message) {
        Log.v("RUFFY_LOG", message);
        if(connectLog.getVisibility()!=View.GONE) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (connectLog.getLineCount() < 1000) {
                        connectLog.append("\n" + message);
                    } else {
                        connectLog.setText("");
                    }
                    final int scrollAmount = connectLog.getLayout().getLineTop(connectLog.getLineCount()) - connectLog.getHeight();
                    if (scrollAmount > 0)
                        connectLog.scrollTo(0, scrollAmount);
                    else
                        connectLog.scrollTo(0, 0);
                }
            });
        }
    }

    private boolean rtModeRunning = false;
    private long lastRtMessageSent = 0;

    private final Object rtSequenceSemaphore = new Object();
    private short rtSequence = 0;


    private void stopRT()
    {
        rtModeRunning = false;
        rtSequence =0;
    }

    private void startRT() {
        appendLog("starting RT keepAlive");
        new Thread(){
            @Override
            public void run() {
                rtModeRunning = true;
                rtSequence = 0;
                lastRtMessageSent = System.currentTimeMillis();
                rtModeRunning = true;
                display.clear();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connect.setText("Disconnect");
                        connect.setEnabled(true);
                    }
                });
                while(rtModeRunning)
                {
                    if(System.currentTimeMillis() > lastRtMessageSent +1000L) {
                        appendLog("sending keep alive");
                        synchronized (rtSequenceSemaphore) {
                            rtSequence = Application.sendRTKeepAlive(rtSequence, btConn);
                            lastRtMessageSent = System.currentTimeMillis();
                        }
                    }
                    try{Thread.sleep(500);}catch(Exception e){/*ignore*/}
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectLog.setVisibility(View.VISIBLE);
                    }
                });
            }
        }.start();
    }
    private int modeErrorCount = 0;

    int step = 0;



}
