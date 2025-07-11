package com.voxacode.wave.selection.fragments.files;

import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Singleton;
import javax.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

@Singleton
public class FilesRepository {
        
    public interface OnFilesAvailableListener {
        void onFilesAvailable( List< FileInfo > files );
    }
    
    private ExecutorService executor;
    
    private static final Comparator< FileInfo > COMPARATOR = ( firstFile, secondFile ) -> {
                            
        if( firstFile.isDirectory() && !secondFile.isDirectory() )
            return -1;
                            
        if( !firstFile.isDirectory() && secondFile.isDirectory() )
            return 1;
                            
        return firstFile.getName().compareToIgnoreCase(
            secondFile.getName()
        );
    };
    
    @Inject
    public FilesRepository() {
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    public CompletableFuture< List< FileInfo > > getFiles( String directoryPath ) {
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    Path directory = Paths.get( directoryPath );
                    List< FileInfo > files = Files.list( directory )
                        .map( FileInfo::new )
                        .sorted( COMPARATOR )
                        .collect( Collectors.toList() );
                
                    if( Thread.currentThread().isInterrupted() ) 
                        new InterruptedException( "Task interrupted");
                    
                    return files;
                    
                } catch( Exception e  ) {
                    throw new CompletionException( e );
                }
            }
        );
    }
    
    public static class FileInfo {
        
        private String name;
        private String path;
        private long lastModified;
        private long size;
        private long itemsInside;
        private boolean isDirectory; 
    
        public FileInfo( Path file ) {
            
            this.name = file.getFileName().toString();
            this.path = file.toAbsolutePath().toString();
            this.isDirectory = Files.isDirectory( file );
            
            try {
                this.lastModified = Files.getLastModifiedTime( file ).toMillis();
            } catch( IOException e ) {
                this.lastModified = -1L;
            }
            
            if( !isDirectory ) {
                
                try {
                    this.size = Files.size( file );
                } catch( IOException e ) {
                    this.size = -1L;
                } 
            
            } else try( Stream< Path > stream = Files.list( file ) ) {
                this.itemsInside = stream.count();
            } catch ( IOException e ) {
                this.itemsInside = -1L;
            }
              
        }   
        
        public String getName() {
            return name;
        }
        
        public long getSize() {
            return size;
        }
    
        public long getItemsInside() {
            return itemsInside;
        }
        
        public String getAbsolutePath() {
            return path;
        }
   
        public long getLastModified() {
            return lastModified;
        }
    
        public boolean isDirectory() {
            return isDirectory;
        }
    }
}
