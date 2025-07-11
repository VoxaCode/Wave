package com.voxacode.wave.delegates.exit;

import android.content.Context;
import android.os.Build;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.voxacode.wave.utils.AlertDialogUtils;

public class BackPressHandler {
    
    public interface OnExitInvokedListener {
        void onExitInvoked();
    }
    
    public interface ShouldShowDialogChecker {
        boolean shouldShowDialog();
    }
    
    public interface OnBackPressedListener {
        void onBackPressed();
    }
   
    private ComponentActivity activity;
    
    private ShouldShowDialogChecker dialogChecker; 
    private OnBackPressedListener backListener;
    private OnExitInvokedListener exitListener;
    
    private String message;
    private AlertDialog areYouSureDialog;
   
    private BackPressHandler( ComponentActivity activity, String message, ShouldShowDialogChecker dialogChecker, OnBackPressedListener backListener, OnExitInvokedListener exitListener ) {
        this.activity = activity;
        this.message = message;
        this.dialogChecker = dialogChecker;
        this.backListener = backListener;
        this.exitListener = exitListener;
    }
    
    public static void observeBackPress(
        ComponentActivity activity, 
        String message, 
        ShouldShowDialogChecker dialogChecker,
        OnBackPressedListener backListener,
        OnExitInvokedListener exitListener ) {
       
        BackPressHandler backHandler = new BackPressHandler( 
            activity, message, dialogChecker, backListener, exitListener
        );
        
        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ) {
            activity.getOnBackPressedDispatcher().addCallback(
                activity, backHandler.getBackPressedCallback() 
            );
            
        } else {
            activity.getLifecycle().addObserver(
                backHandler.getBackInvokedCallbackLifecycleObserver()
            );
        }
    }
    
    private BackPressedCallback getBackPressedCallback() {
        return new BackPressedCallback( true );
    }
    
    private BackInvokedCallback getBackInvokedCallback() {
        return new BackInvokedCallback();
    }
    
    private BackInvokedCallbackLifecycleObserver getBackInvokedCallbackLifecycleObserver() {
        return new BackInvokedCallbackLifecycleObserver(
            getBackInvokedCallback(), true 
        );
    }
    
    //should only be called from BackPressedCallback 
    //or BackInvokedCallback calling this outside of a
    //back event context can cause unintented behavior
    private void callbackAndShowDialogIfShould() {
         
        if( backListener != null ) 
            backListener.onBackPressed();
            
        if( dialogChecker != null && dialogChecker.shouldShowDialog() ) 
            showAreYouSureDialog();
    }
    
    private boolean isShowingAreYouSureDialog() {
        return areYouSureDialog != null && areYouSureDialog.isShowing();
    }
    
    private void showAreYouSureDialog() {
        
        if( isShowingAreYouSureDialog() )
            return;
        
        areYouSureDialog = 
            AlertDialogUtils.newAlertDialogBuilder( activity )
                .setCancelable( true )
                .setTitle( "Are you sure?" )
                .setMessage( message )
                .setNeutralButton( "cancel", null )
                .setNegativeButton( "exit", null ) 
                .create();
        
        areYouSureDialog.setOnShowListener(
            dlg -> {
                
                areYouSureDialog.getButton( AlertDialog.BUTTON_NEGATIVE )
                .setOnClickListener( 
                    view -> {
                        dismissAreYouSureDialog();
                        if( exitListener != null ) {
                            exitListener.onExitInvoked();
                        }
                    } 
                );
                
                areYouSureDialog.getButton( AlertDialog.BUTTON_NEUTRAL )
                .setOnClickListener( view -> dismissAreYouSureDialog() );
            }
        );
        
        areYouSureDialog.show();
    }
    
    private void dismissAreYouSureDialog() {
        if( areYouSureDialog != null ) {
            areYouSureDialog.dismiss();
        }
    }
    
    private class BackPressedCallback extends OnBackPressedCallback {
        
        private BackPressedCallback( boolean enabled ) {
            super( enabled );
        }
        
        @Override
        public void handleOnBackPressed() {
            callbackAndShowDialogIfShould();
        }
    }
    
    private class BackInvokedCallback implements OnBackInvokedCallback {
        @Override
        public void onBackInvoked() {
            callbackAndShowDialogIfShould();
        }
    }
    
    private class BackInvokedCallbackLifecycleObserver implements DefaultLifecycleObserver {
        
        private BackInvokedCallback backCallback;
        private boolean isRegistered;
        
        public BackInvokedCallbackLifecycleObserver( BackInvokedCallback backCallback, boolean registerImmediately ) {
            this.backCallback = backCallback;
            if( registerImmediately ) {
                registerCallback();     
            }
        }
        
        private void registerCallback() {
            if( !isRegistered ) {
                isRegistered = true;
                activity.getOnBackInvokedDispatcher()
                .registerOnBackInvokedCallback( 
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    backCallback
                );
            }
        }
        
        private void unregisterCallback() {
            if( isRegistered ) {
                isRegistered = false;
                activity.getOnBackInvokedDispatcher()
                .unregisterOnBackInvokedCallback( backCallback );
            }
        }
        
        @Override
        public void onResume( LifecycleOwner owner ) {
            registerCallback();
        }
        
        @Override
        public void onPause( LifecycleOwner owner ) {
            unregisterCallback();
        }
    }
}
