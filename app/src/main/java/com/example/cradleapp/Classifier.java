package com.example.cradleapp;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.TextView;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.ops.CastOp;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.common.ops.QuantizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class Classifier {
    final int N_CLASSES = 3;        // number of classes to classify e.g. Happy, Neutral, Distressed
    private final MainActivity mainActivity;
    private Interpreter interpreter;
    private final ImageProcessor imageProcessor;

    private final ImageView iv_capturedImage;
    private final TextView tv_happyInference, tv_neutralInference, tv_distressInference;

    final String ASSET_AI_MODEL_FILENAME = "model_tflite_quantized/model.tflite";

    public Classifier( MainActivity mainActivity ) {
        this.mainActivity = mainActivity;

        iv_capturedImage     = mainActivity.findViewById(R.id.iv_captured_image);
        tv_happyInference    = mainActivity.findViewById(R.id.tv_happy_inference);
        tv_neutralInference  = mainActivity.findViewById(R.id.tv_neutral_inference);
        tv_distressInference = mainActivity.findViewById(R.id.tv_distress_inference);

        try {
            interpreter = new Interpreter( loadModelFile() );
        } catch ( Exception e ) {
           mainActivity.abortAppLaunchDueToError("tensorflow-lite model is not found/loaded");
        }

        imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(127.5f, 127.5f))
                .add(new QuantizeOp(128.0f, 0.0078125f ))
                .add(new CastOp(DataType.UINT8))
                .build();
    }

    public int classify( Bitmap bitmap ) {

        // Process image before feeding into model
        TensorImage tensorImage = new TensorImage(DataType.UINT8);
        tensorImage.load( bitmap );
        tensorImage = imageProcessor.process(tensorImage);

        // To hold the output
        TensorBuffer probabilityBuffer = TensorBuffer.createFixedSize(new int[]{1, N_CLASSES}, DataType.UINT8);

        // Run model
        interpreter.run( tensorImage.getBuffer(), probabilityBuffer.getBuffer() );
        int[] results = probabilityBuffer.getIntArray();

        // Display results
        int sum = 0;
        int max_idx = 0;
        int max = results[0];
        for (int i = 0; i < results.length; i++) {
            sum += results[i];
            if ( results[i] > max ) {
                max = results[i];
                max_idx = i;
            }
        }
        iv_capturedImage.setImageBitmap(tensorImage.getBitmap());
        tv_happyInference.setText("Happy: " + String.format("%.2f", (float)results[0]/(float)sum));
        tv_neutralInference.setText("Neutral: " + String.format("%.2f", (float)results[1]/(float)sum));
        tv_distressInference.setText("Distress: " + String.format("%.2f", (float)results[2]/(float)sum));

        return max_idx;
    }

    public void releaseClassifier() {
        interpreter.close();
    }

    /** UTILITY: Memory-maps the model file stored in Assets; used for loading model */
    private MappedByteBuffer loadModelFile() throws IOException {
        // Open model using input stream and memory map it to load
        AssetFileDescriptor fileDescriptor = mainActivity.getAssets().openFd( ASSET_AI_MODEL_FILENAME );
        FileInputStream inputStream = new FileInputStream( fileDescriptor.getFileDescriptor() );
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map( FileChannel.MapMode.READ_ONLY, startOffset, declaredLength );
    }
}
