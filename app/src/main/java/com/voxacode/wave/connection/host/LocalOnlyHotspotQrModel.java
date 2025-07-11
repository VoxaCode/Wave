package com.voxacode.wave.connection.host;

import android.graphics.drawable.Drawable;

public class LocalOnlyHotspotQrModel {
    
    private String wifiName;
    private Drawable qrCode;
    
    public LocalOnlyHotspotQrModel( String wifiName, Drawable qrCode ) {
        this.wifiName = wifiName;
        this.qrCode = qrCode;
    }
    
    public String getWifiName() {
        return wifiName;
    }
    
    public Drawable getQrCode() {
        return qrCode;
    }
}
