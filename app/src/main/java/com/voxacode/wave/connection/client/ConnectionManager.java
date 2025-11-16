package com.voxacode.wave.connection.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;

import com.google.android.gms.common.ConnectionResult;
import com.voxacode.wave.connection.host.LocalOnlyHotspotInfo;

import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ConnectionManager {
    
    public interface ConnectionListener {
        void onConnected(Network network);
        void onFailed();
    }
    
    private Context context;
    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    
    @Inject
    public ConnectionManager( @ApplicationContext Context context ) {
        this.context = context;
        this.wifiManager = ( WifiManager ) context.getSystemService( Context.WIFI_SERVICE );
        this.connectivityManager = ( ConnectivityManager ) context.getSystemService( Context.CONNECTIVITY_SERVICE );
    }
    
    public void connect( LocalOnlyHotspotInfo hotspotInfo, ConnectionListener connectionListener ) {
        
        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ) {
            
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = hotspotInfo.getSsid();
            wifiConfig.preSharedKey = hotspotInfo.getPassword();
            wifiConfig.allowedKeyManagement.set( WifiConfiguration.KeyMgmt.WPA2_PSK );
            
            int netId = wifiManager.addNetwork( wifiConfig );
            if( netId != -1 ) {
                wifiManager.disconnect();
                wifiManager.enableNetwork( netId, true );
                
                IntentFilter intentFilter = new IntentFilter(
                    WifiManager.WIFI_STATE_CHANGED_ACTION    
                );
                
                ConnectionSuccessReceiver receiver = new ConnectionSuccessReceiver(
                    () -> {
                        if( connectionListener == null ) return;
                        Network network = connectivityManager.getActiveNetwork();
                        if( network != null ) connectionListener.onConnected( network );
                    }
                );
                
                new Thread(
                    () -> {
                        try {
                            Thread.sleep(1000);
                            Network network = connectivityManager.getActiveNetwork();
                            
                            //Connection timeout
                            if( network == null && connectionListener != null ) {
                                connectionListener.onFailed();
                            }
                            
                        } catch( InterruptedException ex ) { }
                        context.unregisterReceiver( receiver );
                    }
                ).start();
                
                context.registerReceiver( receiver, intentFilter );
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
                if( connectionListener != null ) {
                    connectionListener.onConnected( network );
                }
            }
            
            @Override
            public void onUnavailable() {
                if( connectionListener != null ) {
                    connectionListener.onFailed();
                }
            }
        };
        
        connectivityManager.requestNetwork( networkRequest, networkCallback );
    }
}

//This broadcast is only for receiving result from
//api < 29 don't register this on api above 28 
class ConnectionSuccessReceiver extends BroadcastReceiver  {
        
    interface SuccessListener {
        void onSuccess();
    }
        
    private SuccessListener successListener;
    public ConnectionSuccessReceiver( SuccessListener successListener ) {
        this.successListener = successListener;
    }
        
    @Override
    public void onReceive( Context context, Intent intent ) {
        if( successListener == null ) return;
        if( !WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction()) ) return;
            
        NetworkInfo info = intent.getParcelableExtra(
            WifiManager.EXTRA_NETWORK_INFO
        );
                
        if( info != null && info.isConnected() ) {
            successListener.onSuccess();
        }
        
        context.unregisterReceiver( this );
    }
}    