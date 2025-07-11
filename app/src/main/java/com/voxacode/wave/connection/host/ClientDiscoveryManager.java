package com.voxacode.wave.connection.host;

import android.os.Build;
import androidx.core.content.ContextCompat;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

import javax.inject.Inject;
import javax.inject.Singleton;
import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class ClientDiscoveryManager {
   
    public interface OnClientDiscoveredListener {
        void onClientDiscovered();
    }
    
    public interface OnErrorOccuredListener {
        void onErrorOccured( Exception e );
    }
     
    public static final String SERVICE_NAME = "Wave";
    public static final String SERVICE_TYPE = "_presence._tcp";
    
    private ServerSocket serverSocket;
    private NsdManager nsdManager;
    private NsdManager.RegistrationListener registrationListener;
    
    private boolean isDiscovering;
    
    @Inject
    public ClientDiscoveryManager( @ApplicationContext Context context ) {
        this.nsdManager = ( NsdManager ) context.getSystemService( Context.NSD_SERVICE );
    }

    private void callbackError( OnErrorOccuredListener errorListener, Exception e ) {
        stopDiscovery();
        if( errorListener != null )
            errorListener.onErrorOccured( e );
    }

    public boolean isDiscovering() {
        return isDiscovering;
    }
    
    public void startDiscovery( OnClientDiscoveredListener clientListener, OnErrorOccuredListener errorListener ) {
        
        if( isDiscovering() )
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
                    isDiscovering = true;                  
                    new Thread(
                        () -> {
                            try( Socket clientSocket = serverSocket.accept() ) {
                                stopDiscovery();
                                clientListener.onClientDiscovered();
                            } catch( Exception e ) {
                                isDiscovering = false;    
                            }
                        }
                    ).start();
                }

                @Override
                public void onServiceUnregistered( NsdServiceInfo serviceInfo ) {
                    isDiscovering = false;
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
     
    private void closeServerSocket() {
        try{ if( serverSocket != null ) serverSocket.close(); }
        catch( Exception e ) { }
    }
    
    public void stopDiscovery() {
        isDiscovering = false;
        closeServerSocket();
        if( registrationListener != null ) {
            nsdManager.unregisterService( registrationListener );
            registrationListener = null;
        }
    }
}
