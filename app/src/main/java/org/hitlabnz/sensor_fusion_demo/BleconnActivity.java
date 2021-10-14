package org.hitlabnz.sensor_fusion_demo;

import android.content.Intent;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class BleconnActivity extends FragmentActivity {
    Button btnClick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bleconn);
        Log.d("shit", "fuck");
        btnClick = findViewById(R.id.btnJump);
        Log.d("shit", "fuck2");
        btnClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("shit", "fuck3");
                Intent intent = new Intent(
                        BleconnActivity.this,
                        SensorSelectionActivity.class
                );
//                startActivityForResult(intent, 0);
                startActivity(intent);
            }
        });
    }
}