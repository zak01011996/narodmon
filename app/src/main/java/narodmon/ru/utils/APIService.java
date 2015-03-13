package narodmon.ru.utils;

import android.app.Service;
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
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class APIService extends Service implements SensorEventListener {

    SharedPreferences preferences;
    private SensorManager sensorManager;
    private DecimalFormat df = new DecimalFormat("0.0");
    boolean isBarometer, isThermometer, isLight, isLocation, withCoordinates;
    private String pressure = null, temperature = null, light = null,
                          lat = null, lng = null, alt = null, humidity = null;


    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        withCoordinates = preferences.getBoolean("sendGeoData", true);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        Sensor temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        isBarometer = sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        isThermometer = sensorManager.registerListener(this, temperatureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        isLight = sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);

        if(withCoordinates) {
            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            LocationListener locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    lat = String.valueOf(location.getLatitude());
                    lng = String.valueOf(location.getLongitude());
                    //alt = String.valueOf(location.getAltitude());
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


        new Thread(new Runnable() {
            boolean isInterrupted;
            @Override
            public void run() {
                boolean checker;
                while(!isInterrupted) {
                    try {
                        Thread.sleep(1000);
                        checker = checkSensorValues();
                        if(checker) {
                            isInterrupted = true;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }


    private boolean checkSensorValues() {
        //if((pressure == null || alt == null) && isBarometer) {
        if(pressure == null && isBarometer) {
            return false;
        } else if (temperature == null && isThermometer) {
            return false;
        } else if (light == null && isLight) {
            return false;
        } else if ((lat == null || lng == null) && isLocation) {
            return false;
        } else {
            if(!isBarometer && !isLight && !isThermometer) {
                stopSelf();
            } else {
                sensorManager.unregisterListener(this);
                new HTTPRequestTask().execute();
                stopSelf();
            }
            return true;
        }
    }

    @Override
    public final void onSensorChanged(SensorEvent sensorEvent) {
        Sensor currentSensor = sensorEvent.sensor;
        double sensorValue = sensorEvent.values[0];
        if (currentSensor.getType() == Sensor.TYPE_PRESSURE) {
            pressure = df.format(sensorValue);
            //alt = String.valueOf(SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, sensorEvent.values[0]));
        } else if (currentSensor.getType() == Sensor.TYPE_LIGHT) {
            light = df.format(sensorValue);
        } else if (currentSensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            temperature = df.format(sensorValue);
        } else if(currentSensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
            humidity = df.format(sensorValue);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

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
                    nameValuePairs.add(new BasicNameValuePair("P1", pressure));
                }

                if(temperature != null) {
                    nameValuePairs.add(new BasicNameValuePair("T1", temperature));
                }

                if(humidity != null) {
                    nameValuePairs.add(new BasicNameValuePair("H1", humidity));
                }

                if(light != null) {
                    nameValuePairs.add(new BasicNameValuePair("L1", light));
                }

                if(alt != null && lat != null && lng != null) {
                    //nameValuePairs.add(new BasicNameValuePair("ele", alt));
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
