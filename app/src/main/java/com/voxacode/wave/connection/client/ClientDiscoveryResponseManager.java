package com.voxacode.wave.connection.client;

import android.os.Build;
import androidx.core.content.ContextCompat;
import com.voxacode.wave.connection.host.ClientDiscoveryManager;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

import javax.inject.Inject;
import javax.inject.Singleton;
import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class ClientDiscoveryResponseManager {
    
    public interface OnClientDiscoveryRespondedListener {
        void onClientDiscoveryResponded();
    }
    
     
    public interface OnErrorOccuredListener {
        void onErrorOccured( Exception e );
    }
    
    private Executor mainExecutor;
    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    
    private boolean isResponding;
    
    @Inject
    public ClientDiscoveryResponseManager( @ApplicationContext Context context ) {
        this.nsdManager = ( NsdManager ) context.getSystemService( Context.NSD_SERVICE );
        this.mainExecutor = context.getMainExecutor();
    }

    private void callbackError( OnErrorOccuredListener errorListener, Exception e ) {
        stopRespondingDiscovery();
        if( errorListener != null )
            errorListener.onErrorOccured( e );
    }
    
    //Connects to ServerSocket created by discoverer to
    //as discovery response 
    private void notifyDiscoverer( NsdServiceInfo serviceInfo ) throws Exception {
        int port = serviceInfo.getPort();
        InetAddress address = serviceInfo.getHost();
    
        Socket socket = new Socket( address, port );
        try { socket.close(); }
        catch( Exception e ) { }
    }

    public boolean isRespondingDiscovery() {
        return isResponding;
    }
    
    public void startRespondingDiscovery( OnClientDiscoveryRespondedListener respondedListener, OnErrorOccuredListener errorListener ) {
        
        if( isRespondingDiscovery() )
            return;
        
        discoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onServiceLost( NsdServiceInfo serviceInfo ) { }
                
            @Override
            public void onStopDiscoveryFailed( String serviceType, int errorCode ) { }
            
            @Override
            public void onDiscoveryStarted( String regType ) {
                isResponding = true;          
            }
      
            @Override
            public void onDiscoveryStopped( String serviceType ) {
                isResponding = false;
            }
                
            @Override
            public void onStartDiscoveryFailed( String serviceType, int errorCode ) {
                callbackError( 
                    errorListener,
                    new Exception( "Starting service discovery failed with code: " + errorCode )
                );
            }

            @Override
            public void onServiceFound( NsdServiceInfo serviceInfo ) {
                    
                if( Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ) {
                        
                    NsdManager.ResolveListener resolveListener = new NsdManager.ResolveListener() {
                        
                        @Override
                        public void onResolveFailed( NsdServiceInfo serviceInfo, int errorCode ) {
                            callbackError(
                                errorListener,
                                new Exception( "Service resolve failed with code: " + errorCode )     
                            );      
                        }

                        @Override
                        public void onServiceResolved(NsdServiceInfo serviceInfo) {
                            try {
                                stopRespondingDiscovery();
                                notifyDiscoverer( serviceInfo );
                                respondedListener.onClientDiscoveryResponded();
                            } catch( Exception e ) {
                                callbackError( errorListener, e );
                            }
                        }
                    };
                    
                    nsdManager.resolveService( serviceInfo, resolveListener );
                    return;
                }
                    
                NsdManager.ServiceInfoCallback serviceCallback = new NsdManager.ServiceInfoCallback() {
                          
                    @Override
                    public void onServiceLost() { }
                        
                    @Override
                    public void onServiceInfoCallbackUnregistered() { }
                
                    @Override
                    public void onServiceUpdated( NsdServiceInfo serviceInfo ) {
                        try {
                            stopRespondingDiscovery();
                            notifyDiscoverer( serviceInfo );
                            nsdManager.unregisterServiceInfoCallback( this );
                            respondedListener.onClientDiscoveryResponded();
                        } catch( Exception e ) {
                            callbackError( errorListener, e );
                        }
                    }
                        
                    @Override
                    public void onServiceInfoCallbackRegistrationFailed( int reason ) {
                        callbackError( 
                            errorListener,
                            new Exception( "Service info callback registration failed with error code: " + reason )
                        );
                    }
                };
                    
                nsdManager.registerServiceInfoCallback( serviceInfo, mainExecutor, serviceCallback );
            }
        };
        
        nsdManager.discoverServices(
            ClientDiscoveryManager.SERVICE_TYPE, 
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        );
    }
    
    public void stopRespondingDiscovery() {
        isResponding = false;
        if( discoveryListener != null ) {
            nsdManager.stopServiceDiscovery( discoveryListener );
            discoveryListener = null;
        }
    }
}
