package com.voxacode.wave.transfer.receiver.hilt;

import android.content.Context;
import android.content.pm.PackageManager;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.nio.file.Path;
import java.nio.file.Paths;
import android.os.Environment;

import com.voxacode.wave.selection.SelectedFiles;
import com.voxacode.wave.transfer.receiver.FileReceiver;
import com.voxacode.wave.transfer.receiver.ReceiverAdvertisementManager;
import com.voxacode.wave.transfer.sender.ReceiverDiscoveryManager;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ViewModelComponent;

@Module
@InstallIn( ViewModelComponent.class )
public class FileReceiverModule {
    
    @Provides
    public FileReceiver provideFileSender( @ApplicationContext Context context, ReceiverAdvertisementManager advertisementManager ) {
        
        PackageManager packageManager = context.getPackageManager();
        String appName = packageManager.getApplicationLabel( context.getApplicationInfo() ).toString();
        
        String storagePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        Path receiveDirectory = Paths.get( storagePath ).resolve( appName );
        
        return new FileReceiver(
            receiveDirectory,
            advertisementManager
        );
    }
}
