/*
 * Copyright 2013 Keisuke Kobayashi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kobakei.ratethisapp.sample;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kobakei.ratethisapp.Callback;
import com.kobakei.ratethisapp.Config;
import com.kobakei.ratethisapp.RateThisApp;

/**
 * Sample application of RateThisApp
 */
public class MainActivity extends AppCompatActivity {

    public RateThisApp rateThisApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //The criteria needs to match the operator. Need to be 1 day AND 4 launches.
        rateThisApp = RateThisApp.getInstance(this);
        rateThisApp.getConfig().setCancelMode(Config.CANCEL_MODE_NONE);

        final Callback callback = new Callback() {
            @Override
            public void onYesClicked() {
                Toast.makeText(MainActivity.this, "Yes event", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNoClicked() {
                Toast.makeText(MainActivity.this, "No event", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLaterClicked() {
                Toast.makeText(MainActivity.this, "Later event", Toast.LENGTH_SHORT).show();
            }};

        rateThisApp.setCallback(callback);


        Button button1 = (Button) findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show rating dialog explicitly.
                rateThisApp.showRateDialogIfNeeded(true);
            }
        });

        Button button2 = (Button) findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show rating dialog explicitly.
                rateThisApp.showRateDialogIfNeeded(R.style.MyAlertDialogStyle2, true);
            }
        });

        Button button3 = (Button) findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rateThisApp.showRateDialogIfNeeded(false);
            }
        });
    }
}
