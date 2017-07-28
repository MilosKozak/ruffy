package org.monkey.d.ruffy.ruffy.driver;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.RemoteException;

import java.security.InvalidKeyException;
import java.util.LinkedList;
import java.util.Set;

/**
 * Created by fishermen21 on 20.05.17.
 */

public class PumpData {
    private String pumpMac;
    private Object pump_tf;
    private Object driver_tf;
    private byte address;
    private byte[] nonceTx;
    private Context activity;

    public PumpData(Context activity) {
        this.activity = activity;
        this.nonceTx = new byte[13];
    }

    public static boolean isPumpBound(Context activity ) {
        SharedPreferences prefs = activity.getSharedPreferences("pumpdata", Activity.MODE_PRIVATE);
        if(prefs==null)
            return false;
        return prefs.getString("device",null) != null;
    }

    public static PumpData loadPump(Context activity, Set<IRTHandler> handlers) {
        PumpData data = new PumpData(activity);
        
        SharedPreferences prefs = activity.getSharedPreferences("pumpdata", Activity.MODE_PRIVATE);
        String dp = prefs.getString("dp",null);
        String pd = prefs.getString("pd",null);
        data.pumpMac = prefs.getString("device",null);

        for(IRTHandler handler : new LinkedList<>(handlers))
        {
            try
            {
                handler.log("Loading data of Pump "+data.pumpMac);
            }catch(RemoteException e)
            {
                handlers.remove(handler);
            }
        }

        if(data.pumpMac != null)
        {
            try {
                data.pump_tf = Twofish_Algorithm.makeKey(Utils.hexStringToByteArray(pd));
                data.driver_tf = Twofish_Algorithm.makeKey(Utils.hexStringToByteArray(dp));
                data.address = (byte)prefs.getInt("address",0);

                data.nonceTx = Utils.hexStringToByteArray(prefs.getString("nonceTx","00 00 00 00 00 00 00 00 00 00 00 00 00"));

            } catch(Exception e)
            {
                e.printStackTrace();
                for(IRTHandler handler : new LinkedList<>(handlers))
                {
                    try
                    {
                        handler.fail("unable to load keys!");
                    }catch(RemoteException e1)
                    {
                        handlers.remove(handler);
                    }
                }
                return null;
            }
        }
        return data;
    }

    public byte[] getNonceTx() {
        return nonceTx;
    }

    public void setAndSaveAddress(byte address) {
        this.address = address;
        SharedPreferences prefs = activity.getSharedPreferences("pumpdata", Activity.MODE_PRIVATE);
        prefs.edit().putInt("address",address).commit();

    }

    public byte getAddress() {
        return address;
    }

    public String getPumpMac() {
        return pumpMac;
    }

    public void resetTxNonce() {
        for (int i = 0; i < nonceTx.length; i++)
            nonceTx[i] = 0;
        SharedPreferences prefs = activity.getSharedPreferences("pumpdata", Activity.MODE_PRIVATE);
        prefs.edit().putString("nonceTx",Utils.byteArrayToHexString(nonceTx,nonceTx.length)).apply();
    }

    public void incrementNonceTx() {
        Utils.incrementArray(nonceTx);
        SharedPreferences prefs = activity.getSharedPreferences("pumpdata", Activity.MODE_PRIVATE);
        prefs.edit().putString("nonceTx",Utils.byteArrayToHexString(nonceTx,nonceTx.length)).apply();
    }

    public Context getActivity() {
        return activity;
    }

    public Object getToPumpKey() {
        return driver_tf;
    }

    public Object getToDeviceKey() {
        return pump_tf;
    }

    public void setAndSaveToDeviceKey(byte[] key_pd, Object tf) throws InvalidKeyException {

        byte[] key_pd_de = Twofish_Algorithm.blockDecrypt(key_pd, 0, tf);
        this.pump_tf = Twofish_Algorithm.makeKey(key_pd_de);

        SharedPreferences prefs = getActivity().getSharedPreferences("pumpdata", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("pd",Utils.byteArrayToHexString(key_pd_de,key_pd_de.length));
        editor.apply();
    }
    public void setAndSaveToPumpKey(byte[] key_dp, Object tf) throws InvalidKeyException {
        byte[] key_dp_de = Twofish_Algorithm.blockDecrypt(key_dp, 0, tf);

        this.driver_tf = Twofish_Algorithm.makeKey(key_dp_de);
        SharedPreferences prefs = getActivity().getSharedPreferences("pumpdata", Activity.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("dp",Utils.byteArrayToHexString(key_dp_de,key_dp_de.length));
        editor.apply();

    }

    public void setAndSavePumpMac(String pumpMac) {
        this.pumpMac = pumpMac;
        SharedPreferences prefs = getActivity().getSharedPreferences("pumpdata", Activity.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("device",pumpMac);
        editor.putBoolean("paired",true);
        editor.apply();
    }
}
