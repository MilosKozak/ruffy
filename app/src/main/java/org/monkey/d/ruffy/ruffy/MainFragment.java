package org.monkey.d.ruffy.ruffy;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.monkey.d.ruffy.ruffy.driver.Application;
import org.monkey.d.ruffy.ruffy.driver.BTConnection;
import org.monkey.d.ruffy.ruffy.driver.BTHandler;
import org.monkey.d.ruffy.ruffy.driver.Frame;
import org.monkey.d.ruffy.ruffy.driver.Packet;
import org.monkey.d.ruffy.ruffy.driver.Twofish_Algorithm;
import org.monkey.d.ruffy.ruffy.driver.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainFragment extends Fragment implements View.OnClickListener {

    private TextView connectLog;

    //private byte recvSeqNo;

    private BTConnection btConn;

    public MainFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main, container, false);
        Button connect = (Button) v.findViewById(R.id.main_connect);
        connect.setOnClickListener(this);

        Button reset = (Button) v.findViewById(R.id.main_reset);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getActivity().getSharedPreferences("pumpdata", Activity.MODE_PRIVATE);
                prefs.edit().putBoolean("paired",false).commit();

                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.container,new SetupFragment()).addToBackStack("Start").commit();
            }
        });

        connectLog = (TextView) v.findViewById(R.id.main_log);
        connectLog.setMovementMethod(new ScrollingMovementMethod());

        return v;
    }
    boolean r = true;
    Thread t = new Thread(){
        @Override
        public void run() {
            while(r)
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
                    r=true;
                    t.start();


                }

                @Override
                public void log(String s) {
                    appendLog(s);
                }

                @Override
                public void fail(String s) {
                    appendLog("failed: "+s);
                    r=false;
                    btConn.connect(device,4);
                }

                @Override
                public void deviceFound(BluetoothDevice bd) {
                    appendLog("not be here!?!");
                }

                @Override
                public void handleRawData(byte[] buffer, int bytes) {
                    appendLog("got data from pump");
                    r=false;
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
                connectLog.append("\n" + message);
                final int scrollAmount = connectLog.getLayout().getLineTop(connectLog.getLineCount()) - connectLog.getHeight();
                if (scrollAmount > 0)
                    connectLog.scrollTo(0, scrollAmount);
                else
                    connectLog.scrollTo(0, 0);
            }
        });

    }

    public void handleRX(byte[] inBuf, int length, boolean rel) {
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
        appendLog(String.format("Version: %02X", version));
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
                            Application.sendAppDisconnect(btConn);

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

        String descrip = null;
        if (reliable)                                            //If its a reliable packet the next 2 bytes are an error code to be evaluated
        {
            short error = b.getShort();
            if (!cmdProcessError(error)) {
                return;
            }

            switch (commId) {
                case (short) 0xA055://AL_CONNECT_RES:
                    Application.sendAppCommand(Application.COMMAND_MODE,btConn);
                    break;
                case (short) 0xA065://AL_SERVICE_VERSION_RES:
                case (short) 0xA095://AL_BINDING_RES:
                    appendLog("not should happen here!");
                    break;
                case (short) 0xA066://AL_SERVICE_ACTIVATE_RES:
                    descrip = "AL_SERVICE_ACTIVATE_RES";
                    Application.cmdPing(btConn);
                    break;
                case (short) 0x005A://AL_DISCONNECT_RES:
                    descrip = "AL_DISCONNECT_RES";
                    break;
                case (short) 0xA069://AL_SERVICE_DEACTIVATE_RES:
                    descrip = "AL_DEACTIVATE_RES";
                    break;
                case (short) 0x00AA://AL_SERVICE_ERROR_RES:
                    descrip = "AL_SERVICE_ERROR_RES";
                    break;
                case (short) 0xA06A://AL_SERVICE_DEACTIVATE_ALL_RES:
                    descrip = "AL_DEACTIVATE_ALL_RES";
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
                    //cmdProcessErrStatus(b);//TODO
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
                    //rtProcessDisplay(b);//TODO
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
                    //rtProcessAlive(b);//TODO
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
                    //modeErrorCount++;

                    /*if(modeErrorCount > MODE_ERROR_THRESH)
                    {
                        Debug.e(TAG, FUNC_TAG, "The system is in the wrong mode, transitioning to COMMAND mode!");
                        Driver.log("ROCHE", FUNC_TAG, "The system is in the wrong mode, transitioning to COMMAND mode!");

                        modeErrorCount = 0;
                        startMode(Driver.COMMAND, false);
                    }*/
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
            if ((x.get(1) & 0x20) == 0x20) {
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

}
