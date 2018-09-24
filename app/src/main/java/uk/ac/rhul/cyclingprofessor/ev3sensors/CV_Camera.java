package uk.ac.rhul.cyclingprofessor.ev3sensors;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.List;

class CV_Camera implements CvCameraViewListener2 {
    private static final String TAG = "EV3Sensors::CV_Camera";
    private final MainActivity mActivity;

    private Mat mRgba;
    private ColorBlobDetector mDetector;
    private Scalar CONTOUR_COLOR;
    private Scalar MARKER_COLOR;
    private Scalar TEXT_COLOR;
    private String QRCode = null;

    public CV_Camera(MainActivity activity) {
        mActivity = activity; // just for sending messages to the EV3 through the server.
    }
    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        Scalar mBlobColorHsv = new Scalar(280 / 2, 0.65 * 255, 0.75 * 255, 255);
        mDetector.setHsvColor(mBlobColorHsv);
        CONTOUR_COLOR = new Scalar(255, 0, 0, 255);
        MARKER_COLOR = new Scalar(255,255,255);
        TEXT_COLOR = new Scalar(255,255,255,255);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }


    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        try {
            String nextQRCode = zxing();
            if (nextQRCode != null && !nextQRCode.equals(QRCode)) { // Found a new code - or the same one after not seeing it.
                QRCode = nextQRCode;
                mActivity.sendMessage("QR: " + QRCode);
            }
        } catch (ChecksumException | FormatException e) {
            e.printStackTrace();
        }

        mDetector.process(mRgba);
        List<MatOfPoint> contours = mDetector.getContours();
        Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);


        Point center = mDetector.getCenterOfMaxContour();
        if( center != null ) {
            Imgproc.drawMarker(mRgba, center, MARKER_COLOR);
            double direction = (center.x - mRgba.cols() / 2) / mRgba.cols(); // portrait orientation
        }
        return mRgba;
    }

    private String zxing() throws ChecksumException, FormatException {

        Bitmap bMap = Bitmap.createBitmap(mRgba.width(), mRgba.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRgba, bMap);
        int[] intArray = new int[bMap.getWidth() * bMap.getHeight()];
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());

        LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(), intArray);

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Reader reader = new QRCodeMultiReader();

        String sResult = null;

        try {
            Result result = reader.decode(bitmap);
            sResult = result.getText();
            Log.d(TAG, "Found something: " + sResult);

            Imgproc.putText(mRgba, sResult,
                    new org.opencv.core.Point(mRgba.cols() / 4, mRgba.rows() / 2),
                    Core.FONT_HERSHEY_COMPLEX, 1, TEXT_COLOR);
        } catch (NotFoundException e) {
            Log.d(TAG, "Code Not Found");
            e.printStackTrace();
        }
        return sResult;

    }
}
