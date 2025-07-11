package com.voxacode.wave.transfer;

public class ChunkInfo {
    
    private long chunkTransferElapsedTime;
    private long bytesSent;
    private long fileSize;
    private long totalBytesSent;
    
    public ChunkInfo( long chunkTransferElapsedTime, long byteSent, long fileSize, long totalBytesSent ) {
        
        if( totalBytesSent < bytesSent )
            throw new IllegalArgumentException( "total bytes sent is less than bytes sent by chunk" );
        
        if( fileSize < bytesSent || fileSize < totalBytesSent )
            throw new IllegalArgumentException( "file size less than bytes send or total bytes send" );
        
        this.chunkTransferElapsedTime = chunkTransferElapsedTime;
        this.bytesSent = byteSent;
        this.fileSize = fileSize;
        this.totalBytesSent = totalBytesSent;
    }
    
    //time taken by chunk to transfer in milliseconds
    public long getElapsedTime() {
        return chunkTransferElapsedTime;
    }
    
    //bytes send by that chunk
    public long getBytesSent() {
        return bytesSent;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public long getTotalBytesSent() {
        return totalBytesSent;
    }
}
