package com.cameraswitcher.app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {
    private CameraPreferences cameraPreferences;
    private LinearLayout cameraListContainer;
    private List<CameraItem> cameraItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        cameraPreferences = new CameraPreferences(this);
        cameraListContainer = findViewById(R.id.cameraListContainer);

        Button btnSave = findViewById(R.id.btnSave);
        Button btnReset = findViewById(R.id.btnReset);
        Button btnBack = findViewById(R.id.btnBack);

        btnSave.setOnClickListener(v -> saveAllNames());
        btnReset.setOnClickListener(v -> resetAllNames());
        btnBack.setOnClickListener(v -> finish());

        loadCameras();
    }

    private void loadCameras() {
        cameraListContainer.removeAllViews();
        cameraItems.clear();

        // Get all cameras from preferences
        SharedPreferences prefs = getSharedPreferences("camera_preferences", Context.MODE_PRIVATE);
        java.util.Map<String, ?> allPrefs = prefs.getAll();

        // Also add cameras that might not have custom names yet
        // We'll discover cameras similar to MainActivity
        discoverAllCameras();

        // Create UI for each camera
        for (CameraItem item : cameraItems) {
            View cameraItemView = createCameraItemView(item);
            cameraListContainer.addView(cameraItemView);
        }

        if (cameraItems.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No cameras found. Please use the app first to detect cameras.");
            emptyText.setPadding(16, 16, 16, 16);
            emptyText.setTextSize(16);
            cameraListContainer.addView(emptyText);
        }
    }

    private void discoverAllCameras() {
        // Discover cameras similar to MainActivity
        android.hardware.camera2.CameraManager cameraManager = 
            (android.hardware.camera2.CameraManager) getSystemService(Context.CAMERA_SERVICE);
        
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            
            for (String cameraId : cameraIds) {
                android.hardware.camera2.CameraCharacteristics characteristics = 
                    cameraManager.getCameraCharacteristics(cameraId);
                Integer lensFacing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING);
                
                String defaultName = getDefaultCameraName(cameraId, lensFacing);
                String customName = cameraPreferences.getCameraName(cameraId, defaultName);
                
                CameraItem item = new CameraItem(cameraId, defaultName, customName);
                cameraItems.add(item);
            }
        } catch (android.hardware.camera2.CameraAccessException e) {
            android.util.Log.e("SettingsActivity", "Error accessing cameras", e);
        }
        
        // Also check for USB and Bluetooth cameras
        discoverUSBCameras();
        discoverBluetoothCameras();
        
        // If still no cameras, show cameras from preferences
        if (cameraItems.isEmpty()) {
            SharedPreferences prefs = getSharedPreferences("camera_preferences", Context.MODE_PRIVATE);
            java.util.Map<String, ?> allPrefs = prefs.getAll();

            for (java.util.Map.Entry<String, ?> entry : allPrefs.entrySet()) {
                if (entry.getKey().startsWith("camera_name_")) {
                    String cameraId = entry.getKey().substring("camera_name_".length());
                    String customName = entry.getValue().toString();
                    String defaultName = getDefaultCameraName(cameraId, null);
                    
                    CameraItem item = new CameraItem(cameraId, defaultName, customName);
                    if (!cameraItems.contains(item)) {
                        cameraItems.add(item);
                    }
                }
            }
        }
    }
    
    private void discoverUSBCameras() {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (usbManager != null) {
            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
            int usbCameraIndex = 1;
            
            for (UsbDevice device : deviceList.values()) {
                boolean isCamera = false;
                for (int i = 0; i < device.getInterfaceCount(); i++) {
                    if (device.getInterface(i).getInterfaceClass() == 14) {
                        isCamera = true;
                        break;
                    }
                }
                
                if (isCamera || device.getDeviceName().toLowerCase().contains("camera") ||
                    device.getProductName().toLowerCase().contains("camera")) {
                    String deviceId = "usb_" + device.getDeviceId();
                    String defaultName = "USB Camera " + usbCameraIndex;
                    String customName = cameraPreferences.getCameraName(deviceId, defaultName);
                    
                    CameraItem item = new CameraItem(deviceId, defaultName, customName);
                    cameraItems.add(item);
                    usbCameraIndex++;
                }
            }
        }
    }
    
    private void discoverBluetoothCameras() {
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
                        String customName = cameraPreferences.getCameraName(deviceId, defaultName);
                        
                        CameraItem item = new CameraItem(deviceId, defaultName, customName);
                        cameraItems.add(item);
                        btCameraIndex++;
                    }
                }
            }
        } catch (SecurityException e) {
            android.util.Log.w("SettingsActivity", "Bluetooth permission not granted", e);
        } catch (Exception e) {
            android.util.Log.w("SettingsActivity", "Error detecting Bluetooth cameras", e);
        }
    }

    private String getDefaultCameraName(String cameraId, Integer lensFacing) {
        // Try to infer default name from camera ID and lens facing
        if (cameraId.startsWith("usb_")) {
            return "USB Camera";
        } else if (cameraId.startsWith("bt_")) {
            return "Bluetooth Camera";
        } else if (lensFacing != null) {
            if (lensFacing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT) {
                return "Front Camera";
            } else if (lensFacing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK) {
                // Try to determine type based on camera ID index
                try {
                    int id = Integer.parseInt(cameraId);
                    if (id == 0) {
                        return "Main Camera";
                    } else if (id == 1) {
                        return "Wide Camera";
                    } else if (id == 2) {
                        return "Macro Camera";
                    } else {
                        return "Back Camera " + (id + 1);
                    }
                } catch (NumberFormatException e) {
                    return "Back Camera";
                }
            }
        } else if (cameraId.equals("0")) {
            return "Front Camera";
        } else if (cameraId.equals("1")) {
            return "Main Camera";
        }
        return "Camera " + cameraId;
    }

    private View createCameraItemView(CameraItem item) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.item_camera_setting, null);

        TextView cameraIdText = view.findViewById(R.id.cameraIdText);
        TextView defaultNameText = view.findViewById(R.id.defaultNameText);
        EditText customNameEdit = view.findViewById(R.id.customNameEdit);

        cameraIdText.setText("ID: " + item.cameraId);
        defaultNameText.setText("Default: " + item.defaultName);
        customNameEdit.setText(item.customName);
        customNameEdit.setHint(item.defaultName);

        customNameEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                item.customName = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void saveAllNames() {
        for (CameraItem item : cameraItems) {
            if (item.customName != null && !item.customName.trim().isEmpty() 
                && !item.customName.trim().equals(item.defaultName)) {
                // Only save if custom name is different from default
                cameraPreferences.setCameraName(item.cameraId, item.customName.trim());
            } else {
                // If empty or same as default, remove custom name to use default
                cameraPreferences.removeCameraName(item.cameraId);
            }
        }
        Toast.makeText(this, "Camera names saved!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void resetAllNames() {
        for (CameraItem item : cameraItems) {
            cameraPreferences.removeCameraName(item.cameraId);
            item.customName = item.defaultName;
        }
        loadCameras();
        Toast.makeText(this, "Camera names reset to defaults", Toast.LENGTH_SHORT).show();
    }

    private static class CameraItem {
        String cameraId;
        String defaultName;
        String customName;

        CameraItem(String cameraId, String defaultName, String customName) {
            this.cameraId = cameraId;
            this.defaultName = defaultName;
            this.customName = customName;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof CameraItem) {
                return cameraId.equals(((CameraItem) obj).cameraId);
            }
            return false;
        }
    }
}
