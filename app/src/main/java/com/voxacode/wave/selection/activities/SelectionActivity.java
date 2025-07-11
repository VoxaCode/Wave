package com.voxacode.wave.selection.activities;

import android.view.WindowInsets;
import android.widget.Toast;
import androidx.core.app.ActivityOptionsCompat;
import android.content.Intent;
import android.os.Environment;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.voxacode.wave.R;
import com.voxacode.wave.delegates.exit.BackPressHandler;
import com.voxacode.wave.delegates.permission.Permissions;
import com.voxacode.wave.delegates.permission.PermissionsHandler;
import com.voxacode.wave.selection.fragments.files.FilesFragment;
import com.voxacode.wave.selection.SelectedFiles;
import com.voxacode.wave.databinding.ActivitySelectionBinding;
import com.voxacode.wave.selection.utils.FragmentManagerUtils;
import com.voxacode.wave.transfer.activities.SenderActivity;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

import com.google.android.material.transition.platform.MaterialSharedAxis;

@AndroidEntryPoint
public class SelectionActivity extends AppCompatActivity {
    
    @Inject
    SelectedFiles selectedFiles;
    
    private ActivitySelectionBinding binding;
    private PermissionsHandler permissionsHandler;
    
    private FragmentManager fragmentManager;
    
    private void startSenderActivity() {
        startActivity( 
            new Intent( this , SenderActivity.class ),  
            ActivityOptionsCompat.makeSceneTransitionAnimation( SelectionActivity.this ).toBundle() 
        );
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         
        getWindow().setEnterTransition( new MaterialSharedAxis( MaterialSharedAxis.X , true ) );
        getWindow().setReturnTransition( new MaterialSharedAxis( MaterialSharedAxis.X , false ) );
        getWindow().setExitTransition( new MaterialSharedAxis( MaterialSharedAxis.X , true ) );
        getWindow().setReenterTransition( new MaterialSharedAxis( MaterialSharedAxis.X , false ) );
        
        binding = ActivitySelectionBinding.inflate( getLayoutInflater() );
        setContentView( binding.getRoot() );
        
        fragmentManager = getSupportFragmentManager();
        
        binding.btnCountinue.setOnClickListener( view -> {
                
            if( !selectedFiles.isEmpty() ) {
                startSenderActivity();
                finishAfterTransition();     
                return;      
            } 
                
            Toast.makeText( 
                this,
                "Select files first",
                Toast.LENGTH_SHORT
            ).show();
        } );
        
        permissionsHandler = PermissionsHandler.bindToLifecycle(
            Permissions.getStoragePermissions(),
            this, null
        );
        
        BackPressHandler.observeBackPress( 
            this,
            "Going back will disconnect you. You'll need to reconnect. Continue?",
            () -> fragmentManager.getBackStackEntryCount() == 1,
            () -> {
                if( fragmentManager.getBackStackEntryCount() > 1 ) 
                    fragmentManager.popBackStack();
            },
            () -> finishAfterTransition()
        );
         
        FragmentManagerUtils.stackFragment( 
            fragmentManager,
            new FilesFragment(),
            R.id.files_fragment_container
        );
    }
}