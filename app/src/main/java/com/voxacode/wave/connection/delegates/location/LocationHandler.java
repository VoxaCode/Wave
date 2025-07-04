package com.voxacode.wave.connection.delegates.location;

import android.location.LocationManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.content.DialogInterface;
import androidx.activity.ComponentActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import android.os.Build;
import android.provider.Settings;

import com.voxacode.wave.utils.AlertDialogUtils;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class LocationHandler implements DefaultLifecycleObserver {
    
    public interface LocationStateListener {
        void onLocationEnabled();
        void onLocationDisabled();
    }
    
    public interface ShouldRequestLocationChecker {
        boolean shouldRequestLocation();
    }
    
    private AlertDialog locationDisabledDialog;
   
    private ComponentActivity activity;
    private LocationManager locationManager;
    
    private LocationStateListener stateListener;
    private ShouldRequestLocationChecker requestChecker;
    
    private LocationStateReceiver receiver;
    private IntentFilter intentFilter;
    
    private boolean receiverRegistered;
    private boolean isRequesting;
    
    private LocationHandler(
        ComponentActivity activity,
        ShouldRequestLocationChecker requestChecker,
        LocationStateListener stateListener ) {
        
        this.activity = activity;
        this.locationManager = ( LocationManager ) activity.getSystemService( Context.LOCATION_SERVICE );
        
        this.stateListener = stateListener;
        this.requestChecker = requestChecker;
        
        this.receiver = new LocationStateReceiver();
        this.intentFilter = new IntentFilter( LocationManager.PROVIDERS_CHANGED_ACTION );
    }
    
    public static LocationHandler bindToLifecycle( 
        ComponentActivity activity,
        ShouldRequestLocationChecker requestChecker,
        LocationStateListener stateListener ) {
        
        LocationHandler locationHandler = new LocationHandler( activity, requestChecker, stateListener );
        activity.getLifecycle().addObserver( locationHandler.new LifecycleObserver() );
        
        return locationHandler;
    }
    
    public boolean isLocationEnabled() {
        
        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.P )
            return Settings.Secure.getInt( activity.getContentResolver() , Settings.Secure.LOCATION_MODE , Settings.Secure.LOCATION_MODE_OFF )
                   != Settings.Secure.LOCATION_MODE_OFF;
        
        if( locationManager != null )
            return locationManager.isLocationEnabled();
        
        return false;
    }
    
    private void startOrFinishRequestingOnStateChange() {
        
        if( isLocationEnabled() ) {
            
            if( !isRequesting() ) 
                return;
            
            finishRequesting();
            if( stateListener != null ) 
                stateListener.onLocationEnabled();
            
        } else if( !isRequesting() ) {
            
            if( requestChecker != null && requestChecker.shouldRequestLocation() )
                startRequesting(); 
                  
            if( stateListener != null ) 
                stateListener.onLocationDisabled();
        }
    }
    
    private boolean isRequesting() {
        return isRequesting;
    }
    
    private void finishRequesting() {
        if( isRequesting() ) {
            isRequesting = false;
            dismissLocationDisabledDialog(); 
        }
    }
    
    private void startRequesting() {
        if( !isRequesting() ) {
            isRequesting = true;
            showLocationDisabledDialog();
        }
    }
    
    private void showLocationDisabledDialog() {
        
        if( isShowingLocationDisabledDialog() )
            return;
        
        locationDisabledDialog = 
            AlertDialogUtils.newAlertDialogBuilder( activity )
               .setCancelable( false )
               .setTitle( "Location Required" )    
               .setMessage( "Location services are disabled. Please enable location services in your device settings." )
               .setPositiveButton( "enable", null )
               .setNegativeButton( "exit", null )
               .create();
        
        locationDisabledDialog
        .setOnShowListener(
            dlg -> {
                
                locationDisabledDialog.getButton( AlertDialog.BUTTON_POSITIVE ) 
                .setOnClickListener( view -> activity.startActivity( getLocationSettingsIntent() ) );
                
                locationDisabledDialog.getButton( AlertDialog.BUTTON_NEGATIVE )    
                .setOnClickListener( view -> activity.finishAffinity() );
            }
        );
        
        locationDisabledDialog.show();
    }
     
    private void dismissLocationDisabledDialog() {
        if( locationDisabledDialog != null ) {
            locationDisabledDialog.dismiss();
        }
    }
    
    private boolean isShowingLocationDisabledDialog() {
        return locationDisabledDialog != null && locationDisabledDialog.isShowing();
    }
   
    private Intent getLocationSettingsIntent() {
        
        Intent locationSettings = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS );
        locationSettings.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        
        return locationSettings;
    }
    
    private class LifecycleObserver implements DefaultLifecycleObserver {
        
        @Override
        public void onPause( LifecycleOwner owner ) {
            if( receiverRegistered ) {
                activity.unregisterReceiver( receiver );
                receiverRegistered = false;
            }
        }
    
        @Override
        public void onResume( LifecycleOwner owner ) {
            if( !receiverRegistered ) {
                activity.registerReceiver( receiver, intentFilter );
                receiverRegistered = true;
            }
            
            startOrFinishRequestingOnStateChange();
        }
    }     
    
    public class LocationStateReceiver extends BroadcastReceiver {
    
        public void onReceive( Context context , Intent intent ) {
            if( intent.getAction().equals( LocationManager.PROVIDERS_CHANGED_ACTION ) )
                startOrFinishRequestingOnStateChange();
        }
    }
}