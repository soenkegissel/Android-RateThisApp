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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kobakei.ratethisapp.Config;
import com.kobakei.ratethisapp.RateThisApp;

/**
 * Sample application of RateThisApp
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Config config = new Config(0,0);
        config.setCancelMode(Config.CANCEL_MODE_NONE);


        RateThisApp rateThisApp = new RateThisApp(this, config);
        rateThisApp.setConfig(config);
        rateThisApp.showRateDialogIfNeeded();

        // Set callback (optional)
        rateThisApp.setCallback(new RateThisApp.Callback() {
            @Override
            public void onYesClicked() {
                Toast.makeText(MainActivity.this, "Yes event", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNoClicked() {
                Toast.makeText(MainActivity.this, "No event", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelClicked() {
                Toast.makeText(MainActivity.this, "Cancel event", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
