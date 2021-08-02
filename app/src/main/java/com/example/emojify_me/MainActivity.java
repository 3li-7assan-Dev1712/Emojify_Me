/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.emojify_me;


import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String BITMAP_KEY = "mResultBitmap";
    private static final double SMILING_PROB_THRESHOLD = .13;
    private static final double EYE_OPEN_THRESHOLD= .5;
    private static final String LOG_TAG ="D:Emojify:";
    private static final float EMOJI_SCALE_FACTOR = .9f;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 1;

    private static final String FILE_PROVIDER_AUTHORITY = "com.example.android.fileprovider";

    private ImageView mImageView;

    private Button mEmojifyButton;
    private FloatingActionButton mShareFab;
    private FloatingActionButton mSaveFab;
    private FloatingActionButton mClearFab;

    private TextView mTitleTextView;

    private String mTempPhotoPath;

    private Bitmap mResultsBitmap;

    private ProgressBar imageProgress;
    static List<Face> faceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind the views
        mImageView =findViewById(R.id.image_view);
        mEmojifyButton =  findViewById(R.id.emojify_button);
        mShareFab =  findViewById(R.id.share_button);
        mSaveFab =  findViewById(R.id.save_button);
        mClearFab =  findViewById(R.id.clear_button);
        mTitleTextView =  findViewById(R.id.title_text_view);
        imageProgress = findViewById(R.id.imageProgress);
        if (savedInstanceState != null && mResultsBitmap != null){
            mResultsBitmap = savedInstanceState.getParcelable(BITMAP_KEY);
            mEmojifyButton.setVisibility(View.GONE);
            mTitleTextView.setVisibility(View.GONE);
            mSaveFab.setVisibility(View.VISIBLE);
            mShareFab.setVisibility(View.VISIBLE);
            mClearFab.setVisibility(View.VISIBLE);
            mImageView.setImageBitmap(mResultsBitmap);
        }
    }

    /**
     * OnClick method for "Emojify Me!" Button. Launches the camera app.
     *
     * @param view The emojify me button.
     */
    public void emojifyMe(View view) {
        // Check for the external storage permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // If you do not have permission, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        } else {
            // Launch the camera if the permission exists
            launchCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // Called when you request permission to read and write to external storage
        switch (requestCode) {
            case REQUEST_STORAGE_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // If you get permission, launch the camera
                    launchCamera();
                } else {
                    // If you do not get permission, show a Toast
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    /**
     * Creates a temporary image file and captures a picture to store in it.
     */
    private void launchCamera() {

        // Create the capture image intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the temporary File where the photo should go
            File photoFile = null;
            try {
                photoFile = BitmapUtils.createTempImageFile(this);
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {

                // Get the path of the temporary file
                mTempPhotoPath = photoFile.getAbsolutePath();

                // Get the content URI for the image file
                Uri photoURI = FileProvider.getUriForFile(this,
                        FILE_PROVIDER_AUTHORITY,
                        photoFile);

                // Add the URI so the camera can store the image
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                // Launch the camera activity
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If the image capture activity was called and was successful
        // the user captured an image
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Process the image and set it to the TextView
            processAndSetImage();
        } else /* the user return and didn't take any picture */ {

            // Otherwise, delete the temporary image file
            BitmapUtils.deleteImageFile(this, mTempPhotoPath);
        }
    }

    /**
     * Method for processing the captured image and setting it to the TextView.
     */
    private void processAndSetImage() {

        // Toggle Visibility of the views
        mEmojifyButton.setVisibility(View.GONE);
        mTitleTextView.setVisibility(View.GONE);
        mSaveFab.setVisibility(View.VISIBLE);
        mShareFab.setVisibility(View.VISIBLE);
        mClearFab.setVisibility(View.VISIBLE);

        // Resample the saved image to fit the ImageView


        mResultsBitmap = BitmapUtils.resamplePic(MainActivity.this, mTempPhotoPath);

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();

        final InputImage image = InputImage.fromBitmap(mResultsBitmap, 0);
        final FaceDetector detector = FaceDetection.getClient(options);
        final Task<List<Face>> result = detector.process(image);
        result.addOnSuccessListener(this,
                new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(@NonNull List<Face> faces) {
                        Log.d("Emojifier", "Start processing image to complete");
                        faceList = faces;
                        Toast.makeText(getApplicationContext(), "Yeah, you image has successfully processed!", Toast.LENGTH_SHORT).show();
                        if (faceList.size() == 0) {
                            imageProgress.setVisibility(View.GONE);
                            new AlertDialog.Builder(MainActivity.this).setMessage("There are not any detected face in you image").show();
                        } else {
                            for (Face face : faceList) {
                                Emoji emoji = whichEmoji(face);
                                Bitmap emjiBitmap = getBitmapFromEmoji(emoji, MainActivity.this);
                                mResultsBitmap = addBitmapToFace(mResultsBitmap, emjiBitmap, face);
                            }
                        }
                    }
                });
        result.addOnCompleteListener(
                this,
                new OnCompleteListener<List<Face>>() {
            @Override
            public void onComplete(@NonNull Task<List<Face>> task) {
                imageProgress.setVisibility(View.GONE);
                mImageView.setImageBitmap(mResultsBitmap);
                Log.d("Going to nofiiy", "notify to return the main thread");
                detector.close();
                Toast.makeText(MainActivity.this, "Image processing has been successfully released", Toast.LENGTH_SHORT).show();
            }
        });
        result.addOnCanceledListener(this,
                new OnCanceledListener() {
                    @Override
                    public void onCanceled() {
                        detector.close();
                        Toast.makeText(MainActivity.this, "Processing canceled!", Toast.LENGTH_SHORT).show();
                    }
                });
        if (!result.isComplete()) {
            imageProgress.setVisibility(View.VISIBLE);
            Toast.makeText(MainActivity.this, "Please wait for seconds until you image process", Toast.LENGTH_SHORT).show();

            mImageView.setImageBitmap(mResultsBitmap);
        }
    }


    /**
     * OnClick method for the save button.
     *
     * @param view The save button.
     */
    public void saveMe(View view) {
        // Delete the temporary image file
        BitmapUtils.deleteImageFile(this, mTempPhotoPath);

        // Save the image
        BitmapUtils.saveImage(this, mResultsBitmap);
    }

    /**
     * OnClick method for the share button, saves and shares the new bitmap.
     *
     * @param view The share button.
     */
    public void shareMe(View view) {
        // Delete the temporary image file
        BitmapUtils.deleteImageFile(this, mTempPhotoPath);

        // Save the image
        BitmapUtils.saveImage(this, mResultsBitmap);

        // Share the image
        BitmapUtils.shareImage(this, mTempPhotoPath);
    }

    /**
     * OnClick for the clear button, resets the app to original state.
     *
     * @param view The clear button.
     */
    public void clearImage(View view) {
        // Clear the image and toggle the view visibility
        mImageView.setImageResource(0);
        mEmojifyButton.setVisibility(View.VISIBLE);
        mTitleTextView.setVisibility(View.VISIBLE);
        mShareFab.setVisibility(View.GONE);
        mSaveFab.setVisibility(View.GONE);
        mClearFab.setVisibility(View.GONE);

        // Delete the temporary image file



    }


    static Bitmap getBitmapFromEmoji(Emoji emoji, Context context){
        Bitmap emojiBitmap;
        switch (emoji) {
            case SMILE:
                emojiBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.smile);
                break;
            case FROWN:
                emojiBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.frown);
                break;
            case CLOSED_EYE_SMILE:
                emojiBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.closed_smile);
                break;
            case RIGHT_WINK:
                emojiBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.rightwink);
                break;
            case LEFT_WINK:
                emojiBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.leftwink);
                break;
            case RIGHT_WINK_FROWN:
                emojiBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.rightwinkfrown);
                break;
            case LEFT_WINK_FROWN:
                emojiBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.leftwinkfrown);
                break;
            case CLOSED_EYE_FROWN:
                emojiBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.closed_frown);
                break;
            default:
                emojiBitmap = null;
                Toast.makeText(context, "There is not appropriate emoji :)", Toast.LENGTH_SHORT).show();
        }
        return emojiBitmap;
    }

    static Emoji whichEmoji(Face face){

        Log.d(LOG_TAG, "getClassificiations: smilingProb = " + face.getSmilingProbability());
        Log.d(LOG_TAG, "getClassificiations: leftEyeOpenProb = " + face.getLeftEyeOpenProbability());
        Log.d(LOG_TAG, "getClassificiations: rightEyeOpenProb = " + face.getRightEyeOpenProbability());
        //Toast.makeText(context,"Your smiling probability is : " + face.getIsSmilingProbability(), Toast.LENGTH_LONG).show();
        boolean smiling = true;
        if (face.getSmilingProbability() != null) {
            smiling= face.getSmilingProbability() > SMILING_PROB_THRESHOLD;
        }
        boolean leftEyeClosed = true;
        if (face.getLeftEyeOpenProbability() != null){
            leftEyeClosed= face.getLeftEyeOpenProbability() < EYE_OPEN_THRESHOLD;
        }
        boolean rightEyeClosed = true;
        if (face.getRightEyeOpenProbability() != null) {
            rightEyeClosed= face.getRightEyeOpenProbability() < EYE_OPEN_THRESHOLD;
        }
        Emoji emoji;
        if (smiling){
            if (leftEyeClosed && !rightEyeClosed)
                emoji = Emoji.LEFT_WINK;
            else if (rightEyeClosed && !leftEyeClosed)
                emoji = Emoji.RIGHT_WINK;
            else if (leftEyeClosed)
                emoji = Emoji.CLOSED_EYE_SMILE;
            else
                emoji = Emoji.SMILE;
        }else{
            if (leftEyeClosed && !rightEyeClosed)
                emoji = Emoji.LEFT_WINK_FROWN;
            else if (rightEyeClosed && !leftEyeClosed)
                emoji = Emoji.RIGHT_WINK_FROWN;
            else if (leftEyeClosed)
                emoji = Emoji.CLOSED_EYE_FROWN;
            else
                emoji = Emoji.FROWN;
        }
        Log.d(LOG_TAG, "whichEmoji: " + emoji.name());
        return emoji;
    }

    private static Bitmap addBitmapToFace(Bitmap backgroundBitmap, Bitmap emojiBitmap, Face face) {

        if (backgroundBitmap == null || emojiBitmap == null){
            Log.d("Emojifier.class", "One of the image is null");
        }

        // Initialize the results bitmap to be a mutable copy of the original image
        assert backgroundBitmap != null;
        Bitmap resultBitmap = Bitmap.createBitmap(backgroundBitmap.getWidth(),
                backgroundBitmap.getHeight(), backgroundBitmap.getConfig());

        // Scale the emoji so it looks better on the face
        float scaleFactor = EMOJI_SCALE_FACTOR;

        // Determine the size of the emoji to match the width of the face and preserve aspect ratio
        Rect bounds = face.getBoundingBox();

        Canvas canvas = new Canvas(resultBitmap);

        canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        assert emojiBitmap != null;
        canvas.drawBitmap(emojiBitmap, null, bounds, null);
        Log.d("addBitmapToFace", "return by a bitmap");
        return resultBitmap;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(BITMAP_KEY, mResultsBitmap);


    }
}
