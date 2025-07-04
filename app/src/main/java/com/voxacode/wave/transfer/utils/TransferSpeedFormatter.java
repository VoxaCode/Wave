package com.voxacode.wave.transfer.utils;

import com.voxacode.wave.utils.SizeFormatter;

public class TransferSpeedFormatter {
    
    //elapsedTime should be in milliseconds
    public static String format( long elapsedTime, long bytesSend ) {
        
        //formatting bytes send in one second 
        return SizeFormatter.format(
            ( bytesSend / elapsedTime ) * 1000
        ) + "/s";
    }
}
