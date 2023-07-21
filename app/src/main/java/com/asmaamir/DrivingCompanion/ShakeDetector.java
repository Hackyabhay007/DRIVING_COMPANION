package com.asmaamir.DrivingCompanion;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.asmaamir.DrivingCompanion.RealTimeFaceDetection.RealTimeFaceDetectionActivity;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ShakeDetector implements SensorEventListener, LocationListener {
    private static final int SHAKE_THRESHOLD = 1000; // Adjust this value as needed
    private static final int MIN_TIME_BETWEEN_SHAKES = 10000; // 10 seconds
    private static final String MESSAGE = "Hey  , Heavy shake detected! in Car , It may be an accident. Call to confirm: ";

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTime;
    private Context context;
    private String phoneNumber;
    private LocationManager locationManager;
    private Geocoder geocoder;

    private Button stopButton;
    private TextView timerText;
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning;

    boolean isenabled= false;

    boolean accidentdetected = false;
    public ShakeDetector(boolean accidentdetected)
    {
        this.accidentdetected = accidentdetected;
    }
    public ShakeDetector(Context context, String phoneNumber, Button stopButton, TextView timerText) {
        this.context = context;
        this.phoneNumber = phoneNumber;
        this.stopButton = stopButton;
        this.timerText = timerText;

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        geocoder = new Geocoder(context, Locale.getDefault());
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isenabled=true;
                stopTimer();
                timerText.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void startListening() {
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
            Toast.makeText(context, "STARTED ACCIDENT DETECTION ", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Accelerometer not supported", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopListening() {
        sensorManager.unregisterListener(this);
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void startTimer() {
        if (!isTimerRunning) {
            Toast.makeText(context, "Timer started", Toast.LENGTH_SHORT).show();
            stopButton.setVisibility(View.VISIBLE);
            timerText.setVisibility(View.VISIBLE);


            timerText.setText("00");

            countDownTimer = new CountDownTimer(20000, 1) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int secondsRemaining = (int) (millisUntilFinished / 1000);
                    timerText.setText(String.valueOf(secondsRemaining));
                }

                @Override
                public void onFinish() {
                    stopButton.setVisibility(View.INVISIBLE);
                    stopButton.setEnabled(true);
                    timerText.setVisibility(View.INVISIBLE);

                    sendSMS();
                }
            };

            countDownTimer.start();
            isTimerRunning = true;
        }
    }

    private void stopTimer() {

            countDownTimer.cancel();
            timerText.setText("Timer stopped");
            stopButton.setEnabled(false);
            timerText.setVisibility(View.INVISIBLE);
            stopButton.setVisibility(View.INVISIBLE);
            isTimerRunning = false;
    }

    private void sendSMS() {
      if (!isenabled)
      {
          isenabled = false;
          Toast.makeText(context, "SENDING LOCATION", Toast.LENGTH_SHORT).show();
          if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
              try {
                  // Get current location
                  Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                  if (lastLocation != null) {



                      List<Address> addresses = geocoder.getFromLocation(lastLocation.getLatitude(), lastLocation.getLongitude(), 1);
                      String locationLink = "https://maps.google.com/?q=" + lastLocation.getLatitude() + "," + lastLocation.getLongitude();

                      String message = MESSAGE + phoneNumber + "\nLocation: " + locationLink;

                      // Send SMS
                      SmsManager smsManager = SmsManager.getDefault();
                      smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                      Toast.makeText(context, "SMS sent", Toast.LENGTH_SHORT).show();
                  } else {
                      Toast.makeText(context, "Location not available", Toast.LENGTH_SHORT).show();
                  }
              } catch (IOException e) {
                  e.printStackTrace();
              }
          } else {
              Toast.makeText(context, "SMS permission not granted", Toast.LENGTH_SHORT).show();
          }
      }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            double acceleration = Math.sqrt(x * x + y * y + z * z);

            if (acceleration > SHAKE_THRESHOLD) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastShakeTime > MIN_TIME_BETWEEN_SHAKES) {
                    lastShakeTime = currentTime;
                    Toast.makeText(context, "HEAVY SHAKE DETECTED", Toast.LENGTH_SHORT).show();
                    startTimer();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    @Override
    public void onLocationChanged(Location location) {
        // Do nothing
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Do nothing
    }

    @Override
    public void onProviderEnabled(String provider) {
        // Do nothing
    }

    @Override
    public void onProviderDisabled(String provider) {
        // Do nothing
    }
}
