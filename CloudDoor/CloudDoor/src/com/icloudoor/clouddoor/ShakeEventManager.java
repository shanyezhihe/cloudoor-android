package com.icloudoor.clouddoor;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class ShakeEventManager implements SensorEventListener {
	
	private SensorManager sManager;
    private Sensor s;

    private Context context;
    
    private static final int MOV_COUNTS = 2;
    private static final int MOV_THRESHOLD = 3;
    private static final float ALPHA = 0.8F;
    private static final int SHAKE_WINDOW_TIME_INTERVAL = 250; 

    private float gravity[] = new float[3];

    private int counter;
    private long firstMovTime;
    private ShakeListener listener;

    public ShakeEventManager(Context context) {
    	super();
    	this.context = context;
    }

    public void setListener(ShakeListener listener) {
        this.listener = listener;
    }

    public void init(Context ctx) {
        sManager = (SensorManager)  ctx.getSystemService(Context.SENSOR_SERVICE);
        s = sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        register();
    }

    public void register() {
        sManager.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
    	if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
    		float maxAcc = calcMaxAcceleration(sensorEvent);
    		if (maxAcc >= MOV_THRESHOLD) {
    			if (counter == 0) {
    				counter++;
    				firstMovTime = System.currentTimeMillis();
    			} else {
    				long now = System.currentTimeMillis();
    				if ((now - firstMovTime) < SHAKE_WINDOW_TIME_INTERVAL)
    					counter++;
    				else {
    					resetAllData();
    					counter++;
    					return;
    				}

    				if (counter >= MOV_COUNTS)
    					if (listener != null) {
    						listener.onShake();
    					}
                        
    			}
    		}
    	}

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    public void deregister()  {
        sManager.unregisterListener(this);
    }


    private float calcMaxAcceleration(SensorEvent event) {
        gravity[0] = calcGravityForce(event.values[0], 0);
        gravity[1] = calcGravityForce(event.values[1], 1);
        gravity[2] = calcGravityForce(event.values[2], 2);

        float accX = event.values[0] - gravity[0];
        float accY = event.values[1] - gravity[1];
        float accZ = event.values[2] - gravity[2];

        float max1 = Math.max(accX, accY);
        return Math.max(max1, accZ);
    }

    private float calcGravityForce(float currentVal, int index) {
        return  ALPHA * gravity[index] + (1 - ALPHA) * currentVal;
    }


    private void resetAllData() {
        counter = 0;
        firstMovTime = System.currentTimeMillis();
    }


    public static interface ShakeListener {
        public void onShake();
    }
}
