package com.voxacode.wave.connection.activities.viewmodels;

import androidx.annotation.MainThread;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import com.voxacode.wave.utils.SingleLiveEvent;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.util.Log;
import android.util.TypedValue;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONException;
import org.json.JSONObject;

import com.voxacode.wave.R;
import com.voxacode.wave.connection.activities.ConnectionActivity;
import com.voxacode.wave.connection.client.ClientAdvertisementManager;
import com.voxacode.wave.connection.client.ConnectionManager;
import com.voxacode.wave.connection.host.ClientDiscoveryManager;
import com.voxacode.wave.connection.host.LocalOnlyHotspotInfo;
import com.voxacode.wave.connection.host.LocalOnlyHotspotManager;
import com.voxacode.wave.connection.host.LocalOnlyHotspotQrModel;
import com.voxacode.wave.connection.host.LocalOnlyHotspotManager.OnLocalOnlyHotspotStartedListener;
import com.voxacode.wave.connection.host.LocalOnlyHotspotManager.OnLocalOnlyHotspotFailedListener;

import com.github.alexzhirkevich.customqrgenerator.QrData;
import com.github.alexzhirkevich.customqrgenerator.QrErrorCorrectionLevel;
import com.github.alexzhirkevich.customqrgenerator.style.BitmapScale;
import com.github.alexzhirkevich.customqrgenerator.vector.QrCodeDrawableKt;
import com.github.alexzhirkevich.customqrgenerator.vector.QrVectorOptions;
import com.github.alexzhirkevich.customqrgenerator.vector.style.CircleVectorShape;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBackground;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBallShape;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColor;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColors;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorFrameShape;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogo;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogoPadding;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogoShape;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape;
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorShapes;

import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class ConnectionActivityViewModel extends ViewModel {
    
    public static final String KEY_WIFI_SSID = "wifi_ssid";
    public static final String KEY_WIFI_PASSWORD = "wifi_password";
    
    private final SingleLiveEvent< LocalOnlyHotspotQrModel > hotspotQrModel = new SingleLiveEvent<>(); 
    private final ExecutorService qrCodeExecutor = Executors.newSingleThreadExecutor();
    
    private final MutableLiveData< Exception > unexpectedErrorObservable = new MutableLiveData<>();
    private boolean isAnyUnexpectedErrorOccured;
    
    private LocalOnlyHotspotManager hotspotManager;
    private ConnectionManager connectionManager;
    
    private ClientDiscoveryManager discoveryManager;
    private final MutableLiveData< Void > clientDiscoveryObservable = new MutableLiveData<>();
  
    private ClientAdvertisementManager advertisementManager;
    private final MutableLiveData< Void > advertisementResponseObservable = new MutableLiveData<>();
    
    @Inject
    public ConnectionActivityViewModel( 
        LocalOnlyHotspotManager hotspotManager,
        ConnectionManager connectionManager,
        ClientDiscoveryManager discoveryManager, 
        ClientAdvertisementManager advertisementManager ) {
        
        this.hotspotManager = hotspotManager;
        this.connectionManager = connectionManager;
    
        this.discoveryManager = discoveryManager;
        this.advertisementManager = advertisementManager;
    }
    
    public boolean isAnyUnexpectedErrorOccured() {
        return isAnyUnexpectedErrorOccured;
    }
    
    public LiveData< Exception > getUnexpectedErrorObservable() {
        return unexpectedErrorObservable;
    }
    
    private void postUnexpectedError( Exception e ) {
        isAnyUnexpectedErrorOccured = true;
        unexpectedErrorObservable.postValue( e );
    }
    
    public LiveData< LocalOnlyHotspotQrModel > getHotspotQrModel() {
        return hotspotQrModel;
    }
  
    public void updateDeviceQr( Context context, LocalOnlyHotspotInfo hotspotInfo ) {
        
        JSONObject json = new JSONObject(
            Map.of(
                KEY_WIFI_SSID, hotspotInfo.getSsid(),
                KEY_WIFI_PASSWORD, hotspotInfo.getPassword()
            )
        );
        
        QrData qrData = new QrData.Text( json.toString() );
        
        TypedValue colorPrimaryValue = new TypedValue();
        TypedValue colorSurfaceValue = new TypedValue();
        
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute( 
            com.google.android.material.R.attr.colorPrimary,
            colorPrimaryValue,
            true 
        );
        theme.resolveAttribute(
            com.google.android.material.R.attr.colorSurface,
            colorSurfaceValue,
            true
        );
     
        Drawable mLogo = ContextCompat.getDrawable( context, R.drawable.app_logo );
        mLogo.setTint( colorPrimaryValue.data );
              
        QrVectorColor colorPrimary = new QrVectorColor.Solid( colorPrimaryValue.data );
        QrVectorColor colorSurface = new QrVectorColor.Solid( colorSurfaceValue.data );
      
        QrVectorLogo logo = new QrVectorLogo(
            mLogo,                                           //Logo
            .21f,                                            //Size
            new QrVectorLogoPadding.Natural( .2f ),          //Padding
            QrVectorLogoShape.Circle.INSTANCE,               //Shape
            BitmapScale.FitXY.INSTANCE,                      //Bitmap scale
            QrVectorColor.Unspecified.INSTANCE               //Background color
        );
        
        QrVectorColors colors = new QrVectorColors(
            colorPrimary,                                    //Dark pixels
            colorSurface,                                    //Light pixels
            colorPrimary,                                    //Eye ball
            colorPrimary                                     //Eye frame
        );
            
        QrVectorShapes shapes = new QrVectorShapes(
            new QrVectorPixelShape.Circle( .65f ),                                        //Dark pixels
            QrVectorPixelShape.Default.INSTANCE,                                          //Light pixels
            new QrVectorBallShape.RoundCorners( .25f, true, true, true, true ),           //Eye ball
            new QrVectorFrameShape.RoundCorners( .25f, 1f, true, true, true, true ),      //Eye frame
            true                                                                          //Center coordination
        );
        
        QrErrorCorrectionLevel correctionLevel = QrErrorCorrectionLevel.Auto;
        
        QrVectorOptions options = new QrVectorOptions.Builder()
            .setErrorCorrectionLevel( correctionLevel )
            .setLogo( logo )
            .setColors( colors )
            .setShapes( shapes )
            .build();
        
        qrCodeExecutor.execute(
            () -> {
                Drawable qrCode = QrCodeDrawableKt.QrCodeDrawable( qrData, options, StandardCharsets.UTF_8 );
                hotspotQrModel.postValue(
                    new LocalOnlyHotspotQrModel(
                        hotspotInfo.getSsid(),
                        qrCode
                    )
                );
            }
        );
    }
    
    public LocalOnlyHotspotInfo convertRawDataToHotspotInfo( String rawData ) throws JSONException, IllegalArgumentException {
        JSONObject json = new JSONObject( rawData );
        return new LocalOnlyHotspotInfo(
             ( String ) json.get( KEY_WIFI_SSID ),
             ( String ) json.get( KEY_WIFI_PASSWORD )
        );
    }
    
    public boolean isLocalOnlyHotspotActive() {
        return hotspotManager.isLocalOnlyHotspotActive();
    }
    
    public void stopLocalOnlyHotspot() {
        hotspotManager.stopLocalOnlyHotspot();
    }
    
    public void startLocalOnlyHotspot( OnLocalOnlyHotspotStartedListener successListener ) {
        hotspotManager.startLocalOnlyHotspot(
            successListener,
            error -> postUnexpectedError( error )
        );
    }
     
    public void connectToHotspot( LocalOnlyHotspotInfo hotspotInfo, ConnectionManager.OnConnectionSuccessfulListener successListener ) {
        connectionManager.connect( hotspotInfo, successListener );
    }
    
    public boolean isDiscoveringClients() {
        return discoveryManager.isDiscovering();
    }
    
    public LiveData< Void > getClientDiscoveryObservable() {
        return clientDiscoveryObservable;
    }
    
    public void startDiscoveringClients() {
        discoveryManager.startDiscovery(
            () -> clientDiscoveryObservable.postValue( null ),
            error -> postUnexpectedError( error )
        );
    }
   
    public void stopDiscoveringClients() {
        discoveryManager.stopDiscovery();
    }
    
    public boolean isAdvertisingDevice() {
        return advertisementManager.isAdvertising();
    }
    
    public LiveData< Void > getAdvertisementResponseObservable() {
        return advertisementResponseObservable;
    }
    
    public void startAdvertisingDevice() {
        advertisementManager.startAdvertising(
            () -> advertisementResponseObservable.postValue( null ),
            error -> postUnexpectedError( error )
        );
    }
    
    public void stopAdvertisingDevice() {
        advertisementManager.stopAdvertising();
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        qrCodeExecutor.shutdown();
    }
}
