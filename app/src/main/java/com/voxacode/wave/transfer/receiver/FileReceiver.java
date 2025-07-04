package com.voxacode.wave.transfer.receiver;

import android.app.admin.DeviceAdminReceiver;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

import com.voxacode.wave.transfer.FileMetadata;
import com.voxacode.wave.transfer.receiver.FileReceiverTask;
import com.voxacode.wave.transfer.receiver.ReceiverAdvertisementManager.OnAdvertisementStartedListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.inject.Inject;

public class FileReceiver {
    
    public interface OnErrorOccuredListener {
        void onErrorOccured( Exception e );
    }
    
    private Path fileDirectory;
    private ReceiverAdvertisementManager advertisementManager;
    
    private FileReceiverTask receiverTask;
    private Future< Void > receiverTaskFuture;
    
    private IncomingConnectionListener connectionListener;
    private ExecutorService executorService;
    
    public FileReceiver( 
        Path fileDirectory, 
        ReceiverAdvertisementManager advertisementManager ) {
        
        this.fileDirectory = fileDirectory;
        this.advertisementManager = advertisementManager;   
        this.executorService = Executors.newSingleThreadExecutor();
    }
   
    private void callbackError( OnErrorOccuredListener errorListener, Exception e ) {
        //will stop remaining things 
        stopReceivingProcess();
        if( errorListener != null ) {
            errorListener.onErrorOccured( e );
        }
    }
    
    private boolean isReceivingFiles() {
        return receiverTask != null && receiverTaskFuture != null ?
               !receiverTaskFuture.isDone() :
               false;
    }
    
    public boolean isReceivingProcessRunning() {
        return isReceivingFiles() || advertisementManager.isAdvertising();
    }
    
    public void startReceivingProcess( FileReceiverTask.ReceiverEventListener eventListener, OnErrorOccuredListener errorListener ) {
        
        if( isReceivingProcessRunning() )
            return;
        
        connectionListener = new IncomingConnectionListener();
        connectionListener.startListening(
            
            socketChannel -> {
                receiverTask = new FileReceiverTask( fileDirectory, socketChannel, eventListener, e -> callbackError( errorListener, e ) );
                receiverTaskFuture = executorService.submit( receiverTask );
            },
            
            socketAddress -> {
                advertisementManager.startAdvertising(
                   socketAddress.getPort(),
                   null,
                   e -> callbackError( errorListener, e )
                );
            },
            
            e -> callbackError( errorListener, e )
        );
    }
    
    public void stopReceivingProcess() {
        
        advertisementManager.stopAdvertising();
        
        if( connectionListener != null ) {
            connectionListener.shutdown();
            connectionListener = null;
        }
        
        if( isReceivingFiles() ) {
            receiverTaskFuture.cancel( true );
            receiverTaskFuture = null;
            receiverTask = null;
        }
    }
    
    public void shutdown() {
        stopReceivingProcess();
        executorService.shutdownNow();
    }
    
    private static class IncomingConnectionListener {
        
        public interface OnErrorOccuredListener {
            void onErrorOccured( Exception e );
        }
        
        public interface OnConnectionEstablishedListener {
            void onConnectionEstablished( SocketChannel socketChannel );
        }
        
        public interface OnServerInstantiated {
            void onServerInstantiated( InetSocketAddress serverAddress );
        }
        
        private ServerSocketChannel serverChannel;
        private boolean isUsed;
        private boolean isShutdown;
        
        private void callbackError( OnErrorOccuredListener errorListener, Exception e ) {
            if( errorListener != null && !isShutdown ) {
                errorListener.onErrorOccured( e );
            }
        }
        
        private void callbackConnectionEstablished( OnConnectionEstablishedListener connectionListener, SocketChannel socketChannel ) {
            if( connectionListener != null && !isShutdown ) {
                connectionListener.onConnectionEstablished( socketChannel );
            }
        }
        
        private void callbackServerInstantiated( OnServerInstantiated instantiatedListener, InetSocketAddress serverAddress ) {
            if( instantiatedListener != null && !isShutdown ) {
                instantiatedListener.onServerInstantiated( serverAddress );
            }
        }
        
        public void startListening( OnConnectionEstablishedListener connectionListener, OnServerInstantiated serverListener, OnErrorOccuredListener errorListener ) {
            
            if( isUsed ) 
                throw new IllegalStateException( "Same instance cannot be used again" );
            
            isUsed = true;
            new Thread( 
                () -> {
                    try{
                        serverChannel = ServerSocketChannel.open();
                        serverChannel.bind( new InetSocketAddress( 0 ) );
                        
                        callbackServerInstantiated( 
                            serverListener, 
                            ( InetSocketAddress ) serverChannel.getLocalAddress()
                        );
                        
                        SocketChannel socketChannel = serverChannel.accept();
                        callbackConnectionEstablished(
                            connectionListener,
                            socketChannel
                        );
                        
                    } catch( Exception e ) {
                        callbackError( errorListener, e );
                    } finally {
                        shutdown();
                    }
                }
            ).start();
        }
        
        public void shutdown() {
            if( isUsed ) {
                isShutdown = true;
                try { if( serverChannel != null ) serverChannel.close(); }
                catch( Exception e ) { }
            }
        }
    }
}
