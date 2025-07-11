package com.voxacode.wave.transfer.sender.hilt;

import com.voxacode.wave.transfer.sender.FileSender;
import com.voxacode.wave.selection.SelectedFiles;
import com.voxacode.wave.transfer.sender.ReceiverDiscoveryManager;

import dagger.hilt.android.components.ViewModelComponent;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;

@Module
@InstallIn( ViewModelComponent.class )
public class FileSenderModule {
    
    @Provides
    public FileSender provideFileSender( SelectedFiles selectedFiles, ReceiverDiscoveryManager discoveryManager ) {
        return new FileSender(
            selectedFiles,
            discoveryManager
        );
    }
}
