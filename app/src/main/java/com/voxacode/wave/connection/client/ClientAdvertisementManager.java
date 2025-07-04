package com.voxacode.wave.connection.client;

import java.net.ServerSocket;
import java.net.Socket;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Looper;

import javax.inject.Inject;
import javax.inject.Singleton;
import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class ClientAdvertisementManager {
   
    public interface OnAdvertisementResponseReceivedListener {
        void onAdvertisementResponseReceived();
    }
    
    public interface OnErrorOccuredListener {
        void onErrorOccured( Exception e );
    }
    
    public static final String SERVICE_NAME = "Wave";
    public static final String SERVICE_TYPE = "_presence._tcp";
    
    private ServerSocket serverSocket;
    private NsdManager nsdManager;
    private NsdManager.RegistrationListener registrationListener;
    
    private boolean isAdvertising;
    
    @Inject
    public ClientAdvertisementManager( @ApplicationContext Context context ) {
        this.nsdManager = ( NsdManager ) context.getSystemService( Context.NSD_SERVICE );
    }
   
    public boolean isAdvertising() {
        return isAdvertising;
    }
    
    private void callbackError( OnErrorOccuredListener errorListener, Exception e ) {
        stopAdvertising();
        if( errorListener != null ) {
            errorListener.onErrorOccured( e );
        }
    }
    
    private void closeServerSocket() {
        try{ if( serverSocket != null ) serverSocket.close(); }
        catch( Exception e ) { }
    }
    
    public void startAdvertising( OnAdvertisementResponseReceivedListener responseListener, OnErrorOccuredListener errorListener ) {
        
        if( responseListener == null )
            return;
        
        if( isAdvertising )
            return;
        
        try {
            
            serverSocket = new ServerSocket( 0 );
            
            NsdServiceInfo serviceInfo = new NsdServiceInfo();
            serviceInfo.setServiceName( SERVICE_NAME );
            serviceInfo.setServiceType( SERVICE_TYPE );
            serviceInfo.setPort( serverSocket.getLocalPort() );
            
            registrationListener = new NsdManager.RegistrationListener() {
                
                @Override
                public void onUnregistrationFailed( NsdServiceInfo serviceInfo, int errorCode ) { }
                
                @Override
                public void onServiceRegistered( NsdServiceInfo NsdServiceInfo ) {
                    isAdvertising = true;                   
                    new Thread(
                        () -> {
                            try( Socket clientSocket = serverSocket.accept() ) {
                                stopAdvertising();
                                responseListener.onAdvertisementResponseReceived();
                            } catch( Exception e ) {
                                callbackError( errorListener, e );
                            }
                        }
                    ).start();
                }

                @Override
                public void onServiceUnregistered( NsdServiceInfo serviceInfo ) {
                    isAdvertising = false;
                }
                
                @Override
                public void onRegistrationFailed( NsdServiceInfo serviceInfo, int errorCode ) {
                    callbackError(
                        errorListener,
                        new Exception( "service registeration failed with code: " + errorCode )
                    );
                }
            };
            
            nsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener
            );
            
        } catch( Exception e ) {
            callbackError( errorListener, e );
        }
    }
    
    public void stopAdvertising() {
        isAdvertising = false;
        closeServerSocket();
        if( registrationListener != null ) {
            nsdManager.unregisterService( registrationListener );
            registrationListener = null;
        }
    }
}
