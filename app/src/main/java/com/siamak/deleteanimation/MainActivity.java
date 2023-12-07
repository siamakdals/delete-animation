package com.siamak.deleteanimation;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.constraintlayout.widget.ConstraintLayout;

public class MainActivity extends Activity {

    DeleteMessageEffect deleteMessageEffect;

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View originalView = findViewById(R.id.originalView);
        Button deleteBtn = findViewById(R.id.deleteBtn);
        MainActivity that = this;

        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MainActivity.this.deleteMessageEffect = new DeleteMessageEffect(that, originalView,
                        DeleteMessageEffect.DEVICE_PERFORMANCE_LOW);

            }
        });


    }
}