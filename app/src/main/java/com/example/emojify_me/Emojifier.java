package com.example.emojify_me;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.SuccessContinuation;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;
import java.util.concurrent.Executor;

public class Emojifier{

    private static final double SMILING_PROB_THRESHOLD = .15;
    private static final double EYE_OPEN_THRESHOLD= .5;
    private static final String LOG_TAG ="D:Emojify:";
    private static final float EMOJI_SCALE_FACTOR = .9f;
    private static List<Face> faceList;
    private static final Object LOCK = new Object();
    public static Bitmap finalBitmap;

    static void detectFace(final Context context, final Bitmap myBitmap) throws InterruptedException {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();

        final InputImage image = InputImage.fromBitmap(myBitmap, 0);
        final FaceDetector detector = FaceDetection.getClient(options);

        final Task<List<Face>> result = detector.process(image);
        result.addOnCompleteListener(new OnCompleteListener<List<Face>>() {
            @Override
            public void onComplete(@NonNull Task<List<Face>> task) {
                    Log.d("Emojifier", "Start processing image to complete");
                    faceList = task.getResult();
                    new AlertDialog.Builder(context).setMessage("Detection completed").show();
                    finalBitmap = myBitmap;
                    if (faceList.size() == 0) {
                        new AlertDialog.Builder(context).setMessage("There are not any dected face in you imag").show();
                    } else {
                        for (Face face : faceList) {
                            Emoji emoji = whichEmoji(face);
                            Bitmap emjiBitmap = getBitmapFromEmoji(emoji, context);
                            finalBitmap = addBitmapToFace(finalBitmap, emjiBitmap, face);
                        }
                    }
                    Log.d("Going to nofiiy", "notify to return the main thread");
                }
        });



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

}
