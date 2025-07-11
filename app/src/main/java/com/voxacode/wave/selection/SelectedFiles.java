package com.voxacode.wave.selection;

import java.util.HashSet;
import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SelectedFiles implements Iterable< String > {
    
    private HashSet< String > files;
    
    @Inject
    public SelectedFiles() {
        files = new HashSet<>();
    }
    
    public void add( String path ) {
        if( path != null ) {
            files.add( path );
        }
    }
    
    public void remove( String path ) {
        if( path != null ) {
            files.remove( path );
        }
    }
    
    public boolean contains( String path ) {
        return path != null ? files.contains( path ) : false;
    }
    
    public boolean isEmpty() {
        return files.isEmpty();
    }
    
    public void clear() {
        files.clear();
    }
    
    @Override
    public Iterator< String > iterator() {
        return files.iterator();
    }
}
