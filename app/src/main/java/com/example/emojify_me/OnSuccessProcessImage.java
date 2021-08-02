package com.example.emojify_me;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.face.Face;

import java.util.List;
import java.util.concurrent.Executor;

public class OnSuccessProcessImage extends Task <List<Face>> {


    @Override
    public boolean isComplete() {
        return false;
    }

    @Override
    public boolean isSuccessful() {
        return false;
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @NonNull
    @Override
    public List<Face> getResult() {
        return null;
    }

    @NonNull
    @Override
    public <X extends Throwable> List<Face> getResult(@NonNull Class<X> aClass) throws X {
        return null;
    }

    @Nullable
    @Override
    public Exception getException() {
        return null;
    }

    @NonNull
    @Override
    public Task<List<Face>> addOnSuccessListener(@NonNull OnSuccessListener<? super List<Face>> onSuccessListener) {
        return null;
    }

    @NonNull
    @Override
    public Task<List<Face>> addOnSuccessListener(@NonNull Executor executor, @NonNull OnSuccessListener<? super List<Face>> onSuccessListener) {
        return null;
    }

    @NonNull
    @Override
    public Task<List<Face>> addOnSuccessListener(@NonNull Activity activity, @NonNull OnSuccessListener<? super List<Face>> onSuccessListener) {
        return null;
    }

    @NonNull
    @Override
    public Task<List<Face>> addOnFailureListener(@NonNull OnFailureListener onFailureListener) {
        return null;
    }

    @NonNull
    @Override
    public Task<List<Face>> addOnFailureListener(@NonNull Executor executor, @NonNull OnFailureListener onFailureListener) {
        return null;
    }

    @NonNull
    @Override
    public Task<List<Face>> addOnFailureListener(@NonNull Activity activity, @NonNull OnFailureListener onFailureListener) {
        return null;
    }
}
