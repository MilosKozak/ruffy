package org.monkey.d.ruffy.ruffy;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Debug;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.monkey.d.ruffy.ruffy.driver.Application;
import org.monkey.d.ruffy.ruffy.driver.BTConnection;
import org.monkey.d.ruffy.ruffy.driver.BTHandler;
import org.monkey.d.ruffy.ruffy.driver.Frame;
import org.monkey.d.ruffy.ruffy.driver.Packet;
import org.monkey.d.ruffy.ruffy.driver.PumpDisplay;
import org.monkey.d.ruffy.ruffy.driver.Twofish_Algorithm;
import org.monkey.d.ruffy.ruffy.driver.Utils;
import org.monkey.d.ruffy.ruffy.driver.rtFrame;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static android.content.ContentValues.TAG;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainFragment extends Fragment implements View.OnClickListener {

    private TextView connectLog;

    //private byte recvSeqNo;

    private BTConnection btConn;
    private PumpDisplay display;
    private LinearLayout displayView;
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
                SharedPreferences prefs = getActivity().getSharedPreferences("pumpdata", Activity.MODE_PRIVATE);
                prefs.edit().putBoolean("paired",false).commit();
                synRun=false;
                rtmode=false;
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.container,new SetupFragment()).addToBackStack("Start").commit();
            }
        });

        connectLog = (TextView) v.findViewById(R.id.main_log);
        connectLog.setMovementMethod(new ScrollingMovementMethod());

        displayView = (LinearLayout) v.findViewById(R.id.pumpPanel);
        display = (PumpDisplay) displayView.findViewById(R.id.pumpView);

        Button menu = (Button) displayView.findViewById(R.id.pumpMenu);
        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                time = System.currentTimeMillis();
                synchronized (sem) {
                    rtSeq = Application.rtSendKey(Application.MENU, true, rtSeq, btConn);
                }
                sleep(100);
                synchronized (sem) {
                    rtSeq = Application.rtSendKey(Application.NO_KEY, true, rtSeq, btConn);
                }
            }
        });
        Button check = (Button) displayView.findViewById(R.id.pumpCheck);
        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                time = System.currentTimeMillis();
                synchronized (sem) {
                    rtSeq = Application.rtSendKey(Application.CHECK, true, rtSeq, btConn);
                }
                sleep(100);
                synchronized (sem) {
                    rtSeq = Application.rtSendKey(Application.NO_KEY, true, rtSeq, btConn);
                }
            }
        });
        Button up = (Button) displayView.findViewById(R.id.pumpUp);
        up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                time = System.currentTimeMillis();
                synchronized (sem) {
                    rtSeq = Application.rtSendKey(Application.UP, true, rtSeq, btConn);
                }
                sleep(100);
                synchronized (sem) {
                    rtSeq = Application.rtSendKey(Application.NO_KEY, true, rtSeq, btConn);
                }
            }
        });
        Button down= (Button) displayView.findViewById(R.id.pumpDown);
        down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                time = System.currentTimeMillis();
                synchronized (sem) {
                    rtSeq = Application.rtSendKey(Application.DOWN, true, rtSeq, btConn);
                }
                sleep(100);
                synchronized (sem) {
                    rtSeq = Application.rtSendKey(Application.NO_KEY, true, rtSeq, btConn);
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
                    if(s.equals("got error in read"))
                    {
                        btConn.connect(device,4);
                    }
                }

                @Override
                public void fail(String s) {
                    appendLog("failed: "+s);
                    synRun=false;
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
        Log.v("RUFFY_LOG", message);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(connectLog.getLineCount()<1000)
                {
                    connectLog.append("\n" + message);
                }
                else
                {
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

    public void handleRX(byte[] inBuf, int length, boolean rel) {
        if(length<10)
        {
            appendLog("to short package? "+length+" bytes: "+Utils.bufferString(inBuf,length));
        }
        //Byte command = (byte) (inBuf[1] & 0x1F);
        boolean expected = false;
        String descrip = "";

        ByteBuffer buffer = ByteBuffer.wrap(inBuf, 0, length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        ByteBuffer nBuf, pBuf, uBuf;

        byte[] nonce, payload, umac, paddedPacket, packetNoUmac;

        Byte version, command, addresses;
        version = buffer.get();                    //Get version and other data
        command = buffer.get();

        short payloadlength = buffer.getShort();

        addresses = buffer.get();

        nonce = new byte[13];                            //Copy buffers for nonce
        buffer.get(nonce, 0, nonce.length);
        nBuf = ByteBuffer.wrap(nonce);                    //Copy to ByteBuffers too for extracting data

        payload = new byte[payloadlength];                        //Payload
        buffer.get(payload, 0, payload.length);
        pBuf = ByteBuffer.wrap(payload);

        umac = new byte[8];                                //U-MAC
        buffer.get(umac, 0, umac.length);
        uBuf = ByteBuffer.wrap(umac);

        packetNoUmac = new byte[buffer.capacity() - umac.length];
        buffer.rewind();
        for (int i = 0; i < packetNoUmac.length; i++)
            packetNoUmac[i] = buffer.get();

        buffer.rewind();
//logging:
        /*appendLog(String.format("Version: %02X", version));
        appendLog(String.format("Command: %02X", command));
        appendLog(String.format("Length: %04X", payloadlength));
        appendLog(String.format("Address: %02X", addresses));

        String dat = "";
        for (byte b : nonce)
            dat += String.format("%02X ", b);
        appendLog("Nonce: " + dat);

        dat = "";
        for (byte b : payload)
            dat += String.format("%02X ", b);
        appendLog("Payload: " + dat);

        dat = "";
        for (byte b : umac)
            dat += String.format("%02X ", b);
        appendLog("UMAC: " + dat);

        dat = "";
        for (byte b : packetNoUmac)
            dat += String.format("%02X ", b);
        appendLog("Packet No UMAC: " + dat);

*/
        /*byte seq = 0x00;
        if ((inBuf[1] & 0x80) == 0x80)
            seq = (byte) 0x80;
        else
            seq = (byte) 0x00;
*/
        byte c = (byte)(command & 0x1F);
        switch (c) {
            case 0x11://key response?
                appendLog("key response: " );
                break;
            case 20:
                appendLog("got an id response");
                if (Utils.ccmVerify(packetNoUmac, btConn.pump_tf, umac, nonce)) {
                    byte[] device = new byte[13];

                    pBuf.order(ByteOrder.LITTLE_ENDIAN);
                    int serverId = pBuf.getInt();
                    pBuf.get(device);
                    String deviceId = new String(device);

                    appendLog("Server ID: " + String.format("%X", serverId) + " Device ID: " + deviceId);

                    try {
                        Protokoll.sendSyn(btConn);
                        appendLog("send Syn!");
                    }catch(Exception e) {
                        e.printStackTrace();
                        appendLog("failed to send Syn!");
                    }

                }
                break;
            case 24:
                appendLog("got a sync response ");
                if (Utils.ccmVerify(packetNoUmac, btConn.pump_tf, umac, nonce)) {
                    //if(getState() == Transport.P3_SYN_ACK) FIXME maybe later
                    {
                        btConn.seqNo = 0x00;

                        appendLog("Sequence Number reset!");
                        appendLog("parseRx >>> Sending APP_SEND_CONNECT!");

                        if(step<100)
                            Application.sendAppConnect(btConn);
                        else
                        {
                            Application.sendAppDisconnect(btConn);
                            step = 200;
                        }

                    }
                    /*else if(getState() == Transport.P3_SYN_DIS_RESP)
                    {
                        Debug.i(TAG, FUNC_TAG, "Resetting TX layer of pump after binding...");
                        setState(Transport.P3_APP_DISCONNECT);
                    }
                    else if(getState() == Transport.CM_SYN_RESP)
                    {
                        setState(Transport.CM_SYN_ACKD);
                    }*/
                }
                break;

            case 0x23: //recieved reliable data/
            case 0x03: //recieve unreliable data
                if (Utils.ccmVerify(packetNoUmac, btConn.pump_tf, umac, nonce)) {
                    MainFragment.this.processAppResponse(payload, rel);
                }
                break;

            case 5://ack response
                Utils.ccmVerify(packetNoUmac, btConn.pump_tf, umac, nonce);
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
                           // drv.a.startMode(Driver.COMMAND, true);
                            err = "Forcing starting of command mode, since the transport layer broke!";
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

    enum state {
        CONNECT,
        COMM_VER,
        RT_VER
    }

    ;

    private static state state;

    private void processAppResponse(byte[] payload, boolean reliable) {
        appendLog("processing app response");
        ByteBuffer b = ByteBuffer.wrap(payload);
        b.order(ByteOrder.LITTLE_ENDIAN);

        byte mVersion = b.get();
        byte servId = b.get();
        short commId = b.getShort();

        appendLog("Service ID: " + String.format("%X", servId) + " Comm ID: " + String.format("%X", commId) + " reliable: " + reliable);

        //time = System.currentTimeMillis();
        String descrip = null;
        if (reliable)                                            //If its a reliable packet the next 2 bytes are an error code to be evaluated
        {
            short error = b.getShort();
            if (!cmdProcessError(error)) {
                return;
            }

            switch (commId) {
                case (short) 0xA055://AL_CONNECT_RES:
                    Application.sendAppCommand(Application.RT_MODE,btConn);
                    break;
                case (short) 0xA065://AL_SERVICE_VERSION_RES:
                case (short) 0xA095://AL_BINDING_RES:
                    appendLog("not should happen here!");
                    break;
                case (short) 0xA066://AL_SERVICE_ACTIVATE_RES:
                    descrip = "AL_SERVICE_ACTIVATE_RES";
                    startRT();
                    break;
                case (short) 0x005A://AL_DISCONNECT_RES:
                    descrip = "AL_DISCONNECT_RES";
                    break;
                case (short) 0xA069://AL_SERVICE_DEACTIVATE_RES:
                    descrip = "AL_DEACTIVATE_RES";
                    rtmode=false;
                    Application.sendAppCommand(Application.RT_MODE,btConn);
                    break;
                case (short) 0x00AA://AL_SERVICE_ERROR_RES:
                    descrip = "AL_SERVICE_ERROR_RES";
                    break;
                case (short) 0xA06A://AL_SERVICE_DEACTIVATE_ALL_RES:
                    descrip = "AL_DEACTIVATE_ALL_RES";
                    rtmode=false;
                    Application.sendAppCommand(Application.RT_MODE,btConn);
                    //setAppState(Application.SERVICE_ACTIVATE);*///TODO
                    break;
                case (short) 0xAAAA://PING_RES:
                    descrip = "PING_RES";
                    new Thread(){
                        @Override
                        public void run() {
                            try{Thread.sleep(2000);}catch(Exception e){/*ignore*/}
                            Application.cmdPing(btConn);
                        }
                    }.start();
                    break;
                case (short) 0xA996://HIST_READ_RES:
                    descrip = "HISTORY_READ_RES";
                    // cmdProcessHistory(b);//TODO
                    break;
                case (short) 0xA999://HIST_CONF_RES:
                    descrip = "HISTORY_CONF_RES";

                    /*if(getBolusState() == BOLUS_CMD_HIST_CONF_RESP)//TODO
                    {
                        Debug.i(TAG, FUNC_TAG, "Bolus cycle complete, block confirmed, moving to idle state!");
                        setBolusState(BOLUS_CMD_IDLE);
                    }*/
                    break;
                case (short) 0xA695://CBOL_CANCEL_BOLUS:
                    descrip = "CBOL_CANCEL_BOLUS";
                    //cmdProcessCancelBolus(b);//TODO
                    break;
                case (short) 0xA66A://CBOL_BOLUS_STATUS:
                    descrip = "CBOL_BOLUS_STATUS";
                    //cmdProcessBolusStatus(b);//TODO
                    break;
                case (short) 0xA669://CBOL_BOLUS_DELIVER:
                    descrip = "CBOL_BOLUS_DELIVER";
                    //cmdProcessBolusDeliver(b);//TODO
                    break;
                case (short) 0xAA9A://READ_OP_STATUS_RESP:
                    descrip = "READ_OP_STATUS_RESP";
                    //cmdProcessOpStatus(b);//TODO
                    break;
                case (short) 0xAAA5://READ_ERR_STATUS_RESP:
                    descrip = "READ_ERR_STATUS_RESP";
                    cmdProcessErrStatus(b);
                    break;
                case (short) 0xAAA6://READ_TIME_RESP:
                    descrip = "READ_TIME_RESP";
                    //cmdProcessTime(b);//TODO
                    break;
                default:
                    descrip = "UNKNOWN";
                    break;
            }
        } else {
            switch (commId) {
                case (short) 0x0555://RT_DISPLAY:
                    descrip = "RT_DISPLAY";
                    rtProcessDisplay(b);
                    break;
                case (short) 0x0556://RT_KEY_CONF:
                    descrip = "RT_KEY_CONF";
                    //rtProcessKeyConfirmation(b);//TODO
                    break;
                case (short) 0x0559://RT_AUDIO:
                    descrip = "RT_AUDIO";
                    //rtProcessAudio(b);//TODO
                    break;
                case (short) 0x055A://RT_VIB:
                    descrip = "RT_VIB";
                    //rtProcessVibe(b);//TODO
                    break;
                case (short) 0x0566://RT_ALIVE:
                    descrip = "RT_ALIVE";
                    rtProcessAlive(b);
                    break;
                case (short) 0x0569://RT_PAUSE:
                    descrip = "RT_PAUSE";
                    break;
                case (short) 0x056A://RT_RELEASE:
                    descrip = "RT_RELEASE";
                    break;
                default:
                    descrip = "UNKNOWN";
                    break;
            }
        }
        appendLog("appProcess: "+descrip);
        //rx.add(commId); FIXME maybe later

    }
    private void rtProcessAlive(ByteBuffer b)
    {
        final String FUNC_TAG = "processAlive";

        short sequence = b.getShort();

        appendLog("alive Seq: "+sequence);
    }

    boolean rtmode = false;
    long time = 0;

    final Object sem = new Object();
    short rtSeq = 0;


    private void stopRT()
    {
        rtmode = false;
        rtSeq=0;
    }
    enum RTMODE
    {

    }

    int numStartRT = 0;
    private void startRT() {
        appendLog("starting RT keepAlive");
        new Thread(){
            @Override
            public void run() {
                rtmode = true;
                rtSeq = 0;
                time = System.currentTimeMillis();
                rtmode = true;
                numStartRT++;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connect.setText("Connected the "+numStartRT+". time!");
                    }
                });
                while(rtmode)
                {
                    if(System.currentTimeMillis() > time+1000L) {
                        appendLog("sending keep alive");
                        synchronized (sem) {
                            rtSeq = Application.sendRTKeepAlive(rtSeq, btConn);
                            time = System.currentTimeMillis();
                        }
                    }
                    try{Thread.sleep(500);}catch(Exception e){/*ignore*/}
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayView.setVisibility(View.GONE);
                    }
                });
            }
        }.start();
    }
    private static int MODE_ERROR_TRESHHOLD = 3;
    private int modeErrorCount = 0;

    private boolean cmdProcessError(short error) {
        final String FUNC_TAG = "processError";

        String sError = "Error > " + String.format("%X", error) + " ";

        if (error == 0x0000) {
            sError = "No error found!";
            //Debug.i(TAG, FUNC_TAG, sError);
            return true;
        } else {
            switch (error) {
                //Application Layer **********************************************//
                case (short) 0xF003:
                    sError = "Unknown Service ID, AL, RT, or CMD";
                    break;
                case (short) 0xF005:
                    sError = "Incompatible AL packet version";
                    break;
                case (short) 0xF006:
                    sError = "Invalid payload length";
                    break;
                case (short) 0xF056:
                    sError = "AL not connected";
                    //startMode(Driver.COMMAND, true);FIXME
                    break;

                case (short) 0xF059:
                    sError = "Incompatible service version";
                    break;
                case (short) 0xF05A:
                    sError = "Version, activate, deactivate request with unknown service ID";
                    break;
                case (short) 0xF05C:
                    sError = "Service activation not allowed";
                    break;
                case (short) 0xF05F:
                    sError = "Command not allowed, RT while in CMD mode, CMD while in RT mode";
                    modeErrorCount++;

                    if(modeErrorCount > MODE_ERROR_TRESHHOLD)
                    {
                        stopRT();
                        appendLog("The system is in the wrong mode, transitioning to COMMAND mode!");

                        modeErrorCount = 0;
                        Application.sendAppCommand(Application.DEACTIVATE_ALL,btConn);
                    }
                    break;

                //Remote Terminal ************************************************//
                case (short) 0xF503:
                    sError = "RT payload wrong length";
                    break;
                case (short) 0xF505:
                    sError = "RT display with incorrect row index, update, or display index";
                    break;
                case (short) 0xF506:
                    sError = "RT display timeout, obsolete";
                    break;
                case (short) 0xF509:
                    sError = "RT unknown audio sequence";
                    break;
                case (short) 0xF50A:
                    sError = "RT unknown vibra sequence";
                    break;
                case (short) 0xF50C:
                    sError = "RT command has incorrect sequence number";
                    Application.sendAppCommand(Application.RT_DEACTIVATE,btConn);
                    break;
                case (short) 0xF533:
                    sError = "RT alive timeout expired";
                    break;

                //Command Mode ***************************************************//
                case (short) 0xF605:
                    sError = "CBOL values not within threshold";
                    break;
                case (short) 0xF606:
                    sError = "CBOL wrong bolus type";
                    break;
                case (short) 0xF60A:
                    sError = "CBOL bolus not delivering";
                    break;
                case (short) 0xF60C:
                    sError = "History read EEPROM error";
                    break;
                case (short) 0xF633:
                    sError = "History confirm FRAM not readable or writeable";
                    break;
                case (short) 0xF635:
                    sError = "Unknown bolus type, obsolete";
                    break;
                case (short) 0xF636:
                    sError = "CBOL bolus is not available at the moment";
                    sError = "For some reason we are unable to currently process a bolus, resetting to command mode!";
                    break;
                case (short) 0xF639:
                    sError = "CBOL incorrect CRC value";
                    break;
                case (short) 0xF63A:
                    sError = "CBOL ch1 and ch2 values inconsistent";
                    break;
                case (short) 0xF63C:
                    sError = "CBOL pump has internal error (RAM values changed)";
                    break;
            }

            appendLog(sError);
            return false;
        }
    }



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

                byte recvSeqNo = seq;
                {
                    btConn.incrementNonceTx();

                    List<Byte> packet = Packet.buildPacket(new byte[]{16, 5, 0, 0, 0}, null, true,btConn);

                    //drv.recvSeqNo ^= 0x80;

                    seq = recvSeqNo;
                    packet.set(1, (byte) (packet.get(1) | recvSeqNo));                //OR the received sequence number

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
                }
            } else {
                rel = false;
            }
            handleRX(xx, x.size(), rel);
        }
    }

    private void cmdProcessErrStatus(ByteBuffer b)
    {
        final String FUNC_TAG = "processErrStatus";

        byte error = b.get();
        byte warn = b.get();

        boolean er = false, wn = false;

        if(error == (byte)0x48)
            appendLog("No Error");
        else
        {
            appendLog("Error found!");
            er = true;
        }

        if(warn == (byte)0x48)
            appendLog( "No warning/reminder!");
        else
        {
            appendLog("Warning/reminder found!");
            wn = true;
        }

        String message = "";

        if(wn && er)			//Both an error and warning!
        {
            message = "The pump has both a warning and an error message, please clear to continue...";
        }
        else if(wn && !er)		//A warning!
        {
            message = "The pump has a warning message, please clear to continue...";
        }
        else if(!wn && er)		//An error!
        {
            message = "The pump has an error message, please clear to continue...";
        }

        /*if(drv.cancelBolus)
        {
            drv.cancelBolus = false;
*/
            if(wn || er)
            {
                appendLog("Clearing warning as part of stopping TBR!");
                //FIXME setRtState(Application.TBR_CLEAR_WARNING);
            }
            else
            {
                appendLog("No warnings to clear...");
                //FIXME setRtState(Application.TBR_EVALUATE_MAIN_SCREEN);
            }
/*
            if(Driver.getMode() == Driver.COMMAND)					//We have to transition to RT
                startMode(Driver.RT, false);
        }
        else if(!message.equalsIgnoreCase(""))
        {
            Bundle bun = new Bundle();
            bun.putString("description", message);
            Event.addEvent(Driver.serv, Event.EVENT_PUMP_WARNING_ERROR, Event.makeJsonString(bun), Event.SET_POPUP_AUDIBLE_ALARM);
        }
        */
    }

    public static rtFrame frame = new rtFrame();


    private void rtProcessDisplay(ByteBuffer b)
    {
        final String FUNC_TAG = "rtProcessDisplay";

        short sequence = b.getShort();
        byte reason = b.get();
        int index = (int)(b.get() & 0xFF);
        byte row = b.get();

        byte[] map = new byte[96];		//New array
        b.get(map);						//Read in array from packet

        String r = "";					//Determine reason
        if(reason == (byte)0x48)
            r = "Pump";
        else if(reason == (byte)0xB7)
            r = "DM";

        appendLog("rtProcessDisplay: Seq: "+sequence+" | Reason: "+r+" | Index: "+index+" | Row: "+String.format("%X", row));

        String[] screen = new String[]{"","","","","","","",""};
        for(byte d:map)
        {
            if((d & 0x01) == 0x01)
                screen[0] += "8";
            else
                screen[0] += " ";

            if((d & 0x02) == 0x02)
                screen[1] += "8";
            else
                screen[1] += " ";

            if((d & 0x04) == 0x04)
                screen[2] += "8";
            else
                screen[2] += " ";

            if((d & 0x08) == 0x08)
                screen[3] += "8";
            else
                screen[3] += " ";

            if((d & 0x10) == 0x10)
                screen[4] += "8";
            else
                screen[4] += " ";

            if((d & 0x20) == 0x20)
                screen[5] += "8";
            else
                screen[5] += " ";

            if((d & 0x40) == 0x40)
                screen[6] += "8";
            else
                screen[6] += " ";

            if((d & 0x80) == 0x80)
                screen[7] += "8";
            else
                screen[7] += " ";
        }

        screen[0] = new StringBuffer(screen[0]).reverse().toString();
        screen[1] = new StringBuffer(screen[1]).reverse().toString();
        screen[2] = new StringBuffer(screen[2]).reverse().toString();
        screen[3] = new StringBuffer(screen[3]).reverse().toString();
        screen[4] = new StringBuffer(screen[4]).reverse().toString();
        screen[5] = new StringBuffer(screen[5]).reverse().toString();
        screen[6] = new StringBuffer(screen[6]).reverse().toString();
        screen[7] = new StringBuffer(screen[7]).reverse().toString();

        if(frame.index == -1)
        {
            appendLog(FUNC_TAG+ ": Brand new frame!");
            frame.index = index;
            frame.reason = reason;
        }
        else if(frame.index != index)
        {
            appendLog(FUNC_TAG+ ": Different index so we need to start a new frame!");
            frame = new rtFrame();		//Generate a new frame
            frame.index = index;			//Copy the index
            frame.reason = reason;
        }

        if(row == (byte)0x47)		//Row 1
            frame.addR1(screen);
        else if(row == (byte)0x48)	//Row 2
            frame.addR2(screen);
        else if(row == (byte)0xB7)	//Row 3
            frame.addR3(screen);
        else if(row == (byte)0xB8)	//Row 4
            frame.addR4(screen);

        if(frame.isComplete())
        {
            //final String TAG = "rtFrame";

            appendLog(FUNC_TAG+ ": Found complete frame!");

            /*int i = 0;
            appendLog(FUNC_TAG+ ": ----------------------------------------------------------------------------------------------------");
            for(i = 0;i<8;i++)
                appendLog(FUNC_TAG+ ": "+ frame.row1[i]);
            //appendLog(FUNC_TAG+ ": ----------------------------------------------------------------------------------------------------");
            for(i = 0;i<8;i++)
                appendLog(FUNC_TAG+ ": "+ frame.row2[i]);
            //appendLog(FUNC_TAG+ ": ----------------------------------------------------------------------------------------------------");
            for(i = 0;i<8;i++)
                appendLog(FUNC_TAG+ ": "+ frame.row3[i]);
            //appendLog(FUNC_TAG+ ": ----------------------------------------------------------------------------------------------------");
            for(i = 0;i<8;i++)
                appendLog(FUNC_TAG+ ": "+ frame.row4[i]);
            appendLog(FUNC_TAG+ ": ----------------------------------------------------------------------------------------------------");
*/
            //rtTbrFSM();
            if(displayView.getVisibility()==View.GONE)
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayView.setVisibility(View.VISIBLE);
                    }
                });

            display.draw(frame);
        }
    }
}
