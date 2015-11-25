package dk.schaumburgit.fastbarcodescanner;
/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.media.Image;
import android.util.Log;
import android.view.TextureView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.Date;

import dk.schaumburgit.stillsequencecamera.IStillSequenceCamera;
import dk.schaumburgit.stillsequencecamera.StillSequenceCamera2;
import dk.schaumburgit.trackingbarcodescanner.TrackingBarcodeScanner;

public class FastBarcodeScanner implements IStillSequenceCamera.OnImageAvailableListener
{
    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "FastBarcodeScanner";

    private Activity mActivity;
    private Activity getActivity() {
        return mActivity;
    }

    private IStillSequenceCamera mImageSource;
    private TrackingBarcodeScanner mBarcodeFinder;
    public FastBarcodeScanner(Activity activity, TextureView textureView) {
        this.mActivity = activity;
        this.mBarcodeFinder = new TrackingBarcodeScanner();
        this.mImageSource = new StillSequenceCamera2(activity, textureView, mBarcodeFinder.GetPreferredFormats());
        this.mImageSource.setImageListener(this);
    }

    public void StartFocus()
    {
        mImageSource.StartFocus();
    }

    public void StartScan()
    {
        mImageSource.StartCapture();
    }

    public void StopScan()
    {
        mImageSource.StopCapture();
    }

    @Override
    public void onImageAvailable() {
        Image image = mImageSource.GetLatestImage();

        if (image == null)
            return;

        // Get the image data (we requested JPEG) into a byte buffer:
        Date first = new Date();
        byte[] bytes = null;
        int format = image.getFormat();
        int width = image.getWidth();
        int height = image.getHeight();
        try {
            Image.Plane plane = image.getPlanes()[0];
            ByteBuffer buffer = plane.getBuffer();
            bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
        } catch (Exception e) {
            Log.e(TAG, "Error extracting image", e);
            return;
        } finally {
            // Important: free the image as soon as possible,
            // thus making room for a new capture to begin:
            image.close();
        }

        // Decode the image:
        Date second = new Date();
        try {
            String newBarcode = mBarcodeFinder.find(format, width, height, bytes);
            Date third = new Date();

            // Tell the world:
            callBarcodeListener(newBarcode);
            Date fourth = new Date();
            Log.e(
                    TAG, "Timing;"
                            + new Timestamp(first.getTime())
                            + ";"
                            + new Timestamp(second.getTime())
                            + ";"
                            + new Timestamp(mBarcodeFinder.a.getTime())
                            + ";"
                            + new Timestamp(mBarcodeFinder.b.getTime())
                            + ";"
                            + new Timestamp(mBarcodeFinder.c.getTime())
                            + ";"
                            + new Timestamp(mBarcodeFinder.d.getTime())
                            + ";"
                            + new Timestamp(mBarcodeFinder.f.getTime())
                            + ";"
                            + new Timestamp(mBarcodeFinder.g.getTime())
                            + ";"
                            + new Timestamp(third.getTime())
                            + ";"
                            + new Timestamp(fourth.getTime())
                            + ";");
        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
        }
    }

    private int nImagesProcessed = 0;
    private void saveImage(byte[] bytes) {
        File dir = getActivity().getExternalFilesDir(null);
        nImagesProcessed++;
        File saveAs = new File(dir, "qr" + nImagesProcessed + ".jpg");

        FileOutputStream output = null;
        try {
            output = new FileOutputStream(saveAs);
            output.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //*********************************************************************
    //* Managing Barcode events:
    //* ========================
    //*
    //*********************************************************************

    private BarcodeDetectedListener mBarcodeListener = null;
    public void setBarcodeListener(BarcodeDetectedListener listener)
    {
        mBarcodeListener = listener;
    }

    private String mLastReportedBarcode = null;
    private int mNoBarcodeCount = 0;
    private void callBarcodeListener(String barcode)
    {
        //mBarcodeListener.onBarcodeAvailable(barcode);
        //Log.d(TAG, "Scanned " + barcode);
        //if ( 1 == 1)
        //    return;

        if (barcode == null)
        {
            mNoBarcodeCount++;
            if (mLastReportedBarcode != null && mNoBarcodeCount >= 5)
            {
                mLastReportedBarcode = null;
                mBarcodeListener.onBarcodeAvailable(mLastReportedBarcode);
            }
        }
        else
        {
            mNoBarcodeCount = 0;
            if (!barcode.equals(mLastReportedBarcode))
            {
                mLastReportedBarcode = barcode;
                if (mBarcodeListener != null)
                    mBarcodeListener.onBarcodeAvailable(mLastReportedBarcode);
            }
        }
    }

    //*********************************************************************
    //*********************************************************************

    /**
     * Callback interface for being notified that a barcode has been detected.
     *
     * <p>
     * The onBarcodeAvailable is called when a new barcode is detected - that is,
     * if the same barcode is detected in 20 consecutive frames, onBarcodeAvailable
     * is only called on the first.
     * </p>
     * <p>
     * When no barcodes have been detected in 3 consecutive frames, onBarcodeAvailable
     * with a null barcode parameter.
     * </p>
     */
    public interface BarcodeDetectedListener {
        /**
         * Callback that is called when a new image is available from ImageReader.
         *
         * @param barcode the barcode detected.
         */
        void onBarcodeAvailable(String barcode);
    }

}
