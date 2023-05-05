package com.example.cradleapp;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class CradleController {

    final int HAPPY_IDX = 0, NEUTRAL_IDX = 1, DISTRESS_IDX = 2;
    final byte INFERENCE_HAPPY = 'h';
    final byte INFERENCE_NEUTRAL = 'n';
    final byte INFERENCE_DISTRESS = 'd';
    final byte CLASSIFIER_MALFUNCTION = '-';
    final byte PLAY_HAPPY_MUSIC = '1';
    final byte PLAY_NEUTRAL_MUSIC = '2';
    final byte PLAY_DISTRESS_MUSIC = '3';
    final byte PAUSE_MUSIC = 'p';
    final byte RESUME_MUSIC = 'r';
    final byte CAMERA_ON = 'e';
    final byte CAMERA_OFF = 'd';
    final byte INFER = 'i';

    MainActivity mainActivity;
    Classifier classifier;
    LullabyPlayer lullabyPlayer;
    SerialCommunicator serialCommunicator;
    CameraHandler cameraHandler;

    private final TextView tv_communicationsLog;

    Button btn_sendHappy, btn_sendNeutral, btn_sendDistressed;

    public CradleController( MainActivity mainActivity ) {
        this.mainActivity = mainActivity;

        btn_sendHappy = mainActivity.findViewById(R.id.btn_send_happy);
        btn_sendNeutral = mainActivity.findViewById(R.id.btn_send_neutral);
        btn_sendDistressed = mainActivity.findViewById(R.id.btn_send_distresed);
        tv_communicationsLog = mainActivity.findViewById(R.id.tv_communications_log);
        tv_communicationsLog.setMovementMethod(new ScrollingMovementMethod());

        classifier = mainActivity.classifier;
        lullabyPlayer = mainActivity.lullabyPlayer;
        serialCommunicator = mainActivity.serialCommunicator;
        cameraHandler = mainActivity.cameraHandler;

        btn_sendHappy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendInferenceResultToArduino(HAPPY_IDX);
            }
        });
        btn_sendNeutral.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendInferenceResultToArduino(NEUTRAL_IDX);
            }
        });
        btn_sendDistressed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendInferenceResultToArduino(DISTRESS_IDX);
            }
        });
    }

    public void sendToClassify( Bitmap bitmap ) {
        /** Used by CameraHandler upon image capture */
        int inferredClass = classifier.classify(bitmap);
        sendInferenceResultToArduino(inferredClass);
    }

    private void sendInferenceResultToArduino( int inferredClass ) {
        byte msg = switch (inferredClass) {
            case HAPPY_IDX    -> { display("Inferred: Happy"); yield INFERENCE_HAPPY; }
            case NEUTRAL_IDX  -> { display("Inferred: Neutral"); yield INFERENCE_NEUTRAL; }
            case DISTRESS_IDX -> { display("Inferred: Distress"); yield INFERENCE_DISTRESS; }
            default           -> { display("Error! Class inferred: " + inferredClass); yield CLASSIFIER_MALFUNCTION; }
        };
        serialCommunicator.sendMessageToArduino(msg);
    }

    public void onReceivedArduinoMessage( byte arduinoMessage ) {
        /** Called by SerialCommunicator upon receiving message from arduino */

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                switch (arduinoMessage) {
                    case PLAY_HAPPY_MUSIC -> {
                        lullabyPlayer.btn_playHappyMusic.performClick();
                        display("Command: play happy lullaby");
                    }
                    case PLAY_NEUTRAL_MUSIC -> {
                        lullabyPlayer.btn_playNeutralMusic.performClick();
                        display("Command: play neutral lullaby");
                    }
                    case PLAY_DISTRESS_MUSIC -> {
                        lullabyPlayer.btn_playDistressedMusic.performClick();
                        display("Command: play distress lullaby");
                    }
                    case PAUSE_MUSIC -> {
                        lullabyPlayer.btn_play.performClick();
                        display("Command: pausing lullaby");
                    }
                    case RESUME_MUSIC -> {
                        lullabyPlayer.btn_play.performClick();
                        display("Command: resuming lullaby");
                    }
                    case CAMERA_ON -> {
                        cameraHandler.btn_cameraEnabler.performClick();
                        display("Command: starting camera preview");
                    }
                    case CAMERA_OFF -> {
                        cameraHandler.btn_cameraEnabler.performClick();
                        display("Command: stopping camera preview");
                    }
                    case INFER -> {
                        cameraHandler.btn_infer.performClick();
                        display("Command: trigger inference");
                    }
                    default -> display("Unknown command received: " + arduinoMessage);
                }
            }
        });

    }

    private void display(final String message){
        mainActivity.runOnUiThread(() -> tv_communicationsLog.append(message + '\n'));
    }
}
