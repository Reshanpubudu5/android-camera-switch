package com.cameraswitcher.app;

import androidx.camera.core.CameraSelector;

public class CameraInfo {
    public String cameraId;
    public String defaultName;
    public String displayName;  // Will be set from preferences
    public int lensFacing;
    public String cameraType;  // "Built-in", "USB", "Bluetooth", etc.
    public float focalLength;
    public int[] capabilities;
    
    public CameraInfo(String cameraId, String defaultName, int lensFacing, String cameraType) {
        this.cameraId = cameraId;
        this.defaultName = defaultName;
        this.displayName = defaultName;
        this.lensFacing = lensFacing;
        this.cameraType = cameraType;
        this.focalLength = 0f;
        this.capabilities = new int[0];
    }
    
    public CameraInfo(String cameraId, String defaultName, int lensFacing, String cameraType, 
                     float focalLength, int[] capabilities) {
        this.cameraId = cameraId;
        this.defaultName = defaultName;
        this.displayName = defaultName;
        this.lensFacing = lensFacing;
        this.cameraType = cameraType;
        this.focalLength = focalLength;
        this.capabilities = capabilities != null ? capabilities : new int[0];
    }
}
