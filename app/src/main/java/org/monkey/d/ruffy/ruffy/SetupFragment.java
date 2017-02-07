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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static android.content.ContentValues.TAG;

/**
 * A placeholder fragment containing a simple view.
 */
public class SetupFragment extends Fragment implements View.OnClickListener {

    private TextView connectLog;
    private runthread runthread;
    private boolean paired = false;

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
                            SetupFragment.this.run(socket,null,2);
                        } else {
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
                        case 2: //we indicated that we have a key
                        {
                            /*
                            do the analysis of the 60 bytes (needed?)

                            02-06 09:48:22.840 5275-5289/edu.virginia.dtc.RocheDriver I/RochePacket: RochePacket >>> Version: 10
02-06 09:48:22.850 5275-5289/edu.virginia.dtc.RocheDriver I/RochePacket: RochePacket >>> Command: 11
02-06 09:48:22.850 5275-5289/edu.virginia.dtc.RocheDriver I/RochePacket: RochePacket >>> Length: 0020
02-06 09:48:22.850 5275-5289/edu.virginia.dtc.RocheDriver I/RochePacket: RochePacket >>> Address: 01
02-06 09:48:22.850 5275-5289/edu.virginia.dtc.RocheDriver I/RochePacket: RochePacket >>> Nonce: 01 00 00 00 00 00 00 00 00 00 00 00 00
02-06 09:48:22.860 5275-5289/edu.virginia.dtc.RocheDriver I/RochePacket: RochePacket >>> Payload: 53 BF BE A3 DD CF 89 A7 B0 AA 6B A8 F4 04 72 86 A7 25 37 0D AB DD 4A 01 E1 BC 05 0C 94 23 2A 11
02-06 09:48:22.860 5275-5289/edu.virginia.dtc.RocheDriver I/RochePacket: RochePacket >>> UMAC: FF C4 7E 0E CB D7 7D 51
02-06 09:48:22.880 5275-5289/edu.virginia.dtc.RocheDriver I/RochePacket: RochePacket >>> Packet No UMAC: 10 11 20 00 01 01 00 00 00 00 00 00 00 00 00 00 00 00 53 BF BE A3 DD CF 89 A7 B0 AA 6B A8 F4 04 72 86 A7 25 37 0D AB DD 4A 01 E1 BC 05 0C 94 23 2A 11
02-06 09:48:22.950 5275-5289/edu.virginia.dtc.RocheDriver I/Security: ccmVerify >>> ccmVerify >>> U: FF C4 7E 0E CB D7 7D 51
02-06 09:48:22.950 5275-5289/edu.virginia.dtc.RocheDriver I/Security: ccmVerify >>> ccmVerify >>> U_Prime: FF C4 7E 0E CB D7 7D 51
02-06 09:48:22.950 5275-5289/edu.virginia.dtc.RocheDriver I/Security: ccmVerify >>> ccmVerify >>> verify = true
02-06 09:48:22.960 5275-5289/edu.virginia.dtc.RocheDriver I/Transport: parseRx >>> parseRx >>> Key_PD: 53 BF BE A3 DD CF 89 A7 B0 AA 6B A8 F4 04 72 86
02-06 09:48:22.980 5275-5289/edu.virginia.dtc.RocheDriver I/Transport: parseRx >>> parseRx >>> Key_DP: A7 25 37 0D AB DD 4A 01 E1 BC 05 0C 94 23 2A 11
02-06 09:48:23.010 5275-5289/edu.virginia.dtc.RocheDriver I/Transport: parseRx >>> parseRx >>> Decrytped PD: AC 92 C5 9E EB FD DA 94 00 2D 75 D1 CA D8 3D 0E
02-06 09:48:23.010 5275-5289/edu.virginia.dtc.RocheDriver I/Transport: parseRx >>> parseRx >>> Decrytped DP: 3D 3B 3A 79 13 0D FC B3 F3 F5 6D 43 88 46 EE FF

02-06 09:48:23.020 5275-5289/edu.virginia.dtc.RocheDriver I/Transport: parseRx >>> Receiving: KEY_RESPONSE | Seq No: 0 | Length: 58
02-06 09:48:23.020 5275-5289/edu.virginia.dtc.RocheDriver I/Transport: sendTransportLayerCommand >>> BT Friendly Name: NEO S8 plus
02-06 09:48:23.020 5275-5289/edu.virginia.dtc.RocheDriver I/Transport: sendTransportLayerCommand >>> Device ID: 4E 45 4F 20 53 38 20 70 6C 75 73 00 00
02-06 09:48:23.020 5275-5289/edu.virginia.dtc.RocheDriver I/Transport: sendTransportLayerCommand >>> Client ID: 2908
02-06 09:48:23.020 5275-5289/edu.virginia.dtc.RocheDriver I/Transport: sendTransportLayerCommand >>> Payload: 08 29 00 00 4E 45 4F 20 53 38 20 70 6C 75 73 00 00
02-06 09:48:23.050 5275-5289/edu.virginia.dtc.RocheDriver I/RochePacket: setData >>> SetData(): 10 12 11 00 10 01 00 00 00 00 00 00 00 00 00 00 00 00 08 29 00 00 4E 45 4F 20 53 38 20 70 6C 75 73 00 00 6B 18 4E 18 5F E3 14 11
02-06 09:48:23.050 5275-5289/edu.virginia.dtc.RocheDriver I/Transport: setState >>> setState >>> State: 6 Prev: 5

                            afterwards send id request:

                            case SEND_ID_REQUEST:
                            descrip = "ID_REQ";
                            p.resetTxNonce();														//Reset TX Nonce (previous to this the nonce is not used and is zero)
                            incrementTxNonce();														//Increment it to 1

                            ByteBuffer ids = ByteBuffer.allocate(17);								//Allocate payload

                            String btName = InterfaceData.getInstance().bt.getName();				//Get the Device ID

                            Debug.i(TAG, FUNC_TAG, "BT Friendly Name: "+btName);

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
                            Debug.i(TAG, FUNC_TAG, "Device ID: "+dat);

                            String swver = "5.04";													//Get the SW Version
                            int clientId = 0;

                            clientId += (((byte)swver.charAt(3)) - 0x30);
                            clientId += (((byte)swver.charAt(2)) - 0x30)*10;
                            clientId += (((byte)swver.charAt(0)) - 0x30)*100;
                            clientId += (10000);

                            Debug.i(TAG, FUNC_TAG, "Client ID: "+String.format("%X", clientId));

                            ids.order(ByteOrder.LITTLE_ENDIAN);
                            ids.putInt(clientId);
                            ids.put(deviceId);

                            dat = "";																//Print payload
                            for(byte b:ids.array())
                                dat += String.format("%02X ", b);
                            Debug.i(TAG, FUNC_TAG, "Payload: "+dat);

                            packet = p.buildPacket(device_id, ids, true);							//Use real address (gathered in Key Response)
                            packet = s.ccmAuthenticate(packet, drv.dp_key, Packet.nonceTx);			//Add U-MAC (Use D->P key)

                            p.frameEscaping(packet);
                            */
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
