package com.voxacode.wave.transfer.receiver;

import android.os.Handler;
import android.os.Looper;

import android.os.SystemClock;
import com.voxacode.wave.transfer.ChunkInfo;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.SerializationUtils;

import com.voxacode.wave.transfer.utils.DynamicTransferChunkSize;
import com.voxacode.wave.transfer.FileMetadata;
import com.voxacode.wave.transfer.Message;
import com.voxacode.wave.transfer.utils.FileUtils;

public class FileReceiverTask implements Callable< Void > {
    
    public interface ReceiverEventListener {
        void onStartedReceivingFile( FileMetadata metadata );
        void onFinishedReceivingFile( FileMetadata metadata );
        void onTransferUpdate( ChunkInfo chunkInfo );
        void onAllFilesReceived();
    }
    
    public interface OnErrorOccuredListener {
        void onErrorOccured( Exception e );
    }
    
    private Path fileDirectory;
    private SocketChannel socketChannel;
   
    private ReceiverEventListener eventListener;
    private OnErrorOccuredListener errorListener;
    
    private final ByteBuffer intBuffer = ByteBuffer.allocate( 4 );
    private Thread currentThread;
    
    public FileReceiverTask( 
        Path fileDirectory, 
        SocketChannel socketChannel,
        ReceiverEventListener eventListener,
        OnErrorOccuredListener errorListener ) {
        
        this.fileDirectory = fileDirectory;
        this.socketChannel = socketChannel;
        
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
   
    private FileMetadata receiveMetadata() throws IOException {
        
        intBuffer.clear();
        while( intBuffer.hasRemaining() )
            socketChannel.read( intBuffer );
        
        intBuffer.flip();
        ByteBuffer serializedMetadata = ByteBuffer.allocate( intBuffer.getInt() );
        
        while( serializedMetadata.hasRemaining() )
            socketChannel.read( serializedMetadata );
        
        return SerializationUtils.deserialize(
            serializedMetadata.array()
        );
    }
    
    private void tryClosingSocketChannel() {
        try { socketChannel.close(); }
        catch( Exception e ) { }
    }
    
    private void tryDeletingFile( Path file ) {
        try { FileUtils.deleteFile( file ); }
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
            while( true ) {
                
                checkInterruptedAndThrow();
                Message message = receiveMessage();
                
                if( message.equals( Message.REQ_FINISH ) ) 
                    break;
                   
                FileMetadata metadata = receiveMetadata();
                sendMessage( Message.ACK_OK );
          
                Path file = FileUtils.createFile( fileDirectory, metadata.getName() );
                try( FileChannel fileChannel = FileChannel.open( file, StandardOpenOption.WRITE ) ) {
             
                    if( eventListener != null ) {
                        eventListener.onStartedReceivingFile( 
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
                        chunkBytesSent = fileChannel.transferFrom( 
                            socketChannel, position, Math.min( size - position, CHUNK_SIZE ) 
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
                        eventListener.onFinishedReceivingFile( 
                            metadata
                        );
                    }
                    
                } catch( Exception e ) {
                    //considering every error fatal because they 
                    //can only be storage or network related which
                    //are not recoverable
                    tryDeletingFile( file );  
                    throw e;
                }
            }
            
            if( eventListener != null ) {
                eventListener.onAllFilesReceived();
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
