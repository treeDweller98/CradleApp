package com.example.cradleapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CameraHandler {
    MainActivity mainActivity;
    private Executor executor;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    PreviewView pv_cameraPreview;
    Button btn_cameraEnabler;
    Button btn_infer;

    Preview preview;
    Preview.SurfaceProvider surfaceProvider;
    private boolean isPreviewing;

    public CameraHandler( MainActivity mainActivity ) {

        this.mainActivity = mainActivity;
        executor = Executors.newSingleThreadExecutor();

        btn_cameraEnabler = mainActivity.findViewById(R.id.btn_camera_enabler);
        btn_infer         = mainActivity.findViewById(R.id.btn_infer); btn_infer.setEnabled(false);
        pv_cameraPreview  = mainActivity.findViewById(R.id.pv_camera_preview);
//        pv_cameraPreview.setScaleType( PreviewView.ScaleType.FIT_CENTER );

        isPreviewing = false;
        btn_cameraEnabler.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPreviewing) {
                    stopCameraPreview();
                } else {
                    startCameraPreview();
                }
            }
        });

        surfaceProvider = pv_cameraPreview.getSurfaceProvider();
        startCamera();
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(mainActivity);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(mainActivity) );
    }

    public void startCameraPreview() {
        if ( preview != null ) {
            if ( !isPreviewing ) {
                preview.setSurfaceProvider(surfaceProvider);
                btn_cameraEnabler.setText("Disable\nCamera");
                btn_infer.setEnabled(true);
                isPreviewing = true;
            }
        } else {
            mainActivity.showToast("Camera preview null");
        }
    }
    public void stopCameraPreview() {
        if ( preview != null ) {
            if (isPreviewing) {
                preview.setSurfaceProvider(null);
                btn_cameraEnabler.setText("Enable\nCamera");
                btn_infer.setEnabled(false);
                isPreviewing = false;
            }
        } else {
            mainActivity.showToast("Camera preview null");
        }
    }
    public void triggerInference() {
        if ( !isPreviewing ) {
            startCameraPreview();
        }
        btn_infer.performClick();
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageCapture.Builder builder = new ImageCapture.Builder();
        final ImageCapture imageCapture = builder
                .setTargetRotation(mainActivity.getWindowManager().getDefaultDisplay().getRotation())
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .build();

        Camera camera = cameraProvider.bindToLifecycle( mainActivity, cameraSelector, preview, imageCapture);

        btn_infer.setOnClickListener(v -> imageCapture.takePicture( executor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image ) {
                Bitmap imageBitmap = createProcessedBitmapForClassifier(image);
                image.close();

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.cradleController.sendToClassify( imageBitmap );
                    }  // Run in separate thread since classifier updates UI
                } );
            }
            @Override
            public void onError(@NonNull ImageCaptureException error) {
                mainActivity.showToast("Image Capture Error " + error.getImageCaptureError());
            }
        }));
    }

    private Bitmap createProcessedBitmapForClassifier(@NonNull ImageProxy image) {
        // Convert
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length,null);

        // Crop center of frame, rotate, convert to grayscale
        int height  = bitmap.getHeight();
        int width   = bitmap.getWidth();

        Matrix matrix = new Matrix();
        matrix.postRotate( image.getImageInfo().getRotationDegrees() );

        if ( width >= height){
            bitmap = Bitmap.createBitmap( bitmap,width/2 - height/2,0, height, height, matrix, true );
        } else {
            bitmap = Bitmap.createBitmap( bitmap, 0, height/2 - width/2, width, width, matrix, true );
        }
        return toGrayscale(bitmap);
    }

    private Bitmap toGrayscale(Bitmap bmpOriginal)    {

        int height = bmpOriginal.getHeight();
        int width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }
}
