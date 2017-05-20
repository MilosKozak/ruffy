package org.monkey.d.ruffy.ruffy.driver;

import android.app.Activity;
import android.content.SharedPreferences;

import java.security.InvalidKeyException;

/**
 * Created by fishermen21 on 20.05.17.
 */

public class PumpData {
    private String pumpMac;
    private BTConnection btConn;
    private Object pump_tf;
    private Object driver_tf;
    private byte address;
    private byte[] nonceTx;
    private Activity activity;

    public PumpData(Activity activity) {
        this.activity = activity;
        this.nonceTx = new byte[13];
    }

    public static PumpData loadPump(Activity activity,LogHandler handler) {
        PumpData data = new PumpData(activity);
        
        SharedPreferences prefs = activity.getSharedPreferences("pumpdata", Activity.MODE_PRIVATE);
        String dp = prefs.getString("dp",null);
        String pd = prefs.getString("pd",null);
        data.pumpMac = prefs.getString("device",null);

        handler.log("Loading data of Pump "+data.pumpMac);

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
                handler.fail("unable to load keys!");
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
        prefs.edit().putString("nonceTx",Utils.bufferString(nonceTx,nonceTx.length)).apply();
    }

    public void incrementNonceTx() {
        Utils.incrementArray(nonceTx);
        SharedPreferences prefs = activity.getSharedPreferences("pumpdata", Activity.MODE_PRIVATE);
        prefs.edit().putString("nonceTx",Utils.bufferString(nonceTx,nonceTx.length)).apply();
    }

    public Activity getActivity() {
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
        editor.putString("pd",Utils.bufferString(key_pd_de,key_pd_de.length));
        editor.apply();
    }
    public void setAndSaveToPumpKey(byte[] key_dp, Object tf) throws InvalidKeyException {
        byte[] key_dp_de = Twofish_Algorithm.blockDecrypt(key_dp, 0, tf);

        this.driver_tf = Twofish_Algorithm.makeKey(key_dp_de);
        SharedPreferences prefs = getActivity().getSharedPreferences("pumpdata", Activity.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("dp",Utils.bufferString(key_dp_de,key_dp_de.length));
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
