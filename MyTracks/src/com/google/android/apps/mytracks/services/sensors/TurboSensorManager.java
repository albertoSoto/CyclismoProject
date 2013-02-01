package com.google.android.apps.mytracks.services.sensors;

import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.content.Sensor.SensorData;
import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;
import com.google.android.apps.mytracks.content.Sensor.SensorState;
import com.google.android.maps.mytracks.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class TurboSensorManager extends SensorManager {
  
  SensorDataSet sensorDataSet;
  
  private static final String TAG = TurboSensorManager.class.getSimpleName();
  
  private static String NEW_CADENCE_ACTION;
  private static String NEW_SPEED_ACTION;
  private static String NEW_HEART_RATE_ACTION;
  private static String NEW_POWER_ACTION;
  private static String DATA_ID;
  
  private Context context;
  
  public TurboSensorManager(Context context) {
    this.context = context;
    sensorDataSet = Sensor.SensorDataSet.getDefaultInstance();
    NEW_CADENCE_ACTION = context.getString(R.string.sensor_data_cadence);
    NEW_SPEED_ACTION = context.getString(R.string.sensor_data_speed_kmh);
    NEW_HEART_RATE_ACTION = context.getString(R.string.sensor_data_heart_rate);
    NEW_POWER_ACTION = context.getString(R.string.sensor_data_power);
    DATA_ID = context.getString(R.string.sensor_data_double_value);
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  protected void setUpChannel() {
    setSensorState(SensorState.CONNECTING);
    registerTurboReceiver();
    setSensorState(SensorState.CONNECTED);
  }

  @Override
  protected void tearDownChannel() {
    unregisterTurboReceiver();
    setSensorState(SensorState.DISCONNECTED);
  }

  @Override
  public synchronized SensorDataSet getSensorDataSet() {
    return sensorDataSet;
  }
  
  public synchronized void setSensorDataSet(SensorDataSet newSet) {
    this.sensorDataSet = newSet;
  }
  
  private final BroadcastReceiver receiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        setSensorState(SensorState.SENDING);
       String action = intent.getAction();
       double value = intent.getDoubleExtra(DATA_ID, -1);
       SensorData sd = SensorData.newBuilder()
           .setValue((int) value)
           .setState(Sensor.SensorState.SENDING)
           .build();
       if(action.equals(NEW_CADENCE_ACTION)){
         synchronized (TurboSensorManager.this) {
           Log.d(TAG,"cadence raw: " + value);
           SensorDataSet sds = getSensorDataSet();
           sds = sds.toBuilder()
               .setCadence(sd)
               .setCreationTime(System.currentTimeMillis())
               .build();
           setSensorDataSet(sds);
           Log.d(TAG,"sensorDataSet has cadence: " + sds.hasCadence());
           Log.d(TAG,"cadence in sensorDataSet: " + sds.getCadence());
         }
       }
       else if(action.equals(NEW_SPEED_ACTION)){
         //can't handle this atm
       }
       else if(action.equals(NEW_HEART_RATE_ACTION)){
         synchronized (TurboSensorManager.this) {
           SensorDataSet sds = getSensorDataSet();
           sds = sds.toBuilder()
               .setHeartRate(sd)
               .setCreationTime(System.currentTimeMillis())
               .build();
           setSensorDataSet(sds);
         }
       }
       else if(action.equals(NEW_POWER_ACTION)){
         synchronized (TurboSensorManager.this) {
           SensorDataSet sds = getSensorDataSet();
           sds = sds.toBuilder()
               .setPower(sd)
               .setCreationTime(System.currentTimeMillis())
               .build();
           setSensorDataSet(sds);
         }
       }
    }
  };



  public void registerTurboReceiver() {
    IntentFilter filter = new IntentFilter();
    filter.addAction(NEW_POWER_ACTION);
    filter.addAction(NEW_HEART_RATE_ACTION);
    filter.addAction(NEW_SPEED_ACTION);
    filter.addAction(NEW_CADENCE_ACTION);

    context.registerReceiver(receiver, filter);
  }

  public void unregisterTurboReceiver() {
    context.unregisterReceiver(receiver);
  }

}
