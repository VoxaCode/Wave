package com.voxacode.wave.transfer.receiver;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Looper;

import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ReceiverAdvertisementManager {
    
    public interface OnAdvertisementStartedListener {
        void onAdvertisementStarted();
    }
    
    public interface OnErrorOccuredListener {
        void onErrorOccured( Exception e );
    }
    
    public static final String SERVICE_NAME = "Wave";
    public static final String SERVICE_TYPE = "_wave._tcp";
    
    private NsdManager nsdManager;
    private NsdManager.RegistrationListener registrationListener;
   
    private boolean isAdvertising;
    
    @Inject
    public ReceiverAdvertisementManager( @ApplicationContext Context context ) {
        this.nsdManager = ( NsdManager ) context.getSystemService( Context.NSD_SERVICE );
    }
    
    public boolean isAdvertising() {
        return isAdvertising;
    }
    
    private static void callbackError( OnErrorOccuredListener errorListener, Exception e ) {
        if( errorListener != null ) {
            errorListener.onErrorOccured( e );
        }  
    }
   
    public void startAdvertising( int port, OnAdvertisementStartedListener startedListener, OnErrorOccuredListener errorListener ) {
        
        if( isAdvertising() )
            return;
        
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName( SERVICE_NAME );
        serviceInfo.setServiceType( SERVICE_TYPE );
        serviceInfo.setPort( port );
            
        registrationListener = new NsdManager.RegistrationListener() {
            
            @Override
            public void onUnregistrationFailed( NsdServiceInfo serviceInfo, int errorCode ) { }
            
            @Override
            public void onServiceRegistered( NsdServiceInfo serviceInfo ) {
                isAdvertising = true;                  
                if( startedListener != null )
                    startedListener.onAdvertisementStarted();    
            }

            @Override
            public void onServiceUnregistered( NsdServiceInfo serviceInfo ) {
                isAdvertising = false;
            }

            @Override
            public void onRegistrationFailed( NsdServiceInfo serviceInfo, int errorCode ) {
                callbackError(
                    errorListener,
                    new Exception( "Service registeration failed with code: " + errorCode )
                );
            }
        };
            
        nsdManager.registerService(
            serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener
        );
    }
    
    public void stopAdvertising() {
        if( registrationListener != null ) {
            nsdManager.unregisterService( registrationListener );
            registrationListener = null;
            isAdvertising = false;
        }
    }
}
