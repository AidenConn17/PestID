package com.example.pestid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class MainActivity extends AppCompatActivity {
    Button button;
    PreviewView previewView;
    ImageView imageView;
    TextView textView;
    ImageCapture imageCapture;
    Camera camera;
    Executor cameraExecutor;
    ExecutorService executorService = Executors.newCachedThreadPool();
    ThreadPerTaskExecutor identificationExecutor = new ThreadPerTaskExecutor();
    IdentificationViewModel identification = new IdentificationViewModel(identificationExecutor);

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        cameraProviderFuture =ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try{
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException ignored){
            }
        }, ContextCompat.getMainExecutor(this));
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Only supported orientation for camera
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[] {Manifest.permission.CAMERA}, 0);
        }
        button = findViewById(R.id.button);
        previewView = findViewById(R.id.previewView);
        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);

        imageCapture = new ImageCapture.Builder().setTargetRotation(
                        getScreenRotationInDegrees(this))
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            cameraExecutor = getMainExecutor();
        }
        button.setOnClickListener(v -> {
            String name = new SimpleDateFormat("yyyMMddHHmmss", Locale.US).format(new Date());
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P){
                contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image");
            }

            imageCapture.takePicture( cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
                @Override
                public void onCaptureSuccess(@NonNull ImageProxy image) {
                    CharSequence text = "Image captured";
                    Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                    Bitmap bitmap = image.toBitmap();
                    Uri imageUri = bitmapToUri(getApplicationContext(), bitmap);

                    try {

                        Cursor returnCursor = getContentResolver()
                                .query(imageUri, null, null, null, null);
                        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        returnCursor.moveToFirst();
                        textView.setText(returnCursor.getString(nameIndex) + imageUri.getPath());
                        identification.getInfoAboutInsect(returnCursor.getString(nameIndex), imageUri.getPath());
                        returnCursor.close();

                    } catch (IOException e){
                        Log.e("CameraX", "Error converting image: " + e.getMessage(), e);
                    }
                    imageView.setImageBitmap(bitmap);

                    image.close();
                }

                @Override
                public void onError(@NonNull ImageCaptureException error) {
                    CharSequence text = "An error occurred";
                    Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                    Log.e("CameraX", "Image capture failed: " + error.getMessage(), error);
                }
            });
        });
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider){
        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture, preview);
    }

    int getScreenRotationInDegrees(Context context){
        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        Display display;
        int rotationConstant;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            display = context.getDisplay();
        } else {
            display = wm.getDefaultDisplay();
        }

        if(display != null){
            rotationConstant = display.getRotation();
        } else {
            rotationConstant = Surface.ROTATION_0;
        }

        switch(rotationConstant){
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }

        return 0;
    }

    public Uri bitmapToUri(Context inContext, Bitmap inImage){
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }



}