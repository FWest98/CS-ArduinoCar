package com.thomasdh.arduinocar;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothDevice device;
    private OutputStream connection;
    private BluetoothSocket socket;

    private static final int REQUEST_BLUETOOTH_DEVICE = 12;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set listener
        View.OnTouchListener listener = new ControlTouchListener();
        Button up = (Button) findViewById(R.id.button_up);
        up.setOnTouchListener(listener);
        Button down = (Button) findViewById(R.id.button_down);
        down.setOnTouchListener(listener);
        Button left = (Button) findViewById(R.id.button_left);
        left.setOnTouchListener(listener);
        Button right = (Button) findViewById(R.id.button_right);
        right.setOnTouchListener(listener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (device == null)
            startDevicePicker();
        else if (connection == null)
            setupConnection();
    }

    private static final byte[] stop = hexStringToByteArray("00");
    private static final byte[] forward = hexStringToByteArray("0144");
    private static final byte[] left = hexStringToByteArray("0244");
    private static final byte[] backwards = hexStringToByteArray("0344");
    private static final byte[] right = hexStringToByteArray("0444");

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_BLUETOOTH_DEVICE) {
            if (resultCode == RESULT_OK) {
                device = data.getParcelableExtra(BluetoothDevicePicker.EXTRA_BLUETOOTH_DEVICE);
                setupConnection();
            } else
                startDevicePicker();
        } else
            super.onActivityResult(requestCode, resultCode, data);
    }

    private void startDevicePicker() {
        Intent i = new Intent(this, BluetoothDevicePicker.class);
        startActivityForResult(i, REQUEST_BLUETOOTH_DEVICE);
    }

    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private void setupConnection(){
        if (device == null)
            throw new RuntimeException("Called setupConnection while device was null");
        new AsyncTask<BluetoothDevice, Void, BluetoothSocket>(){
            @Override
            protected BluetoothSocket doInBackground(BluetoothDevice[] params) {
                try {
                    return params[0].createRfcommSocketToServiceRecord(uuid);
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(BluetoothSocket outputStream) {
                socket = outputStream;
                if (socket != null)
                    try {
                        connection = socket.getOutputStream();
                    } catch (Exception e) {
                        Log.e("MainActivity", "Could not get OutputStream", e);
                    }
            }
        }.execute(device);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connection != null)
            try {
                connection.close();
            } catch (Exception e) {
                Log.e("MainActivity", "Could not close", e);
            }
        connection = null;
        device = null;
    }

    private class ControlTouchListener implements Button.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (connection == null)
                return false;

            try {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    switch (v.getId()) {
                        case R.id.button_down:
                            connection.write(backwards);
                            connection.flush();
                            break;
                        case R.id.button_left:
                            connection.write(left);
                            connection.flush();
                            break;
                        case R.id.button_right:
                            connection.write(right);
                            connection.flush();
                            break;
                        case R.id.button_up:
                            connection.write(forward);
                            connection.flush();
                            break;
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    connection.write(stop);
                    connection.flush();
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Could not send", e);
            }
            return false;
        }
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];

        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }

        return data;
    }
}
