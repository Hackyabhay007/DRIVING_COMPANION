package com.asmaamir.DrivingCompanion.RealTimeFaceDetection;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.asmaamir.DrivingCompanion.R;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.util.List;
import java.util.Timer;
import java.util.logging.Handler;

public class MLKitFacesAnalyzer implements ImageAnalysis.Analyzer {
    private static final String TAG = "MLKitFacesAnalyzer";
    private FirebaseVisionFaceDetector faceDetector;
    private TextureView tv;
    private static final long EYE_CLOSED_DURATION_THRESHOLD = 3000; // 3 seconds in milliseconds
    private long lastEyeOpenTimestamp = 0;
    private ImageView iv;
    private Button playPause;
    private TextView EyesStatus;
    private boolean isBeeping;
    private Bitmap bitmap;

    int sleepCount = 0;

    private static final long BEEP_DURATION = 3000; // 3 seconds in milliseconds
    private Handler handler;
    private Timer beepTimer;
    private Canvas canvas;
    private Paint dotPaint, linePaint;
    private float widthScaleFactor = 1.0f;
    private float heightScaleFactor = 1.0f;
    private FirebaseVisionImage fbImage;
    private CameraX.LensFacing lens;
    private Activity activity;
    private MediaPlayer mediaPlayer;
    boolean isDrowsinessDetected=false;
    boolean plafav=false;

    MLKitFacesAnalyzer(TextureView tv, ImageView iv, CameraX.LensFacing lens, TextView EyesStatus, RealTimeFaceDetectionActivity realTimeFaceDetectionActivity, Button playPause, boolean isDrowsinessDetected, boolean playfav) {
        this.tv = tv;
        this.iv = iv;
        this.lens = lens;
        this.EyesStatus = EyesStatus.findViewById(R.id.eyesStatus);
        this.activity = realTimeFaceDetectionActivity;
        this.playPause = playPause.findViewById(R.id.playpause);
        this.isDrowsinessDetected = isDrowsinessDetected;
        this.plafav = playfav;

        playPause.setOnClickListener(view -> {

            if (playPause.getVisibility() == View.VISIBLE) {
                playPause.setVisibility(View.INVISIBLE);
                resetAlarm();
            }
        });
    }

    @Override
    public void analyze(ImageProxy image, int rotationDegrees) {
        if (image == null || image.getImage() == null) {
            return;
        }
        int rotation = degreesToFirebaseRotation(rotationDegrees);
        fbImage = FirebaseVisionImage.fromMediaImage(image.getImage(), rotation);
        initDrawingUtils();

        initDetector();
        detectFaces();
    }

    private void initDrawingUtils() {
        bitmap = Bitmap.createBitmap(tv.getWidth(), tv.getHeight(), Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        dotPaint = new Paint();
        dotPaint.setColor(Color.RED);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setStrokeWidth(2f);
        dotPaint.setAntiAlias(true);
        linePaint = new Paint();
        linePaint.setColor(Color.GREEN);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2f);
        widthScaleFactor = canvas.getWidth() / (fbImage.getBitmap().getWidth() * 1.0f);
        heightScaleFactor = canvas.getHeight() / (fbImage.getBitmap().getHeight() * 1.0f);
    }

    private void initDetector() {
        FirebaseVisionFaceDetectorOptions
                options =  new FirebaseVisionFaceDetectorOptions.Builder()
                .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .setMinFaceSize(0.15f)
                .build();
        faceDetector = FirebaseVision
                .getInstance()
                .getVisionFaceDetector(options);
    }

    private void startAlarm() {
        playPause.setVisibility(View.VISIBLE);
        isBeeping = true;
        mediaPlayer = MediaPlayer.create(activity, R.raw.alarm);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    private void resetAlarm() {
        if (isBeeping) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
            isBeeping = false;
        }
    }
    private void detectFaces() {
        faceDetector
                .detectInImage(fbImage)
                .addOnSuccessListener(firebaseVisionFaces -> {
                    if (!firebaseVisionFaces.isEmpty()) {
                        processFaces(firebaseVisionFaces);
                    } else {
                        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY);
                    }
                }).addOnFailureListener(e -> Log.i(TAG, e.toString()));
    }
    private void drawContours(List<FirebaseVisionPoint> points) {
        int counter = 0;
        for (FirebaseVisionPoint point : points) {
            if (counter != points.size() - 1) {
                canvas.drawLine(translateX(point.getX()),
                        translateY(point.getY()),
                        translateX(points.get(counter + 1).getX()),
                        translateY(points.get(counter + 1).getY()),
                        linePaint);
            } else {
                canvas.drawLine(translateX(point.getX()),
                        translateY(point.getY()),
                        translateX(points.get(0).getX()),
                        translateY(points.get(0).getY()),
                        linePaint);
            }
            counter++;
            canvas.drawCircle(translateX(point.getX()), translateY(point.getY()), 6, dotPaint);
        }
    }

    private float translateY(float y) {
        return y * heightScaleFactor;
    }

    private float translateX(float x) {
        float scaledX = x * widthScaleFactor;
        if (lens == CameraX.LensFacing.FRONT) {
            return canvas.getWidth() - scaledX;
        } else {
            return scaledX;
        }
    }


    @SuppressLint("SetTextI18n")
    public void processFaces(List<FirebaseVisionFace> faces) {

        long currentTimeStamp = System.currentTimeMillis();

        for (FirebaseVisionFace face : faces) {


            drawContours(face.getContour(FirebaseVisionFaceContour.FACE).getPoints());
            drawContours(face.getContour(FirebaseVisionFaceContour.LEFT_EYEBROW_BOTTOM).getPoints());
            drawContours(face.getContour(FirebaseVisionFaceContour.RIGHT_EYEBROW_BOTTOM).getPoints());
            drawContours(face.getContour(FirebaseVisionFaceContour.LEFT_EYE).getPoints());
            drawContours(face.getContour(FirebaseVisionFaceContour.RIGHT_EYE).getPoints());
            drawContours(face.getContour(FirebaseVisionFaceContour.LEFT_EYEBROW_TOP).getPoints());
            drawContours(face.getContour(FirebaseVisionFaceContour.RIGHT_EYEBROW_TOP).getPoints());
            drawContours(face.getContour(FirebaseVisionFaceContour.LOWER_LIP_BOTTOM).getPoints());
            drawContours(face.getContour(FirebaseVisionFaceContour.LOWER_LIP_TOP).getPoints());
            drawContours(face.getContour(FirebaseVisionFaceContour.UPPER_LIP_BOTTOM).getPoints());
            drawContours(face.getContour(FirebaseVisionFaceContour.UPPER_LIP_TOP).getPoints());
            drawContours(face.getContour(FirebaseVisionFaceContour.NOSE_BRIDGE).getPoints());
            drawContours(face.getContour(FirebaseVisionFaceContour.NOSE_BOTTOM).getPoints());

            float leftEye = face.getLeftEyeOpenProbability();
            float rightEye = face.getRightEyeOpenProbability();

            if ((leftEye < 0.9817605 && rightEye < 0.9817605)) {
                // Eyes are closed
                Log.d("EYE STATUS", "Closed for " +  String.valueOf(leftEye));
                activity.runOnUiThread(() -> EyesStatus.setText("EYES CLOSED"));

                sleepCount++;
                isDrowsinessDetected = true;



                    if (lastEyeOpenTimestamp == 0) {
                        lastEyeOpenTimestamp = currentTimeStamp;
                    } else {
                        long timeElapsed = currentTimeStamp - lastEyeOpenTimestamp;
                        if (timeElapsed >= EYE_CLOSED_DURATION_THRESHOLD && !isBeeping) {
                            startAlarm();
                        }
                    }
                }

            else {
            lastEyeOpenTimestamp = 0;
                Log.d("EYE STATUS", "Open"  + String.valueOf(rightEye) );
                // Eyes are open
                activity.runOnUiThread(() -> EyesStatus.setText("EYES OPEN"));

            }
        }
        iv.setImageBitmap(bitmap);
    }







    private int degreesToFirebaseRotation(int degrees) {
        switch (degrees) {
            case 0:
                return FirebaseVisionImageMetadata.ROTATION_0;
            case 90:
                return FirebaseVisionImageMetadata.ROTATION_90;
            case 180:
                return FirebaseVisionImageMetadata.ROTATION_180;
            case 270:
                return FirebaseVisionImageMetadata.ROTATION_270;
            default:
                throw new IllegalArgumentException("Invalid rotation degrees: " + degrees);
        }
    }
}
