package com.voxacode.wave.transfer.sender;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.voxacode.wave.transfer.receiver.ReceiverAdvertisementManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class ReceiverDiscoveryManager {
    
    public interface OnReceiverDiscoveredListener {
        void onReceiverDiscovered( ReceiverInfo receiverInfo );
    }
    
    public interface OnErrorOccuredListener {
        void onErrorOccured( Exception e );
    }
    
    private Executor mainExecutor;
    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    
    private boolean isDiscovering;
    
    @Inject
    public ReceiverDiscoveryManager( @ApplicationContext Context context ) {
        this.nsdManager = ( NsdManager ) context.getSystemService( Context.NSD_SERVICE );
        this.mainExecutor = ContextCompat.getMainExecutor( context );
    }
    
    public boolean isDiscovering() {
        return isDiscovering;
    }
    
    private void callbackReceiverDiscovered( OnReceiverDiscoveredListener receiverListener, NsdServiceInfo serviceInfo ) {
        ReceiverInfo receiverInfo = new ReceiverInfo(
            serviceInfo.getHost().getHostAddress(),
            serviceInfo.getPort()
        );
        receiverListener.onReceiverDiscovered( receiverInfo );
    }
    
    private void callbackError( OnErrorOccuredListener errorListener, Exception e ) {
        stopDiscovery();
        if( errorListener != null ) {
            errorListener.onErrorOccured( e );
        }  
    }
    
    public void startDiscovery( OnReceiverDiscoveredListener receiverListener, OnErrorOccuredListener errorListener ) {
        
        if( isDiscovering() )
            return;
        
        discoveryListener = new NsdManager.DiscoveryListener() {
            
            @Override
            public void onServiceLost( NsdServiceInfo serviceInfo ) { }
                
            @Override
            public void onStopDiscoveryFailed( String serviceType, int errorCode ) { }
                
            @Override
            public void onDiscoveryStarted( String regType ) {
                isDiscovering = true;          
            }

      
            @Override
            public void onDiscoveryStopped( String serviceType ) {
                isDiscovering = false;
            }
                
            @Override
            public void onStartDiscoveryFailed( String serviceType, int errorCode ) {
                callbackError(
                    errorListener,
                    new Exception( "Starting receiver discovery failed with code: " + errorCode )
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
                                new Exception( "resolving receiver discovery failed with code: " + errorCode )
                            );    
                        }

                        @Override
                        public void onServiceResolved( NsdServiceInfo serviceInfo ) {
                            callbackReceiverDiscovered( receiverListener, serviceInfo );
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
                        nsdManager.unregisterServiceInfoCallback( this );
                        callbackReceiverDiscovered( receiverListener, serviceInfo );
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
            ReceiverAdvertisementManager.SERVICE_TYPE,
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        );
    }
    
    public void stopDiscovery() {
        if( discoveryListener != null ) {
            isDiscovering = false;
            nsdManager.stopServiceDiscovery( discoveryListener );
            discoveryListener = null;
        }
    }
    
    public class ReceiverInfo {
        
        private String hostAddress;
        private int port;
        
        public ReceiverInfo( String hostAddress, int port ) {
            this.hostAddress = hostAddress;
            this.port = port;
        }
        
        public String getHostAddress() {
            return hostAddress;
        }
        
        public int getPort() {
            return port;
        }
    }
}
