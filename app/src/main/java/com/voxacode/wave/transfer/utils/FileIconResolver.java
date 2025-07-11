package com.voxacode.wave.transfer.utils;

import com.voxacode.wave.R;
import com.voxacode.wave.transfer.FileMetadata;

public class FileIconResolver {
   
    public static int resolve( FileMetadata.FileType type ) {
        
        switch( type ) {
            case FILE : return R.drawable.file_24px;
            case PICTURE : return R.drawable.image_24px;
            case VIDEO : return R.drawable.movie_24px;
            case AUDIO : return R.drawable.graphic_eq_24px;
            case APPLICATION : return R.drawable.android_24px;
        }
        
        throw new IllegalStateException();
    }  
}
