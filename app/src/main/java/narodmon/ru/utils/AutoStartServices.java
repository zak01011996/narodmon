package narodmon.ru.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Calendar;

public class AutoStartServices extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean sendData = preferences.getBoolean("sendData", true);
        if(sendData) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, 10);
            Intent apiService = new Intent(context, APIService.class);
            PendingIntent pIntApiService = PendingIntent.getService(context, 0, apiService, 0);
            Long requestDelay = Long.valueOf(preferences.getString("interval", "10"));
            AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), requestDelay * 60000, pIntApiService);
        }
    }
}
