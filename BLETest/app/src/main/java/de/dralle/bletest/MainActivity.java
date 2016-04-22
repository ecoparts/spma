package de.dralle.bletest;

import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        addButtonListeners();
    }

    private void addButtonListeners() {
        Button btnBTCheck=(Button)findViewById(R.id.btnBTCheck);
        Button btnBLECheck=(Button)findViewById(R.id.btnBLECheck);

        btnBTCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                    Toast.makeText(getApplicationContext(), R.string.btSupported, Toast.LENGTH_SHORT).show();

                }else{
                    Toast.makeText(getApplicationContext(), R.string.btNotSupported, Toast.LENGTH_SHORT).show();
                }
            }
        });
        btnBLECheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                    Toast.makeText(getApplicationContext(), R.string.bleSupported, Toast.LENGTH_SHORT).show();

                }else{
                    Toast.makeText(getApplicationContext(), R.string.bleNotSupported, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
