package com.voxacode.wave.scanning;

import android.util.Log;
import android.media.Image;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

public class ImageAnalyzer implements ImageAnalysis.Analyzer {
    
    public interface QrCodeDetectedListener {
        void onQrCodeDetected( String rawValue );
    }
   
    private BarcodeScanner scanner;
    private QrCodeDetectedListener qrListener;
    
    public ImageAnalyzer( QrCodeDetectedListener qrListener ) {
        BarcodeScannerOptions scanOptions = new BarcodeScannerOptions.Builder()
            .setBarcodeFormats( Barcode.FORMAT_QR_CODE )
            .build();
        
        this.qrListener = qrListener;
        this.scanner = BarcodeScanning.getClient( scanOptions );
    }
   
    @Override
    public void analyze( ImageProxy imageProxy ) {
        if( imageProxy.getImage() == null ) {
            imageProxy.close();
            return;
        }
        
        InputImage inputImage = InputImage.fromMediaImage( 
            imageProxy.getImage(),
            imageProxy.getImageInfo().getRotationDegrees()
        );
        
        scanner.process( inputImage )
            .addOnSuccessListener( barcodes -> {
                if( !barcodes.isEmpty() && qrListener != null ) {
                    qrListener.onQrCodeDetected( barcodes.get( 0 ).getRawValue() );
                }
            } ).addOnCompleteListener( task -> {
                imageProxy.close();
            } );
    }
}
