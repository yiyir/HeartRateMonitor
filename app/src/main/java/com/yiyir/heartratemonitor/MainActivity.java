package com.yiyir.heartratemonitor;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Rect;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "HeartRateMonitor";
    private final double SMOOTHING = 3.5;
    private int Counter = 0;
    private int counter = 1;
    private double meanRed = 0.0;
    private double prev = 0.0;
    private double diff = 0.0;
    private boolean climbUp = false;
    private int heartBeat = 0;
    private long startTime = 0L;
    private long peakStartTime = 0L;
    private double peakWidth = 0;
    private double peakStartValue = 0.0;
    private double peakHeight = 0.0;
    private Tutorial3View mOpenCvCameraView;
    private TextView hr;
    private GraphView graph1;
    private GraphView graph2;
    private LineGraphSeries<DataPoint> series1 = new LineGraphSeries<>();
    private LineGraphSeries<DataPoint> series2 = new LineGraphSeries<>();


    //    static{
//        if(!OpenCVLoader.initDebug()){
//            Log.d(TAG, "OpenCV not loaded");
//        }else{
//            Log.d(TAG, "OpenCV loaded");
//        }
//    }
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override       //callback method: called after OpenCV library initialization
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView(); //enable the camera connection

                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (Tutorial3View) findViewById(R.id.camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        hr = (TextView) findViewById(R.id.heart_rate);
        graph1 = (GraphView) findViewById(R.id.graph1);
        graph1.getViewport().setYAxisBoundsManual(true);
        graph1.getViewport().setXAxisBoundsManual(true);
        graph1.getViewport().setMinX(4);
        graph1.getViewport().setMaxX(80);
        graph1.getViewport().setMinY(220);
        graph1.getViewport().setMaxY(240);
        graph1.addSeries(series1);
        graph2 = (GraphView) findViewById(R.id.graph2);
        graph2.getViewport().setYAxisBoundsManual(true);
        graph2.getViewport().setXAxisBoundsManual(true);
        graph2.getViewport().setMinX(4);
        graph2.getViewport().setMaxX(80);
        graph2.getViewport().setMinY(220);
        graph2.getViewport().setMaxY(240);
        graph2.addSeries(series2);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_10, this, mLoaderCallback);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mOpenCvCameraView.setFlashOn();

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat currentFrame = inputFrame.rgba();
        // take out a subset of the currentFrame as an ROI
        Rect subset = new Rect(125, 93, 100, 100);
        Mat roi = new Mat(currentFrame, subset);
        // calculate the mean and standard deviation of red channel
        final MatOfDouble mean = new MatOfDouble();
        final MatOfDouble std = new MatOfDouble();
        Core.meanStdDev(roi, mean, std);
        //start to monitor heart beat only under the right conditions
        if (std.toArray()[0] < 5.0 && mean.toArray()[0] > 200.0) {
            Log.d(TAG, "entered");
            // leave out some time for initialization and stabilizing
            if (counter < 150) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hr.setText("Initializing...");
                    }
                });
                startTime = System.nanoTime(); // set the start time to be the current moment
                meanRed = mean.toArray()[0];
                prev = meanRed;
                climbUp = false;
                heartBeat = 0;
                diff = 0.0;
            } else {
                Log.d(TAG, "entered else statement");
                // smooth the data using low-pass filter
                meanRed += (mean.toArray()[0] - meanRed) / SMOOTHING;
                // calculate the difference(derivative) at the current point of time
                diff = meanRed - prev;
                // peak detection
                if (diff > 0) {
                    if (!climbUp) {
                        if (peakStartTime != 0L) {
                            peakWidth = ((System.nanoTime() - peakStartTime) * Math.pow(10.0, -9.0));
                        }
                        peakStartTime = System.nanoTime();
                        peakStartValue = meanRed;
                    }
                    climbUp = true;
                } else {
                    if (climbUp) {
                        peakHeight = prev - peakStartValue;
                        Log.d(TAG, "height: " + peakHeight + "    width: " + peakWidth);
                        if (peakWidth > 0.3) heartBeat++;
                        climbUp = false;
                    }
                }
                prev = meanRed;
                Log.d(TAG, "heart beat: " + String.valueOf(heartBeat));
                // time lapse in nanoseconds
                final long timeLapse1 = System.nanoTime() - startTime;
                // time lapse in seconds
                final int timeLapse2 = (int) (timeLapse1 * (Math.pow(10.0, -9.0)));
                int heartRate = 0;
                Log.d(TAG, "time lapse in seconds: " + String.valueOf(timeLapse2));
                if (timeLapse2 != 0) {
                    heartRate = (heartBeat * 60) / timeLapse2;
                }
                final String result = String.valueOf(heartRate) + " bpm";
                Log.d(TAG, "heartRate: " + result);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (timeLapse2 > 10) {
                            hr.setText(result);
                        } else {
                            hr.setText("Measuring...");
                        }
                    }
                });
            }
            // "small" counter to keep track of the total number of peaks detected
            counter++;
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hr.setText("Wrong conditions!");
                }
            });
            counter = 1;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                series1.appendData(new DataPoint(Counter, mean.toArray()[0]), true, 1000);
                series2.appendData(new DataPoint(Counter, meanRed), true, 1000);
            }
        });
        // "Big" counter to keep track of the overall number of samples for plotting
        Counter++;
        return currentFrame;
    }


}
