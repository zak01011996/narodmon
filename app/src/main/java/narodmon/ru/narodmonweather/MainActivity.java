package narodmon.ru.narodmonweather;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import narodmon.ru.utils.APIService;


public class MainActivity extends Activity implements SensorEventListener {

    private TextView pressureTv, temperatureTv, lightTv, pressureMeasurementTv;
    private DecimalFormat df = new DecimalFormat("0.0");
    boolean isBarometer, isThermometer, isLight, isLocation, withCoordinates;
    private String pressure = null, temperature = null, light = null,
            lat = null, lng = null, alt = null;
    private SharedPreferences preferences;
    private boolean isBlocked = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getActionBar().setTitle(getResources().getString(R.string.narod_mon));
        getActionBar().setIcon(R.drawable.sensor);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean sendData = preferences.getBoolean("sendData", true);
        withCoordinates = preferences.getBoolean("sendGeoData", true);

        if(sendData) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, 10);
            Intent apiService = new Intent(this, APIService.class);
            PendingIntent pIntApiService = PendingIntent.getService(this, 0, apiService, 0);
            Long requestDelay = Long.valueOf(preferences.getString("interval", "10"));
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), requestDelay * 60000, pIntApiService);
        }

        if(withCoordinates) {
            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            LocationListener locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    lat = String.valueOf(location.getLatitude());
                    lng = String.valueOf(location.getLongitude());
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {}

                public void onProviderEnabled(String provider) {}

                public void onProviderDisabled(String provider) {}
            };

            isLocation = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        } else {
            isLocation = false;
        }

        pressureTv = (TextView) findViewById(R.id.pressure_sensor_result);
        temperatureTv = (TextView) findViewById(R.id.temperature_sensor_result);
        lightTv = (TextView) findViewById(R.id.light_sensor_result);
        pressureMeasurementTv = (TextView) findViewById(R.id.pressure_measurement);

        implementSensors();
    }

    public void submitSensorData(View v) {
        if(!isBarometer && !isLight && !isThermometer) {
            Toast.makeText(this, getResources().getString(R.string.toast_no_sensors), Toast.LENGTH_SHORT).show();
        } else if ((lat == null || lng == null) && isLocation) {
            Toast.makeText(this, getResources().getString(R.string.toast_not_enough_data), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getResources().getString(R.string.toast_sending_data), Toast.LENGTH_SHORT).show();
            new HTTPRequestTask().execute();
        }
    }

    private void implementSensors() {
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        Sensor temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        isBarometer = sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        isThermometer = sensorManager.registerListener(this, temperatureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        isLight = sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent settingsActivity = new Intent(getBaseContext(),
                    SettingsActivity.class);
            startActivity(settingsActivity);
        } else if (id == R.id.action_about) {
            Intent aboutActivity = new Intent(getBaseContext(),
                    AboutActivity.class);
            startActivity(aboutActivity);
        } else if (id == R.id.action_map) {
            Intent mapActivity = new Intent(getBaseContext(),
                    MapActivity.class);
            startActivity(mapActivity);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor currentSensor = sensorEvent.sensor;
        double sensorValue = sensorEvent.values[0];

        if (currentSensor.getType() == Sensor.TYPE_PRESSURE && isBarometer) {
            pressure = df.format(sensorValue);
            alt = String.valueOf(SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, sensorEvent.values[0]));

            sensorValue = sensorValue * 0.75;
            String pressureText = df.format(sensorValue);
            this.pressureTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
            this.pressureMeasurementTv.setVisibility(View.VISIBLE);
            this.pressureTv.setText(pressureText);
        } else if (!isBarometer) {
            this.pressureTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            this.pressureTv.setText(getString(R.string.no_sensor));
            this.pressureMeasurementTv.setVisibility(View.INVISIBLE);
        }

        if (currentSensor.getType() == Sensor.TYPE_LIGHT && isLight) {
            light = df.format(sensorValue);

            this.lightTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
            this.lightTv.setText(light + " " + getString(R.string.measurement_light));
        } else if(!isLight) {
            this.lightTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            this.lightTv.setText(getString(R.string.no_sensor));
        }

        if (currentSensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE && isThermometer) {
            temperature = df.format(sensorValue);
            this.temperatureTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
            this.temperatureTv.setText(temperature + " \u2103");
        } else if(!isThermometer) {
            this.temperatureTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            this.temperatureTv.setText(getString(R.string.no_sensor));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    private class HTTPRequestTask extends AsyncTask<Object, Object, Object> {

        @Override
        protected Object doInBackground(Object[] objects) {
            sendData();
            return null;
        }

        public void sendData() {
            String apiUrl = preferences.getString("apiUrl", "http://narodmon.ru/post.php");
            TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
            String imei=telephonyManager.getDeviceId();

            String model = Build.MODEL;
            String deviceName = preferences.getString("deviceName", model);
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(apiUrl);
            try {
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                nameValuePairs.add(new BasicNameValuePair("ID", imei));

                if(!deviceName.equals("")) {
                    nameValuePairs.add(new BasicNameValuePair("NAME", deviceName));
                }

                if(pressure != null) {
                    nameValuePairs.add(new BasicNameValuePair(imei + "01", pressure));
                }

                if(temperature != null) {
                    nameValuePairs.add(new BasicNameValuePair(imei + "02", temperature));
                }

                if(light != null) {
                    nameValuePairs.add(new BasicNameValuePair(imei + "03", light));
                }

                if(alt != null && lat != null && lng != null) {
                    nameValuePairs.add(new BasicNameValuePair("ele", alt));
                }

                if(lat != null && lng != null) {
                    nameValuePairs.add(new BasicNameValuePair("lat", lat));
                    nameValuePairs.add(new BasicNameValuePair("lng", lng));
                }
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                httpclient.execute(httppost);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
