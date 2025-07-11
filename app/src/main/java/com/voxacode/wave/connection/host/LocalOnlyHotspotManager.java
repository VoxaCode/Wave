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
    
    public interface LocalOnlyHotspotListener {
        void onLocalOnlyHotspotStarted( LocalOnlyHotspotInfo hotspotInfo );
        void onLocalOnlyHotspotStopped( LocalOnlyHotspotInfo hotspotInfo );
    }
    
    public interface OnErrorOccuredListener {
        void onErrorOccured( Exception e );
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
    
    public LocalOnlyHotspotInfo getActiveHotspotInfo() {
        return isLocalOnlyHotspotActive() ?
               LocalOnlyHotspotInfo.fromLocalOnlyHotspotReservation( reservation ) :
               null;
    }
    
    public void stopLocalOnlyHotspot() {
        if( reservation != null ) {
            reservation.close();
            reservation = null;
        }
    }
    
    public void startLocalOnlyHotspot( LocalOnlyHotspotListener hotspotListener, OnErrorOccuredListener errorListener ) {
     
        if( isLocalOnlyHotspotActive() )
            return;
        
        wifiManager.startLocalOnlyHotspot(
            new WifiManager.LocalOnlyHotspotCallback() {
                
                LocalOnlyHotspotInfo hotspotInfo;
                
                //onStopped will not be called after reservation.close()
                @Override
                public void onStopped() {
                    super.onStopped();
                    reservation = null;
                    if( hotspotListener != null ) {
                        hotspotListener.onLocalOnlyHotspotStopped( hotspotInfo );
                    }
                }
                
                @Override
                public void onFailed( int reason ) {
                    super.onFailed( reason );
                    if( errorListener != null ) {
                        errorListener.onErrorOccured( 
                            new Exception( "Starting local only hotspot failed with error code: " + reason )
                        );
                    }
                }
                
                @Override
                public void onStarted( LocalOnlyHotspotReservation mReservation ) {
                    super.onStarted( mReservation );
                    reservation = mReservation;
                    hotspotInfo = LocalOnlyHotspotInfo.fromLocalOnlyHotspotReservation( reservation );
                    if( hotspotListener != null ) {
                        hotspotListener.onLocalOnlyHotspotStarted( hotspotInfo );
                    } 
                }
            },
            new Handler( Looper.getMainLooper() )
        );
    }
}