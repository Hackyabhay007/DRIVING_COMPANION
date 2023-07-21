package com.asmaamir.DrivingCompanion;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.asmaamir.DrivingCompanion.RealTimeFaceDetection.RealTimeFaceDetectionActivity;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    static final int PICK_CONTACT = 1021;

    private static final String FAVORITE_CONTACT_PHONE_KEY = "favorite_contact_phone";

    private TextView drowsyDrivingTips;
    private static final String LAST_SMS_TIME_KEY = "last_sms_time";

    Button selectContactButton;
    private EditText conatctno;
    Button carbtn;
    private static final String TAG = "MainActivity";

    private static DrawerLayout drawerLayout;

    private   TextView selectedContactTextView;
    private static ActionBarDrawerToggle actionBarDrawerToggle;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn = findViewById(R.id.startDrivingMode);
        carbtn = findViewById(R.id.car_btn);
         selectedContactTextView = findViewById(R.id.selected_contact);
         conatctno = findViewById(R.id.editTextPhone);


        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchActivity(RealTimeFaceDetectionActivity.class);
            }
        });


        drowsyDrivingTips = findViewById(R.id.drowsyDrivingTips);
        animateText();

        carbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectToCarSystem();
            }
        });

        Button selectSongButton = findViewById(R.id.select_song_button);
        selectSongButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });

         selectContactButton = findViewById(R.id.select_contact_button);
        selectContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (conatctno.getText().length()!=10)
                {
                    Toast.makeText(MainActivity.this, "ENTER VALID 10 DIGIT NUMBER", Toast.LENGTH_SHORT).show();
                }
                saveFavoriteContact(conatctno.getText().toString());
            }
        });

        checkAndSetFavoriteSong();
        setEmgContact();


    }

    // Function to open the contact picker


    private void connectToCarSystem() {
        // Open Bluetooth settings
        Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivity(intent);

        // Display toast message to connect to car Bluetooth
        Toast.makeText(this, "Please connect to car Bluetooth", Toast.LENGTH_LONG).show();
    }

    // Function to set the favorite contact in SharedPreferences
    private void saveFavoriteContact(String contactPhone) {
        SharedPreferences sharedPreferences = getSharedPreferences("DRIVING_COMPANION", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(FAVORITE_CONTACT_PHONE_KEY, contactPhone);
        editor.apply();
    }

    @SuppressLint("SetTextI18n")
    private void setEmgContact()
    {
        SharedPreferences sharedPreferences = getSharedPreferences("DRIVING_COMPANION", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(LAST_SMS_TIME_KEY);
        editor.apply();

        if (isFavoriteContactSelected()) {

            selectedContactTextView.setVisibility(View.VISIBLE);
            conatctno.setVisibility(View.GONE);
            selectedContactTextView.setText(sharedPreferences.getString(FAVORITE_CONTACT_PHONE_KEY," "));
            selectContactButton.setText("UPDATE");

            selectContactButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    conatctno.setVisibility(View.VISIBLE);
                    selectedContactTextView.setVisibility(View.GONE);

                    if (conatctno.getText().length()!=10)
                    {
                        Toast.makeText(MainActivity.this, "ENTER VALID 10 DIGIT NUMBER", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        saveFavoriteContact(conatctno.getText().toString());
                        Toast.makeText(MainActivity.this, "NUMBER UPDATED", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    // Function to check if a favorite contact is already selected
    private boolean isFavoriteContactSelected() {
        SharedPreferences sharedPreferences = getSharedPreferences("DRIVING_COMPANION", MODE_PRIVATE);
        return sharedPreferences.contains(FAVORITE_CONTACT_PHONE_KEY);
    }
    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(Intent.createChooser(intent, "Select Song"), 203);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 203 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedFileUri = data.getData();
            String songName = getFileName(selectedFileUri);
            String songPath = getFilePath(selectedFileUri);

            // Save the song name and path for later use
            saveSongInfo(songName, songPath);

            // Update the selected music name TextView
            TextView selectedMusicNameTextView = findViewById(R.id.selected_music_name);
            selectedMusicNameTextView.setText("SELECTED MUSIC: "+ songName);
        }

        if (requestCode == PICK_CONTACT && resultCode == Activity.RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            String[] queryFields = new String[]{ContactsContract.Contacts.DISPLAY_NAME};

            Cursor cursor = this.getContentResolver().query(contactUri, queryFields, null, null, null);
            try {
                if (cursor.getCount() == 0) return;

                cursor.moveToFirst();

                String name = cursor.getString(0);
                selectedContactTextView.setText(name);

            } finally {
                cursor.close();
            }

//            if (contactUri != null) {
//                String contactName = retrieveContactName(contactUri);
//                String contactPhone = retrieveContactPhoneNumber(contactUri);
//                setFavoriteContact(contactName, contactPhone);
//            }
        }
    }

    private void checkAndSetFavoriteSong() {
        // Check if a favorite song exists in SharedPreferences
        SharedPreferences preferences = getSharedPreferences("DRIVING_COMPANION", MODE_PRIVATE);
        String savedSongName = preferences.getString("favouringName", null);
        String savedSongPath = preferences.getString("favouringPath", null);

        // If a favorite song exists, set it automatically
        if (savedSongName != null && savedSongPath != null) {
            setFavoriteSong(savedSongName, savedSongPath);
        }
    }

    private void setFavoriteSong(String songName, String songPath) {
        // Update the selected music name TextView
        TextView selectedMusicNameTextView = findViewById(R.id.selected_music_name);
        selectedMusicNameTextView.setText("Selected Music: " + songName);

        // Perform any additional actions with the favorite song, such as playing it
        // For example:
        // playSong(songPath);
    }

    private String getFileName(Uri uri) {
        String fileName = "";
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex != -1) {
                fileName = cursor.getString(nameIndex);
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return fileName;
    }
    private void saveSongInfo(String songName, String songPath) {
        // Implement your preferred storage mechanism to save the song name and path
        // For example, using SharedPreferences:
        SharedPreferences preferences = getSharedPreferences("DRIVING_COMPANION", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("favouringName", songName);
        editor.putString("favouringPath", songPath);
        editor.apply();
    }



    private String getFilePath(Uri uri) {
        String filePath = "";
        String scheme = uri.getScheme();
        if (scheme != null && scheme.equals("file")) {
            filePath = uri.getPath();
        } else {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int pathIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                if (pathIndex != -1) {
                    filePath = cursor.getString(pathIndex);
                }
            }
            if (cursor != null) {
                cursor.close();
            }
        }
        return filePath;
    }


    private void switchActivity(Class c) {
        Intent intent = new Intent(this, c);
        this.startActivity(intent);
    }

    private void animateText() {
        List<String> tips = new ArrayList<>();
        tips.add("Get enough sleep before driving.");
        tips.add("Take regular breaks during long drives.");
        tips.add("Avoid driving during your body's natural sleep hours.");
        tips.add("Limit the use of sedating medications before driving.");
        tips.add("Use a driving companion app for assistance.");
        tips.add("Stay hydrated while driving.");
        tips.add("Avoid distractions like texting or using a phone.");
        tips.add("Observe and follow traffic rules.");
        tips.add("Adjust your seat and mirrors properly before driving.");
        tips.add("Keep a safe following distance from the vehicle in front of you.");
        tips.add("Be aware of blind spots and check them before changing lanes.");
        tips.add("Use your turn signals to indicate your intentions.");
        tips.add("Watch out for pedestrians and cyclists on the road.");
        tips.add("Be cautious in adverse weather conditions.");
        tips.add("Avoid aggressive driving and road rage.");
        tips.add("Pay attention to road signs and markings.");
        tips.add("Drive at a safe and appropriate speed.");
        tips.add("Check your vehicle's brakes, tires, and lights regularly.");
        tips.add("Never drink and drive.");
        tips.add("Follow the instructions of road authorities during road construction.");
        tips.add("If feeling tired or drowsy, take a short nap before continuing.");
        tips.add("Always wear your seatbelt while driving.");
        tips.add("Be patient and courteous to other drivers.");

        final int delay = 2000; // Delay between each text update in milliseconds
        final long duration = 500; // Animation duration in milliseconds

        for (int i = 0; i < tips.size(); i++) {
            final int index = i;
            drowsyDrivingTips.postDelayed(new Runnable() {
                @Override
                public void run() {
                    drowsyDrivingTips.animate()
                            .alpha(0f)
                            .setDuration(duration / 2)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    drowsyDrivingTips.setText(tips.get(index));
                                    drowsyDrivingTips.animate()
                                            .alpha(1f)
                                            .setDuration(duration / 2)
                                            .start();
                                }
                            })
                            .start();
                }
            }, delay * i);
        }
    }



    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }
}