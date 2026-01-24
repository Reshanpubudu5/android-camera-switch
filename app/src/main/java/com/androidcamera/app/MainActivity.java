package com.androidcamera.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private PreviewView previewView;
    private ProcessCameraProvider cameraProvider;
    private Camera currentCamera;
    private List<CameraInfo> availableCameras = new ArrayList<>();
    private int currentCameraIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);

        if (checkPermissions()) {
            initializeCamera();
        } else {
            requestPermissions();
        }

        setupCameraButtons();
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeCamera() {
        discoverCameras();
        if (!availableCameras.isEmpty()) {
            startCamera(availableCameras.get(0));
        }
    }

    private void discoverCameras() {
        availableCameras.clear();
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            
            for (String cameraId : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                int[] capabilitiesArray = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                
                String cameraName = "Unknown";
                int cameraType = CameraSelector.LENS_FACING_BACK;
                
                if (lensFacing != null) {
                    if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                        cameraName = "Front Camera";
                        cameraType = CameraSelector.LENS_FACING_FRONT;
                    } else if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                        // Check for specific camera types using multiple characteristics
                        float[] focalLengthsArray = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                        
                        // Check for macro capability using focal length
                        // Macro cameras typically have very short focal lengths (< 2mm)
                        if (focalLengthsArray != null && focalLengthsArray.length > 0) {
                            float focalLength = focalLengthsArray[0];
                            // Macro cameras typically have very short focal lengths (< 2mm)
                            if (focalLength < 2.0f) {
                                cameraName = "Macro Camera";
                            } 
                            // Ultra-wide cameras typically have focal lengths around 1.5-2.5mm
                            else if (focalLength >= 1.5f && focalLength <= 2.5f) {
                                cameraName = "Wide Camera";
                            } 
                            // Standard/telephoto cameras have longer focal lengths
                            else if (focalLength > 3.0f) {
                                cameraName = "Telephoto Camera";
                            } 
                            else {
                                cameraName = "Main Camera";
                            }
                        } else {
                            // Fallback: use camera index to differentiate
                            int backCameraCount = 0;
                            for (CameraInfo c : availableCameras) {
                                if (c.lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    backCameraCount++;
                                }
                            }
                            if (backCameraCount == 0) {
                                cameraName = "Main Camera";
                            } else if (backCameraCount == 1) {
                                cameraName = "Wide Camera";
                            } else {
                                cameraName = "Macro Camera";
                            }
                        }
                        cameraType = CameraSelector.LENS_FACING_BACK;
                    }
                }
                
                availableCameras.add(new CameraInfo(cameraId, cameraName, cameraType));
                Log.d(TAG, "Found camera: " + cameraName + " (ID: " + cameraId + ")");
            }

            // Check for USB cameras
            UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
            if (usbManager != null) {
                HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
                for (UsbDevice device : deviceList.values()) {
                    // USB cameras might be detected through USB device list
                    // Note: Direct USB camera access requires additional setup
                    Log.d(TAG, "USB Device found: " + device.getDeviceName());
                }
            }

        } catch (CameraAccessException e) {
            Log.e(TAG, "Error accessing cameras", e);
            Toast.makeText(this, "Error accessing cameras", Toast.LENGTH_SHORT).show();
        }

        if (availableCameras.isEmpty()) {
            Toast.makeText(this, "No cameras found", Toast.LENGTH_LONG).show();
        }
    }

    private void startCamera(CameraInfo cameraInfo) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                // Unbind use cases before rebinding
                cameraProvider.unbindAll();

                // Create preview use case
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Select camera by lens facing
                // Note: CameraX doesn't support direct camera ID selection in this version
                // We use lens facing and rely on the camera discovery logic
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraInfo.lensFacing)
                        .build();

                // Bind use cases to camera
                currentCamera = cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview
                );

                updateCameraButtons();
                Toast.makeText(this, "Switched to: " + cameraInfo.name, Toast.LENGTH_SHORT).show();

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(this, "Error starting camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void setupCameraButtons() {
        Button btnPrev = findViewById(R.id.btnPrevCamera);
        Button btnNext = findViewById(R.id.btnNextCamera);
        Button btnFront = findViewById(R.id.btnFrontCamera);
        Button btnMacro = findViewById(R.id.btnMacroCamera);
        Button btnWide = findViewById(R.id.btnWideCamera);
        Button btnUSB = findViewById(R.id.btnUSBCamera);

        btnPrev.setOnClickListener(v -> switchToPreviousCamera());
        btnNext.setOnClickListener(v -> switchToNextCamera());
        btnFront.setOnClickListener(v -> switchToCameraByType("Front"));
        btnMacro.setOnClickListener(v -> switchToCameraByType("Macro"));
        btnWide.setOnClickListener(v -> switchToCameraByType("Wide"));
        btnUSB.setOnClickListener(v -> switchToCameraByType("USB"));
    }

    private void switchToPreviousCamera() {
        if (availableCameras.isEmpty()) return;
        currentCameraIndex = (currentCameraIndex - 1 + availableCameras.size()) % availableCameras.size();
        startCamera(availableCameras.get(currentCameraIndex));
    }

    private void switchToNextCamera() {
        if (availableCameras.isEmpty()) return;
        currentCameraIndex = (currentCameraIndex + 1) % availableCameras.size();
        startCamera(availableCameras.get(currentCameraIndex));
    }

    private void switchToCameraByType(String type) {
        for (int i = 0; i < availableCameras.size(); i++) {
            CameraInfo info = availableCameras.get(i);
            if (info.name.contains(type)) {
                currentCameraIndex = i;
                startCamera(info);
                return;
            }
        }
        Toast.makeText(this, type + " camera not found", Toast.LENGTH_SHORT).show();
    }

    private void updateCameraButtons() {
        if (availableCameras.isEmpty()) return;
        CameraInfo current = availableCameras.get(currentCameraIndex);
        Log.d(TAG, "Current camera: " + current.name);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    private static class CameraInfo {
        String cameraId;
        String name;
        int lensFacing;

        CameraInfo(String cameraId, String name, int lensFacing) {
            this.cameraId = cameraId;
            this.name = name;
            this.lensFacing = lensFacing;
        }
    }
}
