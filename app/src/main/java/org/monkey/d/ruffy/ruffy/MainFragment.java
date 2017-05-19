package org.monkey.d.ruffy.ruffy;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.monkey.d.ruffy.ruffy.driver.AppHandler;
import org.monkey.d.ruffy.ruffy.driver.Application;
import org.monkey.d.ruffy.ruffy.driver.BTConnection;
import org.monkey.d.ruffy.ruffy.driver.BTHandler;
import org.monkey.d.ruffy.ruffy.driver.Frame;
import org.monkey.d.ruffy.ruffy.driver.Packet;
import org.monkey.d.ruffy.ruffy.view.PumpDisplayView;
import org.monkey.d.ruffy.ruffy.driver.Twofish_Algorithm;
import org.monkey.d.ruffy.ruffy.driver.Utils;
import org.monkey.d.ruffy.ruffy.driver.Display;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

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

    public MainFragment() {

    }

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
        up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lastRtMessageSent = System.currentTimeMillis();
                synchronized (rtSequenceSemaphore) {
                    rtSequence = Application.rtSendKey(Application.UP, true, rtSequence, btConn);
                }
                sleep(100);
                synchronized (rtSequenceSemaphore) {
                    rtSequence = Application.rtSendKey(Application.NO_KEY, true, rtSequence, btConn);
                }
            }
        });
        Button down= (Button) displayLayout.findViewById(R.id.pumpDown);
        down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lastRtMessageSent = System.currentTimeMillis();
                synchronized (rtSequenceSemaphore) {
                    rtSequence = Application.rtSendKey(Application.DOWN, true, rtSequence, btConn);
                }
                sleep(100);
                synchronized (rtSequenceSemaphore) {
                    rtSequence = Application.rtSendKey(Application.NO_KEY, true, rtSequence, btConn);
                }
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
    BluetoothDevice pairingDevice;
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

        SharedPreferences prefs = getActivity().getSharedPreferences("pumpdata", Activity.MODE_PRIVATE);
        String dp = prefs.getString("dp",null);
        String pd = prefs.getString("pd",null);
        final String device = prefs.getString("device",null);

        connectLog.setText("Starting connection to Pump "+device);

        if(device != null)
        {
            btConn = new BTConnection(getActivity(), new BTHandler() {
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
                        btConn.connect(device,4);
                    }
                }

                @Override
                public void fail(String s) {
                    appendLog("failed: "+s);
                    synRun=false;
                    if(step < 200)
                        btConn.connect(device,4);
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
                    handleData(buffer,bytes);
                }
            });

            try {
                btConn.pump_tf = Twofish_Algorithm.makeKey(Utils.hexStringToByteArray(pd));
                btConn.driver_tf = Twofish_Algorithm.makeKey(Utils.hexStringToByteArray(dp));
            } catch(Exception e)
            {
                e.printStackTrace();
                appendLog("unable to load keys!");
                return;
            }

            btConn.connect(device,10);
        }
    }

    private void appendLog(final String message) {
        if(connectLog.getVisibility()!=View.GONE) {
            Log.v("RUFFY_LOG", message);
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

    public void handleRX(byte[] inBuf, int length, boolean rel) {

        ByteBuffer buffer = ByteBuffer.wrap(inBuf, 0, length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        byte[] nonce, payload, umac, packetNoUmac;

        Byte command;
        buffer.get(); //ignore
        command = buffer.get();

        short payloadlength = buffer.getShort();

        buffer.get(); //ignore

        nonce = new byte[13];
        buffer.get(nonce, 0, nonce.length);

        payload = new byte[payloadlength];
        buffer.get(payload, 0, payload.length);

        umac = new byte[8];
        buffer.get(umac, 0, umac.length);

        packetNoUmac = new byte[buffer.capacity() - umac.length];
        buffer.rewind();
        for (int i = 0; i < packetNoUmac.length; i++)
            packetNoUmac[i] = buffer.get();

        buffer.rewind();

        byte c = (byte)(command & 0x1F);
        switch (c) {
            case 20:
                appendLog("got an id response");
                if (Utils.ccmVerify(packetNoUmac, btConn.pump_tf, umac, nonce)) {
                    Protokoll.sendSyn(btConn);
                }
                break;
            case 24:
                appendLog("got a sync response ");
                if (Utils.ccmVerify(packetNoUmac, btConn.pump_tf, umac, nonce)) {
                    btConn.seqNo = 0x00;

                    if(step<100)
                        Application.sendAppConnect(btConn);
                    else
                    {
                        Application.sendAppDisconnect(btConn);
                        step = 200;
                    }
                }
                break;

            case 0x23:
            case 0x03:
                if (Utils.ccmVerify(packetNoUmac, btConn.pump_tf, umac, nonce)) {
                    Application.processAppResponse(payload, rel, new AppHandler() {
                        @Override
                        public void log(String s) {
                            appendLog(s);
                        }

                        @Override
                        public void connected() {
                            Application.sendAppCommand(Application.Command.RT_MODE,btConn);
                        }

                        @Override
                        public void rtModeActivated() {
                            startRT();
                        }

                        @Override
                        public void modeDeactivated() {
                            rtModeRunning =false;
                            Application.sendAppCommand(Application.Command.RT_MODE,btConn);
                        }

                        @Override
                        public void addDisplayFrame(ByteBuffer b) {
                            display.addDisplayFrame(b);
                            if(connectLog.getVisibility()!= View.GONE)
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

                            if(modeErrorCount > Application.MODE_ERROR_TRESHHOLD)
                            {
                                stopRT();
                                appendLog("wrong mode, deactivate");

                                modeErrorCount=0;
                                Application.sendAppCommand(Application.Command.DEACTIVATE_ALL,btConn);
                            }
                        }

                        @Override
                        public void sequenceError() {
                            Application.sendAppCommand(Application.Command.RT_DEACTIVATE,btConn);
                        }
                    });
                }
                break;

            case 0x06:
                if(Utils.ccmVerify(packetNoUmac, btConn.pump_tf, umac, nonce))
                {
                    byte error = 0;
                    String err = "";

                    if(payload.length > 0)
                        error = payload[0];

                    switch(error)
                    {
                        case 0x00:
                            err = "Undefined";
                            break;
                        case 0x0F:
                            err = "Wrong state";
                            break;
                        case 0x33:
                            err = "Invalid service primitive";
                            break;
                        case 0x3C:
                            err = "Invalid payload length";
                            break;
                        case 0x55:
                            err = "Invalid source address";
                            break;
                        case 0x66:
                            err = "Invalid destination address";
                            break;
                    }

                    appendLog( "Error in Transport Layer! ("+err+")");

                }
        break;
            default:
                appendLog("not yet implemented rx command: " + command + " ( " + String.format("%02X", command));

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

    void handleData(byte buffer[], int bytes) {
        List<Byte> t = new ArrayList<>();
        for (int i = 0; i < bytes; i++)
            t.add(buffer[i]);
        for (List<Byte> x : Frame.frameDeEscaping(t)) {
            byte[] xx = new byte[x.size()];
            for (int i = 0; i < x.size(); i++)
                xx[i] = x.get(i);
            boolean rel = false;
            if (x.size()>1 && (x.get(1) & 0x20) == 0x20) {
                rel = true;

                byte seq = 0x00;
                if ((x.get(1) & 0x80) == 0x80)
                    seq = (byte) 0x80;

                btConn.incrementNonceTx();

                List<Byte> packet = Packet.buildPacket(new byte[]{16, 5, 0, 0, 0}, null, true,btConn);

                packet.set(1, (byte) (packet.get(1) | seq));

                packet = Utils.ccmAuthenticate(packet, btConn.driver_tf, btConn.getNonceTx());

                List<Byte> temp = Frame.frameEscape(packet);
                byte[] ro = new byte[temp.size()];
                int i = 0;
                for (byte b : temp)
                    ro[i++] = b;
                try {
                    btConn.write(ro);
                    appendLog(this.getId() + ": succesful wrote " + temp.size() + " bytes!");
                } catch (Exception e) {
                    e.printStackTrace();
                    appendLog(this.getId() + ": error in tx: " + e.getMessage());
                }
            } else {
                rel = false;
            }
            handleRX(xx, x.size(), rel);
        }
    }

}
