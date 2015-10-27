package com.thomasdh.arduinocar;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class BluetoothDevicePicker extends AppCompatActivity{

    public static final String EXTRA_BLUETOOTH_DEVICE = "bluetooth_device";

    class BluetoothDeviceAdapter extends RecyclerView.Adapter<BluetoothDeviceAdapter.BluetoothViewHolder> {
        public class BluetoothViewHolder extends RecyclerView.ViewHolder {

            final TextView view;
            BluetoothDevice bluetoothDevice;

            public BluetoothViewHolder(TextView itemView) {
                super(itemView);
                this.view = itemView;
            }

            public void setDevice(BluetoothDevice device){
                this.bluetoothDevice = device;
                view.setText(device.getName());
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent data = new Intent();
                        data.putExtra(EXTRA_BLUETOOTH_DEVICE, bluetoothDevice);
                        if (getParent() == null) {
                            setResult(Activity.RESULT_OK, data);
                        } else {
                            getParent().setResult(Activity.RESULT_OK, data);
                        }
                        finish();
                    }
                });
            }
        }

        final ArrayList<BluetoothDevice> devices;
        final LayoutInflater inflater;

        public BluetoothDeviceAdapter(Activity context) {
            super();
            devices = new ArrayList<>();
            inflater = LayoutInflater.from(context);
        }

        public void addDevice(BluetoothDevice device){
            devices.add(device);
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }

        @Override
        public BluetoothViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView view = (TextView) inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            return new BluetoothViewHolder(view);
        }

        @Override
        public void onBindViewHolder(BluetoothViewHolder holder, int position) {
            holder.setDevice(devices.get(position));
        }
    }

    private BluetoothDeviceAdapter adapter;
    private BluetoothAdapter bluetooth;
    private static final int REQUEST_ENABLE_BLUETOOTH = 11;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_bluetooth_device_list);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.view_bluetooth_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setHasFixedSize(true);
        adapter = new BluetoothDeviceAdapter(this);
        recyclerView.setAdapter(adapter);

        bluetooth = BluetoothAdapter.getDefaultAdapter();
        if (bluetooth == null) {
            Toast.makeText(this, "Your device doesn't support Bluetooth", Toast.LENGTH_LONG).show();
            finish();
        } else {
            if (!bluetooth.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
            } else {
                startDeviceSearching(bluetooth);
            }
        }
    }

    private BroadcastReceiver mReceiver;

    private void startDeviceSearching(final BluetoothAdapter bluetooth) {
        bluetooth.startDiscovery();

        mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    adapter.addDevice(device);
                }
            }
        };

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK)
                startDeviceSearching(bluetooth);
            else
                Toast.makeText(this, "You cancelled bluetooth enabling", Toast.LENGTH_LONG).show();
        } else
            super.onActivityResult(requestCode, resultCode, data);
    }
}
