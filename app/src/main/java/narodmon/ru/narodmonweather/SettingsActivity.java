package narodmon.ru.narodmonweather;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.view.MenuItem;

import java.util.Calendar;

import narodmon.ru.utils.APIService;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        getActionBar().setIcon(R.drawable.settings);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        SharedPreferences sp = getPreferenceScreen().getSharedPreferences();

        // API URL
        EditTextPreference apiUrl = (EditTextPreference) findPreference("apiUrl");
        apiUrl.setSummary(sp.getString("apiUrl", getString(R.string.default_api_url)));

        // DEVICE NAME
        String model = Build.MODEL;
        final EditTextPreference deviceName = (EditTextPreference) findPreference("deviceName");
        deviceName.setText(sp.getString("deviceName", model));

        // IMEI
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        Preference imei = findPreference("reg_imei");
        imei.setSummary(telephonyManager.getDeviceId());

        Preference myPref = (Preference) findPreference("about");

        myPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent aboutActivity = new Intent(getBaseContext(),
                        AboutActivity.class);
                startActivity(aboutActivity);
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        Preference pref = findPreference(key);
        if (key.equals("apiUrl")) {
            EditTextPreference etp = (EditTextPreference) pref;
            pref.setSummary(etp.getText());
        }

        if(key.equals("sendData")) {
            CheckBoxPreference sendData = (CheckBoxPreference) findPreference(key);
            if(sendData.isChecked()) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.SECOND, 10);
                Intent apiService = new Intent(this, APIService.class);
                PendingIntent pIntApiService = PendingIntent.getService(this, 0, apiService, 0);
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                Long requestDelay = Long.valueOf(preferences.getString("interval", "10"));
                AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
                alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), requestDelay * 60000, pIntApiService);
            } else {
                Intent apiService = new Intent(this, APIService.class);
                PendingIntent pIntApiService = PendingIntent.getService(this, 0, apiService, 0);
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                alarmManager.cancel(pIntApiService);
            }
        }
    }
}
