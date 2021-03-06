package com.example.grabacion;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1234;
    private static final  int SAMPLING_RATE_IN_HZ = 44100;
    private static final  int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final  int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;


    private static final  int BUFFER_SIZE_FACTOR = 2;

    private static final  int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            SAMPLING_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR;

    private final AtomicBoolean recordingInProgress = new AtomicBoolean(false);
    private AudioRecord audioRecord = null;
    private Thread recordingThread = null;
    private Button startButton, stopButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        }

        startButton = findViewById(R.id.btnplay);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording();
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
            }
        });

        stopButton = findViewById(R.id.btn_rec);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRecording();
    }
    private void startRecording(){

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLING_RATE_IN_HZ, CHANNEL_CONFIG,
                AUDIO_FORMAT, BUFFER_SIZE);



        audioRecord.startRecording();

        recordingInProgress.set(true);

        recordingThread = new Thread(new RecordingRunnable(), "Recording Thread");

        recordingThread.start();
    }
    private void stopRecording() {
        if(audioRecord == null){
            return;
        }

        recordingInProgress.set(false);
        audioRecord.stop();
        audioRecord.release();
        audioRecord = null;
        recordingThread = null;

    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startRecording();

                } else {
                    Log.d("TAG", "permission denied by user");
                }
                return;
            }
        }
    }
    private class RecordingRunnable implements Runnable{

        @Override
        public void run() {
            final File file = new File(Environment.getExternalStorageDirectory(), "recording.pcm");
            final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

            try(final FileOutputStream outStream = new FileOutputStream(file)){
                while(recordingInProgress.get()){
                    int result = audioRecord.read(buffer, BUFFER_SIZE);
                    if(result<0 ){
                        throw new RuntimeException("Lectura buffer de audio fallo: "+ getBufferReadFailureReason(result));
                    }
                    outStream.write(buffer.array(), 0 , BUFFER_SIZE);
                    buffer.clear();
                }
            } catch (IOException e){
                throw new RuntimeException("Writing of recorded audio failed", e);
            }
        }

        private String getBufferReadFailureReason(int errorCode){
            switch (errorCode){
                case AudioRecord.ERROR_INVALID_OPERATION:
                    return "ERROR_INVALID_OPERATION";
                case AudioRecord.ERROR:
                    return "ERROR";
                case AudioRecord.ERROR_BAD_VALUE:
                    return "ERROR_BAD_VALUE";
                case AudioRecord.ERROR_DEAD_OBJECT:
                    return "ERROR_DEAD_OBJECT";
                default:
                    return "Unknow ("+errorCode+")";
            }
        }
    }
}
