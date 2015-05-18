package com.gmail.rallen.gridstrument;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity
        extends ActionBarActivity
        implements TuningDialogFragment.OnTuningDialogDoneListener
{
    private final static int PREF_REQ_CODE = 99;

    private GridGLSurfaceView mGLView;

    private String     mOSCServerIP   = "";
    private int        mOSCServerPort = 0;
    private OSCPortOut mOSCPortOut    = null;

    private ArrayList<Integer> mBaseNotes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SetupBaseNotes();

        mGLView = new GridGLSurfaceView(this, mBaseNotes);
        setContentView(mGLView);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        mGLView.setDPI(dm.xdpi, dm.ydpi);

        SetupOSC();

        /*
        final View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        int height = decorView.getHeight();
                        Log.i("YYY", " Current height: " + height);
                        // Note that system bars will only be "visible" if none of the
                        // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            // The system bars are visible.
                            ActionBar actionBar = getSupportActionBar();
                            actionBar.show();
                        } else {
                            // The system bars are NOT visible.
                            ActionBar actionBar = getSupportActionBar();
                            actionBar.hide();
                        }
                    }
                });
                */
    }

    /* TODO figure out how to go fullscreen + use the actionbar
     * http://stackoverflow.com/questions/30104190/showing-the-actionbar-in-immersive-sticky-mode
     *
    // this works, but when enabled, I've lost my actionbar.
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        final View decorView = getWindow().getDecorView();
        int height = decorView.getHeight();
        Log.i("XXX", "focus=" + hasFocus + " Current height: " + height);
        if (hasFocus) {
            decorView.setSystemUiVisibility(
            // tried removing 1st 3...no change
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        }
    }
    /* */

    private void SetupBaseNotes() {
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int default_num = Integer.parseInt(getString(R.string.default_num_base_notes));
        int numBaseNotes = SP.getInt("num_base_notes", default_num);
        for(int i = 0; i < numBaseNotes; i++) {
            mBaseNotes.add(SP.getInt("base_note_" + i, 48 + 5 * i));
        }
    }

    public void ResizeBaseNotes(ArrayList<Integer> baseNotes) {
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = SP.edit();
        editor.putInt("num_base_notes", baseNotes.size());
        mBaseNotes.clear();
        for(int i = 0; i < baseNotes.size(); i++) {
            mBaseNotes.add(i,baseNotes.get(i));
            editor.putInt("base_note_" + i,baseNotes.get(i));
        }
        editor.commit();
    }

    private void SetupOSC() {
        boolean needsUpdate = false;

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String prefServerIP = SP.getString("server_ip", getString(R.string.default_host_ip));
        int prefServerPortNum = GetIntDefault(SP,"server_port",R.string.default_host_port);
        int prefPitchBendRangeNum = GetIntDefault(SP,"pitch_bend_range",R.string.default_pitch_bend_range);
        Log.i("preferences", "server_ip        = " + prefServerIP);
        Log.i("preferences", "server_port      = " + prefServerPortNum);
        Log.i("preferences", "pitch_bend_range = " + prefPitchBendRangeNum);
        if(!mOSCServerIP.equals(prefServerIP)) {
            needsUpdate = true;
            mOSCServerIP = prefServerIP;
        }
        if(mOSCServerPort != prefServerPortNum) {
            needsUpdate = true;
            mOSCServerPort = prefServerPortNum;
        }
        // no harm to always setting these
        mGLView.setPitchBendRange(prefPitchBendRangeNum);

        if(needsUpdate) {
            try {
                // Connect to some IP address and port
                mOSCPortOut = new OSCPortOut(InetAddress.getByName(mOSCServerIP), mOSCServerPort);
            } catch (UnknownHostException e) {
                // Error handling when your IP isn't found
                Log.e("OSCPortOut", "Cannot find OSCServerIP " + mOSCServerIP + ":" + mOSCServerPort + " " + e);
                mOSCPortOut = null;
            } catch (Exception e) {
                // Error handling for any other errors
                Log.e("OSCPortOut", "Unknown exception " + e);
                mOSCPortOut = null;
            }
            mGLView.setOSCPortOut(mOSCPortOut);
        }

    }

    // get value from shared preference, but if not found, get it from the resource_id
    private int GetIntDefault(SharedPreferences SP, String s, int default_id) {
        String prefStr = SP.getString(s, getString(default_id));
        int v;
        try {
            v = Integer.parseInt(prefStr);
        } catch(NumberFormatException ex) {
            v = Integer.parseInt(getString(default_id));
        }
        return v;
    }

    // get value from shared preference, but if not found, get it from the default_value
    private int GetIntDefaultInt(SharedPreferences SP, String s, int default_value) {
        String prefStr = SP.getString(s, ""+default_value);
        int v;
        try {
            v = Integer.parseInt(prefStr);
        } catch(NumberFormatException ex) {
            v = default_value;
        }
        return v;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case R.id.action_notes_off:
            Log.d("select", "NOTES OFF!");
            if (mOSCPortOut != null) {
                for (int c = 0; c < 16; c++) {
                    new OSCSendMessageTask(String.format("/vkb_midi/%d/cc/%d",c,123)).execute(0);
                }
            }
            return true;
        case R.id.action_settings:
            Log.d("select", "preferences...");
            Intent i = new Intent(this, MainPreferenceActivity.class);
            startActivityForResult(i, PREF_REQ_CODE);
            return true;
        case R.id.action_tuning:
            Log.d("select", "tuning...");
            ArrayList<Integer> v = mGLView.getBaseNotes();
            TuningDialogFragment dialog = TuningDialogFragment.newInstance(v);
            dialog.show(getSupportFragmentManager(), "TuningDialogFragment");
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==PREF_REQ_CODE) {
            SetupOSC();
        }
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

    public void onTuningDialogDone(ArrayList<Integer> values) {
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = SP.edit();
        for(int i = 0; i < values.size(); i++) {
            mBaseNotes.set(i,values.get(i));
            editor.putInt("base_note_" + i,values.get(i));
        }
        editor.commit();
        mGLView.setBaseNotes(mBaseNotes);
    }
}
