package com.gmail.rallen.gridstrument;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import java.util.regex.Pattern;

public class MainPreferenceActivity extends PreferenceActivity {

    private static final Pattern IP_ADDRESS = Pattern.compile(
            "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\." +
             "(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\." +
             "(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\." +
             "(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9]))"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            // try to validate server ip and server port
            findPreference("server_ip").setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            return IP_ADDRESS.matcher((String) newValue).matches();
                        }
                    });
            findPreference("server_port").setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            int value = ParseInt((String) newValue);
                            return ((value >= 0) && (value <= 65535));
                        }
                    });
            findPreference("pitch_bend_range").setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        int value = ParseInt((String) newValue);
                        return ((value >= 1) && (value <= 24));
                    }
                });
            findPreference("base_note").setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            int value = ParseInt((String) newValue);
                            return ((value >= 0) && (value <= 127));
                        }
                    });
        }

        private static int ParseInt(String newValue) {
            int value;
            try {
                value = Integer.parseInt(newValue);
            } catch(NumberFormatException ex) {
                value = -9999;
            }
            return value;
        }
    }

}