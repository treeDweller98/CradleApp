package com.example.cradleapp;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    CameraHandler cameraHandler;
    Classifier classifier;
    SerialCommunicator serialCommunicator;
    LullabyPlayer lullabyPlayer;
    CradleController cradleController;

    private final int REQUEST_CODE_PERMISSIONS  = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if( allPermissionsGranted() ){
            initialiseAppModules();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        classifier.releaseClassifier();
        serialCommunicator.releaseArduino();
        lullabyPlayer.releaseMusicPlayer();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                initialiseAppModules();
            } else {
                abortAppLaunchDueToError("Permissions not granted by the user");
            }
        }
    }

    private boolean allPermissionsGranted(){
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    private void initialiseAppModules() {
        this.serialCommunicator = new SerialCommunicator(this);
        this.lullabyPlayer = new LullabyPlayer(this);
        this.classifier = new Classifier(this);
        this.cameraHandler = new CameraHandler(this);
        this.cradleController = new CradleController(this);
        showToast("Welcome");
    }

    public void abortAppLaunchDueToError( String errorMessage ) {
        showToast(errorMessage);
        finish();
    }


    // UI Utilities
    public void showToast(String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // write code to show toast
                Toast.makeText(getApplicationContext(), message,Toast.LENGTH_LONG).show();
            }
        });
    }
}