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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;



public class MainActivity extends AppCompatActivity {
    Button takePictureBtn;
    Button selectPictureBtn;
    PreviewView previewView;
    ImageCapture imageCapture;
    Camera camera;
    Executor cameraExecutor;
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
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
        imageCapture = new ImageCapture.Builder().setTargetRotation(
                        getScreenRotationInDegrees(this))
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            cameraExecutor = getMainExecutor();
        }

        // Launch the gallery for the user to select an image.
        pickImage = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == MainActivity.RESULT_OK && result.getData() != null){
                        Uri selectedImage = result.getData().getData();
                        Bitmap bitmap = uriToBitmap(getContentResolver(), selectedImage);
                        try {
                            identifications = identification.getInfoAboutInsect(bitmapToBase64(bitmap));
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                        Intent intent = new Intent(MainActivity.this, ResponseActivity.class);
                        try {
                            Identification.identificationLatch.await();
                            intent.putExtra("VALUES", jsonObjectsToStringArray(identifications));
//                            Log.v("Values", Arrays.toString(jsonObjectsToStringArray(identifications)));
                            startActivity(intent);
                        } catch (JSONException | InterruptedException e) {
                            Log.e("Failed to parse json: ", Objects.requireNonNull(e.getMessage()));
                        }
                    }
                });

        // Ran when user hits the take picture button - takes a picture and passes it to the API
        takePictureBtn.setOnClickListener(v -> {
            String name = new SimpleDateFormat("yyyMMddHHmmss", Locale.US).format(new Date());
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P){
                contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image");
            }

            imageCapture.takePicture( cameraExecutor, new ImageCapture.OnImageCapturedCallback() {

                /**
                 * Ran when image capture succeeds.
                 * @param image The captured image.
                 */
                @Override
                public void onCaptureSuccess(@NonNull ImageProxy image) {
                    Bitmap bitmap = image.toBitmap();

                    try {
                        identifications = identification.getInfoAboutInsect(bitmapToBase64(bitmap));
                    } catch (JSONException e){
                        Log.e("CameraX", "Error converting image: " + e.getMessage(), e);
                    }

                    image.close();
                    Intent intent = new Intent(MainActivity.this, ResponseActivity.class);
                    try {
                        intent.putExtra("VALUES", jsonObjectsToStringArray(identifications));
                        startActivity(intent);
                    } catch (JSONException ignored) {}
                }

                @Override
                public void onError(@NonNull ImageCaptureException error) {
                    Log.e("CameraX", "Image capture failed: " + error.getMessage(), error);
                }
            });
        });

        // Ran when user hits the select image button - gets image and sends it to the API.
        selectPictureBtn.setOnClickListener(v -> {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        pickImage.launch(intent);
        });
    }

    /**
     * Binds the PreviewView to what the camera sees.
     * @param cameraProvider The object that provides the camera.
     */
    void bindPreview(@NonNull ProcessCameraProvider cameraProvider){
        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture, preview);
    }

    /**
     * Gets the screens current rotation in degrees.
     * @param context The activity.
     * @return screen rotation in degrees.
     */
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

    /**
     * Converts a URI image to a bitmap.
     * @param contentResolver The content resolver.
     * @param inImage The URI image to use.
     * @return a Bitmap.
     */
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

    /**
     * Converts a Bitmap into Base64.
     * @param image The image to convert.
     * @return the image encoded in the Base64 format.
     */
    public String bitmapToBase64(Bitmap image){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        String base64 = Base64.encodeToString(b, Base64.DEFAULT);
        base64 = base64.replaceAll("\\n", "");
        return base64;
    }

    /**
     *
     * @param arrayList The arrayList of JSONObjects to parse through.
     * @return A 8 index long String array containing information about the response.
     * @throws JSONException Error parsing through the JSON.
     */
    public String[] jsonObjectsToStringArray(ArrayList<JSONObject> arrayList) throws JSONException {
        if(arrayList.isEmpty()){
            return new String[]{"No confident identifications"};
        }

        String[] returnValues = new String[10];
        int stringArrayIndex = 0;
        for (int index = 0; index < arrayList.size(); index++){

            // Name
            if(!arrayList.get(index).getJSONObject("details").isNull("common_names")) {
                arrayList.get(index).getJSONObject("details").getJSONArray("common_names");
                returnValues[stringArrayIndex] = "Name: " + arrayList.get(index).
                        getJSONObject("details").
                        getJSONArray("common_names").get(0) + " (" + arrayList.get(index).getString("name") + ")";
            } else {
                returnValues[stringArrayIndex] = "Name: " + arrayList.get(index).getString("name");
            }
            stringArrayIndex++;
            // Confidence
            double confidence = (double) Math.round(arrayList.get(index).getDouble("probability") * 10000) / 100;
            returnValues[stringArrayIndex] = "Confidence: " + confidence + "%";
            stringArrayIndex++;

            StringBuilder dangerBuilder = new StringBuilder();
            dangerBuilder.append("Danger: ");

            // Danger
            try {
                for(int i = 0; i < arrayList.get(index)
                        .getJSONObject("details")
                        .getJSONArray("danger").length(); i++) {
                    if(i == 0) {
                        dangerBuilder.append(arrayList.get(index)
                                .getJSONObject("details")
                                .getJSONArray("danger").get(i));
                    } else{
                        dangerBuilder.append(", ").append(arrayList.get(index)
                                .getJSONObject("details")
                                .getJSONArray("danger").get(i));
                    }
                }
                returnValues[stringArrayIndex] = dangerBuilder.toString();
            } catch (JSONException e){
                returnValues[stringArrayIndex] = "Danger: not available";
            }
            stringArrayIndex++;

            StringBuilder roleBuilder = new StringBuilder();
            roleBuilder.append("Roles: ");
            // Role
            try {
                for(int i = 0; i < arrayList.get(index)
                        .getJSONObject("details")
                        .getJSONArray("role").length(); i++){
                    if(i == 0) {
                        roleBuilder.append(arrayList.get(index)
                                .getJSONObject("details")
                                .getJSONArray("role").get(i));
                    } else{
                        roleBuilder.append(", ").append(arrayList.get(index)
                                .getJSONObject("details")
                                .getJSONArray("role").get(i));
                    }
                }
                returnValues[stringArrayIndex] = roleBuilder.toString();
            } catch (JSONException e){
                returnValues[stringArrayIndex] = "Role: not available";
            }
            stringArrayIndex++;

            // Image
            returnValues[stringArrayIndex] = arrayList.get(index)
                    .getJSONObject("details")
                    .getJSONObject("image")
                    .getString("value");

            stringArrayIndex++;
        }
        return returnValues;
    }
}