package com.cameraswitcher.app;

import android.content.Context;
import android.content.SharedPreferences;

public class CameraPreferences {
    private static final String PREFS_NAME = "camera_preferences";
    private static final String PREFIX_CAMERA_NAME = "camera_name_";
    
    private SharedPreferences prefs;
    
    public CameraPreferences(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Get custom name for a camera, or return default if not set
     */
    public String getCameraName(String cameraId, String defaultName) {
        return prefs.getString(PREFIX_CAMERA_NAME + cameraId, defaultName);
    }
    
    /**
     * Set custom name for a camera
     */
    public void setCameraName(String cameraId, String customName) {
        prefs.edit().putString(PREFIX_CAMERA_NAME + cameraId, customName).apply();
    }
    
    /**
     * Remove custom name for a camera (revert to default)
     */
    public void removeCameraName(String cameraId) {
        prefs.edit().remove(PREFIX_CAMERA_NAME + cameraId).apply();
    }
    
    /**
     * Clear all custom camera names
     */
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
