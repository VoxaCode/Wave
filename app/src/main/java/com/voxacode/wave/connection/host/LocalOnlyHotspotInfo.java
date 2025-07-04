package com.voxacode.wave.connection.host;

import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;

public class LocalOnlyHotspotInfo {
    
    private String ssid;
    private String password;
    
    public LocalOnlyHotspotInfo( String ssid, String password ) {
        this.ssid = ssid;
        this.password = password;
    }
    
    //For api > 29
    public static LocalOnlyHotspotInfo fromSoftApConfiguration( SoftApConfiguration softApConfig ) {
        return new LocalOnlyHotspotInfo(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ? softApConfig.getSsid() : softApConfig.getWifiSsid().toString(),
            softApConfig.getPassphrase()
        );
    }
    
    //For Api < 29
    public static LocalOnlyHotspotInfo fromWifiConfiguration( WifiConfiguration wifiConfig ) {
        return new LocalOnlyHotspotInfo(
            wifiConfig.SSID, 
            wifiConfig.preSharedKey
        );
    }
    
    public String getSsid() {
        return ssid;
    }
    
    public String getPassword() {
        return password;
    }
}