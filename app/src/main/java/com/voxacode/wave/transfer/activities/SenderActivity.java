package com.voxacode.wave.transfer.activities;

import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import android.os.Bundle;

import com.voxacode.wave.R;
import com.voxacode.wave.transfer.utils.FileIconResolver;
import com.voxacode.wave.transfer.utils.TransferSpeedFormatter;
import com.voxacode.wave.utils.SizeFormatter;
import com.voxacode.wave.delegates.exit.BackPressHandler;
import com.voxacode.wave.transfer.FileMetadata;
import com.voxacode.wave.transfer.activities.viewmodels.SenderActivityViewModel;
import com.voxacode.wave.transfer.sender.FileSenderTask;
import com.voxacode.wave.databinding.ActivitySenderBinding;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.transition.platform.MaterialSharedAxis;

import com.voxacode.wave.utils.UnexpectedErrorDialogUtils;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@AndroidEntryPoint
public class SenderActivity extends AppCompatActivity {
    
    private ActivitySenderBinding binding;
    private SenderActivityViewModel viewModel;
    
    private void showToast( String message ) {
        Toast.makeText( this, message, Toast.LENGTH_SHORT ).show();
    }
    
    private void updateFileInfo( FileMetadata metadata ) {
        if( metadata != null ) {
            binding.fileName.setText( metadata.getName() );
            binding.fileSize.setText( SizeFormatter.format( metadata.getSize() ) );
            binding.fileIcon.setImageResource( FileIconResolver.resolve( metadata.getFileType() ) );
        }
    }
   
    private void stopEverything() {
        viewModel.shutdownFileSender();
    }
    
    private void showTransferSuccessful() {
        binding.topContainer.setVisibility( View.INVISIBLE );
        binding.fileProgress.setVisibility( View.INVISIBLE );
        binding.cardviewCenter.setVisibility( View.INVISIBLE );
        binding.cardviewFileInfo.setVisibility( View.INVISIBLE );
        binding.transferSuccessful.getRoot().setVisibility( View.VISIBLE );
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setEnterTransition( new MaterialSharedAxis( MaterialSharedAxis.X , true ) );
        getWindow().setReturnTransition( new MaterialSharedAxis( MaterialSharedAxis.X , false ) );
         
        binding = ActivitySenderBinding.inflate( getLayoutInflater() );
        setContentView( binding.getRoot() );
        
        viewModel = new ViewModelProvider( this ).get( SenderActivityViewModel.class );
        viewModel.getUnexpectedErrorObservable().observe(
            this, e -> {
                stopEverything();
                UnexpectedErrorDialogUtils.showUnexpectedErrorDialog( this );
            }
        );
        
        viewModel.getOnAllFilesSentObservable().observe(
            this, Void -> {
                stopEverything();
                showTransferSuccessful();
            }
        );
        
        if( viewModel.isAnyUnexpectedErrorOccured() || viewModel.areAllFilesSent() )
            return;
        
        BackPressHandler.observeBackPress(
            this,
            "Going back will stop the transfer. Do you want to continue?",
            () -> !viewModel.isAnyUnexpectedErrorOccured() &&
                  !viewModel.areAllFilesSent(),
            () -> {
                if( viewModel.areAllFilesSent() ) {
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
                    TransferSpeedFormatter.format( chunkInfo.getElapsedTime(), chunkInfo.getBytesSent() )
                );
            }
        );
        
        viewModel.getOnStartedSendingFileObservable().observe(
            this, metadata -> {
                updateFileInfo( metadata );
                binding.fileProgress.setProgressCompat( 0, true );
            }
        );
        
         
        viewModel.getOnFinishedSendingFileObservable().observe(
            this, metadata -> binding.fileProgress.setIndeterminate( true )
        );
         
        viewModel.getOnFileSkippedObservable().observe(
            this, path -> showToast( "Skipped file:" + path )
        );
            
        if( !viewModel.isFilesSendingProcessRunning() ) {
            viewModel.startFilesSendingProcess();
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
