package org.monkey.d.ruffy.ruffy;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import org.monkey.d.ruffy.ruffy.driver.Protokoll;
import org.monkey.d.ruffy.ruffy.driver.Twofish_Algorithm;
import org.monkey.d.ruffy.ruffy.driver.Utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class SetupFragment extends Fragment implements View.OnClickListener {

    private TextView connectLog;

    private BTConnection btConn;

    public SetupFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_setup, container, false);
        Button connect = (Button) v.findViewById(R.id.setup_connect);
        connect.setOnClickListener(this);

        connectLog = (TextView) v.findViewById(R.id.setup_log);
        connectLog.setMovementMethod(new ScrollingMovementMethod());

        return v;
    }

    BluetoothDevice pairingDevice;
    @Override
    public void onClick(final View view) {
        view.setEnabled(false);
        connectLog.setText("Starting rfcomm to wait for Pump connection…");


        /*int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText("}gZ='GD?gj2r|B}>");
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("text label", "}gZ='GD?gj2r|B}>");
            clipboard.setPrimaryClip(clip);
        }*/

        btConn = new BTConnection(new BTHandler() {
            BluetoothDevice device;

            @Override
            public void deviceConnected() {
                appendLog("connected to device ");
                pairingDevice = device;
                appendLog("initiate pairing…");
                //FIXME move
                byte[] key = {16,9,2,0,-16};
                step = 1;
                btConn.writeCommand(key);
            }

            @Override
            public void log(String s) {
                if(step == 200 && s.equals("got error in read"))
                {
                    getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.container,new MainFragment()).addToBackStack("Start").commit();
                    return;
                }
                appendLog(s);
            }

            @Override
            public void fail(String s) {
                appendLog(s);
                if(step == 1)//trying to connect
                {
                    appendLog("retrying to connect!");
                    btConn.connect(pairingDevice);
                }
            }

            @Override
            public void deviceFound(BluetoothDevice device) {
                if (this.device == null) {
                    this.device = device;
                    appendLog("found device first time " + device + " waiting for next");
                } else if (this.device.getAddress().equals(device.getAddress())) {
                    pairingDevice = device;
                    btConn.connect(device);
                } else {
                    this.device = device;
                    appendLog("found device first time " + device + " waiting for next");
                }
            }

            @Override
            public void handleRawData(byte[] buffer, int bytes) {
                handleData(buffer,bytes);
            }

            @Override
            public void requestBlueTooth() {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                getActivity().startActivityForResult(enableBtIntent, 1);
            }
        });
        btConn.makeDiscoverable(getActivity());
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

    private byte[] pin;

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
                try {
                    Object tf = Twofish_Algorithm.makeKey(pin);
                    btConn.getPumpData().setAndSaveAddress((byte) ((addresses << 4) & 0xF0));        //Get the address and reverse it since source and destination are reversed from the RX packet

                    byte[] key_pd = new byte[16];                            //Get the bytes for the keys
                    byte[] key_dp = new byte[16];

                    pBuf.rewind();
                    pBuf.get(key_pd, 0, key_pd.length);
                    pBuf.get(key_dp, 0, key_dp.length);

                    String d = "";
                    for (byte b : key_pd)
                        d += String.format("%02X ", b);
                    appendLog("parseRx >>> Key_PD: " + d);

                    d = "";
                    for (byte b : key_dp)
                        d += String.format("%02X ", b);
                    appendLog("parseRx >>> Key_DP: " + d);


                    btConn.getPumpData().setAndSaveToDeviceKey(key_pd,tf);
                    btConn.getPumpData().setAndSaveToPumpKey(key_dp,tf);
                    btConn.getPumpData().setAndSavePumpMac(pairingDevice.getAddress());
                    Protokoll.sendIDReq(btConn);
                } catch (Exception e) {
                    e.printStackTrace();
                    appendLog("failed inRX: " + e.getMessage());
                }
                break;
            case 20:
                appendLog("got an id response");
                if (Utils.ccmVerify(packetNoUmac, btConn.getPumpData().getToDeviceKey(), umac, nonce)) {
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
                if (Utils.ccmVerify(packetNoUmac, btConn.getPumpData().getToDeviceKey(), umac, nonce)) {
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
                            step=200;
                        }
                    }
                }
                break;

            case 0x23: //recieved reliable data/
            case 0x03: //recieve unreliable data
                if (Utils.ccmVerify(packetNoUmac, btConn.getPumpData().getToDeviceKey(), umac, nonce)) {
                    SetupFragment.this.processAppResponse(payload, rel);
                }
                break;

            case 5://ack response
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
                    descrip = "AL_CONNECT_RES";
                    //if (state == state.CONNECT)
                        Application.sendAppCommand(Application.Command.COMMANDS_SERVICES_VERSION,btConn);

                    /*else if(getAppState() == Application.SERVICE_CONNECT)			//We are restarting a connection after P&A //TODO
                        setAppState(Application.SERVICE_ACTIVATE);*/
                    break;
                case (short) 0xA065://AL_SERVICE_VERSION_RES:
                    descrip = "AL_SERVICE_VER_RES";
                    Application.sendAppCommand(Application.Command.BINDING,btConn);
                    /*switch(getAppState())//TODO
                    {
                        case Application.COMM_VER_RESP:
                            setAppState(Application.RT_VER);
                            break;
                        case Application.RT_VER_RESP:
                            setAppState(Application.BIND);
                            break;
                    }*/
                    Application.sendAppCommand(Application.Command.REMOTE_TERMINAL_VERSION,btConn);
                    break;
                case (short) 0xA095://AL_BINDING_RES:
                    step+=100;
                    Protokoll.sendSyn(btConn);

                    descrip = "AL_BINDING_RES";
                    /*if(getAppState() == Application.BIND_RESP)//TODO
                    {
                        setAppState(Application.BOUND);
                    }*/
                    break;
                case (short) 0xA066://AL_SERVICE_ACTIVATE_RES:
                    descrip = "AL_SERVICE_ACTIVATE_RES";
                    /*if(getAppState() == Application.SERVICE_ACTIVATE_SENT)//TODO
                    {
                        setAppState(Application.SERVICE_STARTUP);
                    }
                    Debug.i(TAG, FUNC_TAG, "Service activate response!");*/
                    break;
                case (short) 0x005A://AL_DISCONNECT_RES:
                    descrip = "AL_DISCONNECT_RES";
                    // Debug.i(TAG, FUNC_TAG, "Disconnect response!");

                    break;
                case (short) 0xA069://AL_SERVICE_DEACTIVATE_RES:
                    descrip = "AL_DEACTIVATE_RES";
                    //Debug.i(TAG, FUNC_TAG, "Service deactivate response!");
                    break;
                case (short) 0x00AA://AL_SERVICE_ERROR_RES:
                    descrip = "AL_SERVICE_ERROR_RES";
                    //Debug.i(TAG, FUNC_TAG, "Service error response!");
                    break;
                case (short) 0xA06A://AL_SERVICE_DEACTIVATE_ALL_RES:
                    descrip = "AL_DEACTIVATE_ALL_RES";
                    /*Debug.i(TAG, FUNC_TAG, "Service deactivate all response!  Activating desired service...");
                    setAppState(Application.SERVICE_ACTIVATE);*///TODO
                    break;
                case (short) 0xAAAA://PING_RES:
                    descrip = "PING_RES";
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
        switch (step) {
            case 1: //we requested con, now we try to request auth
            {
                appendLog(this.getId() + " doing A_KEY_REQ");
                byte[] key = {16, 12, 2, 0, -16};

                btConn.writeCommand(key);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final EditText pinIn = new EditText(getContext());
                        pinIn.setInputType(InputType.TYPE_CLASS_NUMBER);
                        pinIn.setHint("XXX XXX XXXX");
                        new AlertDialog.Builder(getContext())
                                .setTitle("Enter Pin")
                                .setMessage("Read the Pin-Code from pump and enter it")
                                .setView(pinIn)
                                .setPositiveButton("ENTER", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        String pin = pinIn.getText().toString();
                                        appendLog("got the pin: " + pin);
                                        SetupFragment.this.pin = Utils.generateKey(pin);
                                        step = 2;
                                        //sending key available:
                                        appendLog(" doing A_KEY_AVA");
                                        byte[] key = {16, 15, 2, 0, -16};
                                        btConn.writeCommand(key);

                                    }
                                })
                                .show();
                    }
                });

            }
            break;
            default: //we indicated that we have a key, now lets handle the handle to the handle with an handler
            {
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
                            btConn.getPumpData().incrementNonceTx();

                            List<Byte> packet = Packet.buildPacket(new byte[]{16, 5, 0, 0, 0}, null, true,btConn);

                            //drv.recvSeqNo ^= 0x80;

                            seq = recvSeqNo;
                            packet.set(1, (byte) (packet.get(1) | recvSeqNo));                //OR the received sequence number

                            packet = Utils.ccmAuthenticate(packet, btConn.getPumpData().getToPumpKey(), btConn.getPumpData().getNonceTx());

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
            break;
                        /* case 3: //we expect an id response
                        {
                        ...
                        }
                        break;*/
        }
        //do something with this

    }

}
