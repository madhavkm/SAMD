package com.rit.madhav.samd;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class ResultFragment extends Fragment implements SensorEventListener {
    private static final int DURATION = 10;
    private int count;

    private SensorManager sensorManager;
    private Sensor lightSensor;
    private Float currentValue;

    private View view;
    public String resultString;
    TextView resultView;


    public ResultFragment() {
        // Required empty public constructor
        currentValue = 0.0f;
    }

    public float getMean () {
        if (count == DURATION) {
            currentValue = currentValue / DURATION;
            return currentValue;
        }

        return 0.0f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_result, container, false);
        resultView = (TextView) view.findViewById(R.id.resultValue);

        sensorManager = (SensorManager)  getActivity().getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        return view;
    }

    public void displayResult (String result) {
        resultString = result;
        resultView.setText(resultString);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //resultView.setText(resultString);
    }

    /**
     * Called when sensor values have changed.
     * <p>See {@link android.hardware.SensorManager SensorManager}
     * for details on possible sensor types.
     * <p>See also {@link android.hardware.SensorEvent SensorEvent}.
     * <p/>
     * <p><b>NOTE:</b> The application doesn't own the
     * {@link android.hardware.SensorEvent event}
     * object passed as a parameter and therefore cannot hold on to it.
     * The object may be part of an internal pool and may be reused by
     * the framework.
     *
     * @param event the {@link android.hardware.SensorEvent SensorEvent}.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if( event.sensor.getType() == Sensor.TYPE_LIGHT)
        {
            Log.d(WiFiDirectActivity.TAG,"reading sensor.."+event.values[0]);
            currentValue += event.values[0];
            count++;
            if (count == DURATION) {
                Log.d(WiFiDirectActivity.TAG,"done reading sensor.."+currentValue);
                sensorManager.unregisterListener(this);
                currentValue = currentValue / DURATION;
                ((ResultDisplayListener)getActivity()).display(currentValue);
            }
        }
    }

    /**
     * Called when the accuracy of the registered sensor has changed.
     * <p/>
     * <p>See the SENSOR_STATUS_* constants in
     * {@link android.hardware.SensorManager SensorManager} for details.
     *
     * @param sensor
     * @param accuracy The new accuracy of this sensor, one of
     *                 {@code SensorManager.SENSOR_STATUS_*}
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public interface ResultDisplayListener {
        public void display (float result);
    }
}
