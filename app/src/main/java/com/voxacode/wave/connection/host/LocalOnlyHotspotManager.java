package com.voxacode.wave.connection.host;

import android.content.Context;
import android.net.Network;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.LocalOnlyHotspotReservation;
import android.net.wifi.WifiManager.LocalOnlyHotspotCallback;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocalOnlyHotspotManager {
    
    public interface OnLocalOnlyHotspotStartedListener {
        void onLocalOnlyHotspotStarted( LocalOnlyHotspotInfo hotspotInfo );
    }
    
    public interface OnLocalOnlyHotspotFailedListener {
        void onLocalOnlyHotspotFailed( Exception e );
    }
    
    private WifiManager wifiManager;
    private LocalOnlyHotspotReservation reservation;
    
    @Inject 
    public LocalOnlyHotspotManager( @ApplicationContext Context appContext ) {
        this.wifiManager = ( WifiManager ) appContext.getSystemService( Context.WIFI_SERVICE );
    }
   
    public boolean isLocalOnlyHotspotActive() {
        return reservation != null;
    }
    
    private static LocalOnlyHotspotInfo getLocalOnlyHotspotInfo( LocalOnlyHotspotReservation reservation ) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R 
               ? LocalOnlyHotspotInfo.fromWifiConfiguration( reservation.getWifiConfiguration() )
               : LocalOnlyHotspotInfo.fromSoftApConfiguration( reservation.getSoftApConfiguration() );
    }
    
    public void stopLocalOnlyHotspot() {
        if( reservation != null ) {
            reservation.close();
            reservation = null;
        }
    }
    
    public void startLocalOnlyHotspot( OnLocalOnlyHotspotStartedListener successListener, OnLocalOnlyHotspotFailedListener failureListener ) {
        
        //Reusing the same hotspot 
        if( isLocalOnlyHotspotActive() ) {
            if( successListener != null ) {
                successListener.onLocalOnlyHotspotStarted(
                    getLocalOnlyHotspotInfo( reservation )
                );
            }
            return;
        }
        
        wifiManager.startLocalOnlyHotspot(
            new WifiManager.LocalOnlyHotspotCallback() {
                
                @Override
                public void onStopped() {
                    super.onStopped();
                    reservation = null;
                }
                
                @Override
                public void onFailed( int reason ) {
                    super.onFailed( reason );
                    if( failureListener != null ) {
                        failureListener.onLocalOnlyHotspotFailed( 
                            new Exception( "Starting local only hotspot failed with error code: " + reason )
                        );
                    }
                }
                
                @Override
                public void onStarted( LocalOnlyHotspotReservation mReservation ) {
                    super.onStarted( mReservation );
                    reservation = mReservation;
                    if( successListener != null ) {
                        successListener.onLocalOnlyHotspotStarted(
                            getLocalOnlyHotspotInfo( reservation )
                        );
                    }
                }
            },
            new Handler( Looper.getMainLooper() )
        );
    }
}