package com.voxacode.wave.selection.utils;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class FragmentManagerUtils {
    public static void stackFragment( FragmentManager fragmentManager, Fragment fragment, int containerId ) {
        fragmentManager.beginTransaction()
            .add( containerId, fragment )
            .addToBackStack( null )
            .commit();
    }
}
