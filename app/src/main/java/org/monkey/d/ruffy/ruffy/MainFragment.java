package org.monkey.d.ruffy.ruffy;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.monkey.d.ruffy.ruffy.driver.IRTHandler.Stub;
import org.monkey.d.ruffy.ruffy.driver.IRuffyService;
import org.monkey.d.ruffy.ruffy.driver.Ruffy;
import org.monkey.d.ruffy.ruffy.driver.display.Menu;
import org.monkey.d.ruffy.ruffy.driver.display.MenuAttribute;
import org.monkey.d.ruffy.ruffy.view.PumpDisplayView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.text.SimpleDateFormat;
import java.util.Date;



/**
 * A placeholder fragment containing a simple view.
 */
public class MainFragment extends Fragment implements View.OnClickListener {

    private TextView connectLog;
    private TextView frameCounter;
    private Button connect;
    private PumpDisplayView displayView;
    private LinearLayout displayLayout;
    private TextView versionNameView;


    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool( 3 );

    public boolean mServiceBound = false;
    public IRuffyService mBoundService;

    public MainFragment() {
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBound = false;
            mBoundService = null;
            connect.setText("Reconnect");
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBoundService = IRuffyService.Stub.asInterface(service);
            mServiceBound = true;

            try {
                reset.setEnabled(mBoundService.isBoundToPump());
            }catch(Exception e)
            {
                e.printStackTrace();
            }
            try {
                mBoundService.addHandler(handler);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            connect.setEnabled(true);
            if(conOnBind)
            {
                conOnBind=false;
                try {
                    mBoundService.doRTConnect();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        try{
            mBoundService.removeHandler(handler);
        }catch(RemoteException e)
        {
            e.printStackTrace();
        }
        if (mServiceBound) {

            try {
                appendLog("Try to disconnect before exit");
                mBoundService.doRTDisconnect();
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            getActivity().unbindService(mServiceConnection);
            mServiceBound = false;
        }
    }

    private int upRunning = 0;
    private int downRunning = 0;
    private int backRunning = 0;
    private int copyRunning = 0;

    private Runnable upThread = new Runnable()
    {
        @Override
        public void run() {
            while(upRunning >0)
            {
                if(upRunning==1) {
                    upRunning++;
                    rtSendKey(Ruffy.Key.UP,true);
                } else {
                    rtSendKey(Ruffy.Key.UP,false);
                }
                try{sleep(200);}catch(Exception e){}
            }
            rtSendKey(Ruffy.Key.NO_KEY,true);
        }
    };

    private Runnable downThread = new Runnable()
    {
        @Override
        public void run() {
            while(downRunning >0)
            {
                if(downRunning==1) {
                    downRunning++;
                    rtSendKey(Ruffy.Key.DOWN,true);
                }
                else
                {
                    rtSendKey(Ruffy.Key.DOWN,false);
                }
                try{sleep(200);}catch(Exception e){}
            }
            rtSendKey(Ruffy.Key.NO_KEY,true);
        }
    };

    private Runnable copyThread = new Runnable()
    {
        @Override
        public void run() {
            while(copyRunning >0)
            {
                if(copyRunning==1) {
                    copyRunning++;
                    rtSendKey(Ruffy.Key.COPY,true);
                }
                else
                {
                    rtSendKey(Ruffy.Key.COPY,false);
                }
                try{sleep(200);}catch(Exception e){}
            }
            rtSendKey(Ruffy.Key.NO_KEY,true);
        }
    };

    private Runnable backThread = new Runnable()
    {
        @Override
        public void run() {
            while(backRunning >0)
            {
                if(backRunning==1) {
                    backRunning++;
                    rtSendKey(Ruffy.Key.BACK,true);
                }
                else
                {
                    rtSendKey(Ruffy.Key.BACK,false);
                }
                try{sleep(200);}catch(Exception e){}
            }
            rtSendKey(Ruffy.Key.NO_KEY,true);
        }
    };

    private void sleep(long millis)
    {
        try{Thread.sleep(millis);}catch(Exception e){/*ignore*/}
    }

    private void rtSendKey(byte keyCode, boolean changed)  {
        try {
            if (mBoundService.isConnected()) {
                try {
                    mBoundService.rtSendKey(keyCode, changed);
                } catch (RemoteException re) {
                    re.printStackTrace();
                    appendLog("failed keySend: " + re.getMessage());
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void resetPairing()
    {
        try {
            mBoundService.resetPairing();
        }catch(RemoteException re)
        {
            re.printStackTrace();
            appendLog("failed keySend: "+re.getMessage());
        }
    }

    private void setButtons(final boolean enabled)
    {
        if(getActivity()!=null) {
            getActivity().runOnUiThread(new Thread() {
                @Override
                public void run() {
                    check.setEnabled(enabled);
                    menu.setEnabled(enabled);
                    up.setEnabled(enabled);
                    down.setEnabled(enabled);
                    back.setEnabled(enabled);
                    copy.setEnabled(enabled);
                }
            });
        }
    }
    private Stub handler = new Stub() {
        @Override
        public void log(String message) throws RemoteException {
            appendLog(message);
        }

        @Override
        public void fail(String message) throws RemoteException {
            appendLog("fail: "+message);
        /*getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connect.setText("Try Connect again!");
                connect.setEnabled(true);
            }
        });*/
        }

        @Override
        public void requestBluetooth() throws RemoteException {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            getActivity().startActivityForResult(enableBtIntent, 1);
        }

        public void rtStarted()
        {
            setButtons(true);
            if(getActivity()!=null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (mBoundService.isConnected()) {
                                connect.setText("Disconnect");
                            } else {
                                connect.setText("Connect");
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        connect.setEnabled(true);
                    }
                });
            }
        }

        @Override
        public void rtClearDisplay() throws RemoteException {
            displayView.clear();
        }

        //TODO just for debug marker byte[][] display = new byte[4][];
        @Override
        public void rtUpdateDisplay(byte[] quarter, int which) throws RemoteException {

            displayView.update(quarter,which);

            //TODO just for debug marker display[which] = quarter;
            if (connectLog.getVisibility() != View.GONE)
                if(getActivity()!=null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connectLog.setVisibility(View.GONE);
                        }
                    });
                }
        }

        @Override
        public void rtDisplayHandleMenu(final Menu menu) throws RemoteException {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String s = "";
                    for(MenuAttribute ma : menu.attributes())
                    {
                        s+="\n"+ma+": "+menu.getAttribute(ma);
                    }

                    frameCounter.setText("display found: "+menu.getType()+s);
                }
            });
        }

        @Override
        public void rtDisplayHandleNoMenu() throws RemoteException {
            if(getActivity()!=null) {
                if(getActivity()!=null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            frameCounter.setText("no display found");
/*                    DisplayParser.findMenu(display, new DisplayParserHandler() {
                        @Override
                        public void menuFound(Menu menu) {

                        }

                        @Override
                        public void noMenuFound() {

                        }
                    });*///TODO just for debug marker
                        }
                    });
                }
            }
        }

        public void rtStopped()
        {
            setButtons(false);
            if(getActivity()!=null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectLog.setVisibility(View.VISIBLE);
                    }
                });
            }
        }
    };

    private Button check;
    private Button menu;
    private Button up;
    private Button down;
    private Button back;
    private Button copy;
    private Button reset;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_main, container, false);
        
        try {
            versionNameView = (TextView) v.findViewById(R.id.versionName);
            versionNameView.setText(getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {}
        
        connect = (Button) v.findViewById(R.id.main_connect);
        connect.setOnClickListener(this);
        connect.setEnabled(false);

        reset = (Button) v.findViewById(R.id.main_reset);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getContext()).setTitle("remove bonding?").setMessage("Really delete bonding informations with pump?").setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        resetPairing();
                        getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.container,new SetupFragment()).addToBackStack("Start").commit();
                    }
                }).setNegativeButton("NO",null).show();
            }
        });
        reset.setEnabled(false);

        connectLog = (TextView) v.findViewById(R.id.main_log);
        connectLog.setMovementMethod(new ScrollingMovementMethod());
        connectLog.setTextIsSelectable(true);

        displayLayout= (LinearLayout) v.findViewById(R.id.pumpPanel);
        displayView = (PumpDisplayView) displayLayout.findViewById(R.id.pumpView);

        frameCounter = (TextView) v.findViewById(R.id.frameCounter);
        menu = (Button) displayLayout.findViewById(R.id.pumpMenu);
        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rtSendKey(Ruffy.Key.MENU,true);
                sleep(100);
                rtSendKey(Ruffy.Key.NO_KEY,true);
            }
        });
        check = (Button) displayLayout.findViewById(R.id.pumpCheck);
        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rtSendKey(Ruffy.Key.CHECK,true);
                sleep(100);
                rtSendKey(Ruffy.Key.NO_KEY,true);
            }
        });
        up = (Button) displayLayout.findViewById(R.id.pumpUp);
        up.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        if(upRunning==0) {
                            upRunning = 1;
                            scheduler.execute(upThread);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        upRunning=0;
                        break;
                }

                return false;
            }
        });
        back = (Button) displayLayout.findViewById(R.id.pumpBack);
        back.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        if(backRunning==0) {
                            backRunning = 1;
                            scheduler.execute(backThread);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        backRunning=0;
                        break;
                }

                return false;
            }
        });
        down= (Button) displayLayout.findViewById(R.id.pumpDown);
        down.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        if(downRunning==0) {
                            downRunning = 1;
                            scheduler.execute(downThread);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        downRunning=0;
                        break;
                }

                return false;
            }
        });
        copy= (Button) displayLayout.findViewById(R.id.pumpCopy);
        copy.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        if(copyRunning==0) {
                            copyRunning = 1;
                            scheduler.execute(copyThread);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        copyRunning=0;
                        break;
                }

                return false;
            }
        });
        setButtons(false);

        Intent intent = new Intent(getActivity(), Ruffy.class);
        ComponentName name = getActivity().startService(intent);
        if(name != null)
        {
            if(getActivity().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE))
            {
                Log.v("Start","bound it");
            }
        }
        return v;
    }

    private boolean conOnBind = true;
    @Override
    public void onClick(final View view) {
        view.setEnabled(false);
        if(connect.getText().toString().startsWith("Disco"))
        {
            try {
                mBoundService.doRTDisconnect();//FIXME
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            connect.setText("Connect!");
            return;
        }
        else if(connect.getText().toString().toLowerCase().startsWith("recon"))
        {
            Intent intent = new Intent(getActivity(), Ruffy.class);
            ComponentName name = getActivity().startService(intent);
            if(name != null)
            {
                if(getActivity().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE))
                {
                    conOnBind = true;
                    Log.v("Start","bound it");
                }
            }
            return;
        }
        try {
            mBoundService.doRTConnect();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void appendLog(final String message) {
        String currentDateTime = "NO_DATE";
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss:SSS");
            currentDateTime = dateFormat.format(new Date()); // Find todays date
        } catch (Exception e) {
            e.printStackTrace();
        }

        final String message_time = currentDateTime + " - " + message;
        Log.v("RUFFY_LOG", message);

        if(connectLog.getVisibility()!=View.GONE) {
            if(getActivity()!=null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (connectLog.getLineCount() < 1000) {
                            connectLog.append("\n" + message_time);
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
    }
}
