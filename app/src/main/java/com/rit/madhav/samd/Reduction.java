package com.rit.madhav.samd;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by maddy on 5/9/15.
 */
public class Reduction {
    private List<Integer> dev1Values = new ArrayList<>();
    private List<Integer> dev2Values = new ArrayList<>();

    private double mean;
    private float standardDeviation;

    public Reduction () {
        addValues();
        calculateMean();
    }

    public double getMean() {
        return mean;
    }

    private void addValues () {
        dev1Values.add(78);
        dev1Values.add(77);
        dev1Values.add(80);
        dev1Values.add(75);
        dev1Values.add(82);

        dev2Values.add(200);
        dev2Values.add(180);
        dev2Values.add(176);
        dev2Values.add(172);
        dev2Values.add(189);
    }

    public void calculateMean() {
        double sum = 0.0;
        for (Integer num : dev1Values) {
            sum += num;
        }

        for (Integer num2 : dev2Values) {
            sum += num2;
        }

        int count = dev1Values.size() + dev2Values.size();
        mean = sum / count;

    }

    public void calculateSD () {

    }

    public float getSD() {
        return standardDeviation;
    }

    public void mergeNewDataSet () {

    }

}
