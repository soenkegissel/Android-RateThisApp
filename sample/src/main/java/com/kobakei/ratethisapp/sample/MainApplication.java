package com.kobakei.ratethisapp.sample;

import android.app.Application;

import com.kobakei.ratethisapp.Config;
import com.kobakei.ratethisapp.Market;
import com.kobakei.ratethisapp.RateThisApp;

/**
 * Created by Sönke Gissel on 12.09.2019.
 */
public class MainApplication extends Application {

    // Called when the application is starting, before any other application objects have been created.
    // Overriding this method is totally optional!
    @Override
    public void onCreate() {
        super.onCreate();

        Config config = new Config(2, 3, Config.Operator.OR);
        // Force FakeReviewManager for testing Google Play Review flow
        // The app must be downloaded from the Google Play Store (e.g., via an Internal Test Track).
        config.setUseFakeReviewManager(true);

        // Using the new automatic initialization based on BuildConfig.MARKET
        // or explicitly passing the market from this app's BuildConfig
        RateThisApp.initialize(this, config, Market.fromName(BuildConfig.MARKET));
    }
}
