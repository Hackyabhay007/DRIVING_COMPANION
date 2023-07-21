package com.asmaamir.DrivingCompanion.RealTimeFaceDetection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import com.asmaamir.DrivingCompanion.R;
import com.asmaamir.DrivingCompanion.ShakeDetector;

import java.io.IOException;

public class RealTimeFaceDetectionActivity extends AppCompatActivity  implements LocationListener{
    public static final int REQUEST_CODE_PERMISSION = 101;
    public static final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    private TextureView tv;
    private LocationManager locationManager;
    private ImageView iv;
    private TextView EyesStatus;
    private int SPEED_LIMIT = 50;
    private Switch speedLimitSwitch ;

    private Switch takeBreakSwitch ,favourateMusic;

    private Handler handler;
    private Runnable breakReminderRunnable;
    private boolean isDrowsinessDetected;
    private ToneGenerator toneGenerator;
    private MediaPlayer mediaPlayer;
    private float currspeed;


    boolean playfav=false;
    private Button playPause;
     boolean accidentdetected = false;
    private TextView timerTextView,SpeedTextView;
    private static final String TAG = "RealTimeFaceDetectionActivity";
    private static final int PERMISSION_REQUEST_CODE = 123;
    public static CameraX.LensFacing lens = CameraX.LensFacing.FRONT;
    private static final String FAVORITE_CONTACT_PHONE_KEY = "favorite_contact_phone";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_face_detection);
        tv = findViewById(R.id.face_texture_view);
        iv = findViewById(R.id.face_image_view);
        playPause = findViewById(R.id.playpause);
        speedLimitSwitch= findViewById(R.id.speedLimitSwitch);
        takeBreakSwitch = findViewById(R.id.takeBreakSwitch);
        timerTextView = findViewById(R.id.timer);
        SpeedTextView = findViewById(R.id.speedview);
        favourateMusic = findViewById(R.id.favouratemusic);

        // Check if the permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        } else {
            // Permission is already granted, proceed with playing music

        }




        // Check for location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        speedLimitSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (speedLimitSwitch.isChecked())
                {
                    dialog();
                }
                else {
                    Toast.makeText(RealTimeFaceDetectionActivity.this, "SPEED LIMIT STOPPED", Toast.LENGTH_SHORT).show();
                }
            }
        });

        takeBreakSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (takeBreakSwitch.isChecked())
                {
                    handler = new Handler();
                    isDrowsinessDetected = false;

                    // Initialize the ToneGenerator with the system alert sound type
                    toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME);

                    // Start the initial reminder
                    dialogForRemider();
                    startBreakReminder();
                }
                else {
                    Toast.makeText(RealTimeFaceDetectionActivity.this, "REMINDERS ARE DISABLED", Toast.LENGTH_SHORT).show();
                }
            }
        });

        favourateMusic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (favourateMusic.isChecked())
                {

                    playMusicPlaylist();

                }
                else {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                }
            }
        });


        SharedPreferences sharedPreferences = getSharedPreferences("DRIVING_COMPANION", Context.MODE_PRIVATE);
        ShakeDetector shakeDetector = new ShakeDetector(this,sharedPreferences.getString(FAVORITE_CONTACT_PHONE_KEY,"7477066373"),playPause,timerTextView);
        shakeDetector.startListening();

        ShakeDetector s1 = new ShakeDetector(accidentdetected);

        if (allPermissionsGranted()) {
            tv.post(this::startCamera);
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSION);
        }

        EyesStatus = findViewById(R.id.eyesStatus);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_PERMISSION);
        }
    }

    private void playMusicPlaylist() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.music1);
        if (mediaPlayer != null) {
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    // Play the next song when the current song finishes
                    playNextSong();
                }
            });
        }
    }

    private void playNextSong() {
        mediaPlayer = MediaPlayer.create(this, R.raw.music2);
        if (mediaPlayer != null) {
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {

                    playNextSong();
                }
            });
        }
    }

    private void startBreakReminder() {
        breakReminderRunnable = new Runnable() {
            @Override
            public void run() {
                // Check if drowsiness is detected
                if (isDrowsinessDetected) {
                    // If drowsiness is detected, remind for a power nap
                    showBreakNotification("Take a Power Nap");
                } else {
                    // If no drowsiness is detected, remind for a coffee break
                    showBreakNotification("Take a Coffee Break");
                }

                // Schedule the next break reminder after 2 hours
                handler.postDelayed(this, 2 * 60 * 60 * 1000);
            }
        };

        // Schedule the initial break reminder
        handler.postDelayed(breakReminderRunnable, 2 * 60 * 60 * 1000);
    }

    private void showBreakNotification(String message) {
        // Play the system alert sound
        playSystemAlertSound();

        // Create an AlertDialog to display the break reminder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.alert, null);
        builder.setView(dialogView);
        builder.setTitle("Break Reminder");
        builder.setMessage(message);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void playSystemAlertSound() {
        if (toneGenerator != null) {
            // Play the system alert sound with a tone type of TYPE_ALERT
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 2000);
        }
    }
    @SuppressLint("RestrictedApi")
    private void startCamera() {
        initCamera();
        ImageButton ibSwitch = findViewById(R.id.btn_switch_face);
        ibSwitch.setOnClickListener(v -> {
            if (lens == CameraX.LensFacing.FRONT)
                lens = CameraX.LensFacing.BACK;
            else
                lens = CameraX.LensFacing.FRONT;
            try {
                Log.i(TAG, "" + lens);
                CameraX.getCameraWithLensFacing(lens);
                initCamera();
            } catch (CameraInfoUnavailableException e) {
                Log.e(TAG, e.toString());
            }
        });
    }
    private void showSpeedLimitExceededDialog(float currentSpeed, float speedLimit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Speed Limit Exceeded")
                .setMessage("Your current speed is " + currentSpeed + " km/h, which exceeds the speed limit of " + speedLimit + " km/h.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void dialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Speed Limit ENABLED")
                .setMessage("if you cross speed limit at the time of drowsiness a high alarm will be initiated")
                .setPositiveButton("OK", null)
                .show();
    }
    private void dialogForRemider() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("REMINDER FOR BREAKS ARE ENABLED")
                .setMessage("It will remind you after every 2 Hours for Breaks of Power nap or Coffee")
                .setPositiveButton("OK", null)
                .show();
    }
    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            float speed = location.getSpeed(); // Speed in meters/second
            float speedKmh = speed * 3.6f; // Convert to kilometers/hour
            SpeedTextView.setText("CURRENT SPEED "+String.format("%.2f km/h", speedKmh));
            if (speedKmh>50)
            {
                playPause.setVisibility(View.VISIBLE);
                showSpeedLimitExceededDialog(speedKmh,50);
                currspeed = speedKmh;
                playPause.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (playPause.getVisibility()==View.VISIBLE)
                        {
                            playPause.setVisibility(View.INVISIBLE);
                        }
                    }
                });
            }
        }
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    private void initCamera() {
        CameraX.unbindAll();
        PreviewConfig pc = new PreviewConfig
                .Builder()
                .setTargetResolution(new Size(tv.getWidth(), tv.getHeight()))
                .setLensFacing(lens)
                .build();

        Preview preview = new Preview(pc);
        preview.setOnPreviewOutputUpdateListener(output -> {
            ViewGroup vg = (ViewGroup) tv.getParent();
            vg.removeView(tv);
            vg.addView(tv, 0);
            tv.setSurfaceTexture(output.getSurfaceTexture());
        });

        ImageAnalysisConfig iac = new ImageAnalysisConfig
                .Builder()
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .setTargetResolution(new Size(tv.getWidth()/3, tv.getHeight()/3))
                .setLensFacing(lens)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis(iac);
        imageAnalysis.setAnalyzer(Runnable::run,
                new MLKitFacesAnalyzer(tv, iv, lens,EyesStatus,RealTimeFaceDetectionActivity.this,playPause,isDrowsinessDetected,playfav));
        CameraX.bindToLifecycle(this, preview, imageAnalysis);
    }

    private String getSavedSongPath() {
        SharedPreferences preferences = getSharedPreferences("DRIVING_COMPANION", MODE_PRIVATE);
        return preferences.getString("favouringPath", "");
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (allPermissionsGranted()) {
                tv.post(this::startCamera);
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted, proceed with playing music
            } else {
                // Permission is denied, handle accordingly (e.g., show an error message)
            }
        }
    }



    public static void createAlertDialog(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Handle the OK button click
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void createAlert(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Handle the OK button click
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    protected void onDestroy() {
        super.onDestroy();

        // Release the ToneGenerator when the activity is destroyed
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }

        // Remove any pending callbacks when the activity is destroyed
        handler.removeCallbacks(breakReminderRunnable);
    }

}