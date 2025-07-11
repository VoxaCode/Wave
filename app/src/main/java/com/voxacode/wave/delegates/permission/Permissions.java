package com.voxacode.wave.delegates.permission;

import android.Manifest;
import android.os.Build;
import java.util.List;

public class Permissions {
    
    public static List< String > getCameraPermissions() {
        return List.of( Manifest.permission.CAMERA );
    }
    
    public static List< String > getStoragePermissions() {
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ) 
           return List.of( Manifest.permission.MANAGE_EXTERNAL_STORAGE );
        
        return List.of( 
            Manifest.permission.READ_EXTERNAL_STORAGE, 
            Manifest.permission.WRITE_EXTERNAL_STORAGE 
        );
    }
    
    public static List< String > getLocationPermissions() {
        return List.of( 
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION 
        );
    }
}
