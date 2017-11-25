package edu.fullerton.kevin.earfacedetection;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;


import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Kevin on 11/22/2017.
 */

public class Camera extends AppCompatActivity{

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static final int ACTIVITY_START_CAMERA_APP = 0;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    private int mState;
    private Size mPreviewSize;
    private TextureView mTextureView;
    private String mCameraId;
    private ImageReader mImageReader;
    private final ImageReader.OnImageAvailableListener mImageAvalibleListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = reader.acquireLatestImage();
                    trainCV(image);
                    if(image != null){
                        image.close();
                    }
                }
            };

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setupCamera(width, height);
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };
    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallback
            = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            //Toast.makeText(getApplicationContext(), "Camera Opened", Toast.LENGTH_SHORT).show();
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };

    private CaptureRequest mPreviewCaptureRequest;
    private CaptureRequest.Builder mPreviewCaptureRequestBuilder;
    private CameraCaptureSession mCameraCaptureSession;

    int num = 0;

    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result){
            switch(mState){
                case STATE_PREVIEW:
                    //Do nothing
                        //captureStillImage();
                    break;
            }
        }

        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);

            process(result);
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);

            Toast.makeText(getApplicationContext(), "Focus Locked Unsuccessful", Toast.LENGTH_SHORT).show();

        }
    };

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    public void trainCV(Image image){
        MatOfRect faces = new MatOfRect();
        org.opencv.core.Size sizeMin = new org.opencv.core.Size(100,100);
       /* try{
            sem.acquire();
            if(numPic < 3) {
                MatOfRect faces = new MatOfRect();
                String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
                String haarPart;
                org.opencv.core.Size sizeMin = new org.opencv.core.Size(100,100);
                org.opencv.core.Size sizeMax = new org.opencv.core.Size(1000, 1000);
                haarPart = "nothing";
                //using front camera switches the ears
                if(bodyPart.equals("close")){
                    if (numBody == 0) {
                        haarPart = "/haarcascade_frontalface_default.xml";
                        sizeMin = new org.opencv.core.Size(430, 430);
                        sizeMax = new org.opencv.core.Size(600, 600);
                    } else if (numBody == 2) {
                        haarPart = "/haarcascade_mcs_leftear.xml";
                        sizeMin = new org.opencv.core.Size(70, 120);
                        sizeMax = new org.opencv.core.Size(100, 160);
                    } else if (numBody == 1) {
                        haarPart = "/haarcascade_mcs_rightear.xml";
                        sizeMin = new org.opencv.core.Size(70, 120);
                        sizeMax = new org.opencv.core.Size(100, 160);
                    }
                }
                else if(bodyPart.equals("medium")){
                    if (numBody == 0) {
                        haarPart = "/haarcascade_frontalface_default.xml";
                        sizeMin = new org.opencv.core.Size(290, 290);
                        sizeMax = new org.opencv.core.Size(350, 350);
                    } else if (numBody == 2) {
                        haarPart = "/haarcascade_mcs_leftear.xml";
                        sizeMin = new org.opencv.core.Size(60, 110);
                        sizeMax = new org.opencv.core.Size(80, 130);
                    } else if (numBody == 1) {
                        haarPart = "/haarcascade_mcs_rightear.xml";
                        sizeMin = new org.opencv.core.Size(60, 110);
                        sizeMax = new org.opencv.core.Size(80, 130);
                    }
                }
                else if(bodyPart.equals("far")){
                    if (numBody == 0) {
                        haarPart = "/haarcascade_frontalface_default.xml";
                        sizeMin = new org.opencv.core.Size(190, 190);
                        sizeMax = new org.opencv.core.Size(250, 250);
                    } else if (numBody == 2) {
                        haarPart = "/haarcascade_mcs_leftear.xml";
                        sizeMin = new org.opencv.core.Size(35, 65);
                        sizeMax = new org.opencv.core.Size(55, 85);
                    } else if (numBody == 1) {
                        haarPart = "/haarcascade_mcs_rightear.xml";
                        sizeMin = new org.opencv.core.Size(35, 65);
                        sizeMax = new org.opencv.core.Size(55, 85);
                    }
                }

                //Log.d(TAG, "Looking for "+ haarPart);

                String tempPath = getTempDirectoryPath();
                CascadeClassifier faceDetector = new CascadeClassifier(extStorageDirectory + haarPart);

                faceDetector.detectMultiScale(gray, faces, 1.05, 5, 2, sizeMin, sizeMax);

                Rect[] facesArray = faces.toArray();
                for (int i = 0; i < facesArray.length; i++) {
                    numBody++;
                    numPic++;



                    Mat grayPic = gray.submat(facesArray[i]);

                    File partFolder = new File(tempPath + "/" + bodyPart);

                    if (!partFolder.exists()) {
                        partFolder.mkdir();
                    }


                    Imgcodecs.imwrite(tempPath + "/" + bodyPart + "/" + bodyPart + "_" + numPic + ".jpg", grayPic);

                    PluginResult result = new PluginResult(PluginResult.Status.OK, "{\"message\":\"pattern detected\", \"index\":" + numPic + "}");
                    result.setKeepCallback(true);
                    cb.sendPluginResult(result);

                    called_success_detection = true;
                    called_failed_detection = false;
                    detected_index = 1;

                }

            }

        } catch (InterruptedException exc) {
            System.out.println(exc);
        }
        sem.release();*/


    }


    class DownloadFeed extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... voids) {



            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }


    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera);




        mTextureView = (TextureView) findViewById(R.id.cameraView);
    }

    public void onResume(){
        super.onResume();

        openBackgroundThread();

        if(mTextureView.isAvailable()){
            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());

        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

    }

    public void onPause() {

        closeCamera();

        closeBackgroundThread();

        super.onPause();
    }


    private void setupCamera(int width, int height){
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try{
            for(String cameraId : cameraManager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);

                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK){
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                Size largestImageSize = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new Comparator<Size>() {
                            @Override
                            public int compare(Size o1, Size o2) {
                                return Long.signum(o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight());
                            }
                        }
                );

                mImageReader =ImageReader.newInstance(largestImageSize.getWidth(),
                        largestImageSize.getHeight(),
                        ImageFormat.YUV_420_888,
                        1);

                mImageReader.setOnImageAvailableListener(mImageAvalibleListener, mBackgroundHandler);

                mPreviewSize = getPreferredPreviewSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                mCameraId = cameraId;
            }
        } catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    private Size getPreferredPreviewSize(Size[] mapSizes, int width, int height){
        List<Size> collectorSizes = new ArrayList<>();

        for(Size option : mapSizes){
            if(width > height) {
                if(option.getWidth() > width &&
                        option.getHeight() > height){
                    collectorSizes.add(option);
                }
            } else {
                if (option.getWidth() > height &&
                        option.getHeight() > width){
                    collectorSizes.add(option);
                }
            }
            if(collectorSizes.size() > 0){
                return Collections.min(collectorSizes, new Comparator<Size>() {
                    @Override
                    public int compare(Size o1, Size o2) {
                        return Long.signum(o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight());
                    }
                });
            }
        }
        return mapSizes[0];
    }

    private  void openCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
        }catch(CameraAccessException e){
            e.printStackTrace();
        }
    }

    private void closeCamera(){
        if(mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        if(mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if(mImageReader != null){
            mImageReader.close();
            mImageReader = null;
        }
    }

    private void createCameraPreviewSession(){
        try{
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            mPreviewCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewCaptureRequestBuilder.addTarget(previewSurface);
            mPreviewCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            if(mCameraDevice == null){
                                return;
                            }
                            try{
                                mPreviewCaptureRequest = mPreviewCaptureRequestBuilder.build();
                                mCameraCaptureSession = session;
                                //this is what streams?
                                mCameraCaptureSession.setRepeatingRequest(
                                        mPreviewCaptureRequest,
                                        mSessionCaptureCallback,
                                        mBackgroundHandler
                                );
                            } catch(CameraAccessException e){
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Toast.makeText(
                                    getApplicationContext(),
                                    "create camera session failed", Toast.LENGTH_SHORT
                            ).show();
                        }
                    }, null);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    private void openBackgroundThread(){
        mBackgroundThread = new HandlerThread("Camera2 background thread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void closeBackgroundThread(){
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;

        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

/*    private void captureStillImage(){
        try {
            CaptureRequest.Builder captureStillBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureStillBuilder.addTarget(mImageReader.getSurface());
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            *//*captureStillBuilder.set(CaptureRequest.JPEG_ORIENTATION,
                    ORIENTATIONS.get(rotation));*//*

            CameraCaptureSession.CaptureCallback captureCallback =
                    new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                            super.onCaptureCompleted(session, request, result);
                            //Toast.makeText(getApplicationContext(), "ImageCaptured", Toast.LENGTH_SHORT).show();
                        }
                    };


            mCameraCaptureSession.capture(captureStillBuilder.build(), captureCallback, mBackgroundHandler);


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }*/
}
