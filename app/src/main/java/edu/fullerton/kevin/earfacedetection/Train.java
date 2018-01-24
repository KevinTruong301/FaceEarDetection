package edu.fullerton.kevin.earfacedetection;

import android.content.Context;
import android.media.Image;
import android.os.Environment;
import android.os.Vibrator;
import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;

import static org.opencv.core.Core.flip;
import static org.opencv.core.Core.transpose;

/**
 * Created by Kevin on 11/22/2017.
 */

public class Train {

    private int numPic;
    private Mat gray;
    private Mat resizeGray;
    private String distance;
    private int numBody;
    private Semaphore sem = new Semaphore(1);
    private MatOfRect faces;
    private Context trainContext;
    private Image trainImage;
    private String TAG = "Training";
    private Vibrator v;

    public Train(){
        distance = "close";
        numBody = 0;
        numPic = 0;
    }

    public void setContext(Context context){
        trainContext = context;
        setVibration();
    }

    public void setVibration(){
        v = (Vibrator) trainContext.getSystemService(trainContext.VIBRATOR_SERVICE);
    }

    public void setImage(Image image){
        gray = new Mat();
        gray = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC1);

        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        gray.put(0, 0, bytes);

        transpose(gray, gray);
        flip(gray,gray, 0);
        flip(gray,gray, 1);

    }

    void changeDistance(String distanceChange){
        distance = distanceChange;
        numBody = 0;
        numPic = 0;

    }


    public void trainCV(){



        try{
            sem.acquire();

            faces = new MatOfRect();
            resizeGray = new Mat();

            if(numPic < 3) {

                //Imgproc.resize(gray, resizeGray, new Size(gray.rows()/2, gray.cols()/2));

                String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
                String haarPart;
                org.opencv.core.Size sizeMin = new org.opencv.core.Size(100,100);
                org.opencv.core.Size sizeMax = new org.opencv.core.Size(1000, 1000);
                haarPart = "nothing";

                if(distance.equals("close")){
                    if (numBody == 0) {
                        haarPart = "/haarcascade_frontalface_default.xml";
                        sizeMin = new Size(1400, 1400);
                        sizeMax = new Size(1600, 1600);
                    } else if (numBody == 2) {
                        haarPart = "/haarcascade_mcs_leftear.xml";
                        sizeMin = new Size(260, 450);
                        sizeMax = new Size(300, 490);
                    } else if (numBody == 1) {
                        haarPart = "/haarcascade_mcs_rightear.xml";
                        sizeMin = new Size(260, 450);
                        sizeMax = new Size(300, 490);
                    }
                }
                else if(distance.equals("far")){
                    if (numBody == 0) {
                        haarPart = "/haarcascade_frontalface_default.xml";
                        sizeMin = new Size(400, 400);
                        sizeMax = new Size(500, 500);
                    } else if (numBody == 2) {
                        haarPart = "/haarcascade_mcs_leftear.xml";
                        sizeMin = new Size(50, 120);
                        sizeMax = new Size(150, 250);
                    } else if (numBody == 1) {
                        haarPart = "/haarcascade_mcs_rightear.xml";
                        sizeMin = new Size(50, 120);
                        sizeMax = new Size(150, 250);
                    }
                }
                else if(distance.equals("medium")){
                    if (numBody == 0) {
                        haarPart = "/haarcascade_frontalface_default.xml";
                        sizeMin = new Size(700, 700);
                        sizeMax = new Size(800, 800);
                    } else if (numBody == 2) {
                        haarPart = "/haarcascade_mcs_leftear.xml";
                        sizeMin = new Size(60, 250);
                        sizeMax = new Size(260, 450);
                    } else if (numBody == 1) {
                        haarPart = "/haarcascade_mcs_rightear.xml";
                        sizeMin = new Size(60, 250);
                        sizeMax = new Size(260, 450);
                    }
                }

                Log.d(TAG, "Looking for "+ haarPart);
                String tempPath = getTempDirectoryPath();
                CascadeClassifier faceDetector = new CascadeClassifier(extStorageDirectory + haarPart);
                faceDetector.detectMultiScale(gray, faces, 1.05, 10, 2, sizeMin, sizeMax);
                File partFolder = new File(tempPath + "/" + distance);
                //Imgcodecs.imwrite(tempPath + "/" + distance + "/" + distance + "_" + numPic + ".jpg", gray);


                if (!partFolder.exists()) {
                    partFolder.mkdir();
                }

                Rect[] facesArray = faces.toArray();
                for (int i = 0; i < facesArray.length; i++) {
                    numBody++;
                    numPic++;

                    Log.d(TAG, haarPart + " detected");
                    //Toast.makeText(trainContext, "Detected", Toast.LENGTH_SHORT).show();
                    v.vibrate(500);
                    Mat grayPic = gray.submat(facesArray[i]);

                    partFolder = new File(tempPath + "/" + distance);

                    if (!partFolder.exists()) {
                        partFolder.mkdir();
                    }


                    Imgcodecs.imwrite(tempPath + "/" + distance + "/" + distance + "_" + numPic + ".jpg", grayPic);


                }

            }

        } catch (InterruptedException exc) {
            System.out.println(exc);
        }
        sem.release();


    }

    private String getTempDirectoryPath() {
        File cache = null;


        // SD Card Mounted
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            cache = trainContext.getExternalCacheDir();
        }
        else {

            cache = trainContext.getCacheDir();

        }

        // Create the cache directory if it doesn't exist
        cache.mkdirs();
        return cache.getAbsolutePath();
    }
}
