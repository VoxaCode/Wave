package com.voxacode.wave.utils;
import java.text.DecimalFormat;

public final class SizeFormatter {
   
    private static final float OneKB = 1024;
    private static final float OneMB = OneKB * 1024;
    private static final float OneGB = OneMB * 1024;

    private static final String B = "B";
    private static final String KB = "KB";
    private static final String MB = "MB";
    private static final String GB = "GB";
     
    private static final DecimalFormat formatter = new DecimalFormat( "#.##" );
    
    public static String format( long bytes ) {
      
        if( bytes < OneKB )
            return bytes + B;
        
        if( bytes < OneMB ) 
            return formatter.format( bytes / OneKB ) + KB;
        
        if( bytes < OneGB ) 
            return formatter.format( bytes / OneMB ) + MB;
        
        return formatter.format( bytes / OneGB ) + GB;
    }
}
