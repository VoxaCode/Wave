package com.voxacode.wave.utils;
import com.voxacode.wave.R;
import android.app.Activity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class UnexpectedErrorDialogUtils {
    
    private static final String DEFAULT_MESSAGE = "An unexpected error occured please reastart the application.";
    
    public static void showUnexpectedErrorDialog( Activity activity ) {
        showUnexpectedErrorDialog( activity, DEFAULT_MESSAGE );
    }    
    
    public static void showUnexpectedErrorDialog( Activity activity, String message ) {
        new MaterialAlertDialogBuilder( activity, R.style.UnexpectedErrorDialogStyle )
            .setCancelable( false )
            .setTitle( "Unexpected Error" )
            .setMessage( message )
            .setNegativeButton( "exit", ( dialog, which ) -> activity.finishAffinity() )
            .show();  
    }
}
