package edu.fullerton.kevin.earfacedetection;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Point;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.Window;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
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
    private File mFile;
    private int mSensorOrientation;
    private static final int ACTIVITY_START_CAMERA_APP = 0;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    private int mState;
    private Size mPreviewSize;
    private AutoFitTextureView mTextureView;
    private String mCameraId;
    private ImageReader mImageReader;
    private Train train;
    private MenuItem currentDistance;


    private final ImageReader.OnImageAvailableListener mImageAvalibleListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {

                    if(OpenCVLoader.initDebug()){
                        try{
                            train.setContext(getApplicationContext());
                            Image image = reader.acquireLatestImage();
                            train.setImage(image);
                            train.trainCV();
                            if(image != null){
                                image.close();
                            }
                        }catch(Exception e){

                        }

                        //mBackgroundHandler.post(new ImageSaver(image, mFile));
                    }
                    else{
                        Toast.makeText(getApplicationContext(), "Open CV not loaded", Toast.LENGTH_SHORT);
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


    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }


    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.camera);



        mFile = new File(getExternalFilesDir(null), "pic.jpg");

        train = new Train();

        mTextureView = (AutoFitTextureView) findViewById(R.id.cameraView);
        openBackgroundThread();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.distance_menu, menu);
        currentDistance = menu.getItem(0);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.close:
                currentDistance.setTitle("Close");
                train.changeDistance("close");
                break;
            case R.id.medium:
                currentDistance.setTitle("Medium");
                train.changeDistance("medium");
                break;
            case R.id.far:
                currentDistance.setTitle("Far");
                train.changeDistance("far");
                break;
        }
        return super.onOptionsItemSelected(item);
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
                        Arrays.asList(map.getOutputSizes(ImageFormat.YUV_420_888)),
                        new CompareSizesByArea());

/*                Size largestImageSize = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new Comparator<Size>() {
                            @Override
                            public int compare(Size o1, Size o2) {
                                return Long.signum(o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight());
                            }
                        }
                );*/

                mImageReader =ImageReader.newInstance(largestImageSize.getWidth(),
                        largestImageSize.getHeight(),
                        ImageFormat.YUV_420_888,
                        4);

                mImageReader.setOnImageAvailableListener(mImageAvalibleListener, mBackgroundHandler);

                int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
                //noinspection ConstantConditions
                mSensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        //Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largestImageSize);

                int orientation = getResources().getConfiguration().orientation;

                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }

                mCameraId = cameraId;
            }
        } catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            //Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
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

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

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

                                //rotation here?

                                int rotation = getWindowManager().getDefaultDisplay().getRotation();
                                mPreviewCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

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

    private void captureStillImage(){
        try {
            CaptureRequest.Builder captureStillBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureStillBuilder.addTarget(mImageReader.getSurface());
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureStillBuilder.set(CaptureRequest.JPEG_ORIENTATION,
                    ORIENTATIONS.get(rotation));

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
    }
}
