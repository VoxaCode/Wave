package com.voxacode.wave.transfer.activities.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.voxacode.wave.transfer.ChunkInfo;
import com.voxacode.wave.transfer.receiver.FileReceiver;
import com.voxacode.wave.transfer.FileMetadata;
import com.voxacode.wave.transfer.receiver.FileReceiverTask;
import com.voxacode.wave.transfer.receiver.ReceiverAdvertisementManager;

import com.voxacode.wave.utils.SingleLiveEvent;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class ReceiverActivityViewModel extends ViewModel {
    
    private boolean isAnyUnexpectedErrorOccured;
    private final MutableLiveData< Exception > unexpectedErrorObservable = new MutableLiveData<>();
    
    private boolean areAllFilesReceived;
    private final MutableLiveData< Void > onAllFilesReceivedObservable = new MutableLiveData<>();
    
    private final SingleLiveEvent< FileMetadata > onFinishedReceivingFileObservable = new SingleLiveEvent<>();
    private final MutableLiveData< FileMetadata > onStartedReceivingFileObservable = new MutableLiveData<>();
    private final MutableLiveData< ChunkInfo > onFileTransferUpdateObservable = new MutableLiveData<>();
    
    private FileReceiver fileReceiver;
    
    @Inject
    public ReceiverActivityViewModel( FileReceiver fileReceiver ) {
        this.fileReceiver = fileReceiver;
    }
   
    public boolean isAnyUnexpectedErrorOccured() {
        return isAnyUnexpectedErrorOccured;
    }
    
    public LiveData< Exception > getUnexpectedErrorObservable() {
        return unexpectedErrorObservable;
    }
    
    public boolean areAllFilesReceived() {
        return areAllFilesReceived;
    }
    
    public LiveData< Void > getOnAllFilesReceivedObservable() {
        return onAllFilesReceivedObservable;
    }
    
    public LiveData< FileMetadata > getOnFinishedReceivingFileObservable() {
        return onFinishedReceivingFileObservable;
    }
    
    public LiveData< FileMetadata > getOnStartedReceivingFileObservable() {
        return onStartedReceivingFileObservable;
    }
    
    public LiveData< ChunkInfo > getOnFileTransferUpdateObservable() {
        return onFileTransferUpdateObservable;
    }
     
    public boolean isFilesReceivingProcessRunning() {
        return fileReceiver.isReceivingProcessRunning();
    }
    
    public void startFilesReceivingProcess() {
        fileReceiver.startReceivingProcess(
            new FileReceiverTask.ReceiverEventListener() {
                
                @Override
                public void onStartedReceivingFile( FileMetadata metadata ) {
                    onStartedReceivingFileObservable.postValue( metadata );
                }
                    
                @Override
                public void onFinishedReceivingFile( FileMetadata metadata ) {
                    onFinishedReceivingFileObservable.postValue( metadata );
                }
                    
                @Override
                public void onTransferUpdate( ChunkInfo chunkInfo ) {
                    onFileTransferUpdateObservable.postValue( chunkInfo );
                }  
                
                @Override
                public void onAllFilesReceived() {
                    areAllFilesReceived = true;
                    onAllFilesReceivedObservable.postValue( null );
                }
            },
            e -> {
                isAnyUnexpectedErrorOccured = true;
                unexpectedErrorObservable.postValue( e );
            }
        );
    }
    
    public void stopFilesReceivingProcess() {
        fileReceiver.stopReceivingProcess();
    }
   
    public void shutdownFileReceiver() {
        fileReceiver.shutdown();
    }
}
