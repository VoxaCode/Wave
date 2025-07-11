package com.voxacode.wave.utils;

import android.app.Activity;
import com.voxacode.wave.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AlertDialogUtils {
    public static MaterialAlertDialogBuilder newAlertDialogBuilder( Activity activity ) {
        return new MaterialAlertDialogBuilder( 
            activity,
            R.style.AlertDialogStyle
        );
    }
}
