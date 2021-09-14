package com.example.controlroomtemprature;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    TextView current_temp, current_hum, set_temp;
    ImageView fan, lamp;
    Button connect, begin;
    SwipeRefreshLayout layout;
    String humidity = "00", temp = "00", fan_status = "0", lamp_status = "0";
    ArrayList<Character> data = new ArrayList<>();
    String HUM = " %";
    String TEMP = " \u00B0C";
    Handler handler = new Handler();
    String max_temp;
    Boolean interrupt_state;


    //------------------
    String tag = "*******************************************************";
    String address = "20:16:06:06:06:19";
    String name;
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice bluetoothDevice;
    BluetoothSocket bluetoothSocket;
    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    OutputStream outputStream;
    InputStream inputStream;
    //-----------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        setup();
        reconnect();
    }

    private void set_data(String hum, String temp, String fan_s, String lamp_s) {
        current_hum.setText(hum + HUM);
        current_temp.setText(temp + TEMP);

        if (fan_s.equals("0")) {
            fan.setImageResource(R.drawable.fan_off);
        } else if (fan_s.equals("1")) {
            fan.setImageResource(R.drawable.fan_on);
        }

        if (lamp_s.equals("0")) {
            lamp.setImageResource(R.drawable.light_off);
        } else if (lamp_s.equals("1")) {
            lamp.setImageResource(R.drawable.light_on);
        }
    }

    private void reconnect() {
        layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (bluetoothSocket.isConnected()) {
                    try {
                        bluetoothSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                connect.setVisibility(View.VISIBLE);
                begin.setVisibility(View.GONE);
                layout.setRefreshing(false);
            }
        });
    }

    private void setup() {
        current_temp = findViewById(R.id.currentTemp_tv);
        current_hum = findViewById(R.id.currentHum_tv);
        set_temp = findViewById(R.id.maxTemp_tv);
        fan = findViewById(R.id.fan_iv);
        lamp = findViewById(R.id.lamp_iv);
        connect = findViewById(R.id.connect_btn);
        layout = findViewById(R.id.swipe_layout);
        begin = findViewById(R.id.begin_btn);

        //Bluetooth stuff
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
        name = bluetoothDevice.getName();
    }

    public void change(View view) {
        final AlertDialog.Builder enter_temp_Dialog = new AlertDialog.Builder(MainActivity.this);
        enter_temp_Dialog.setTitle("Enter the set Temperature...");
        final EditText temp = new EditText(MainActivity.this);
        temp.setInputType(InputType.TYPE_CLASS_NUMBER);
        enter_temp_Dialog.setView(temp);

        enter_temp_Dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String tempr = temp.getText().toString();
                if (tempr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Enter a Temperature ", Toast.LENGTH_SHORT).show();
                } else {
                    int temperature = Integer.parseInt(tempr);
                    sendTemp(temperature);
                }
            }
        });


        enter_temp_Dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        enter_temp_Dialog.show();
    }

    private void sendTemp(int temperature) {
        set_temp.setText("Max temperature = " + temperature);

        try {
            outputStream = bluetoothSocket.getOutputStream();
            outputStream.write(temperature);
            System.out.println(tag + "Begin sending: " + temperature);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void connect(View view) {
        try {
            bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();
            Toast.makeText(this, "Connection Succeeded...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (bluetoothSocket.isConnected()) {
            connect.setVisibility(View.GONE);
            begin.setVisibility(View.VISIBLE);

        } else {
            connect.setVisibility(View.VISIBLE);
            begin.setVisibility(View.GONE);
        }
    }

    public void get_data() {

        try {
            inputStream = bluetoothSocket.getInputStream();
            inputStream.skip(inputStream.available());

            for (int i = 0; i < 7; i++) {
                byte b = (byte) inputStream.read();
                data.add((char) b);
            }
            if ((char) data.get(0) == '0') {
                humidity = String.valueOf(data.get(1)) + String.valueOf(data.get(2));
                temp = String.valueOf(data.get(3)) + String.valueOf(data.get(4));
                fan_status = String.valueOf(data.get(5));
                lamp_status = String.valueOf(data.get(6));
                set_data(humidity, temp, fan_status, lamp_status);
            } else {
                inputStream.skip(inputStream.available());
            }

            for (int i = 0; i < data.size(); i++) {
                System.out.print(data.get(i));
            }
            System.out.println(tag);
            data.clear();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private Runnable repeat = new Runnable() {
        @Override
        public void run() {
            get_data();
            handler.postDelayed(repeat, 500);
        }
    };

    public void start_flow(View view) {
        repeat.run();
        begin.setVisibility(View.GONE);
        Toast.makeText(this, "Start receiving with 500-ms delay ", Toast.LENGTH_SHORT).show();
    }


    public void exit(View view) {
        onDestroy();
    }
}