package com.voxacode.wave.connection.delegates.wifi;

import android.os.Build;
import android.provider.Settings;
import android.net.wifi.WifiManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface;
import android.content.Context;
import androidx.activity.ComponentActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.voxacode.wave.utils.AlertDialogUtils;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class WifiHandler {
    
    public interface WifiStateListener {
        void onWifiEnabled();
        void onWifiDisabled();
    }
    
    private static final int WIFI_STATE_ENABLED = 1;
    private static final int WIFI_STATE_DISABLED = 0;
    private static final int WIFI_STATE_UNDEFINED = -1;
    
    private AlertDialog wifiDisabledDialog;
    
    private ComponentActivity activity;
    private WifiManager wifiManager;
    
    private WifiStateListener stateListener;
    private int wifiState = WIFI_STATE_UNDEFINED;
    
    private WifiStateReceiver receiver; 
    private IntentFilter intentFilter;
    
    private boolean receiverRegistered;
    private boolean isRequesting;
    
    private WifiHandler( 
        ComponentActivity activity, 
        WifiStateListener stateListener ) {
        
        this.activity = activity;
        this.wifiManager = ( WifiManager ) activity.getSystemService( Context.WIFI_SERVICE );
        
        this.receiver = new WifiStateReceiver();
        this.intentFilter = new IntentFilter( WifiManager.WIFI_STATE_CHANGED_ACTION );
       
        this.stateListener = stateListener;
    }
    
    public static WifiHandler bindToLifecycle(
        ComponentActivity activity,
        WifiStateListener stateListener ) {
        
        WifiHandler wifiHandler = new WifiHandler( activity, stateListener );
        activity.getLifecycle().addObserver( wifiHandler.getLifecycleObserver() );
        
        return wifiHandler;
    }
    
    private LifecycleObserver getLifecycleObserver() {
        return new LifecycleObserver();
    }
    
    public boolean isWifiEnabled() {
        return wifiManager.isWifiEnabled();
    }
    
    public void requestWifi() {
        if( !isWifiEnabled() ) {
            startRequesting();
        }
    }
    
    private void callbackIfStateChangedAndFinishRequesting() {
        
        int currentState = isWifiEnabled() ?
        WIFI_STATE_ENABLED : WIFI_STATE_DISABLED;
        
        if( wifiState == currentState ) return;
        else wifiState = currentState;
        
        if( wifiState == WIFI_STATE_ENABLED && isRequesting ) 
            finishRequesting();  
        
        if( wifiState == WIFI_STATE_ENABLED && stateListener != null ) {
            stateListener.onWifiEnabled();        
        } else if( wifiState == WIFI_STATE_DISABLED && stateListener != null ) {
            stateListener.onWifiDisabled();
        }
    }
    
    private void startRequesting() {
        if( !isRequesting ) {
            isRequesting = true;
            showWifiDisabledDialog();
        }
    }
    
    private void finishRequesting() {
        if( isRequesting ) {
            isRequesting = false;
            dismissWifiDisabledDialog();
        }
    }
    
    private void showWifiDisabledDialog() {
         
        if( isShowingWifiDisabledDialog() )
            return;
        
        wifiDisabledDialog = 
            AlertDialogUtils.newAlertDialogBuilder( activity )
                .setCancelable( false )
                .setTitle( "Wi-Fi Required" )
                .setMessage( "Wi-Fi is disabled. This app requires Wi-Fi to function properly. Please enable Wi-Fi in your device settings" )
                .setPositiveButton( "enable", null )                  
                .setNegativeButton( "exit", null ) 
                .create();
        
        wifiDisabledDialog.setOnShowListener(
            dlg -> {
                
                wifiDisabledDialog.getButton( AlertDialog.BUTTON_POSITIVE )
                .setOnClickListener( view -> activity.startActivity( getWifiSettingsIntent() ) );
                
                wifiDisabledDialog.getButton( AlertDialog.BUTTON_NEGATIVE )
                .setOnClickListener( view -> activity.finishAffinity() );
            }
        );
        
        wifiDisabledDialog.show();
    }
    
    private void dismissWifiDisabledDialog() {
        if( isShowingWifiDisabledDialog() )
            wifiDisabledDialog.dismiss();
    }
    
    private boolean isShowingWifiDisabledDialog() {
        return wifiDisabledDialog != null && wifiDisabledDialog.isShowing();
    }
    
    private Intent getWifiSettingsIntent() {
        
        Intent wifiSettings = new Intent( Settings.ACTION_WIFI_SETTINGS );
        wifiSettings.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        
        return wifiSettings;            
    }
    
    private class LifecycleObserver implements DefaultLifecycleObserver {
        
        @Override
        public void onResume( LifecycleOwner owner ) {
            if( !receiverRegistered ) {
                activity.registerReceiver( receiver, intentFilter );
                receiverRegistered = true;
            }
            callbackIfStateChangedAndFinishRequesting();
        }
    
        @Override
        public void onPause( LifecycleOwner owner ) {
            if( receiverRegistered ) {
                activity.unregisterReceiver( receiver );
                receiverRegistered = false;
            }
        }
    }
  
    private class WifiStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive( Context context , Intent intent ) {
            if( intent.getAction().equals( WifiManager.WIFI_STATE_CHANGED_ACTION ) ) 
                callbackIfStateChangedAndFinishRequesting();
        }
    }   
} 
