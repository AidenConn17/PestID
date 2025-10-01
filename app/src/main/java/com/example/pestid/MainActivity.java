package com.example.pestid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;

import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;



public class MainActivity extends AppCompatActivity {
    Button takePictureBtn;
    Button selectPictureBtn;
    PreviewView previewView;
    ImageView imageView;
    TextView textView;
    ImageCapture imageCapture;
    Camera camera;
    Executor cameraExecutor;
    ExecutorService executorService = Executors.newCachedThreadPool();
    ThreadPerTaskExecutor identificationExecutor = new ThreadPerTaskExecutor();
    Identification identification = new Identification(identificationExecutor);
    ActivityResultLauncher<Intent> pickImage;
    ArrayList<JSONObject> identifications;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    @SuppressLint({"SourceLockedOrientationActivity", "IntentReset"})
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
        takePictureBtn = findViewById(R.id.takePictureBtn);
        selectPictureBtn = findViewById(R.id.selectPictureBtn);
        previewView = findViewById(R.id.previewView);
        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);

        imageCapture = new ImageCapture.Builder().setTargetRotation(
                        getScreenRotationInDegrees(this))
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            cameraExecutor = getMainExecutor();
        }

        pickImage = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == MainActivity.RESULT_OK && result.getData() != null){
                        Uri selectedImage = result.getData().getData();
                        Bitmap bitmap = uriToBitmap(getContentResolver(), selectedImage);
                        imageView.setImageBitmap(bitmap);
                        try {
                            identifications = identification.getInfoAboutInsect(bitmapToBase64(bitmap));
                        } catch (IOException e) {
                            Log.e("Identification", "Error on identification");
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                        Intent intent = new Intent(MainActivity.this, ResponseActivity.class);
                        try {
                            Identification.latch.await();
                            intent.putExtra("", jsonArraysToStringArray(identifications)); //TODO: Actually send String[]
                        } catch (JSONException | InterruptedException ignored) {}
                    }
                });

        takePictureBtn.setOnClickListener(v -> {
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

                    try {
                        identifications = identification.getInfoAboutInsect(bitmapToBase64(bitmap));
                    } catch (IOException | JSONException e){
                        Log.e("CameraX", "Error converting image: " + e.getMessage(), e);
                    }
                    imageView.setImageBitmap(bitmap);

                    image.close();
                    Intent intent = new Intent(MainActivity.this, ResponseActivity.class);
                    try {
                        intent.putExtra("", jsonArraysToStringArray(identifications)); //TODO: Actually send String[]
                    } catch (JSONException ignored) {}
                }

                @Override
                public void onError(@NonNull ImageCaptureException error) {
                    CharSequence text = "An error occurred";
                    Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                    Log.e("CameraX", "Image capture failed: " + error.getMessage(), error);
                }
            });
        });

        selectPictureBtn.setOnClickListener(v -> {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        pickImage.launch(intent);
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

    public Bitmap uriToBitmap(ContentResolver contentResolver, Uri inImage){
        Bitmap bitmap = null;
        try {
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(contentResolver, inImage);
                bitmap = ImageDecoder.decodeBitmap(source);
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, inImage);
            }
        } catch (Exception e){
            Log.e("Uri to bitmap", "Error converting Uri to bitmap");
        }
        return bitmap;
    }

    public String bitmapToBase64(Bitmap image){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] b = baos.toByteArray();
        String base64 = Base64.encodeToString(b, Base64.DEFAULT);
        base64 = base64.replaceAll("\\n", "");
        return base64;
    }

    public String[] jsonArraysToStringArray(ArrayList<JSONObject> arrayList) throws JSONException {
        String[] returnValues = new String[15];
        int stringArrayIndex = 0;
        for (int index = 0; index < arrayList.size(); index++){
            Log.v("List", "Value at index " + index + ": " + arrayList.get(index));
            // Name
            returnValues[stringArrayIndex] = arrayList.get(index).getString("name") +
                    arrayList.get(index).
                            getJSONObject("details").
                            getJSONArray("common_names").get(0);
            stringArrayIndex++;

            // Confidence
            returnValues[stringArrayIndex] = "" + arrayList.get(index).getDouble("probability");
            stringArrayIndex++;

            // Danger
            returnValues[stringArrayIndex] = arrayList.get(index).
                    getJSONObject("details").
                    getJSONObject("danger").toString();
            Log.v("Danger", arrayList.get(index).
                    getJSONObject("details").
                    getJSONObject("danger").toString());
            stringArrayIndex++;
        }
        return returnValues;
    }
}