package com.gmail.rallen.gridstrument;

/* JavaDoc http://www.humatic.de/htools/nmj/javadoc/overview-summary.html */
import de.humatic.nmj.NMJConfig;
import de.humatic.nmj.NMJSystemListener;
import de.humatic.nmj.NetworkMidiListener;
import de.humatic.nmj.NetworkMidiSystem;
import de.humatic.nmj.NetworkMidiOutput;
//import de.humatic.nmj.NetworkMidiInput;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends ActionBarActivity implements NetworkMidiListener, NMJSystemListener {

    private GridGLSurfaceView mGLView;

    private NetworkMidiSystem mMidiSys;
    private NetworkMidiOutput mMidiOut;
    //private NetworkMidiInput mMidiIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a non-GL View
        //setContentView(R.layout.sample_grid_view);

        // or, create a GLSurfaceView instance and set it
        // as the ContentView for this Activity.
        mGLView = new GridGLSurfaceView(this);
        setContentView(mGLView);

        // Initialize Midi System
        AsyncLoadMidiSys(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            return true; // FIXME add something...
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try{
            mMidiSys.exit();
            // FIXME cleanup all but the one channel that I want
            int numChannels = NMJConfig.getNumChannels();
            Log.i("cleanup",String.format("4 numChannels: %d", numChannels));
            NMJConfig.cleanup(4, null);
        } catch (Exception e){e.printStackTrace();}
    }

    // ======================================================================
    // NMJ Code
    // ======================================================================
    private void AsyncLoadMidiSys(final Context ctx) {
        AsyncTask at = new AsyncTask() {
            @Override
            protected Object doInBackground(Object... params) {
                try {
                    mMidiSys = NetworkMidiSystem.get(ctx);
                    return mMidiSys;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
            @Override
            protected void onPostExecute(Object result) {
                InitMidiSys();
            }
            protected void onProgressUpdate(Integer... prg) {} // ???
        };
        at.execute(new Object[1]);
    }
    private void InitMidiSys() {
        NMJConfig.addSystemListener(this);
        Log.i("initMidiSys",String.format("sysInfo:\n %s verInfo: %s", NMJConfig.getSystemInfo(), NMJConfig.getVersionInfo()));

        //NMJConfig.resetAll(); // For Development, handy to use just once sometimes.

        int connectivity = NMJConfig.getConnectivity(this);
        boolean adbAllowed = (connectivity & NMJConfig.CONNECTIVITY_ADB) != 0;
        if(!adbAllowed) {
            Log.e("initMidiSys", "ADB NOT ALLOWED.  APP WILL NOT WORK!"); // FIXME dialog box?
        }
        int numChannels = NMJConfig.getNumChannels();
        Log.i("initMidiSys",String.format("channels: %d adbOk? %s", numChannels, adbAllowed));
        // if 3 channels, then add 1 ADB channel.  That's all we want.
        if(numChannels == 3) {
            Log.i("initMidiSys","Adjusting from 3 default to 3+1 ADB channel.");
            // add 1 ADB channel
            NMJConfig.setNumChannels(4);
            NMJConfig.setMode(3, NMJConfig.ADB);
        }
        NMJConfig.setLocalClientPrefix("GridStrument");
        numChannels = NMJConfig.getNumChannels();
        String channelName = NMJConfig.getName(3);
        try{
            mMidiOut = mMidiSys.openOutput(3, this);
            mGLView.setMidiOutput(mMidiOut);
        } catch (Exception e){
            Log.e("initMidiSys","CANNOT OPEN MIDI OUTPUT");
        }
        Log.i("initMidiSys",String.format("channels: %d name: %s", numChannels, channelName));
    }
    // nmj required overrides
    @Override
    public void midiReceived(int channel, int ssrc, byte[] data, long timestamp) {
        Log.i("midiReceived", String.format("channel: %d ssrc: %d len: %d timestamp: %ld", channel, ssrc, data.length, timestamp));
    }
    @Override
    public void systemChanged(int channel, int property, int value) {
        Log.i("systemChanged", String.format("channel: %d property: %d value: %d", channel, property, value));
        if(property == NMJConfig.CH_STATE) {
            switch(value) {
                case NMJConfig.RTPA_CH_CLIENT_CONNECT: Log.i("systemChanged","RTPA_CH_CLIENT_CONNECT"); break;
                case NMJConfig.RTPA_CH_CLIENT_DISCONNECT: Log.i("systemChanged","RTPA_CH_CLIENT_DISCONNECT"); break;
                case NMJConfig.RTPA_CH_CONNECTED: Log.i("systemChanged","RTPA_CH_CONNECTED"); break;
                case NMJConfig.RTPA_CH_DISCONNECTED: Log.i("systemChanged","RTPA_CH_DISCONNECTED"); break;
                case NMJConfig.RTPA_CH_DISCOVERED: Log.i("systemChanged","RTPA_CH_DISCOVERED"); break;
                case NMJConfig.RTPA_CH_GONE: Log.i("systemChanged","RTPA_CH_GONE"); break;
                case NMJConfig.RTPA_CH_LOST: Log.i("systemChanged","RTPA_CH_LOST"); break;
                case NMJConfig.RTPA_CH_PRESENT: Log.i("systemChanged","RTPA_CH_PRESENT"); break;
                case NMJConfig.RTPA_CH_WAITING: Log.i("systemChanged","RTPA_CH_WAITING"); break;
                case NMJConfig.RTPA_NO_RESPONSE: Log.i("systemChanged","RTPA_NO_RESPONSE"); break;
                case NMJConfig.RTPA_PKT_LOSS: Log.i("systemChanged","RTPA_PKT_LOSS"); break;
                case NMJConfig.RTPA_REMOTE_ERR: Log.i("systemChanged","RTPA_REMOTE_ERR"); break;
                default: Log.i("systemChanged","!!!UNKNOWN!!!"); break;
            }
        }
    }
    @Override
    public void systemError(int channel, int err, String description) {
        Log.e("systemError", String.format("channel: %d err: %d desc: %s", channel, err, description));
    }
}
