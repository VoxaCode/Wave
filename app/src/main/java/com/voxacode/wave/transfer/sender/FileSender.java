package com.voxacode.wave.transfer.sender;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import com.voxacode.wave.selection.SelectedFiles;
import com.voxacode.wave.transfer.sender.ReceiverDiscoveryManager.OnReceiverDiscoveredListener;
import com.voxacode.wave.transfer.sender.ReceiverDiscoveryManager.ReceiverInfo;
import com.voxacode.wave.transfer.sender.FileSenderTask;
import com.voxacode.wave.transfer.FileMetadata;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FileSender {
    
    public interface OnErrorOccuredListener {
        void onErrorOccured( Exception e );
    }
    
    private SelectedFiles selectedFiles;
    private ReceiverDiscoveryManager discoveryManager;
  
    private FileSenderTask senderTask;
    private Future< Void > senderTaskFuture;
    
    private ExecutorService executorService;
    
    public FileSender( 
        SelectedFiles selectedFiles, 
        ReceiverDiscoveryManager discoveryManager ) {
        
        this.selectedFiles = selectedFiles;
        this.discoveryManager = discoveryManager;
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    private void callbackError( OnErrorOccuredListener errorListener, Exception e ) {
        //stops the remaining things
        stopSendingProcess();
        if( errorListener != null ) {
            errorListener.onErrorOccured( e );
        }
    }
 
    private boolean isSendingFiles() {
        return senderTaskFuture != null && senderTask != null ? 
               !senderTaskFuture.isDone() :
               false;
    }
     
    public boolean isSendingProcessRunning() {
        return discoveryManager.isDiscovering() || isSendingFiles();
    }
  
    public void stopSendingProcess() {
        discoveryManager.stopDiscovery();
        if( isSendingFiles() ) {
            senderTaskFuture.cancel( true );
            senderTaskFuture = null;
            senderTask = null;
        }
    } 
       
    public void shutdown() {
        stopSendingProcess();
        executorService.shutdownNow();
    }  
        
    public void startSendingProcess( FileSenderTask.SenderEventListener eventListener, OnErrorOccuredListener errorListener ) {
       
        if( isSendingProcessRunning() )   
            return;
        
        OnReceiverDiscoveredListener discoveryListener = receiverInfo -> {
            try {
                    
                discoveryManager.stopDiscovery();
                SocketChannel socketChannel = SocketChannel.open();
                socketChannel.connect(
                    new InetSocketAddress( 
                        receiverInfo.getHostAddress(), 
                        receiverInfo.getPort()
                    )
                );
              
                senderTask = new FileSenderTask( selectedFiles, socketChannel, eventListener, e -> callbackError( errorListener, e ) );   
                senderTaskFuture = executorService.submit( senderTask );
                
            } catch( Exception e ) {
                callbackError( errorListener, e );
            }
        };
       
        discoveryManager.startDiscovery(
            discoveryListener,
            e -> callbackError( errorListener, e )
        );
    }
}
