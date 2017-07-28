package org.monkey.d.ruffy.ruffy;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class Start extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_start);

        askForBatteryOptimizationPermission();

        getSupportFragmentManager().beginTransaction().add(R.id.container,new MainFragment()).commit();

        SharedPreferences prefs = getSharedPreferences("pumpdata", MODE_PRIVATE);
        boolean paired = prefs.getBoolean("paired", false);
        if (!paired) {
            getSupportFragmentManager().beginTransaction().replace(R.id.container,new SetupFragment()).addToBackStack("Start").commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_start, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void askForBatteryOptimizationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final String packageName = getPackageName();
            final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                final Runnable askOptimizationRunnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                            intent.setData(Uri.parse("package:" + packageName));
                            startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            final String msg = "Device does not appear to support battery optimization whitelisting!";
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(Start.this, "Device does not appear to support battery optimization whitelisting!", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                };

                try {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Please Allow Permission")
                            .setMessage("Ruffy needs battery optimalization whitelisting for proper performance")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    SystemClock.sleep(100);
                                    Start.this.runOnUiThread(askOptimizationRunnable);
                                    dialog.dismiss();
                                }
                            }).show();
                } catch (Exception e) {
                    Toast.makeText(Start.this, "Please whitelist Ruffy in the phone settings.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }


}
