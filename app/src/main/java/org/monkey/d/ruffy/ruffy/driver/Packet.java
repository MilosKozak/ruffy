package org.monkey.d.ruffy.ruffy.driver;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by fishermen21 on 16.05.17.
 */

public class Packet {
    public static byte[] padPacket(byte[] packet)
    {
        byte pad;
        byte[] output;

        pad = (byte) (16 - (packet.length % 16));	//(ENCRYPT_BLOCKSIZE - (packet.length % ENCRYPT_BLOCKSIZE));
        if(pad > 0)
        {
            output = new byte[pad+packet.length];
            for(int n=0;n<packet.length;n++)
                output[n] = packet[n];

            for(int i=0;i < pad;i++)
            {
                output[packet.length+i] = pad;
            }
        }
        else
            output =  packet;

        return output;
    }

    public static List<Byte> buildPacket(byte[] command, ByteBuffer payload, boolean address,BTConnection btConn)
    {
        List<Byte> output = new ArrayList<Byte>();

        for(int i=0; i < command.length; i++)
            output.add(command[i]);

        if(address)											//Replace the default address with the real one
        {
            output.remove(command.length-1);				//Remove the last byte (address)
            output.add(btConn.getAddress());		//Add the real address byte
        }

        Packet.addNonce(output, btConn.getNonceTx());

        if(payload!=null)
        {
            payload.rewind();
            for(int i=0;i<payload.capacity();i++)
                output.add(payload.get());
        }

        return output;
    }

    public static void adjustLength(List<Byte> packet, int length)
    {
        packet.set(2, (byte) (length & 0xFF));
        packet.set(3, (byte) (length >> 8));
    }


    public static void addNonce(List<Byte> packet, byte[] nonce)
    {
        for(int i=0;i<nonce.length;i++)
            packet.add(nonce[i]);
    }

}
