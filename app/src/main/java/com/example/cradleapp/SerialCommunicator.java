package com.example.cradleapp;


import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ToggleButton;


public class SerialCommunicator {

    private final MainActivity mainActivity;
    private Arduino arduino;

    RadioButton rb_arduinoStatus;

    private final int ARDUINO_CH340_VID = 6790;           // my CH340 nano v3
    private final int ARDUINO_VID = 9025;                 // ordinary arduino (added by default)

    public SerialCommunicator( MainActivity mainActivity ) {
        this.mainActivity = mainActivity;

        rb_arduinoStatus = mainActivity.findViewById(R.id.rb_arduino_status);;

        arduino = new Arduino( mainActivity );
        arduino.addVendorId( ARDUINO_CH340_VID );
        arduino.setArduinoListener( new ArduinoListener() {
            @Override
            public void onArduinoAttached(UsbDevice device) {
                arduino.open(device);
            }
            @Override
            public void onArduinoDetached() {
                rb_arduinoStatus.setChecked(false);
                rb_arduinoStatus.setText("ARDUINO\nSTATE: OFF");
            }
            @Override
            public void onArduinoMessage(byte[] bytes) {
               mainActivity.cradleController.onReceivedArduinoMessage( bytes[0] );
               // Only single-byte signals are expected
            }
            @Override
            public void onArduinoOpened() {
                rb_arduinoStatus.setChecked(true);
                rb_arduinoStatus.setText("ARDUINO\nSTATE: ON");
            }
            @Override
            public void onUsbPermissionDenied() {
                mainActivity.showToast("Permission denied. Attempting again in 5 sec...");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        arduino.reopen();
                    }
                }, 5000);
            }
        } );
    }

    public void sendMessageToArduino( byte msg ) {
        arduino.send( new byte[]{ msg } );
    }

    public void releaseArduino() {
        rb_arduinoStatus.setChecked(false);
        rb_arduinoStatus.setText("ARDUINO\nSTATE: OFF");
        arduino.unsetArduinoListener();
        arduino.close();
    }
}

