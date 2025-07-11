package com.voxacode.wave.transfer.activities;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.WindowInsets;
import androidx.lifecycle.ViewModelProvider;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.transition.platform.MaterialSharedAxis;
import com.voxacode.wave.R;
import com.voxacode.wave.transfer.FileMetadata;
import com.voxacode.wave.transfer.utils.FileIconResolver;
import com.voxacode.wave.transfer.utils.TransferSpeedFormatter;
import com.voxacode.wave.utils.SizeFormatter;
import com.voxacode.wave.transfer.activities.viewmodels.ReceiverActivityViewModel;
import com.voxacode.wave.delegates.exit.BackPressHandler;
import com.voxacode.wave.transfer.receiver.FileReceiverTask;
import com.voxacode.wave.databinding.ActivityReceiverBinding;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.voxacode.wave.utils.UnexpectedErrorDialogUtils;
import dagger.hilt.android.AndroidEntryPoint;
import java.io.File;
import javax.inject.Inject;

@AndroidEntryPoint
public class ReceiverActivity extends AppCompatActivity {
    
    private ActivityReceiverBinding binding;
    private ReceiverActivityViewModel viewModel;
   
    private void updateFileInfo( FileMetadata metadata ) {
        if( metadata != null ) {
            binding.fileName.setText( metadata.getName() );
            binding.fileSize.setText( SizeFormatter.format( metadata.getSize() ) );
            binding.fileIcon.setImageResource( 
                FileIconResolver.resolve( metadata.getFileType() ) 
            );
        }
    }
    
    private void stopEverything() {
        viewModel.shutdownFileReceiver();
    }
    
    private void showTransferSuccessful() {
        binding.topContainer.setVisibility( View.INVISIBLE );
        binding.fileProgress.setVisibility( View.INVISIBLE );
        binding.cardviewCenter.setVisibility( View.INVISIBLE );
        binding.cardviewFileInfo.setVisibility( View.INVISIBLE );
        binding.cardviewInfo.setVisibility( View.INVISIBLE );
        binding.transferSuccessful.getRoot().setVisibility( View.VISIBLE );
    }
   
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setEnterTransition( new MaterialSharedAxis( MaterialSharedAxis.X , true ) );
        getWindow().setReturnTransition( new MaterialSharedAxis( MaterialSharedAxis.X , false ) );
        
        binding = ActivityReceiverBinding.inflate( getLayoutInflater());
        setContentView( binding.getRoot() );
        
        PackageManager packageManager = getPackageManager();
        String appName = packageManager.getApplicationLabel( getApplicationInfo() ).toString();
        binding.txtInfo.setText(
            "All files are saved inside: " + new File( 
                Environment.getExternalStorageDirectory(), appName
            ).getAbsolutePath()
        );
        
        viewModel = new ViewModelProvider( this ).get( ReceiverActivityViewModel.class );
        viewModel.getUnexpectedErrorObservable().observe(
            this, e -> { 
                stopEverything();
                UnexpectedErrorDialogUtils.showUnexpectedErrorDialog( this );
            }    
        );
          
        viewModel.getOnAllFilesReceivedObservable().observe(
            this, v -> {
                stopEverything();
                showTransferSuccessful();
            }
        );
        
        if( viewModel.isAnyUnexpectedErrorOccured() || viewModel.areAllFilesReceived() ) 
            return;
        
        BackPressHandler.observeBackPress(
            this,
            "Going back will stop the transfer. Do you want to continue?",
            () -> !viewModel.isAnyUnexpectedErrorOccured() &&
                  !viewModel.areAllFilesReceived(),
            () -> {
                if( viewModel.areAllFilesReceived() ) {
                    finishAfterTransition();
                }
            },
            () -> {
                stopEverything();
                finishAfterTransition();
            }
        );
        
        viewModel.getOnFileTransferUpdateObservable().observe(
            this, chunkInfo -> {
                
                binding.fileProgress.setProgressCompat( 
                    ( int ) ( ( chunkInfo.getTotalBytesSent() * 100 ) / chunkInfo.getFileSize() ),
                    true
                );
                
                binding.txtSpeed.setText(
                    TransferSpeedFormatter.format( 
                        chunkInfo.getElapsedTime(), chunkInfo.getBytesSent()
                    )
                );
            }
        );
        
        viewModel.getOnStartedReceivingFileObservable().observe(
            this, metadata -> {
                updateFileInfo( metadata );
                binding.fileProgress.setProgressCompat( 0, true );
            }
        );
        
        viewModel.getOnFinishedReceivingFileObservable().observe(
            this, metadata -> binding.fileProgress.setIndeterminate( true )
        );
       
        if( !viewModel.isFilesReceivingProcessRunning() ) {
            viewModel.startFilesReceivingProcess();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if( !isChangingConfigurations() ) {
            stopEverything();
        }
    }
}
