package com.voxacode.wave.transfer.sender;

import android.os.Handler;
import android.os.Looper;

import android.os.SystemClock;
import android.util.Log;
import com.voxacode.wave.transfer.ChunkInfo;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.SerializationUtils;

import com.voxacode.wave.transfer.Message;
import com.voxacode.wave.transfer.utils.DynamicTransferChunkSize;
import com.voxacode.wave.selection.SelectedFiles;
import com.voxacode.wave.transfer.FileMetadata;

public class FileSenderTask implements Callable< Void > {
    
    public interface SenderEventListener {
        void onStartedSendingFile( FileMetadata metadata );
        void onFinishedSendingFile( FileMetadata metadata );
        void onTransferUpdate( ChunkInfo chunkInfo );
        void onFileSkipped( String path );
        void onAllFilesSent();
    }
    
    public interface OnErrorOccuredListener {
        void onErrorOccured( Exception e );
    }
     
    private SelectedFiles selectedFiles;
    private SocketChannel socketChannel;
    
    private SenderEventListener eventListener;
    private OnErrorOccuredListener errorListener;
    
    private final ByteBuffer intBuffer = ByteBuffer.allocate( 4 );
    private Thread currentThread;
    
    public FileSenderTask(
        SelectedFiles selectedFiles,
        SocketChannel socketChannel,
        SenderEventListener eventListener,
        OnErrorOccuredListener errorListener ) {
        
        this.socketChannel = socketChannel;
        this.selectedFiles = selectedFiles;
        
        this.eventListener = eventListener;
        this.errorListener = errorListener;
    }
    
    private void sendMessage( Message msg ) throws IOException {
        
        intBuffer.clear();
        intBuffer.putInt( msg.ordinal() );
        intBuffer.flip();
        
        while( intBuffer.hasRemaining() )
            socketChannel.write( intBuffer );
    }
    
    private Message receiveMessage() throws IOException {
        
        intBuffer.clear();
        while( intBuffer.hasRemaining() ) 
            socketChannel.read( intBuffer );
        
        intBuffer.flip();
        return Message.values()[ intBuffer.getInt() ];
    }
   
    private void sendMetadata( FileMetadata metadata ) throws IOException {
         
        ByteBuffer serializedMetadata = ByteBuffer.wrap(
             SerializationUtils.serialize( metadata )
        );
        
        intBuffer.clear();
        intBuffer.putInt( serializedMetadata.capacity() );
        intBuffer.flip();
        
        while( intBuffer.hasRemaining() ) 
            socketChannel.write( intBuffer );
        
        while( serializedMetadata.hasRemaining() )
            socketChannel.write( serializedMetadata );
    }
    
    private void tryClosingSocketChannel() {
        try { socketChannel.close(); }
        catch( Exception e ) { }
    }
 
    private void checkInterruptedAndThrow() throws InterruptedException {
        if( currentThread != null && currentThread.isInterrupted() ) 
            throw new InterruptedException();
    }
    
    @Override
    public Void call() {
        currentThread = Thread.currentThread();
        try {
            for( String path : selectedFiles ) {
                
                checkInterruptedAndThrow();
                Path filePath = Paths.get( path );
                try( FileChannel fileChannel = FileChannel.open( filePath, StandardOpenOption.READ ) ) {
 
                    FileMetadata metadata = new FileMetadata( 
                        filePath.getFileName().toString(),
                        fileChannel.size() 
                    );
                    
                    sendMessage( Message.REQ_COUNTINUE );
                    sendMetadata( metadata );
                       
                    Message msg = receiveMessage();
                    if( !msg.equals( Message.ACK_OK ) ) {
                        if( eventListener != null ) {
                            eventListener.onFileSkipped( path );
                        }
                        continue;
                    }
                    
                    if( eventListener != null ) {
                        eventListener.onStartedSendingFile(
                            metadata
                        );   
                    }
                       
                    long chunkTransferStartTime;
                    long chunkTransferFinishTime;
                    long chunkBytesSent;
                    long position = 0;
                    final long size = metadata.getSize();
                    final long CHUNK_SIZE = DynamicTransferChunkSize.calculateFromFileSize( size );
                            
                    while( position < size ) {
                        
                        chunkTransferStartTime = SystemClock.elapsedRealtime(); 
                        chunkBytesSent = fileChannel.transferTo( 
                            position, Math.min( CHUNK_SIZE, size - position ), socketChannel 
                        );
                        chunkTransferFinishTime = SystemClock.elapsedRealtime();
                      
                        position += chunkBytesSent;
                        checkInterruptedAndThrow();
                        
                        if( eventListener != null ) {
                            eventListener.onTransferUpdate(
                                new ChunkInfo( 
                                    chunkTransferFinishTime - chunkTransferStartTime, chunkBytesSent,
                                    size, position
                                )     
                            );
                        }
                    }
                    
                    if( eventListener != null ) {
                        eventListener.onFinishedSendingFile(
                            metadata
                        );
                    }
                    
                } catch( NoSuchFileException | SecurityException e ) {
                    if( eventListener != null ) {
                        eventListener.onFileSkipped( path );
                    }
                } 
                
                Thread.sleep( 1000 );
            } 
                                 
            sendMessage( Message.REQ_FINISH );
            if( eventListener != null ) {
                eventListener.onAllFilesSent();
            }
            
        } catch( InterruptedException ex ) {
            //nothing to do here socket will be closed 
            //by the below tryClosingSocketChannel() call 
            
        } catch( Exception e ) {
            if( errorListener != null ) {
                errorListener.onErrorOccured( e );
            }       
        }
        
        tryClosingSocketChannel(); 
        return null;
    }
}
