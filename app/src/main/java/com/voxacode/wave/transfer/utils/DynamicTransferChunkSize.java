package com.voxacode.wave.transfer.utils;

public class DynamicTransferChunkSize {
    
    private static final long CHUNK_SIZE_4MB = 4 * 1024*1024;
    private static final long CHUNK_SIZE_8MB = 2 * CHUNK_SIZE_4MB;
    private static final long CHUNK_SIZE_16MB = 2 * CHUNK_SIZE_8MB;
    private static final long CHUNK_SIZE_32MB = 2 * CHUNK_SIZE_16MB;
    private static final long CHUNK_SIZE_64MB = 2 * CHUNK_SIZE_32MB;
    
    private static final long FILE_SIZE_50MB = 50 * 1024*1024;
    private static final long FILE_SIZE_100MB = 2 * FILE_SIZE_50MB;
    private static final long FILE_SIZE_500MB = 5 * FILE_SIZE_100MB;
    private static final long FILE_SIZE_1GB = 2 * FILE_SIZE_500MB;

    public static long calculateFromFileSize( long bytes ) {
        
        if( bytes < CHUNK_SIZE_4MB ) 
            return bytes;
        
        if( bytes < FILE_SIZE_50MB )
            return CHUNK_SIZE_4MB;
        
        if( bytes < FILE_SIZE_100MB ) 
            return CHUNK_SIZE_8MB;
        
        if( bytes < FILE_SIZE_500MB ) 
            return CHUNK_SIZE_16MB;
        
        if( bytes < FILE_SIZE_1GB ) 
            return CHUNK_SIZE_32MB;
        
        return CHUNK_SIZE_64MB;
    }
}
