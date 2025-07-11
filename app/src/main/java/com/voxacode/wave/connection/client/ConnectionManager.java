package com.voxacode.wave.connection.client;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;

import com.voxacode.wave.connection.host.LocalOnlyHotspotInfo;

import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ConnectionManager {
    
    public interface OnConnectionSuccessfulListener {
        void onConnectionSuccessful();
    }
    
    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    
    @Inject
    public ConnectionManager( @ApplicationContext Context appContext ) {
        this.wifiManager = ( WifiManager ) appContext.getSystemService( Context.WIFI_SERVICE );
        this.connectivityManager = ( ConnectivityManager ) appContext.getSystemService( Context.CONNECTIVITY_SERVICE );
    }
    
    public void connect( LocalOnlyHotspotInfo hotspotInfo, OnConnectionSuccessfulListener successListener ) {
        
        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ) {
            
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = hotspotInfo.getSsid();
            wifiConfig.preSharedKey = hotspotInfo.getPassword();
            wifiConfig.allowedKeyManagement.set( WifiConfiguration.KeyMgmt.WPA2_PSK );
            
            int netId = wifiManager.addNetwork( wifiConfig );
            if( netId != -1 ) {
                wifiManager.disconnect();
                wifiManager.enableNetwork( netId, true );
                if( successListener != null ) {
                    successListener.onConnectionSuccessful();
                }
            } 
            return;
        }
        
        WifiNetworkSpecifier networkSpecifier = new WifiNetworkSpecifier.Builder()
            .setSsid( hotspotInfo.getSsid() )
            .setWpa2Passphrase( hotspotInfo.getPassword() )
            .build();
        
        NetworkRequest networkRequest = new NetworkRequest.Builder()
            .addTransportType( NetworkCapabilities.TRANSPORT_WIFI )
            .setNetworkSpecifier( networkSpecifier )
            .build();
        
        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable( Network network ) {
                connectivityManager.bindProcessToNetwork( network );
                if( successListener != null ) 
                    successListener.onConnectionSuccessful();
            }
        };
        
        connectivityManager.requestNetwork( networkRequest, networkCallback );
    }
}
