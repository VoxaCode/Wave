package com.voxacode.wave.transfer.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtils {
     
    public static Path createFile( Path fileDirectory , String fileName ) throws IOException {
        
        Files.createDirectories( fileDirectory );
        Path file = fileDirectory.resolve( fileName );
        
        if( !Files.exists( file ) ) {
            Files.createFile( file );
            return file;
        }
        
        int idx = fileName.indexOf( "." );
        
        String baseName = fileName.substring( 0 , idx );
        String extension = fileName.substring( idx );
        
        int counter = 1;
        
        while( true ) {
            
            file = fileDirectory.resolve(
                baseName + "(" + counter + ")" + extension 
            );
            
            if( !Files.exists( file ) ) {
                Files.createFile( file );
                return file;
            }
            
            counter++;
        }
    }
    
    public static void deleteFile( Path file ) throws IOException {
        Files.deleteIfExists( file );
    }
}
