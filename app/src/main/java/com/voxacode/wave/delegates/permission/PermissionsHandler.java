package com.voxacode.wave.delegates.permission;

import java.util.List;

import android.net.Uri;
import android.provider.Settings;
import android.view.View;
import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import androidx.activity.ComponentActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import com.voxacode.wave.utils.AlertDialogUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class PermissionsHandler {
    
    public interface PermissionsStateListener {
        void onAnyPermissionDenied();
        void onPermissionsGranted(); 
    }
    
    public interface ShouldRequestPermissionsChecker {
        boolean shouldRequestPermissions();
    }
    
    private interface MultiplePermissionsReportListener {
        void onMultiplePermissionsReportAvailable( MultiplePermissionsReport report );
    }
    
    private ComponentActivity activity;
    private AlertDialog permissionsRequiredDialog;
    private List< String > requiredPermissions;
    
    private PermissionsStateListener stateListener;
    private ShouldRequestPermissionsChecker requestChecker;
    
    private boolean isRequesting;
    
    private PermissionsHandler(
        ComponentActivity activity, 
        List< String > requiredPermissions, 
        PermissionsStateListener stateListener,
        ShouldRequestPermissionsChecker requestChecker ) {
        
        this.activity = activity;
        this.requiredPermissions = requiredPermissions;
        
        this.stateListener = stateListener;
        this.requestChecker = requestChecker;
    }
    
    public static PermissionsHandler bindToLifecycle(
        ComponentActivity activity, 
        List< String > requiredPermissions,
        ShouldRequestPermissionsChecker requestChecker,
        PermissionsStateListener stateListener ) {
   
        PermissionsHandler permissionsHandler = new PermissionsHandler( activity , requiredPermissions , stateListener, requestChecker );
        activity.getLifecycle().addObserver( permissionsHandler.new LifecycleObserver() );
        
        return permissionsHandler;
    }
    
    public boolean arePermissionsGranted() {
        for( String permission : requiredPermissions ) {
            if( permission.equals( Manifest.permission.MANAGE_EXTERNAL_STORAGE ) ) {
                if( !Environment.isExternalStorageManager() )
                    return false;
            } else if( ContextCompat.checkSelfPermission( activity , permission ) == PackageManager.PERMISSION_DENIED ) {
                return false;
            }
        }
        return true;
    }
      
    private void requestPermissionsWithDexter( MultiplePermissionsReportListener listener ) {
        
        Dexter.withContext( activity )
              .withPermissions( requiredPermissions )
              .withListener( new MultiplePermissionsListener() {
                  @Override
                  public void onPermissionRationaleShouldBeShown( List< PermissionRequest > permissions , PermissionToken token ) {
                      token.continuePermissionRequest();  
                  }   
                    
                  @Override
                  public void onPermissionsChecked( MultiplePermissionsReport report ) {
                     listener.onMultiplePermissionsReportAvailable( report );
                  }  
              } )
              .check();
    } 
    
    private boolean isRationaleSuppressedForAnyPermission() {
        
        for( String permission : requiredPermissions ) {
                    
            if( permission.equals( Manifest.permission.MANAGE_EXTERNAL_STORAGE ) ) 
                return true;
            
            if( ContextCompat.checkSelfPermission( activity , permission ) == PackageManager.PERMISSION_DENIED
                && !activity.shouldShowRequestPermissionRationale( permission ) ) 
                return true;
        }       
        
        return false;
    }  
   
    private boolean isRequesting() {
        return isRequesting;
    }
   
    private void finishRequesting() {
        if( isRequesting() ) {
            isRequesting = false;
            dismissPermissionsRequiredDialog();
        }
    }
     
    private void startRequesting() {
        
        if( isRequesting() )
            return;
        
        isRequesting = true;
        if( isRationaleSuppressedForAnyPermission() ) {
            showPermissionsRequiredDialog(
                view -> activity.startActivity( getAppSettingsIntent() )
            );
            
            return;
        }
        
        requestPermissionsWithDexter( 
            report -> {
                
                if( report.areAllPermissionsGranted()  ) {
                    
                    finishRequesting();
                    if( stateListener != null )
                        stateListener.onPermissionsGranted();
                    
                    return;
                }
                
                showPermissionsRequiredDialog( 
                    view -> {
                        
                        if( report.isAnyPermissionPermanentlyDenied() ) {
                            activity.startActivity( getAppSettingsIntent() );
                            return;
                        }
                        
                        requestPermissionsWithDexter(
                            mreport -> {
                                if( mreport.areAllPermissionsGranted() ) {
                                    
                                    finishRequesting();
                                    if( stateListener != null )     
                                        stateListener.onPermissionsGranted();
                                }
                            } 
                        );
                    }
                );
            }
        );
    }
 
    private void showPermissionsRequiredDialog( View.OnClickListener allowListener ) {
        
        if( isShowingPermissionsRequiredDialog() )
            return;
        
        permissionsRequiredDialog =
            AlertDialogUtils.newAlertDialogBuilder( activity )
                .setCancelable( false )
                .setTitle( "Permissions Required" )
                .setMessage( "Some permissions have been denied. These are required for the app to fuction properly. Please tap 'Allow' to enable them." )
                .setPositiveButton( "Allow", null )
                .setNegativeButton( "Deny", null )
                .create();
        
        permissionsRequiredDialog
        .setOnShowListener(
            dlg -> {
                
                permissionsRequiredDialog.getButton( AlertDialog.BUTTON_POSITIVE )
                .setOnClickListener( allowListener );
                
                permissionsRequiredDialog.getButton( AlertDialog.BUTTON_NEGATIVE )
                .setOnClickListener( view -> activity.finishAffinity() );
            }
        );
        
        permissionsRequiredDialog.show();
    }
    
    private void dismissPermissionsRequiredDialog() {
        if( permissionsRequiredDialog != null ) {
            permissionsRequiredDialog.dismiss();
        }
    }
    
    private boolean isShowingPermissionsRequiredDialog() {
        return permissionsRequiredDialog != null && permissionsRequiredDialog.isShowing();
    }
    
    private Intent getAppSettingsIntent() {
         
        Intent appSettingsIntent = new Intent( Settings.ACTION_APPLICATION_DETAILS_SETTINGS );
        appSettingsIntent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        appSettingsIntent.setData( Uri.parse( "package:" + activity.getPackageName() ) );
                                                            
        return appSettingsIntent;
    }
        
    private class LifecycleObserver implements DefaultLifecycleObserver {
        
        @Override
        public void onResume( LifecycleOwner owner ) {
  
            if( arePermissionsGranted() )  {
           
                if( !isRequesting() )
                    return;
                
                finishRequesting();
                if( stateListener != null )     
                    stateListener.onPermissionsGranted();
                
            } else if( !isRequesting() ) {
              
                if( stateListener != null )
                    stateListener.onAnyPermissionDenied();
                
                if( requestChecker != null && requestChecker.shouldRequestPermissions() ) 
                    startRequesting();
            }
        }          
    }
}
