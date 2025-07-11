package com.voxacode.wave.utils;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import java.util.concurrent.atomic.AtomicBoolean;

public class SingleLiveEvent< T > extends MutableLiveData< T > {
    private final AtomicBoolean pending = new AtomicBoolean( false );
        
    @Override
    public void observe( LifecycleOwner owner, Observer< ? super T > observer ) {
        super.observe(
            owner, t -> {
                if( pending.compareAndSet( true, false ) ) {
                    observer.onChanged( t );
                }
            }
        );
    }
    
    @Override
    public void postValue( T value ) {
        pending.set( true );
        super.postValue( value );
    }
        
    @Override
    public void setValue( T value ) {
        pending.set( true );
        super.setValue( value );
    }
}
