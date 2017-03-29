package com.mobilecomputing.sebastian.mobilecomputinglab4;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;

public class BeaconMonitoring extends AppCompatActivity implements BeaconConsumer{
    private static final String TAG = "TESTING-BEACONS";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 4;
    private static final String API_URL = "http://192.168.1.12:8080";
    private static final String ENTER = "/enter/";
    private static final String LEAVE = "/leave/";

    private BeaconManager mBeaconManager;
    private RequestQueue requestQ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBeaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());
        mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        mBeaconManager.bind(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                            requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
        Log.d(TAG, "Start");

        requestQ = Volley.newRequestQueue(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    @Override
    public void onBeaconServiceConnect(){
        Log.d(TAG, "Connect");
        mBeaconManager.addMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                Log.i(TAG, "I just saw an beacon for the first time!");
                Log.i(TAG, "-id1:" + region.getId1());
                Log.i(TAG, "-id2:" + region.getId2());
                sendRegionRequest(1, ENTER);
            }

            @Override
            public void didExitRegion(Region region) {
                Log.i(TAG, "I no longer see an beacon");
                sendRegionRequest(1, LEAVE);
            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                Log.i(TAG, "I have just switched from seeing/not seeing beacons: "+state);
            }
        });

        try {
            mBeaconManager.startMonitoringBeaconsInRegion(new Region("myMonitoringUniqueId", null, null, null));
        } catch (RemoteException e) {
            Log.d(TAG, "Unable to start monitoring for beacons");
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mBeaconManager.unbind(this);
    }


    // Initiate a request to the local node server
    private void sendRegionRequest(int beaconInstance, String action) {
        // Make a request
        final Context self = this;
        Log.d(TAG, API_URL + action + beaconInstance + "/");
        StringRequest res = new StringRequest(Request.Method.POST, API_URL + action + beaconInstance + "/",
                new Response.Listener<String>() {
                    // On response
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Response:" + response);
                        try {
                            Toast toast = Toast.makeText(self, response, Toast.LENGTH_LONG);
                            toast.show();
                        } catch (Exception e) {
                            // Print to console on error
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    // Print to console on Error
                    @Override
                    public void onErrorResponse(VolleyError err) {
                        Log.d(TAG, "Response:" + err.getMessage());
                        err.printStackTrace();
                    }
                }
        );

        // Start the request
        requestQ.add(res);
    }
    private void changeButtonState(){
        Button statusBttn = (Button)findViewById(R.id.statusBttn);
        boolean bttnStatus = (statusBttn.getBackground().equals(R.color.lightRed)) ? false : true;
        if(bttnStatus) {
            // go red because we are green
            statusBttn.setBackgroundColor(getColor(R.color.lightRed));
            statusBttn.setText("Checked Out!");
        }
        else {
            //go green because we are red
            statusBttn.setBackgroundColor(getColor(R.color.green));
            statusBttn.setText("Checked In!");
        }
    }
}
