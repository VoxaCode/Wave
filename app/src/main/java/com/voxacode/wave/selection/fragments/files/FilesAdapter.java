package com.voxacode.wave.selection.fragments.files;

import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.content.Context;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.voxacode.wave.R;
import com.voxacode.wave.utils.DateAndTimeFormatter;
import com.voxacode.wave.utils.SizeFormatter;
import com.voxacode.wave.selection.fragments.files.FilesRepository.FileInfo;
import com.voxacode.wave.databinding.FilesItemBinding;
import java.util.List;

public class FilesAdapter extends ListAdapter< FileInfo, FilesAdapter.ViewHolder > {
   
    public interface OnFileClickListener {
        void onFileClicked( FileInfo file );    
    }
    
    public interface FileSelectionChecker {
        boolean isFileSelected( FileInfo file );
    }
  
    private LayoutInflater inflater;
    private OnFileClickListener clickListener;
    private FileSelectionChecker selectionChecker;
     
    public FilesAdapter( LayoutInflater layoutInflater, OnFileClickListener clickListener, FileSelectionChecker selectionChecker ) {
        super( new FilesDiffCallback() );
        this.inflater = layoutInflater;
        this.clickListener = clickListener;
        this.selectionChecker = selectionChecker;
    }
    
    @Override
    public ViewHolder onCreateViewHolder( ViewGroup parent , int viewType ) {
        return new ViewHolder( 
            FilesItemBinding.inflate( inflater , parent , false )
        );
    }
    
    @Override 
    public void onBindViewHolder( ViewHolder viewHolder , int position ) { }
    
    @Override 
    public void onBindViewHolder( ViewHolder viewHolder , int position , List< Object > payloads ) { 
          
        FileInfo file = getItem( position );
        
        viewHolder.setSelectionState( 
            selectionChecker != null
            ? selectionChecker.isFileSelected( file )
            : false
        );
        
        viewHolder.itemView.setOnClickListener( 
            view -> {
                
                if( clickListener != null )
                    clickListener.onFileClicked( file );
                
                if( selectionChecker != null ) {
                    viewHolder.setSelectionState( 
                        selectionChecker.isFileSelected( file )
                    );
                }
            } 
        );
        
        if( payloads.isEmpty() ) {
            
            viewHolder.setFileName( file.getName() );
            viewHolder.setLastModified( DateAndTimeFormatter.format( file.getLastModified() ) );
          
            if( file.isDirectory() ) {
                viewHolder.setFileIcon( R.drawable.folder_24px );
                viewHolder.setFileInfo( file.getItemsInside() + " items" );
            } else {
                viewHolder.setFileIcon( R.drawable.file_24px );
                viewHolder.setFileInfo( SizeFormatter.format( file.getSize() ) );
            }
           
            return;
        } 
        
        FilePayload payload = ( FilePayload )payloads.get( 0 );
           
        if( file.isDirectory() ) {
            
            if( payload.isItemsInsideChanged() ) {
                viewHolder.setFileInfo( 
                    file.getItemsInside() + " items"
                );
            }
            
        } else if( payload.isSizeChanged() ) {
            viewHolder.setFileInfo(
                SizeFormatter.format( file.getSize() )
            );
        }
           
        if( payload.isLastModifiedChanged() ) {
            viewHolder.setLastModified( 
                DateAndTimeFormatter.format( file.getLastModified() ) 
            );
        }
    }
    
    private static class FilePayload {
        
        private boolean isSizeChanged;
        private boolean isItemsInsideChanged;
        private boolean isLastModifiedChanged;
        
        public FilePayload( boolean isSizeChanged, boolean isItemsInsideChanged, boolean isLastModifiedChanged ) {
            this.isSizeChanged = isSizeChanged;
            this.isItemsInsideChanged = isItemsInsideChanged;
            this.isLastModifiedChanged = isLastModifiedChanged;
        }
        
        public boolean isSizeChanged() {
            return isSizeChanged;
        }
        
        public boolean isItemsInsideChanged() {
            return isItemsInsideChanged;
        }
        
        public boolean isLastModifiedChanged() {
            return isLastModifiedChanged;
        }
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        
        private FilesItemBinding binding;
        private boolean isStateSelected;
         
        public ViewHolder( FilesItemBinding binding ) {
            super( binding.getRoot() );
            this.binding = binding;
        }
        
        public void setFileIcon( int resource ) {
            binding.fileIcon.setImageResource( resource );
        }
        
        public void setFileName( String name ) {
            binding.fileName.setText( name );
        }
        
        public void setFileInfo( String info ) {
            binding.fileInfo.setText( info );
        }
        
        public void setLastModified( String lastModified ) {
            binding.lastModified.setText( lastModified );
        }
        
        public void setSelectionState( boolean state ) {
            
            if( state && !isStateSelected ) {
                binding.checkIcon.setVisibility( View.VISIBLE );
                isStateSelected = true;
            }
            
            else if( !state && isStateSelected ) {
                binding.checkIcon.setVisibility( View.INVISIBLE );
                isStateSelected = false;
            }
        }
    }
    
    private static class FilesDiffCallback extends DiffUtil.ItemCallback< FileInfo > {
        
        @Override
        public boolean areItemsTheSame( FileInfo oldFile , FileInfo newFile ) {
            return !( oldFile.isDirectory() ^ newFile.isDirectory() ) &&
                   oldFile.getName().equals( newFile.getName() );
        }
            
        @Override
        public boolean areContentsTheSame( FileInfo oldFile , FileInfo newFile ) {
            return oldFile.getLastModified() == newFile.getLastModified() && 
                   oldFile.getItemsInside() == newFile.getItemsInside() &&
                   oldFile.getSize() == newFile.getSize();
        }
            
        @Override
        public Object getChangePayload( FileInfo oldFile , FileInfo newFile ) {
            return new FilePayload( 
               oldFile.getSize() != newFile.getSize(),
               oldFile.getItemsInside() != newFile.getItemsInside(),
               oldFile.getLastModified() != newFile.getLastModified()
           );
        }
    }
}
