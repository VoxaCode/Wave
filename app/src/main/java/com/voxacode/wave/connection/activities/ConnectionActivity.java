package com.voxacode.wave.connection.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.view.WindowInsets;
import android.widget.Toast;
import android.os.Build;
import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;
import androidx.core.app.ActivityOptionsCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;

import com.voxacode.wave.selection.activities.SelectionActivity;
import com.voxacode.wave.transfer.activities.ReceiverActivity;
import com.voxacode.wave.scanning.activities.ScannerActivity;
import com.voxacode.wave.connection.activities.viewmodels.ConnectionActivityViewModel;
import com.voxacode.wave.connection.delegates.location.LocationHandler;
import com.voxacode.wave.connection.delegates.wifi.WifiHandler;
import com.voxacode.wave.connection.host.LocalOnlyHotspotInfo;
import com.voxacode.wave.delegates.exit.BackPressHandler;
import com.voxacode.wave.delegates.permission.Permissions;
import com.voxacode.wave.delegates.permission.PermissionsHandler;
import com.voxacode.wave.databinding.ActivityConnectionBinding;
import com.voxacode.wave.utils.UnexpectedErrorDialogUtils;
import com.voxacode.wave.utils.AlertDialogUtils;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.transition.platform.MaterialSharedAxis;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ConnectionActivity extends AppCompatActivity {

    private ActivityConnectionBinding binding;
    private ConnectionActivityViewModel viewModel;
    
    private LocationHandler locationHandler;
    private WifiHandler wifiHandler;
    private PermissionsHandler permissionsHandler;
    
    private ActivityResultLauncher< Intent > scannerLauncher;
    
    private boolean isTransferProcessStarted;
    private boolean isFirstResume = true;
    
    private void showToast( String message ) {
        Toast.makeText( this, message, Toast.LENGTH_SHORT ).show();
    }
    
    private String getDeviceNickname() {
        return Settings.Global.getString(
            getContentResolver(),
            Settings.Global.DEVICE_NAME
        );
    }
    
    private void startScannerActivity() {
        scannerLauncher.launch(
           new Intent( this, ScannerActivity.class ),
           ActivityOptionsCompat.makeSceneTransitionAnimation( this )  
        );
    }
    
    private void startSelectionActivity() {
        startActivity(
            new Intent( this, SelectionActivity.class ),
            ActivityOptionsCompat.makeSceneTransitionAnimation( this ).toBundle()
        );
    }

    private void startReceiverActivity() {
        startActivity(
            new Intent( this, ReceiverActivity.class ),
            ActivityOptionsCompat.makeSceneTransitionAnimation( this ).toBundle()
        );
    }
    
    private void startLocalOnlyHotspot() {
        
        if( permissionsHandler.arePermissionsGranted() && locationHandler.isLocationEnabled() )
            viewModel.startLocalOnlyHotspot();
        
       else if( !permissionsHandler.arePermissionsGranted() ) 
           permissionsHandler.requestPermissions();
        
       else if( !locationHandler.isLocationEnabled() ) 
           locationHandler.requestLocation();
    }
    
    private void stopHotspotAndDiscovery() {
        viewModel.stopLocalOnlyHotspot();
        viewModel.stopDiscoveringClients();
    }
    
    private void shutdownEverything() {
        stopHotspotAndDiscovery();
        viewModel.stopRespondingClientDiscovery();
    }
    
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        
        getWindow().setExitTransition( new MaterialSharedAxis( MaterialSharedAxis.X , true ) );
        getWindow().setReenterTransition( new MaterialSharedAxis( MaterialSharedAxis.X , false ) );
        
        binding = ActivityConnectionBinding.inflate( getLayoutInflater() );
        setContentView( binding.getRoot() );
       
        if( !getPackageManager().hasSystemFeature( PackageManager.FEATURE_WIFI ) ) {
      
            AlertDialogUtils.newAlertDialogBuilder( this )       
                .setCancelable( false )
                .setTitle( "Device Unsupported" )
                .setMessage( "Your device doesn't support the necessary components for this app to work. Please use a supported device." )
                .setPositiveButton( "exit" , ( dialog , which ) -> finishAffinity() )
                .show();
            
            return;
        } 
       
        viewModel = new ViewModelProvider( this ).get( ConnectionActivityViewModel.class );
        binding.deviceNickname.setText( getDeviceNickname() );
        binding.btnScan.setOnClickListener( view -> 
           startScannerActivity() 
        );
        
        viewModel.getUnexpectedErrorObservable().observe(
            this, error -> {
                shutdownEverything(); 
                UnexpectedErrorDialogUtils.showUnexpectedErrorDialog( this , error.toString());
            }
        );
        
        if( viewModel.isAnyUnexpectedErrorOccured() )
            return;
        
        viewModel.getHotspotQrModel().observe(
            this,
            qrModel -> {
                if( qrModel != null ) {
                    binding.wifiQrCode.setImageDrawable( qrModel.getQrCode() );
                    binding.wifiName.setText( qrModel.getWifiName() );
                }
            }
        );
     
        viewModel.getOnHotspotStartedObservable().observe(
            this, hotspotInfo -> {
                viewModel.startDiscoveringClients();
                viewModel.updateDeviceQr(
                    ConnectionActivity.this,
                    hotspotInfo
                );
            }
        );
        
        //This will be executed only when hotspot is shutdown 
        //unexpectedly or by system not by reservation.close()
        //like wifi disabled in api < 30
        viewModel.getOnHotspotStoppedObservable().observe(
            this, hotspotInfo -> {
                viewModel.stopDiscoveringClients();
                startLocalOnlyHotspot();
            }
        );
        
        viewModel.getClientDiscoveryObservable().observe(
            this, nil -> {
                isTransferProcessStarted = true;
                startReceiverActivity();
            }
        );
        
        viewModel.getClientDiscoveryResponsedObservable().observe(
            this, nil -> {
                isTransferProcessStarted = true;
                startSelectionActivity();
            }
        );
       
        scannerLauncher = registerForActivityResult( 
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                
                if( result.getResultCode() != RESULT_OK ) {
                    showToast( "Failed to scan QR code" );
                    return;
                }
                
                try {
                    //Creating LocalOnlyHotspotInfo instance
                    String rawData = result.getData().getStringExtra( ScannerActivity.KEY_QR_DATA );
                    LocalOnlyHotspotInfo hotspotInfo = viewModel.convertRawDataToHotspotInfo( rawData );
                    
                    //stopping hotspot and discovery
                    stopHotspotAndDiscovery();
                    
                    //add the hotspot info as pending and 
                    //request user to enable wifi if wifi is disabled
                    if( !wifiHandler.isWifiEnabled() ) {
                        viewModel.addPendingConnection( hotspotInfo );
                        wifiHandler.requestWifi();
                        return;
                    }
                    
                   //else connecting to the wifi network
                    viewModel.connectToHotspot( 
                        hotspotInfo,
                        () -> viewModel.startRespondingClientDiscovery()
                    );
            
                } catch( Exception e ) {
                    showToast( "Invalid QR code" );
                }
            }
        );
           
       /**
        * should not request user to enable wifi if hotspot is active 
        * in api < 30 cause they use hardware to create hotspot which
        * disables wifi to work
        *
        * onWifiEnabled callback will only be called if requestWifi 
        * is called and user enables wifi
        */
        wifiHandler = WifiHandler.bindToLifecycle(
            this, new WifiHandler.WifiStateListener() {
                
                @Override
                public void onWifiDisabled() { }
                
                @Override
                public void onWifiEnabled() {
                    if( viewModel.isConnectionPending() ) {
                        //connecting to wifi network
                        viewModel.connectToHotspot( 
                            viewModel.getPendingConnection(),
                            () -> viewModel.startRespondingClientDiscovery()
                        );
                    }
                }
            }
        );
        
        //onPermissionsGranted callback will only be called if requestPermissions
        //is called and user grants permissions
        permissionsHandler = PermissionsHandler.bindToLifecycle(
            Permissions.getLocationPermissions(),
            this, new PermissionsHandler.PermissionsStateListener() {
                
                @Override 
                public void onAnyPermissionDenied() { }
                
                @Override
                public void onPermissionsGranted() {
                    if( !viewModel.isLocalOnlyHotspotActive() ) {
                        startLocalOnlyHotspot();
                    }
                }
            }
        );
        
       //onLocationEnabled callback will only be called if requestLocation
       //is called and user enables location
       locationHandler = LocationHandler.bindToLifecycle(
            this, new LocationHandler.LocationStateListener() {
                
                @Override
                public void onLocationDisabled() { }
                
                @Override
                public void onLocationEnabled() { 
                    if( !viewModel.isLocalOnlyHotspotActive() ) {
                        startLocalOnlyHotspot();
                    }
                }
            }
        );
     
        BackPressHandler.observeBackPress(
            this,
            "Are you sure you want to exit?",
            () -> !viewModel.isAnyUnexpectedErrorOccured(),
            null,
            () -> {
                shutdownEverything();
                finishAffinity();
            }
        );
        
        startLocalOnlyHotspot();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        viewModel.stopDiscoveringClients();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if( isTransferProcessStarted ) {
            isTransferProcessStarted = false;
            viewModel.stopRespondingClientDiscovery();
            viewModel.startDiscoveringClients();
            
            //when receiver comes back the hotspot will be reused
            //and when sender comes back the hotspot will be created again
            //allowing any device to become sender or receiver
            if( !viewModel.isLocalOnlyHotspotActive() ) {
                startLocalOnlyHotspot();
            } 
            
        } else if( !isFirstResume ) {
            viewModel.startDiscoveringClients();
        }
        
        if( isFirstResume )
            isFirstResume = false;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if( !isChangingConfigurations() ) {
            shutdownEverything();
        }
    }
}