package com.cameraswitcher.app;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private PreviewView previewView;
    private ProcessCameraProvider cameraProvider;
    private Camera currentCamera;
    private List<CameraInfo> availableCameras = new ArrayList<>();
    private int currentCameraIndex = 0;
    private CameraPreferences cameraPreferences;
    private LinearLayout cameraButtonsContainer;
    private TextView currentCameraText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        cameraButtonsContainer = findViewById(R.id.cameraButtonsContainer);
        currentCameraText = findViewById(R.id.currentCameraText);
        
        ImageButton btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> openSettings());

        cameraPreferences = new CameraPreferences(this);

        if (checkPermissions()) {
            initializeCamera();
        } else {
            requestPermissions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh camera list and names when returning from settings
        if (checkPermissions()) {
            discoverCameras();
            updateCameraButtons();
        }
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
            updateCameraButtons();
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
                float[] focalLengthsArray = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                
                String defaultName = "Unknown Camera";
                int cameraType = CameraSelector.LENS_FACING_BACK;
                float focalLength = 0f;
                
                if (lensFacing != null) {
                    if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                        defaultName = "Front Camera";
                        cameraType = CameraSelector.LENS_FACING_FRONT;
                    } else if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                        if (focalLengthsArray != null && focalLengthsArray.length > 0) {
                            focalLength = focalLengthsArray[0];
                            // Determine camera type based on focal length
                            if (focalLength < 2.0f) {
                                defaultName = "Macro Camera";
                            } else if (focalLength >= 1.5f && focalLength <= 2.5f) {
                                defaultName = "Wide Camera";
                            } else if (focalLength > 3.0f) {
                                defaultName = "Telephoto Camera";
                            } else {
                                defaultName = "Main Camera";
                            }
                        } else {
                            // Count existing back cameras to differentiate
                            int backCameraCount = 0;
                            for (CameraInfo c : availableCameras) {
                                if (c.lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    backCameraCount++;
                                }
                            }
                            if (backCameraCount == 0) {
                                defaultName = "Main Camera";
                            } else if (backCameraCount == 1) {
                                defaultName = "Wide Camera";
                            } else {
                                defaultName = "Back Camera " + (backCameraCount + 1);
                            }
                        }
                        cameraType = CameraSelector.LENS_FACING_BACK;
                    }
                }
                
                // Get display name from preferences
                String displayName = cameraPreferences.getCameraName(cameraId, defaultName);
                
                CameraInfo cameraInfo = new CameraInfo(cameraId, defaultName, cameraType, "Built-in", 
                                                      focalLength, capabilitiesArray);
                cameraInfo.displayName = displayName;
                availableCameras.add(cameraInfo);
                
                Log.d(TAG, "Found camera: " + displayName + " (ID: " + cameraId + ", Type: Built-in)");
            }

            // Check for USB cameras
            detectUSBCameras();
            
            // Check for Bluetooth cameras (if available)
            detectBluetoothCameras();

        } catch (CameraAccessException e) {
            Log.e(TAG, "Error accessing cameras", e);
            Toast.makeText(this, "Error accessing cameras", Toast.LENGTH_SHORT).show();
        }

        if (availableCameras.isEmpty()) {
            Toast.makeText(this, "No cameras found", Toast.LENGTH_LONG).show();
        }
    }

    private void detectUSBCameras() {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (usbManager != null) {
            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
            int usbCameraIndex = 1;
            
            for (UsbDevice device : deviceList.values()) {
                // Check if device is a camera (USB video class device)
                // USB cameras typically have class code 14 (Video) or interface class 14
                boolean isCamera = false;
                for (int i = 0; i < device.getInterfaceCount(); i++) {
                    if (device.getInterface(i).getInterfaceClass() == 14) { // USB_CLASS_VIDEO
                        isCamera = true;
                        break;
                    }
                }
                
                if (isCamera || device.getDeviceName().toLowerCase().contains("camera") ||
                    device.getProductName().toLowerCase().contains("camera")) {
                    String deviceId = "usb_" + device.getDeviceId();
                    String defaultName = "USB Camera " + usbCameraIndex;
                    String displayName = cameraPreferences.getCameraName(deviceId, defaultName);
                    
                    CameraInfo cameraInfo = new CameraInfo(deviceId, defaultName, 
                                                          CameraSelector.LENS_FACING_BACK, "USB");
                    cameraInfo.displayName = displayName;
                    availableCameras.add(cameraInfo);
                    
                    Log.d(TAG, "Found USB camera: " + displayName + " (Device: " + device.getDeviceName() + ")");
                    usbCameraIndex++;
                }
            }
        }
    }

    private void detectBluetoothCameras() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                int btCameraIndex = 1;
                
                for (BluetoothDevice device : pairedDevices) {
                    String deviceName = device.getName();
                    if (deviceName != null && deviceName.toLowerCase().contains("camera")) {
                        String deviceId = "bt_" + device.getAddress();
                        String defaultName = "Bluetooth Camera " + btCameraIndex;
                        String displayName = cameraPreferences.getCameraName(deviceId, defaultName);
                        
                        CameraInfo cameraInfo = new CameraInfo(deviceId, defaultName, 
                                                              CameraSelector.LENS_FACING_BACK, "Bluetooth");
                        cameraInfo.displayName = displayName;
                        availableCameras.add(cameraInfo);
                        
                        Log.d(TAG, "Found Bluetooth camera: " + displayName + " (Device: " + deviceName + ")");
                        btCameraIndex++;
                    }
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Bluetooth permission not granted", e);
        } catch (Exception e) {
            Log.w(TAG, "Error detecting Bluetooth cameras", e);
        }
    }

    private void startCamera(CameraInfo cameraInfo) {
        // Only start built-in cameras through CameraX
        // USB and Bluetooth cameras require different handling
        if (!"Built-in".equals(cameraInfo.cameraType)) {
            Toast.makeText(this, cameraInfo.displayName + " requires special setup", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraInfo.lensFacing)
                        .build();

                currentCamera = cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview
                );

                // Find camera index by ID to avoid indexOf returning -1
                int foundIndex = -1;
                for (int i = 0; i < availableCameras.size(); i++) {
                    if (availableCameras.get(i).cameraId.equals(cameraInfo.cameraId)) {
                        foundIndex = i;
                        break;
                    }
                }
                if (foundIndex >= 0) {
                    currentCameraIndex = foundIndex;
                }
                updateCameraButtons();
                Toast.makeText(this, "Switched to: " + cameraInfo.displayName, Toast.LENGTH_SHORT).show();

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(this, "Error starting camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void updateCameraButtons() {
        cameraButtonsContainer.removeAllViews();
        
        if (availableCameras.isEmpty()) {
            currentCameraText.setText("No cameras found");
            return;
        }
        
        // Validate currentCameraIndex to prevent IndexOutOfBoundsException
        if (currentCameraIndex < 0 || currentCameraIndex >= availableCameras.size()) {
            currentCameraIndex = 0; // Reset to first camera if index is invalid
        }
        
        // Show current camera name
        CameraInfo current = availableCameras.get(currentCameraIndex);
        currentCameraText.setText("Current: " + current.displayName);
        
        // Add previous navigation button with icon
        ImageButton btnPrev = new ImageButton(this);
        btnPrev.setImageResource(R.drawable.ic_arrow_back);
        btnPrev.setBackgroundResource(R.drawable.rounded_nav_button);
        btnPrev.setPadding(8, 8, 8, 8);
        btnPrev.setContentDescription("Previous Camera");
        btnPrev.setOnClickListener(v -> switchToPreviousCamera());
        
        LinearLayout.LayoutParams prevParams = new LinearLayout.LayoutParams(
                (int) (48 * getResources().getDisplayMetrics().density), // 48dp
                (int) (48 * getResources().getDisplayMetrics().density)  // 48dp
        );
        prevParams.setMargins(4, 0, 8, 0);
        btnPrev.setLayoutParams(prevParams);
        cameraButtonsContainer.addView(btnPrev);
        
        // Create buttons for each camera
        for (int i = 0; i < availableCameras.size(); i++) {
            CameraInfo camera = availableCameras.get(i);
            Button btn = new Button(this);
            btn.setText(camera.displayName);
            btn.setTextSize(12); // Smaller text
            btn.setPadding((int)(10 * getResources().getDisplayMetrics().density), 
                          (int)(6 * getResources().getDisplayMetrics().density), 
                          (int)(10 * getResources().getDisplayMetrics().density), 
                          (int)(6 * getResources().getDisplayMetrics().density));
            btn.setTextColor(0xFFFFFFFF);
            btn.setAllCaps(false);
            
            // Highlight current camera with rounded background
            if (i == currentCameraIndex) {
                btn.setBackgroundResource(R.drawable.rounded_camera_button_selected);
            } else {
                btn.setBackgroundResource(R.drawable.rounded_camera_button);
            }
            
            final int cameraIndex = i;
            btn.setOnClickListener(v -> {
                if (cameraIndex >= 0 && cameraIndex < availableCameras.size()) {
                    currentCameraIndex = cameraIndex;
                    startCamera(availableCameras.get(cameraIndex));
                    updateCameraButtons();
                }
            });
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (int) (36 * getResources().getDisplayMetrics().density) // 36dp height
            );
            params.setMargins(4, 0, 4, 0);
            btn.setLayoutParams(params);
            
            cameraButtonsContainer.addView(btn);
        }
        
        // Add next navigation button with icon
        ImageButton btnNext = new ImageButton(this);
        btnNext.setImageResource(R.drawable.ic_arrow_forward);
        btnNext.setBackgroundResource(R.drawable.rounded_nav_button);
        btnNext.setPadding(8, 8, 8, 8);
        btnNext.setContentDescription("Next Camera");
        btnNext.setOnClickListener(v -> switchToNextCamera());
        
        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(
                (int) (48 * getResources().getDisplayMetrics().density), // 48dp
                (int) (48 * getResources().getDisplayMetrics().density)  // 48dp
        );
        nextParams.setMargins(8, 0, 4, 0);
        btnNext.setLayoutParams(nextParams);
        cameraButtonsContainer.addView(btnNext);
    }

    private void switchToPreviousCamera() {
        if (availableCameras.isEmpty()) return;
        // Ensure currentCameraIndex is valid
        if (currentCameraIndex < 0 || currentCameraIndex >= availableCameras.size()) {
            currentCameraIndex = 0;
        }
        currentCameraIndex = (currentCameraIndex - 1 + availableCameras.size()) % availableCameras.size();
        startCamera(availableCameras.get(currentCameraIndex));
        updateCameraButtons();
    }

    private void switchToNextCamera() {
        if (availableCameras.isEmpty()) return;
        // Ensure currentCameraIndex is valid
        if (currentCameraIndex < 0 || currentCameraIndex >= availableCameras.size()) {
            currentCameraIndex = 0;
        }
        currentCameraIndex = (currentCameraIndex + 1) % availableCameras.size();
        startCamera(availableCameras.get(currentCameraIndex));
        updateCameraButtons();
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}
