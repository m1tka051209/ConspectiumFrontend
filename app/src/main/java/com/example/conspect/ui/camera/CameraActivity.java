package com.example.conspect.ui.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.conspect.databinding.ActivityCameraBinding;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    private ActivityCameraBinding binding;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private TessBaseAPI tessBaseAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initTesseract();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        binding.btnCapture.setOnClickListener(v -> takePhoto());
        binding.btnBack.setOnClickListener(v -> finish());

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void initTesseract() {
        tessBaseAPI = new TessBaseAPI();
        String dataPath = getFilesDir() + "/tesseract/";
        String language = "rus";

        try {
            File dir = new File(dataPath + "tessdata/");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File trainedDataFile = new File(dataPath + "tessdata/rus.traineddata");
            if (!trainedDataFile.exists()) {
                copyAssets("tessdata/rus.traineddata", trainedDataFile);
            }

            tessBaseAPI.init(dataPath, language);
            tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);

        } catch (Exception e) {
            Log.e(TAG, "Error initializing Tesseract", e);
            Toast.makeText(this, "Ошибка инициализации OCR", Toast.LENGTH_SHORT).show();
        }
    }

    private void copyAssets(String assetPath, File outFile) throws IOException {
        try (InputStream in = getAssets().open(assetPath);
             FileOutputStream out = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder().build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        binding.btnCapture.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Обработка изображения...", Toast.LENGTH_SHORT).show();

        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                recognizeText(imageProxy);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> {
                    binding.btnCapture.setEnabled(true);
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(CameraActivity.this, "Ошибка камеры: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void recognizeText(ImageProxy imageProxy) {
        Bitmap bitmap = imageProxyToBitmap(imageProxy);
        imageProxy.close();

        if (bitmap == null) {
            runOnUiThread(() -> {
                binding.btnCapture.setEnabled(true);
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Ошибка обработки изображения", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        cameraExecutor.execute(() -> {
            tessBaseAPI.setImage(bitmap);
            String recognizedText = tessBaseAPI.getUTF8Text();

            runOnUiThread(() -> {
                binding.btnCapture.setEnabled(true);
                binding.progressBar.setVisibility(View.GONE);

                if (recognizedText == null || recognizedText.trim().isEmpty()) {
                    Toast.makeText(this, "Текст не распознан", Toast.LENGTH_LONG).show();
                } else {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("recognized_text", recognizedText.trim());
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }
            });

            bitmap.recycle();
        });
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            Log.e(TAG, "Unsupported image format: " + image.getFormat());
            return null;
        }

        ImageProxy.PlaneProxy yPlane = image.getPlanes()[0];
        ImageProxy.PlaneProxy uPlane = image.getPlanes()[1];
        ImageProxy.PlaneProxy vPlane = image.getPlanes()[2];

        ByteBuffer yBuffer = yPlane.getBuffer();
        ByteBuffer uBuffer = uPlane.getBuffer();
        ByteBuffer vBuffer = vPlane.getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);
        
        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);
        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Разрешение камеры необходимо", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (tessBaseAPI != null) {
            tessBaseAPI.end();
        }
    }
}