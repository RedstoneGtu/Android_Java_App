package com.ozanselte.redstone;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;;

import android.os.StrictMode;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.harrysoft.androidbluetoothserial.BluetoothManager;
import com.harrysoft.androidbluetoothserial.BluetoothSerialDevice;
import com.harrysoft.androidbluetoothserial.SimpleBluetoothDeviceInterface;

import android.media.MediaPlayer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MainActivity
        extends AppCompatActivity
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, MediaPlayer.OnCompletionListener {

    Button btnConnect;
    ToggleButton toggle;
    RadioButton radioClassic, radioElectro, radioJazz, radioRock, radioPop, radioHiphop;
    TextView textView;
    BluetoothManager btManager;
    SimpleBluetoothDeviceInterface btInterface;
    MediaPlayer mediaPlayer;
    String data;
    boolean isConnected;
    boolean isSampling;
    String esp32_mac = "30:C6:F7:04:C7:22";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnConnect = findViewById(R.id.btnConnect);
        textView = findViewById(R.id.textView);
        toggle = findViewById(R.id.toggleButton);
        radioClassic = findViewById(R.id.rbClassic);
        radioElectro = findViewById(R.id.rbElectro);
        radioJazz = findViewById(R.id.rbJazz);
        radioRock = findViewById(R.id.rbRock);
        radioPop = findViewById(R.id.rbPop);
        radioHiphop = findViewById(R.id.rbHiphop);
        mediaPlayer = MediaPlayer.create(this, R.raw.test3);

        btManager = BluetoothManager.getInstance();
        if (null == btManager) {
            // Bluetooth unavailable on this device :( tell the user
            Toast.makeText(this, "Bluetooth not available.", Toast.LENGTH_LONG).show();
            finish();
        }

        btnConnect.setOnClickListener(this);
        toggle.setOnCheckedChangeListener(this);
        mediaPlayer.setOnCompletionListener(this);
        data = "";
        isSampling = true;
        isConnected = false;

        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT)) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    1
            );
        }
        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1
            );
        }
    }

    protected void onConnected(BluetoothSerialDevice connectedDevice) {
        btInterface = connectedDevice.toSimpleDeviceInterface();
        btInterface.setListeners(this::onMessageReceived, this::onMessageSent, this::onError);
        mediaPlayer.start();
    }

    private void onMessageSent(String message) {
        // EMPTY
    }

    private void onMessageReceived(String message) {
        data += message;
    }

    private void onError(Throwable error) {
        Toast.makeText(this, error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
    }

    @SuppressLint("CheckResult")
    @Override
    public void onClick(View view) {
        isConnected = !isConnected;
        if (isConnected) {
            btManager
                    .openSerialDevice(esp32_mac)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onConnected, this::onError);

            btnConnect.setText("DISCONNECT");
        } else {
            btManager.closeDevice(esp32_mac);
            btManager.close();
            mediaPlayer.stop();
            btnConnect.setText("CONNECT");
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isTest) {
        if (!isTest) { // Sampling
            radioClassic.setEnabled(true);
            radioElectro.setEnabled(true);
            radioJazz.setEnabled(true);
            radioRock.setEnabled(true);
            radioPop.setEnabled(true);
            radioHiphop.setEnabled(true);
        } else { // Test
            radioClassic.setEnabled(false);
            radioElectro.setEnabled(false);
            radioJazz.setEnabled(false);
            radioRock.setEnabled(false);
            radioPop.setEnabled(false);
            radioHiphop.setEnabled(false);
        }
        this.isSampling = !isTest;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        try {
            URL url = new URL("https://redstone-gtu.herokuapp.com/sendvalue");
            JSONObject json = new JSONObject();
            json.put("raw_string", data);
            if (isSampling) {
                List<Integer> result = new ArrayList<Integer>();
                result.add(radioClassic.isChecked() ? 1 : 0);
                result.add(radioElectro.isChecked() ? 1 : 0);
                result.add(radioJazz.isChecked() ? 1 : 0);
                result.add(radioRock.isChecked() ? 1 : 0);
                result.add(radioPop.isChecked() ? 1 : 0);
                result.add(radioHiphop.isChecked() ? 1 : 0);
                json.put("result", result);
            }
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(json.toString());
            wr.flush();
            wr.close();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String output;
            StringBuffer response = new StringBuffer();

            int i = 0;
            RadioButton rb = radioClassic;
            while ((output = in.readLine()) != null) {
                switch (i) {
                    case 0: rb = radioClassic; break;
                    case 1: rb = radioElectro; break;
                    case 2: rb = radioJazz; break;
                    case 3: rb = radioRock; break;
                    case 4: rb = radioPop; break;
                    case 5: rb = radioHiphop; break;
                }
                ++i;
                if (output.contains("%")) {
                    rb.setText(output);
                } else {
                    textView.setText(output);
                }
                response.append(output);
                response.append("\n");
            }
            in.close();
            btnConnect.setText("SENT");
            btnConnect.setEnabled(false);
        } catch (Exception e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

    }
}