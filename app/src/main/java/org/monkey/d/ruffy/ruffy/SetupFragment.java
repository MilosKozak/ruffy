package org.monkey.d.ruffy.ruffy;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static android.content.ContentValues.TAG;

/**
 * A placeholder fragment containing a simple view.
 */
public class SetupFragment extends Fragment implements View.OnClickListener {

    private TextView connectLog;
    private runthread runthread;
    private boolean paired = false;
    private byte addresses;

    public SetupFragment() {

        if (Build.VERSION.SDK_INT >= 18) {
            bt = ((BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        } else {
            bt = BluetoothAdapter.getDefaultAdapter();
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_setup, container, false);
        Button connect = (Button)v.findViewById(R.id.setup_connect);
        connect.setOnClickListener(this);

        connectLog = (TextView)v.findViewById(R.id.setup_log);
        connectLog.setMovementMethod(new ScrollingMovementMethod());

        IntentFilter filter = new IntentFilter(
                "android.bluetooth.device.action.PAIRING_REQUEST");

        getActivity().registerReceiver(
                new PairingRequest(), filter);
        return v;
    }

    @Override
    public void onClick(final View view) {
        view.setEnabled(false);
        connectLog.setText("Starting rfcomm to wait for Pump connection…");
        IntentFilter filter = new IntentFilter("android.bluetooth.device.action.ACL_CONNECTED");
        getActivity().registerReceiver(this.btReceiver, filter);

        Intent discoverableIntent = new Intent("android.bluetooth.adapter.action.REQUEST_DISCOVERABLE");
        discoverableIntent.putExtra("android.bluetooth.adapter.extra.DISCOVERABLE_DURATION", 60);
        startActivity(discoverableIntent);

        BluetoothServerSocket srvSock = null;
        try {
            srvSock = bt.listenUsingInsecureRfcommWithServiceRecord("SerialLink", UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
        } catch (IOException e) {
            appendLog("socket listen() failed");
            view.setEnabled(true);
            return;
        }

        final BluetoothServerSocket lSock = srvSock;
        Thread listen = new Thread(){
            @Override

            public void run() {
                this.setName("listening with device: "+deviceMac);
                BluetoothSocket socket = null;

                try {
                    if (socket != null) {
                        socket = lSock.accept();
                    }
                    if (socket != null) {
                        if (deviceMac != null && paired) {
                            //FIXME maybe this make doubles bonding dialog?
                       //     SetupFragment.this.run(socket,null,2);
                        //} else {
                            socket.close();
                            socket=null;
                        }
                    }
                }
                catch(Exception e)
                {

                }
            }
        };
        listen.start();

        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText("}gZ='GD?gj2r|B}>");
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("text label","}gZ='GD?gj2r|B}>");
            clipboard.setPrimaryClip(clip);
        }
        /*while (!bt.isDiscovering()) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/

        new Thread(){
            @Override
            public void run() {
                while(deviceMac==null){// && bt.isDiscovering()) {
                    appendLog("waiting…");
                    try {
                        sleep(2500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                if(deviceMac==null)
                {
                    appendLog("no pump connection recieved!");
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            view.setEnabled(true);
                        }
                    });
                }
            }
        }.start();
    }
    public BluetoothAdapter bt = null;
    private String deviceMac = null;

    private BroadcastReceiver btReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice bd = null;
            if(step==0) {
                for(String k: intent.getExtras().keySet())
                {
                    if(k.equals(BluetoothDevice.EXTRA_DEVICE))
                    {
                        if(intent.getStringExtra("address")== null)
                        {

                            bd = ((BluetoothDevice)intent.getExtras().get(k));
                            String address = bd.getAddress();
                            intent.getExtras().putString("address",address);
                            if (address.substring(0, 8).equals("00:0E:2F")) {
                                bt.cancelDiscovery();

                                appendLog("Pump found: "+address);
                                deviceMac = address;

                                connect(bd, 4);
                            }
                        }
                    }
                }
            }
        }
    };
    class PairingRequest extends BroadcastReceiver {
        public PairingRequest() {
            super();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.bluetooth.device.action.PAIRING_REQUEST")) {
                try {
                    final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try
                            {
                                byte[] pinBytes;
                                pinBytes = ("}gZ='GD?gj2r|B}>").getBytes("UTF-8");

                                appendLog( "Try to set the PIN");
                                Method m = device.getClass().getMethod("setPin", byte[].class);
                                m.invoke(device, pinBytes);
                                appendLog("Success to add the PIN.");

                                paired=true;

                                /*m = device.getClass().getMethod("createBond");
                                m.invoke(device);
                                appendLog("Success to start bond.");
*/
                                try {
                                    device.getClass().getMethod("setPairingConfirmation", boolean.class).invoke(device, true);
                                    appendLog( "Success to setPairingConfirmation.");
                                } catch (Exception e) {
                                    // TODO Auto-generated catch block
                                    Log.e(TAG, e.getMessage());
                                    e.printStackTrace();
                                }
                            }catch(Exception e)
                            {
                                Log.e(TAG, e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    private void connect(BluetoothDevice bd, int retry)
    {
        if(step>0)
        {
            appendLog("not connect again, already in pairing process!");
            return;
        }
        appendLog("connecting to pump…");

        BluetoothSocket tmp = null;
        try {
            bt.cancelDiscovery();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            tmp = bd.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));

            if(paired)
                run(tmp, bd, retry);
            else
                tmp.close();
        } catch (IOException e) {
            appendLog("socket create() failed: "+e.getMessage());
        }

    }

    private void appendLog(final String message)
    {
        Log.v("RUFFY_LOG",message);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectLog.append("\n"+message);
                final int scrollAmount = connectLog.getLayout().getLineTop(connectLog.getLineCount()) - connectLog.getHeight();
                if (scrollAmount > 0)
                    connectLog.scrollTo(0, scrollAmount);
                else
                    connectLog.scrollTo(0, 0);
            }
        });

    }

    public static byte[] nonceRx = new byte[13];
    public static byte[] nonceTx = new byte[13];

    private int step = 0;

    public byte[] generateKey(String strKey)
    {
        final String FUNC_TAG = "generateKey";

        byte[] pin = new byte[10];

        for(int i=0;i<strKey.length();i++)
            pin[i] = ((byte)(strKey.charAt(i)));		//Don't convert to decimal here

        String d = "";
        for(byte b:pin)
            d += String.format("%02X ", b);

        byte[] key = new byte[16];

        for(int i = 0; i<16; i++)
        {
            if(i < 10)
            {
                key[i] = pin[i];
            }
            else
            {
                key[i] = (byte) (0xFF ^ pin[i - 10]);
            }
        }

        d = "";
        for(byte b:key)
            d += String.format("%02X ", b);

        return key;
    }

    private byte[] pin;
    private Object pump_tf;
    private Object driver_tf;
    public List<List<Byte>> frameDeEscaping(List<Byte> buffer)//FIXME add buffering of long packets!
    {
        final String FUNC_TAG = "frameDeEscaping";

        List<List<Byte>> complete = new ArrayList<List<Byte>>();

       /*FIXME handle big packages!  if(start)
        {
            //This is a scenario where a packet is so big it isn't complete in a buffer
            //So we don't want to erase the data or reset the flags
            Debug.i(TAG, FUNC_TAG, "Start is true, so we have an incomplete packet waiting...");
        }
        else
        {
            //Reset flags and clear data for starting a new packet
            start = stop = escaped = false;
            packet.clear();
        }*/
        List<Byte> packet = new ArrayList<Byte>();
        boolean escaped = false ,start = false, stop=false;
        for(int i=0;i<buffer.size();i++)
        {
            if(escaped == true)
            {
                escaped = false;
                if(buffer.get(i) == -35)
                {
                    packet.add((byte)-52);
                }
                else if(buffer.get(i) == -18)
                {
                    packet.add((byte)119);
                }
            }
            else if(buffer.get(i) == 119)
            {
                if(i+1 >= buffer.size())
                {
                    escaped = true;				//If we are at the end of the buffer and find an escape character
                }
                else
                {
                    Byte next = buffer.get(i+1);
                    if(next == -35)
                    {
                        packet.add((byte)-52);
                        i++;								//Skip the next byte
                    }
                    else if(next == -18)
                    {
                        packet.add((byte)119);			//Skip the next byte
                        i++;
                    }
                }
            }
            else if(buffer.get(i) == -52)	//We need to cover the chance that there are multiple packets in the buffer
            {
                if(!start)
                {
                    start = true;
                }
                else
                {
                    stop = true;
                }

                if(start && stop)
                {
                    start = false;
                    stop = false;

                    if(packet.size() == 0)
                    {
                        start = true;
                        stop = false;
                    }
                    else if(i == 0)
                    {
                        start = true;
                        stop = false;
                    }
                    else
                    {
                        complete.add(packet);
                        packet = new ArrayList<Byte>();
                    }
                }
            }
            else
            {
                //Debug.i(TAG, FUNC_TAG, "Adding byte to packet...");
                if(start)
                    packet.add(buffer.get(i));
            }
        }

        return complete;
    }
    public void handleRX(byte[] inBuf, int length)
    {
        //Byte command = (byte) (inBuf[1] & 0x1F);
        boolean expected = false;
        String descrip = "";

        ByteBuffer buffer = ByteBuffer.wrap(inBuf,0,length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        ByteBuffer nBuf, pBuf, uBuf;

        byte[] nonce, payload, umac, paddedPacket, packetNoUmac;

        Byte version, command, addresses;
        version = buffer.get();					//Get version and other data
        command = buffer.get();

        short payloadlength = buffer.getShort();

        addresses = buffer.get();

        nonce = new byte[13];							//Copy buffers for nonce
        buffer.get(nonce, 0, nonce.length);
        nBuf = ByteBuffer.wrap(nonce);					//Copy to ByteBuffers too for extracting data

        payload = new byte[payloadlength];						//Payload
        buffer.get(payload, 0, payload.length);
        pBuf = ByteBuffer.wrap(payload);

        umac = new byte[8];								//U-MAC
        buffer.get(umac, 0, umac.length);
        uBuf = ByteBuffer.wrap(umac);

        packetNoUmac = new byte[buffer.capacity()-umac.length];
        buffer.rewind();
        for(int i = 0; i<packetNoUmac.length;i++)
            packetNoUmac[i] = buffer.get();

        buffer.rewind();
//logging:
        appendLog(String.format("Version: %02X", version));
        appendLog(String.format("Command: %02X", command));
        appendLog(String.format("Length: %04X", payloadlength));
        appendLog(String.format("Address: %02X", addresses));

        String dat = "";
        for(byte b: nonce)
            dat += String.format("%02X ", b);
        appendLog("Nonce: "+dat);

        dat = "";
        for(byte b:payload)
            dat += String.format("%02X ", b);
        appendLog("Payload: "+dat);

        dat = "";
        for(byte b:umac)
            dat += String.format("%02X ", b);
        appendLog("UMAC: "+dat);

        dat = "";
        for(byte b:packetNoUmac)
            dat += String.format("%02X ", b);
        appendLog("Packet No UMAC: "+dat);


        byte seq = 0x00;
        if((inBuf[1] & 0x80)==0x80)
            seq = (byte) 0x80;
        else
            seq = (byte) 0x00;

        switch(command)
        {
            case 0x11://key response?
                try {
                    Object tf = Twofish_Algorithm.makeKey(pin);
                    this.addresses = (byte)((addresses << 4) & 0xF0);		//Get the address and reverse it since source and destination are reversed from the RX packet

                    byte[] key_pd = new byte[16];							//Get the bytes for the keys
                    byte[] key_dp = new byte[16];

                    pBuf.rewind();
                    pBuf.get(key_pd, 0, key_pd.length);
                    pBuf.get(key_dp, 0, key_dp.length);

                    String d = "";
                    for(byte b:key_pd)
                        d += String.format("%02X ", b);
                    appendLog("parseRx >>> Key_PD: "+d);

                    d = "";
                    for(byte b:key_dp)
                        d += String.format("%02X ", b);
                    appendLog( "parseRx >>> Key_DP: "+d);

                    byte[] key_pd_de = Twofish_Algorithm.blockDecrypt(key_pd, 0, tf);
                    byte[] key_dp_de = Twofish_Algorithm.blockDecrypt(key_dp, 0, tf);

                    //FIXME save saveKeysToPrefs(key_pd_de, key_dp_de);

                    d = "";
                    for(byte b:key_pd_de)
                        d += String.format("%02X ", b);
                    appendLog("parseRx >>> Decrytped PD: "+d);

                    d = "";
                    for(byte b:key_dp_de)
                        d += String.format("%02X ", b);
                    appendLog("parseRx >>> Decrytped DP: "+d);

                    //CREATE THE KEY OBJECTS (WHITENING SUBKEYS, ROUND KEYS, S-BOXES)
                    pump_tf = Twofish_Algorithm.makeKey(key_pd_de);
                    driver_tf = Twofish_Algorithm.makeKey(key_dp_de);

                    runthread.sendIDReq();
                }catch(Exception e)
                {
                    e.printStackTrace();
                    appendLog("failed inRX: "+e.getMessage());
                }
                break;
            default:
                appendLog("not yet implemented rx command: "+command);

        }
    }

    public void resetTxNonce()
    {
        for(int i=0;i<nonceTx.length;i++)
            nonceTx[i] = 0;
    }

    public void resetRxNonce()
    {
        for(int i=0;i<nonceRx.length;i++)
            nonceRx[i] = 0;
    }

    public int incrementArray(byte[] array)
    {
        int i=0, carry=0;

        array[i]++;
        if(array[i] == 0)
            carry =1;

        for(i=1;i<array.length;i++)
        {
            if(carry==1)
            {
                array[i] += carry;
                if(array[i] > 0)
                {
                    carry = 0;
                    return carry;
                }
                else
                    carry = 1;
            }
        }

        return carry;
    }
    class runthread extends Thread{
        private final BluetoothDevice device;
        private BluetoothSocket socket;
        private InputStream input = null;
        private OutputStream output = null;
        private Boolean running = false;
        private int retry;

        runthread(BluetoothSocket inSock,BluetoothDevice device, int retry)
        {
            socket=inSock;
            this.device=device;
            this.retry=retry;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[512];
            running=true;
            while (running) {
                try {
                    int bytes = input.read(buffer);
                    appendLog(this.getId()+": read something: "+bytes+" bytes");
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < bytes;i++) {
                        sb.append(String.format("%02X ", buffer[i]));
                    }
                    appendLog(this.getId()+": got "+sb.toString());

                    switch(step)
                    {
                        case 1: //we requested con, now we try to request auth
                        {
                            appendLog(this.getId()+" doing A_KEY_REQ");
                            byte[] key = {16,12,2,0,-16};
                            writeCommand(key);

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
                                                    appendLog(runthread.this.getId()+": got the pin: "+pin);
                                                    SetupFragment.this.pin = generateKey(pin);
                                                    step=2;
                                                    //sending key available:
                                                    appendLog(runthread.this.getId()+" doing A_KEY_AVA");
                                                    byte[] key = {16,15,2,0,-16};
                                                    writeCommand(key);
                                                }
                                            })
                                            .show();
                                }
                            });

                        }
                        break;
                        case 2: //we indicated that we have a key, now lets handle the handle to the handle with an handler
                        {
                            List<Byte> t = new ArrayList<>();
                            for(int i = 0; i < bytes;i++)
                                t.add(buffer[i]);
                            for(List<Byte> x : frameDeEscaping(t)) {
                                byte[] xx = new byte[x.size()];
                                for (int i = 0; i < x.size(); i++)
                                    xx[i] = x.get(i);
                                handleRX(xx, x.size());
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
                } catch (IOException e) {
                    appendLog(this.getId()+": unable to read!");
                    break;
                }

            }
        }
        public void kill()
        {
            appendLog(this.getId()+": killing run…");
            running=false;
            try {input.close();}catch(Exception e){};
            try {output.close();}catch(Exception e){};
            try {socket.close();}catch(Exception e){};
        }

        public short updateCrc(short crc, byte input) {
            Object[] objArr = new Object[1];
            objArr[0] = Byte.valueOf(input);
            short crcTemp = (short) (((short) input) ^ crc);
            crc = (short) (((short) (crc >> 8)) & 255);
            objArr = new Object[1];
            objArr[0] = Short.valueOf(crc);
            objArr = new Object[1];
            objArr[0] = Short.valueOf(crcTemp);
            if ((crcTemp & 128) > 0) {
                crc = (short) (crc ^ -31736);
            }
            objArr = new Object[1];
            objArr[0] = Short.valueOf(crc);
            if ((crcTemp & 64) > 0) {
                crc = (short) (crc ^ 16900);
            }
            objArr = new Object[1];
            objArr[0] = Short.valueOf(crc);
            if ((crcTemp & 32) > 0) {
                crc = (short) (crc ^ 8450);
            }
            objArr = new Object[1];
            objArr[0] = Short.valueOf(crc);
            if ((crcTemp & 16) > 0) {
                crc = (short) (crc ^ 4225);
            }
            objArr = new Object[1];
            objArr[0] = Short.valueOf(crc);
            if ((crcTemp & 8) > 0) {
                crc = (short) (crc ^ -29624);
            }
            objArr = new Object[1];
            objArr[0] = Short.valueOf(crc);
            if ((crcTemp & 4) > 0) {
                crc = (short) (crc ^ 17956);
            }
            objArr = new Object[1];
            objArr[0] = Short.valueOf(crc);
            if ((crcTemp & 2) > 0) {
                crc = (short) (crc ^ 8978);
            }
            objArr = new Object[1];
            objArr[0] = Short.valueOf(crc);
            if ((crcTemp & 1) > 0) {
                crc = (short) (crc ^ 4489);
            }
            objArr = new Object[1];
            objArr[0] = Short.valueOf(crc);
            return crc;
        }
        @Override
        public synchronized void start() {
            appendLog(this.getId()+": starting run thread…");
            try {
                socket.connect();
                input = socket.getInputStream();
                output = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                appendLog(this.getId()+": unable to open streams: "+e.getMessage());
                try{socket.close();}catch(Exception e2){e2.printStackTrace();}
                kill();
                if(device!=null && retry > 0)
                {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            appendLog(runthread.this.getId()+": restarting…");
                            connect(device,retry-1);
                        }
                    });

                }
                return;
            }
            super.start();
            if(output!=null && step==0)
            {
                appendLog(this.getId()+": initiate pairing…");
                //send:        KEY_REQUEST

                byte[] key = {16,9,2,0,-16};
                step = 1;
                writeCommand(key);
            }
        }

        public void writeCommand(byte[] key)
        {
            List<Byte> out = new LinkedList<Byte>();
            for(Byte b : key)
                out.add(b);
            for (Byte n : SetupFragment.nonceTx)
                out.add(n);
            addCRC(out);

            List<Byte> temp = frameEscape(out);

            byte[] ro = new byte[temp.size()];
            int i = 0;
            for(byte b : temp)
                ro[i++]=b;
            try
            {
                output.write(ro);
                appendLog(this.getId()+": succesful wrote "+temp.size()+" bytes!");
            }catch(Exception e){
                e.printStackTrace();
                appendLog(this.getId()+": error in tx: "+e.getMessage());
                kill();
                if(device!=null && retry > 0)
                {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            appendLog(runthread.this.getId()+": restarting…");
                            connect(device,retry-1);
                        }
                    });
                }
            }
        }
        public void addNonce(List<Byte> packet, byte[] nonce)
        {
            for(int i=0;i<nonce.length;i++)
                packet.add(nonce[i]);
        }
        public List<Byte> buildPacket(byte[] command, ByteBuffer payload, boolean address)
        {
            List<Byte> output = new ArrayList<Byte>();

            for(int i=0; i < command.length; i++)
                output.add(command[i]);

            if(address)											//Replace the default address with the real one
            {
                output.remove(command.length-1);				//Remove the last byte (address)
                output.add(addresses);		//Add the real address byte
            }

            addNonce(output, nonceTx);

            if(payload!=null)
            {
                payload.rewind();
                for(int i=0;i<payload.capacity();i++)
                    output.add(payload.get());
            }

            return output;
        }
        public void addCRC(List<Byte> out)
        {
            short crc = -1;
            Object[] objArr = new Object[1];
            objArr[0] = Short.valueOf((short)-1);
            for (int i = 0; i < out.size(); i += 1) {
                crc = updateCrc(crc, ((Byte) out.get(i)).byteValue());
                objArr = new Object[1];
                objArr[0] = Short.valueOf(crc);
            }
            out.add(Byte.valueOf((byte) (crc & 255)));
            out.add(Byte.valueOf((byte) (crc >> 8)));
            for (int i = 0; i < 8; i += 1) {
                out.add(Byte.valueOf((byte) 0));
            }
        }
        public List<Byte> frameEscape(List<Byte> out)
        {
            List<Byte> temp = new ArrayList();
            temp.add((byte)-52);
            for (int i = 0; i < out.size(); i++) {
                if (((Byte) out.get(i)).byteValue() == -52) {
                    temp.add((byte)119);
                    temp.add((byte)-35);
                } else if (((Byte) out.get(i)).byteValue() == (byte) 119) {
                    temp.add((byte) 119);
                    temp.add((byte) -18);
                } else {
                    temp.add((Byte) out.get(i));
                }
            }
            temp.add((byte)-52);
            return temp;
        }

        public void sendIDReq() {
            resetTxNonce();														//Reset TX Nonce (previous to this the nonce is not used and is zero)
            incrementArray(nonceTx);														//Increment it to 1

            ByteBuffer ids = ByteBuffer.allocate(17);								//Allocate payload

            String btName = bt.getName();				//Get the Device ID

            appendLog("BT Friendly Name: "+btName);

            byte[] deviceId = new byte[13];
            for(int i=0;i<deviceId.length;i++)
            {
                if(i < btName.length())
                    deviceId[i] = (byte)btName.charAt(i);
                else
                    deviceId[i] = (byte)0x00;
            }

            String dat = "";
            for(byte b:deviceId)
                dat += String.format("%02X ", b);
            appendLog("Device ID: "+dat);

            String swver = "5.04";													//Get the SW Version
            int clientId = 0;

            clientId += (((byte)swver.charAt(3)) - 0x30);
            clientId += (((byte)swver.charAt(2)) - 0x30)*10;
            clientId += (((byte)swver.charAt(0)) - 0x30)*100;
            clientId += (10000);

            appendLog("Client ID: "+String.format("%X", clientId));

            ids.order(ByteOrder.LITTLE_ENDIAN);
            ids.putInt(clientId);
            ids.put(deviceId);

            dat = "";																//Print payload
            for(byte b:ids.array())
                dat += String.format("%02X ", b);
            appendLog("Payload: "+dat);

            byte[] p_r = {16,0x12,17,0,0};

            List<Byte> packet = buildPacket(p_r, ids, true);							//Use real address (gathered in Key Response)
            packet = ccmAuthenticate(packet, driver_tf, nonceTx);			//Add U-MAC (Use D->P key)

            List<Byte> temp = frameEscape(packet);
            byte[] ro = new byte[temp.size()];
            int i = 0;
            for(byte b : temp)
                ro[i++]=b;
            try
            {
                output.write(ro);
                appendLog(this.getId()+": succesful wrote "+temp.size()+" bytes!");
            }catch(Exception e) {
                e.printStackTrace();
                appendLog(this.getId() + ": error in tx: " + e.getMessage());
            }
        }
        public List<Byte> ccmAuthenticate(List<Byte> buffer, Object key, byte[] nonceIn)
        {
            List<Byte> output = new ArrayList<Byte>();
            int origLength = buffer.size();					//Hang on to the original length

            byte[] packet = new byte[buffer.size()];		//Create primitive array
            for(int i=0;i<packet.length;i++)				//Copy to byte array
                packet[i] = buffer.get(i);

            byte[] paddedPacket = padPacket(packet);
            byte[] nonce = nonceIn;

            byte[] umac = ccmEncrypt(paddedPacket,key,nonce);							//Generate U-MAC value

            ByteBuffer packetBuffer = ByteBuffer.allocate(origLength + umac.length);
            packetBuffer.order(ByteOrder.LITTLE_ENDIAN);

            packetBuffer.put(paddedPacket, 0, origLength);
            packetBuffer.put(umac);

            String dat = "";
            for(byte b:packetBuffer.array())
                dat += String.format("%02X ", b);
            appendLog("SetData(): "+dat);

            for(int i=0;i<packetBuffer.array().length;i++)	//Convert to List<Byte>
                output.add(packetBuffer.array()[i]);

            return output;
        }
        public byte[] ccmEncrypt(byte[] padded, Object key,byte[] nonce)
        {
            byte[] xi = new byte[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};		//Initialization vector
            byte[] u = new byte[8];											//Output array U-MAC

            //SETUP INITIALIZATION VECTOR

            xi[0] = 0x79;													//Set flags for IV

            for(int i=0;i<nonce.length;i++)								//Copy nonce
                xi[i+1] = nonce[i];										//TODO: check endianness

            xi[14] = 0;														//Length is zero
            xi[15] = 0;

            String dat = "";
            for(byte b:xi)
                dat += String.format("%02X ", b);
            //Debug.i(TAG, FUNC_TAG, "ccmEncrypt >>> Initial XI: "+dat);

            xi = Twofish_Algorithm.blockEncrypt(xi, 0, key);				//Encrypt to generate XI from IV

            dat = "";
            for(byte b:xi)
                dat += String.format("%02X ", b);
            //Debug.i(TAG, FUNC_TAG, "ccmEncrypt >>> Encrypt 1 XI: "+dat);

            //RUN CBC AND ENCRYPT PACKET

            //Debug.i(TAG, FUNC_TAG, "ccmEncrypt >>> Length = "+p.paddedPacket.length);
            //Debug.i(TAG, FUNC_TAG, "ccmEncrypt >>> Running loop "+p.paddedPacket.length/ENCRYPT_BLOCKSIZE+" iterations!");

            for(int i=0;i<(padded.length / 16);i++)
            {
                //Debug.i(TAG, FUNC_TAG, "ccmEncrypt >>> ==========================================================================================================");

                for(int n=0;n<16;n++)
                {
                    //Debug.i(TAG, FUNC_TAG, "ccmEncrypt >>> POSITION: "+ ((i * ENCRYPT_BLOCKSIZE)+n) +" xi[n]: "+String.format("%02X", xi[n])+" XOR'd with Data[n]: "+String.format("%02X",p.paddedPacket[(i * ENCRYPT_BLOCKSIZE)+n]));
                    xi[n] ^= padded[(i * 16)+n];		//Do the XOR chaining
                    //Debug.i(TAG, FUNC_TAG, "ccmEncrypt >>> POSITION: "+ ((i * ENCRYPT_BLOCKSIZE)+n) +" xi[n] RESULT: "+String.format("%02X", xi[n]));
                }

                xi = Twofish_Algorithm.blockEncrypt(xi, 0, key);			//Encrypt with TwoFish

                //Debug.i(TAG, FUNC_TAG, "ccmEncrypt >>> ITERATION: "+i);

                dat = "";
                for(byte b:xi)
                    dat += String.format("%02X ", b);
                //Debug.i(TAG, FUNC_TAG, "ccmEncrypt >>> XI: "+dat);

                //Debug.i(TAG, FUNC_TAG, "ccmEncrypt >>> ==========================================================================================================");
            }

            //PAD DATA IF IT ISN'T A MULTIPLE OF THE BLOCKSIZE

            if ((padded.length % 16) != 0)
            {
                //Debug.i(TAG, FUNC_TAG, "ccmEncrypt >>> Packet needs padding! Condition="+p.paddedPacket.length % ENCRYPT_BLOCKSIZE);

                for (int i=0; i < 16; i++)
                {
                    //Debug.i(TAG, FUNC_TAG, "ccmEncrypt >>> ==========================================================================================================");

                    if ( i < (padded.length % 16) )
                    {
                        // Fill with trailing data
                        //Debug.i(TAG, FUNC_TAG, "ccmEncrypt >>> I ="+i+" trailing data...");
                        //Debug.i(TAG, FUNC_TAG, "ccmEncrypt >>> POSITION: "+(((p.paddedPacket.length / ENCRYPT_BLOCKSIZE) * ENCRYPT_BLOCKSIZE) + i)+" xi[n]: "+String.format("%02X", xi[i])+" XOR'd with Data[n]: "+String.format("%02X",p.paddedPacket[((p.paddedPacket.length / ENCRYPT_BLOCKSIZE) * ENCRYPT_BLOCKSIZE) + i]));
                        xi[i] ^= padded[((padded.length / 16) * 16) + i];
                        //Debug.i(TAG, FUNC_TAG, "ccmEncrypt >>> POSITION: "+(((p.paddedPacket.length / ENCRYPT_BLOCKSIZE) * ENCRYPT_BLOCKSIZE) + i) +" xi[n] RESULT: "+String.format("%02X", xi[i]));
                    }
                    else
                    {
                        // Fill with bytes with value of bytes required to padding blocksize
                        // Difference to RFC 3610 section 2.2 (padding with zeroes)
                        //Debug.i(TAG, FUNC_TAG, "ccmEncrypt >>> I ="+i+" padding size...");
                        //Debug.i(TAG, FUNC_TAG, "ccmEncrypt >>> xi[n]: "+String.format("%02X", xi[i])+" XOR'd with size: "+String.format("%02X",ENCRYPT_BLOCKSIZE - (p.paddedPacket.length % ENCRYPT_BLOCKSIZE)));
                        xi[i] ^= 16 - (padded.length % 16);
                        //Debug.i(TAG, FUNC_TAG, "ccmEncrypt >>> xi[n] RESULT: "+String.format("%02X", xi[i]));
                    }

                    //Debug.i(TAG, FUNC_TAG, "ccmEncrypt >>> ==========================================================================================================");
                }

                xi = Twofish_Algorithm.blockEncrypt(xi, 0, key);
            }

            //COMPUTE U-MAC

            for(int i=0;i<u.length;i++)										//Copy XI to U
                u[i] = xi[i];

            dat = "";
            for(byte b:u)
                dat += String.format("%02X ", b);
            //Debug.i(TAG, FUNC_TAG, "ccmEncrypt >>> Copy 8 Bytes to UMAC: "+dat);

            xi[0] = 65;														//Set flags

            dat = "";
            for(byte b:nonce)
                dat += String.format("%02X ", b);
            //Debug.i(TAG, FUNC_TAG, "ccmEncrypt >>> Nonce: "+dat);

            for(int n=0;n<nonce.length;n++)
                xi[n+1] = nonce[n];

            xi[14] = 0;
            xi[15] = 0;

            dat = "";
            for(byte b:xi)
                dat += String.format("%02X ", b);
            //Debug.i(TAG, FUNC_TAG, "ccmEncrypt >>> Encrypt A0: "+dat);

            xi = Twofish_Algorithm.blockEncrypt(xi, 0, key);				//Encrypt XI

            dat = "";
            for(byte b:xi)
                dat += String.format("%02X ", b);
            //Debug.i(TAG, FUNC_TAG, "ccmEncrypt >>> Encrypt A0 after Encryption: "+dat);

            for(int i=0;i<u.length;i++)										//XOR to create U-MAC
                u[i] ^= xi[i];

            dat = "";
            for(byte b:u)
                dat += String.format("%02X ", b);
            //Debug.i(TAG, FUNC_TAG, "ccmEncrypt >>> U: "+dat);

            return u;
        }
        public byte[] padPacket(byte[] packet)
        {
            byte pad;
            byte[] output;

            //Debug.i(TAG, FUNC_TAG, "padPacket >>> Packet Length: "+packet.length);

            String dat = "";
            for(byte b:packet)
                dat += String.format("%02X ", b);
            //Debug.i(TAG, FUNC_TAG, "padPacket >>> Input Packet: "+dat);

            pad = (byte) (16 - (packet.length % 16));	//(ENCRYPT_BLOCKSIZE - (packet.length % ENCRYPT_BLOCKSIZE));
            if(pad > 0)
            {
                output = new byte[pad+packet.length];
                //Debug.i(TAG, FUNC_TAG, "padPacket >>> Output Length: "+output.length+" Padding needed: "+pad);

                for(int n=0;n<packet.length;n++)		//Copy packet into output
                    output[n] = packet[n];

                for(int i=0;i < pad;i++)
                {
                    output[packet.length+i] = pad;
                }
            }
            else
                output =  packet;

            dat = "";
            for(byte b:output)
                dat += String.format("%02X ", b);
            //Debug.i(TAG, FUNC_TAG, "padPacket >>> Output Packet: "+dat);

            return output;
        }
    }
    void run(final BluetoothSocket socket,BluetoothDevice device, int retry)
    {
        appendLog("got connection to pump…");
        if(runthread!=null)
        {
            runthread.kill();
        }
        runthread = new runthread(socket,device,retry);
        runthread.start();

    }
}
