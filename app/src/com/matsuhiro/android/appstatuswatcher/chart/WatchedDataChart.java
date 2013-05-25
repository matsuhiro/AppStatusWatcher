/**
 * Copyright (C) 2009 - 2013 SC 4ViewSoft SRL
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.matsuhiro.android.appstatuswatcher.chart;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint.Align;
//import android.util.Log;

/**
 * Average temperature demo chart.
 */
public class WatchedDataChart extends AbstractDemoChart {
//    private static final String TAG = WatchedDataChart.class.getSimpleName();
    // Time(msec),RAM(MB),CPU(%),Traffic(byte)
    public enum TYPE {
        RAM, CPU, TRAFFIC
    }
    private String mFilePath;
    private TYPE mType;
    public WatchedDataChart(String filePath, TYPE type) {
        mFilePath = filePath;
        mType = type;
    }

    /**
     * Returns the chart name.
     * 
     * @return the chart name
     */
    public String getName() {
        return "Average temperature";
    }

    /**
     * Returns the chart description.
     * 
     * @return the chart description
     */
    public String getDesc() {
        return "The average temperature in 4 Greek islands (line chart)";
    }

    private String getTypeString(TYPE type) {
        switch (type) {
            case RAM: return "RAM(MB)";
            case CPU: return "CPU(%)";
            case TRAFFIC: return "Traffic(byte)";
        }
        return "";
    }
    /**
     * Executes the chart demo.
     * 
     * @param context the context
     * @return the built intent
     */
    public Intent execute(Context context) {
        String[] titles = new String[] {
                getTypeString(mType)
        };
        int index = 0;
        switch (mType) {
            case RAM:
                index = 1;
                break;
            case CPU:
                index = 2;
                break;
            case TRAFFIC:
                index = 3;
                break;
        }

        List<Double> time = new ArrayList<Double>();
        List<Double> watched = new ArrayList<Double>();
        
        File file = new File(mFilePath);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] datas = line.split(",");
                time.add(Double.parseDouble(datas[0]));
                watched.add(Double.parseDouble(datas[index]));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
        }
        double xmax = 0;
        double ymax = 0;

        List<double[]> x = new ArrayList<double[]>();
        List<double[]> values = new ArrayList<double[]>();
        double[] timeX = new double[time.size()];
        double[] watchedValue = new double[watched.size()];
        for (int i = 0; i < time.size(); i++) {
            timeX[i] = time.get(i);
            watchedValue[i] = watched.get(i);
            if (timeX[i] > xmax) {
                xmax = timeX[i];
            }
            if (watchedValue[i] > ymax) {
                ymax = watchedValue[i];
            }
//            Log.v(TAG, "x : " + timeX[i] + ", y : " + watchedValue[i]);
        }
        xmax = xmax * 1.2;
        ymax = ymax * 1.2;
        double xmin = - xmax * 0.2;
        double ymin = - ymax * 0.2;
        x.add(timeX);
        values.add(watchedValue);

        int[] colors = new int[] {
                Color.YELLOW
        };
        PointStyle[] styles = new PointStyle[] {
                PointStyle.CIRCLE
        };
        XYMultipleSeriesRenderer renderer = buildRenderer(colors, styles);
        int length = renderer.getSeriesRendererCount();
        for (int i = 0; i < length; i++) {
            ((XYSeriesRenderer) renderer.getSeriesRendererAt(i)).setFillPoints(true);
        }
        setChartSettings(renderer, getTypeString(mType), "Time(msec)", getTypeString(mType), xmin, xmax, ymin, ymax,
                Color.LTGRAY, Color.LTGRAY);
        renderer.setXLabels(15);
        renderer.setYLabels(15);
        renderer.setShowGrid(true);
        renderer.setXLabelsAlign(Align.RIGHT);
        renderer.setYLabelsAlign(Align.RIGHT);
        renderer.setZoomButtonsVisible(true);
        renderer.setPanLimits(new double[] {
                xmin, xmax, ymin, ymax
        });
        renderer.setZoomLimits(new double[] {
                xmin, xmax, ymin, ymax
        });

        XYMultipleSeriesDataset dataset = buildDataset(titles, x, values);
        Intent intent = ChartFactory.getLineChartIntent(context, dataset, renderer,
                getTypeString(mType));
        return intent;
    }

}
