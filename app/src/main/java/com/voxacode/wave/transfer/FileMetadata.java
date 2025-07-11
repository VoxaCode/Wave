package com.voxacode.wave.transfer;

import android.webkit.MimeTypeMap;
import java.io.Serializable;

public class FileMetadata implements Serializable {
    
    public enum FileType {
        FILE , PICTURE , VIDEO , AUDIO , APPLICATION
    }
    
    private String name;
    private long size;
    
    private FileType type;
    
    public FileMetadata( String name , long size ) {
        this.name = name;
        this.size = size;
        this.type = getFileTypeFromName( name );
    }
    
    private static FileType getFileTypeFromName( String name ) {
        
        String extension = name.substring( name.indexOf( "." ) + 1 );
        String mime = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension( extension );
        
        if( mime != null ) {
             
            if( mime.startsWith( "image" ) )
                return FileType.PICTURE; 
                
            if( mime.startsWith( "video" ) ) 
                return FileType.VIDEO; 
                
            if( mime.startsWith( "audio" ) ) 
                return FileType.AUDIO; 
                
            if( mime.equals( "application/vnd.android.package-archive" ) ) 
                return FileType.APPLICATION; 
                
            return FileType.FILE; 
        }
        
        else return FileType.FILE;
    }
    
    public String getName() {
        return name;
    }
    
    public long getSize() {
        return size;
    }
    
    public FileType getFileType() {
        return type;
    }
}
