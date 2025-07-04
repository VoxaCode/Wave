package com.voxacode.wave.scanning.activities;

import java.util.concurrent.ExecutionException;

import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.lifecycle.ProcessCameraProvider;

import com.voxacode.wave.databinding.ActivityScannerBinding;
import com.voxacode.wave.delegates.permission.PermissionsHandler;
import com.voxacode.wave.delegates.permission.Permissions;
import com.voxacode.wave.scanning.ImageAnalyzer;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.transition.platform.MaterialSharedAxis;

public class ScannerActivity extends AppCompatActivity {
    
    public static final String EXTRA_QR_DATA = "QR_DATA";
    
    private PermissionsHandler permissionsHandler;
    private ActivityScannerBinding binding;
    private ProcessCameraProvider cameraProvider;
   
    private void showFatalErrorDialog( Exception error ) {
        new MaterialAlertDialogBuilder( this )
            .setCancelable( false )
            .setTitle( "Fatal Error!" )
            .setMessage( error.toString() )
            .setPositiveButton( "ok", ( dlg, which ) -> finish() )
            .show();
    }
    
    private void stopCamera() {
        if( cameraProvider != null ) {
            cameraProvider.unbindAll();
        }
    }
    
    private void startCamera() {
        
        ListenableFuture< ProcessCameraProvider > cameraProviderFuture = ProcessCameraProvider.getInstance( this );
        
        cameraProviderFuture.addListener( () -> {
           
            try {
                    
                ImageAnalyzer.QrCodeDetectedListener qrListener = new ImageAnalyzer.QrCodeDetectedListener() {
                        
                     @Override     
                     public void onQrCodeDetected( String rawValue ) {
                                
                         Intent result = new Intent();
                         result.putExtra( EXTRA_QR_DATA, rawValue );
                                    
                         setResult( RESULT_OK , result );
                         finish();     
                     }
                };
                     
                cameraProvider = cameraProviderFuture.get();
              
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider( binding.cameraPreview.getSurfaceProvider() );
            
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                         .setBackpressureStrategy( ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST )
                         .build();
          
                imageAnalysis.setAnalyzer(
                         ContextCompat.getMainExecutor( this ) ,
                         new ImageAnalyzer( qrListener )
                );
                
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
           
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle( this , cameraSelector , preview , imageAnalysis );   
                     
            } catch( ExecutionException | InterruptedException e ) {
                showFatalErrorDialog( e );
            }
       
        } , ContextCompat.getMainExecutor( this ) );
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setEnterTransition( new MaterialSharedAxis( MaterialSharedAxis.X , true ) );
        getWindow().setReturnTransition( new MaterialSharedAxis( MaterialSharedAxis.X , false ) );
       
        binding = ActivityScannerBinding.inflate( getLayoutInflater() );
        setContentView( binding.getRoot() );
       
        permissionsHandler = PermissionsHandler.bindToLifecycle(
            this,
            Permissions.getCameraPermissions(),
            () -> true,
            new PermissionsHandler.PermissionsStateListener() {
                
                @Override
                public void onPermissionsGranted() {
                    startCamera();
                }
                
                @Override
                public void onAnyPermissionDenied() {
                    stopCamera();
                }
            }
        );
        
        if( permissionsHandler.arePermissionsGranted() ) {
            startCamera();
        }
    }
}
