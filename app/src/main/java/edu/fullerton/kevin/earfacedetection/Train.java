package edu.fullerton.kevin.earfacedetection;

import android.content.Context;
import android.media.Image;
import android.os.Environment;
import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
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
    private int numBody = 0;
    private Semaphore sem = new Semaphore(1);
    private MatOfRect faces;
    private Context trainContext;
    private Image trainImage;
    private String TAG = "Training";

    public Train(){

    }

    public void setContext(Context context){
        trainContext = context;
    }

    public void setImage(Image image){
        gray = new Mat();
        gray = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC1);

        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        gray.put(0, 0, bytes);

        transpose(gray, gray);
    }


    void rotateMat(Mat matImage, int rotFlag) {
        //1=ClockWise
        //2=CounterClockWise
        //3=180degree
        if(rotFlag == 1) {transpose(matImage, matImage); flip(matImage, matImage, 1);}
        else if(rotFlag == 2) {transpose(matImage, matImage);flip(matImage, matImage, 0);}
        else if(rotFlag == 3) {flip(matImage, matImage, -1);}
    }

    public void trainCV(){
        try{
            sem.acquire();

            faces = new MatOfRect();
            resizeGray = new Mat();

/*            Mat rotMat;
            Point center = new Point(yuv.cols() / 2, yuv.rows() / 2);
            rotMat = Imgproc.getRotationMatrix2D(center, 270, 1);
            Imgproc.warpAffine(yuv, gray, rotMat, yuv.size());
            //Imgcodecs.imwrite("E://out//lena-rotate.jpg", destination);*/




            if(numPic < 3) {

                Imgproc.resize(gray, resizeGray, new Size(gray.rows()/2, gray.cols()/2));

                String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
                String haarPart;
                org.opencv.core.Size sizeMin = new org.opencv.core.Size(100,100);
                org.opencv.core.Size sizeMax = new org.opencv.core.Size(1000, 1000);
                haarPart = "nothing";
                //using front camera switches the ears
                distance = "close";
                if(distance.equals("close")){
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
                else if(distance.equals("medium")){
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
                else if(distance.equals("far")){
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

                Log.d(TAG, "Looking for "+ haarPart);
                String tempPath = getTempDirectoryPath();
                CascadeClassifier faceDetector = new CascadeClassifier(extStorageDirectory + haarPart);
                faceDetector.detectMultiScale(resizeGray, faces, 1.05, 5, 2, sizeMin, sizeMax);
                File partFolder = new File(tempPath + "/" + distance);

                if (!partFolder.exists()) {
                    partFolder.mkdir();
                }


                //Imgcodecs.imwrite(tempPath + "/" + distance + "/" + distance + "_" + numPic + ".jpg", gray);
                Rect[] facesArray = faces.toArray();
                for (int i = 0; i < facesArray.length; i++) {
                    numBody++;
                    numPic++;

                    Log.d(TAG, haarPart + " detected");

                    Mat grayPic = resizeGray.submat(facesArray[i]);

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
