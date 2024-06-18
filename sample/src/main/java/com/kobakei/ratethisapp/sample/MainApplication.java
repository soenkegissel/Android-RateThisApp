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

        //RateThisApp.initialize(this, Market.SAMSUNG);

        //RateThisApp.initialize(this, new Config(3, 5), Market.GOOGLE);

        Config config = new Config(2, 3, Config.Operator.OR);
        RateThisApp.initialize(this, config, Market.valueOf(BuildConfig.MARKET));
        //Take a look at build.gradle to see which markets are available.
    }
}
