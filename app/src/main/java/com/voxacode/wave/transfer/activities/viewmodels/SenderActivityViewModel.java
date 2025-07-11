package com.voxacode.wave.transfer.activities.viewmodels;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.voxacode.wave.selection.SelectedFiles;
import com.voxacode.wave.transfer.FileMetadata;
import com.voxacode.wave.transfer.ChunkInfo;
import com.voxacode.wave.transfer.sender.FileSender;
import com.voxacode.wave.transfer.sender.FileSenderTask;
import com.voxacode.wave.utils.SingleLiveEvent;

import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class SenderActivityViewModel extends ViewModel {
    
    private boolean isAnyUnexpectedErrorOccured;
    private final MutableLiveData< Exception > unexpectedErrorObservable = new MutableLiveData<>();
    
    private boolean areAllFilesSent;
    private final MutableLiveData< Void > onAllFilesSentObservable = new MutableLiveData<>();
    
    private final MutableLiveData< FileMetadata > onStartedSendingFileObservable = new MutableLiveData<>();
    private final MutableLiveData< ChunkInfo > onFileTransferUpdateObservable = new MutableLiveData<>();
    private final SingleLiveEvent< FileMetadata > onFinishedSendingFileObservable = new SingleLiveEvent<>();
    private final SingleLiveEvent< String > onFileSkippedObservable = new SingleLiveEvent<>();
   
    private FileSender fileSender;
    
    @Inject
    public SenderActivityViewModel( FileSender fileSender ) {
        this.fileSender = fileSender;
    }
    
    public boolean isAnyUnexpectedErrorOccured() {
        return isAnyUnexpectedErrorOccured;
    }
    
    public LiveData< Exception > getUnexpectedErrorObservable() {
        return unexpectedErrorObservable;
    }
     
    public boolean areAllFilesSent() {
        return areAllFilesSent;
    }
   
    public LiveData< Void > getOnAllFilesSentObservable() {
        return onAllFilesSentObservable;
    }
    
    public LiveData< FileMetadata > getOnStartedSendingFileObservable() {
        return onStartedSendingFileObservable;
    }
    
    public LiveData< FileMetadata > getOnFinishedSendingFileObservable() {
        return onFinishedSendingFileObservable;
    }
    
    public LiveData< String > getOnFileSkippedObservable() {
        return onFileSkippedObservable;
    }
    
    public LiveData< ChunkInfo > getOnFileTransferUpdateObservable() {
        return onFileTransferUpdateObservable;
    }
    
    public boolean isFilesSendingProcessRunning() {
        return fileSender.isSendingProcessRunning();
    }
    
    public void startFilesSendingProcess() {
        fileSender.startSendingProcess(
            new FileSenderTask.SenderEventListener() {
                       
                @Override
                public void onStartedSendingFile( FileMetadata metadata ) {
                     onStartedSendingFileObservable.postValue( metadata );
                }
                     
                @Override
                public void onFinishedSendingFile( FileMetadata metadata ) {
                    onFinishedSendingFileObservable.postValue( metadata );
                }
                    
                @Override
                public void onFileSkipped( String path ) {
                    onFileSkippedObservable.postValue( path );
                }
                
                @Override
                public void onAllFilesSent() {
                    areAllFilesSent = true;
                    onAllFilesSentObservable.postValue( null );
                }
                   
                @Override
                public void onTransferUpdate( ChunkInfo chunkInfo ) {
                    onFileTransferUpdateObservable.postValue( chunkInfo );
                }
            },
            error -> {
                isAnyUnexpectedErrorOccured = true;
                unexpectedErrorObservable.postValue( error );
            }
        );     
    }
      
    public void stopFilesSendingProcess() {
        fileSender.stopSendingProcess();
    }
    
    public void shutdownFileSender() {
        fileSender.shutdown();
    }
}
