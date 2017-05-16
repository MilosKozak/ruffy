package org.monkey.d.ruffy.ruffy.driver;

import org.monkey.d.ruffy.ruffy.SetupFragment;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

/**
 * Created by fishermen21 on 16.05.17.
 */

public class Application {
    public static final int COMMANDS_SERVICES_VERSION = 0;
    public static final int REMOTE_TERMINAL_VERSION = 1;
    public static final int BINDING = 2;
    public static final int COMMAND_MODE = 3;

    public static void sendAppConnect(BTConnection btConn) {
        ByteBuffer payload = null;
        byte[] connect_app_layer = new byte[]
                {
                        // VERSION: 8 bits \    Service ID \               Command ID
                        16    ,  0,    85, -112
                };
        payload = ByteBuffer.allocate(8);				//4 bytes for application header, 4 for payload
        payload.put(connect_app_layer);					//Add prefix array
        payload.order(ByteOrder.LITTLE_ENDIAN);
        payload.putInt(Integer.parseInt("12345"));

        Application.sendData(payload,true,btConn);
    }

    private static void sendData(ByteBuffer payload, boolean reliable,BTConnection btConn)  {
        btConn.incrementNonceTx();

        byte[] sendR = {16,3,0,0,0};

        List<Byte> packet  = Packet.buildPacket(sendR, payload, true,btConn);					//Add the payload, set the address if valid

        if(reliable) {
            int seq = btConn.seqNo;
            packet.set(1, setSeqRel(packet.get(1), true,btConn));                        //Set the sequence and reliable bits
        }
        Packet.adjustLength(packet, payload.capacity());							//Set the payload length

        packet = Utils.ccmAuthenticate(packet, btConn.driver_tf, btConn.getNonceTx());		//Authenticate packet


        List<Byte> temp = Frame.frameEscape(packet);
        byte[] ro = new byte[temp.size()];
        int i = 0;
        for(byte b : temp)
            ro[i++]=b;

        btConn.write(ro);
    }

    private static Byte setSeqRel(Byte b, boolean rel,BTConnection btConn)
    {
        b = (byte) (b | btConn.seqNo);			//Set the sequence bit

        if(rel)
            b = (byte) (b |0x20);			//Set the reliable bit

        btConn.seqNo ^= 0x80;

        return b;
    }
    private static byte[] service_activate = new byte[]
            {
                    // VERSION: 8 bits \    Service ID \                Command ID
                    16    , 0,    0x66, (byte)0x90
            };
    public static void sendAppCommand(int command, BTConnection btConn){
        ByteBuffer payload = null;

        String s = "";

        switch(command)
        {
                case COMMAND_MODE:
                    s = "COMMAND_ACTIVATE";
                    payload = ByteBuffer.allocate(7);
                    payload.put(service_activate);
                    payload.put((byte)0xB7);
                    payload.put((byte)0x01);
                    payload.put((byte)0x00);
                    break;
                /*
                case COMMANDS_COMMAND_DEACTIVATE:
                    s = "COMMAND DEACTIVATE";
                    payload = ByteBuffer.allocate(5);
                    payload.put(service_deactivate);
                    payload.put(COMMAND_MODE_SERVICE_ID);
                    break;
                case COMMANDS_RT_ACTIVATE:
                    s = "RT_ACTIVATE";
                    payload = ByteBuffer.allocate(7);
                    payload.put(service_activate);
                    payload.put(REMOTE_TERMINAL_SERVICE_ID);
                    payload.put(RT_MODE_MAJOR_VERSION);
                    payload.put(RT_MODE_MINOR_VERSION);
                    break;
                case COMMANDS_RT_DEACTIVATE:
                    s = "RT DEACTIVATE";
                    payload = ByteBuffer.allocate(5);
                    payload.put(service_deactivate);
                    payload.put(REMOTE_TERMINAL_SERVICE_ID);
                    break;*/
            case COMMANDS_SERVICES_VERSION:
                s = "COMMAND_SERVICES_VERSION";
                payload = ByteBuffer.allocate(5);
                payload.put((byte)16);
                payload.put((byte)0);
                payload.put((byte) (((short)0x9065) & 0xFF));
                payload.put((byte) ((((short)0x9065)>>8) & 0xFF));
                payload.put((byte)0xb7);
                //state=state.COMM_VER;
                break;
            case REMOTE_TERMINAL_VERSION:
                s = "REMOTE_TERMINAL_VERSION";
                payload = ByteBuffer.allocate(5);
                payload.put((byte)16);
                payload.put((byte)0);
                payload.put((byte) (((short)0x9065) & 0xFF));
                payload.put((byte) ((((short)0x9065)>>8) & 0xFF));
                payload.put((byte)0x48);
                //state=state.RT_VER;
                break;
            case BINDING:
                    s = "BINDING";
                    payload = ByteBuffer.allocate(5);
                    payload.put((byte)16);
                    payload.put((byte)0);
                    payload.put((byte) (((short)0x9095) & 0xFF));
                    payload.put((byte) ((((short)0x9095)>>8) & 0xFF));
                    payload.put((byte) 0x48);		//Binding OK
                    break;
                /*
                case NONE:
                    s="";
                break;*/
            default:
                s = "uknown subcommand: "+command;
                break;
        }

        if(payload != null)
        {
            sendData(payload,true,btConn);
        }
    }

    public static void sendAppDisconnect(BTConnection btConn) {
        ByteBuffer payload = null;
        byte[] connect_app_layer = new byte[]
                {
                        // VERSION: 8 bits \    Service ID \               Command ID
                        16    ,  0,    0x5a, 0x00
                };
        payload = ByteBuffer.allocate(6);				//4 bytes for application header, 4 for payload
        payload.put(connect_app_layer);					//Add prefix array
        payload.order(ByteOrder.LITTLE_ENDIAN);
        payload.putShort((byte)0x6003);

        Application.sendData(payload,true,btConn);
    }

    public static void cmdPing(BTConnection btConn)
    {
        ByteBuffer payload = ByteBuffer.allocate(4);
        payload.put((byte)16);
        payload.put((byte)0xB7);
        payload.put((byte) (0x9AAA & 0xFF));
        payload.put((byte) ((0x9AAA>>8) & 0xFF));

        sendData(payload, true, btConn);
    }
}
