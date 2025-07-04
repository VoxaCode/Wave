package com.voxacode.wave.selection.fragments.files;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.voxacode.wave.selection.SelectedFiles;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

@HiltViewModel
public class FilesFragmentViewModel extends ViewModel {
    
    private FilesRepository filesRepository;
    private SelectedFiles selectedFiles;
    
    private final MutableLiveData< List< FilesRepository.FileInfo > > fetchedFiles = new MutableLiveData<>();
    private CompletableFuture< List< FilesRepository.FileInfo > > filesFuture;
    
    private boolean areFilesFetchedAtleastOnce;
    
    @Inject
    public FilesFragmentViewModel( FilesRepository filesRepository, SelectedFiles selectedFiles ) {
        this.filesRepository = filesRepository;
        this.selectedFiles = selectedFiles;
    }
    
    public LiveData< List< FilesRepository.FileInfo > > getFetchedFiles() {
        return fetchedFiles;
    }
    
    public void fetchFiles( String directoryPath ) {
        filesFuture = filesRepository.getFiles( directoryPath );
        filesFuture.thenAccept( files -> {
            areFilesFetchedAtleastOnce = true;      
            fetchedFiles.postValue( files );
        } );
    }
    
    public void stopFetchingFiles() {
        if( filesFuture != null && !filesFuture.isCancelled() ) {
            filesFuture.cancel( true );
        }
    }
    
    public boolean areFilesFetchedAtleastOnce() {
        return areFilesFetchedAtleastOnce;
    }
    
    public void addInSelectedFiles( String filePath ) {
        selectedFiles.add( filePath );
    }
    
    public void removeFromSelectedFiles( String filePath ) {
        selectedFiles.remove( filePath );
    }
    
    public boolean doesExistInSelectedFiles( String filePath ) {
        return selectedFiles.contains( filePath );
    }
}
