package com.lasarobotics.tests.camera;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.features2d.FeatureDetector;
import org.opencv.highgui.Highgui;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import com.lasarobotics.vision.Cameras;
import com.lasarobotics.vision.Util;
import com.lasarobotics.vision.detection.Detection;
import com.lasarobotics.vision.detection.FeatureDetection;
import com.lasarobotics.vision.detection.Homography;
import com.lasarobotics.vision.detection.Features;
import com.lasarobotics.vision.Camera;

import java.io.File;

public class CameraTestActivity extends Activity implements CvCameraViewListener2 {

    private Mat mRgba; //RGBA image matrix
    private Mat mGray; //Grayscale image matrix
    private Mat mTarget; //Target image grayscale

    private float focalLength; //Camera lens focal length

    private CameraBridgeViewBase mOpenCvCameraView;

    private FeatureDetection.ObjectAnalysis analysis;

    static {
        System.loadLibrary("ftcvision");
        System.loadLibrary("opencv_java");
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    // OpenCV loaded successfully!
                    // Load native library AFTER OpenCV initialization


                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_cameratest);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.surfaceView);
        mOpenCvCameraView.setCvCameraViewListener(this);

        //CAMERA PROPERTIES TEST
        Camera cam = Cameras.getPrimaryCamera();
        assert cam != null;
        android.hardware.Camera.Parameters pam = cam.getCamera().getParameters();
        focalLength = pam.getFocalLength();
        cam.getCamera().release();

        //TEST JNI
        String s = Features.stringFromJNI();

        //GET TARGET IMAGE
        //Read the target image file
        String dir = Util.getDCIMDirectory();
        File file = new File(dir + "/Object-FTCLogo.png");

        if (!file.exists())
        {
            // print error and abort execution
            Log.e("CameraTester", "FAILED TO FIND IMAGE FILE!");
            System.exit(1);
        }
        mTarget = Highgui.imread(file.getAbsolutePath(), Highgui.IMREAD_GRAYSCALE);
        if (mTarget.empty())
        {
            // print error and abort execution
            Log.e("CameraTester", "FAILED TO LOAD IMAGE FILE!");
            System.exit(1);
        }

        //ANALYZE OBJECT
        FeatureDetection detection = new FeatureDetection(FeatureDetection.FeatureDetectorType.GFTT,
                                                          FeatureDetection.DescriptorExtractorType.BRIEF,
                                                          FeatureDetection.DescriptorMatcherType.BRUTEFORCE);
        analysis = detection.analyzeObject(mTarget);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            // Internal OpenCV library not found. Using OpenCV Manager for initialization
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_12, this, mLoaderCallback);
        } else {
            // OpenCV library found inside package. Using it!
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
    }

    int j = 0;

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        // input frame has RBGA format
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        if (j < 20) { j++; return mRgba; }

        FeatureDetection detection = new FeatureDetection(FeatureDetection.FeatureDetectorType.FAST,
                FeatureDetection.DescriptorExtractorType.BRIEF,
                FeatureDetection.DescriptorMatcherType.BRUTEFORCE_HAMMING);
        detection.locateObject(mGray, mTarget, analysis, mRgba);

        //Features.highlightFeatures(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr());

        return mRgba;
    }
}