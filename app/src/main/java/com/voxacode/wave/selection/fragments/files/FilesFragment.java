package com.voxacode.wave.selection.fragments.files;

import android.os.Bundle;

import android.os.Environment;
import androidx.transition.Transition;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.content.Context;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.transition.MaterialSharedAxis;
import com.voxacode.wave.R;
import com.voxacode.wave.selection.SelectedFiles;
import com.voxacode.wave.selection.fragments.files.FilesRepository.FileInfo;
import com.voxacode.wave.databinding.FilesFragmentBinding;


import com.voxacode.wave.selection.utils.FragmentManagerUtils;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@AndroidEntryPoint
public class FilesFragment extends Fragment {
  
    public interface DirectoryClickListener {
        void onDirectoryClicked( String directory );
    }
    
    public static final String KEY_DIRECTORY_PATH = "DIRECTORY_PATH";
    
    private FilesFragmentViewModel viewModel;
    private FilesFragmentBinding binding;
    private FilesAdapter adapter;
    
    private String getDefaultPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }
    
    private String getDirectoryPath() {
        
        if( getArguments() == null ) 
            return getDefaultPath();
        
        return getArguments().getString(
            KEY_DIRECTORY_PATH,
            getDefaultPath()
        );
    }
    
    @Override
    public void onViewCreated( View layoutView , Bundle savedInstanceState ) {
        super.onViewCreated( layoutView , savedInstanceState );
     
        FilesAdapter.OnFileClickListener clickListener = file -> {
            
            if( file.isDirectory() ) {
                
                Bundle bundle = new Bundle();
                bundle.putString(
                    KEY_DIRECTORY_PATH,
                    file.getAbsolutePath() 
                );
                
                FilesFragment filesFragment = new FilesFragment();
                filesFragment.setArguments( bundle );
                
                FragmentManagerUtils.stackFragment(
                    getParentFragmentManager(),
                    filesFragment,
                    R.id.files_fragment_container
                );
                
            } else if( viewModel.doesExistInSelectedFiles( file.getAbsolutePath() ) ) {
                viewModel.removeFromSelectedFiles( file.getAbsolutePath() );
            } else {
                viewModel.addInSelectedFiles( file.getAbsolutePath() );
            }
        };
        
        FilesAdapter.FileSelectionChecker selectionChecker = file -> {
            return viewModel.doesExistInSelectedFiles( file.getAbsolutePath() );
        };
        
        adapter = new FilesAdapter( 
            getLayoutInflater(),
            clickListener,
            selectionChecker
        );
         
        binding.filesRecyclerView.setLayoutManager( new LinearLayoutManager( getContext() ) );
        binding.filesRecyclerView.setAdapter( adapter );
        
        viewModel = new ViewModelProvider( this ).get( FilesFragmentViewModel.class );
        viewModel.getFetchedFiles().observe(
            this, files -> {
                if( binding.loadingIndicator.getVisibility() == View.VISIBLE )
                    binding.loadingIndicator.setVisibility( View.INVISIBLE );
                
                if( binding.swipeRefresh.isRefreshing() )
                    binding.swipeRefresh.setRefreshing( false );
                
                adapter.submitList( files );
            }
        );
        
        binding.swipeRefresh.setOnRefreshListener( () -> {
            binding.swipeRefresh.setRefreshing( true ); 
            viewModel.fetchFiles( getDirectoryPath() );
        } );
      
        if( !viewModel.areFilesFetchedAtleastOnce() ) {
            binding.loadingIndicator.setVisibility( View.VISIBLE );
            viewModel.fetchFiles( getDirectoryPath() );
        }
    } 
    
    
    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setEnterTransition( new MaterialSharedAxis( MaterialSharedAxis.X , true ) );
        setExitTransition( new MaterialSharedAxis( MaterialSharedAxis.X , true ) );
        setReturnTransition( new MaterialSharedAxis( MaterialSharedAxis.X , false ) );
        setReenterTransition( new MaterialSharedAxis( MaterialSharedAxis.X , false ) );
    }
    
    @Override
    public View onCreateView( LayoutInflater inflater , ViewGroup viewGroup  , Bundle savedInstanceState ) {
        
        if( binding == null ) {
            binding = FilesFragmentBinding.inflate( inflater );
        }
        
        return binding.getRoot();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        viewModel.stopFetchingFiles();
    }
}