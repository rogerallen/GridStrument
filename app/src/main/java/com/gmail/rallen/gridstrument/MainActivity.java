package com.gmail.rallen.gridstrument;

import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends ActionBarActivity {

    private GridGLSurfaceView mGLView;

    private String     mOSCServerIP   = "10.10.0.19"; // FIXME GUI Config
    private int        mOSCServerPort = 8675;         // FIXME GUI Config
    private OSCPortOut mOSCPortOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a non-GL View
        //setContentView(R.layout.sample_grid_view);

        // or, create a GLSurfaceView instance and set it
        // as the ContentView for this Activity.
        mGLView = new GridGLSurfaceView(this);
        setContentView(mGLView);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        mGLView.setDPI(dm.xdpi,dm.ydpi);

        try {
            // Connect to some IP address and port
            mOSCPortOut = new OSCPortOut(InetAddress.getByName(mOSCServerIP), mOSCServerPort);
        } catch(UnknownHostException e) {
            // Error handling when your IP isn't found
            Log.e("OSCPortOut","Cannot find OSCServerIP "+mOSCServerIP+":"+mOSCServerPort+" "+e);
        } catch(Exception e) {
            // Error handling for any other errors
            Log.e("OSCPortOut","Unknown exception "+e);
        }
        mGLView.setOSCPortOut(mOSCPortOut);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_notes_off) {
            Log.d("select","NOTES OFF!");
            if (mOSCPortOut != null) {
                for(int c = 0; c < 16; c++) {
                    new OSCSendMessageTask("/allNotesOff").execute(c);
                }
            }
            return true;
        } else if (id == R.id.action_settings) {


        }
        return super.onOptionsItemSelected(item);
    }

    private class OSCSendMessageTask extends AsyncTask<Object, Void, Boolean> {
        private String mAddress;
        OSCSendMessageTask(String address) {
            mAddress = address;
        }
        protected Boolean doInBackground(Object... objs) {
            try {
                OSCMessage message = new OSCMessage(mAddress, Arrays.asList(objs));
                mOSCPortOut.send(message);
            } catch (Exception e) {
                Log.e("OSCSendMessageTask","Unknown exception "+e);
            }
            return true;
        }
    }
}
