/*
 * Copyright 2013-2017 Keisuke Kobayashi
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
package com.kobakei.ratethisapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.review.testing.FakeReviewManager;
import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.OnFailureListener;
import com.google.android.play.core.tasks.Task;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * RateThisApp<br>
 * A library to show the app rate dialog
 * @author Keisuke Kobayashi (k.kobayashi.122@gmail.com)
 *
 */
public class RateThisApp implements Callback {

    private static final String TAG = RateThisApp.class.getSimpleName();

    public static final String MARKET_AMAZON_URL = "amzn://apps/android?p=";
    public static final String WEB_AMAZON_URL = "http://www.amazon.com/gp/mas/dl/android?p=";

    public static final String MARKET_HUAWEI_URL = "appmarket://details?id=";

    public static final String MARKET_GOOGLE_URL = "market://details?id=";
    public static final String WEB_GOOGLE_URL = "http://play.google.com/store/apps/details?id=";

    public static final String MARKET_SAMSUNG_URL = "samsungapps://ProductDetail/";
    public static final String WEB_SAMSUNG_URL = "http://www.samsungapps.com/appquery/appDetail.as?appId=";


    private static final String PREF_NAME = "RateThisApp";
    private static final String KEY_INSTALL_DATE = "rta_install_date";
    private static final String KEY_LAUNCH_TIMES = "rta_launch_times";
    private static final String KEY_OPT_OUT = "rta_opt_out";
    private static final String KEY_ASK_LATER_DATE = "rta_ask_later_date";

    private Date mInstallDate;
    private Date mAskLaterDate;
    private int mLaunchTimes;
    private boolean mOptOut;

    private final Config sConfig;
    private Callback sCallback;

    private FragmentActivity mFragmentActivity;
    private final Context mContext;

    private final Market mMarket;

    private final ReviewManager mReviewManager;
    private ReviewInfo mReviewInfo;

    //https://de.wikibooks.org/wiki/Muster:_Java:_Singleton
    @SuppressLint("StaticFieldLeak")
    private static volatile RateThisApp INSTANCE;

    private RateThisApp(Context context, Config config, Market market) {
        this.mContext = context;
        this.sConfig = config;
        this.mMarket = market;

        this.mReviewManager = BuildConfig.DEBUG ? new FakeReviewManager(mContext) : ReviewManagerFactory.create(mContext);

        setup();
    }

    public static RateThisApp initialize(Context context, Config config, Market market) {
        Context applicationContext;
        if (context.getApplicationContext() == null) {
            applicationContext = context;
        } else {
            applicationContext = context.getApplicationContext();
        }
        return INSTANCE = new RateThisApp(applicationContext, config, market);
    }

    public static RateThisApp initialize(Context context, Market market) {
        Log.w(TAG, "RateThisApp initialized without a custom configuration. Will use default values.");
        return initialize(context, new Config(7, 10, Config.Operator.OR), market);
    }

    public static RateThisApp getInstance(FragmentActivity fragmentActivity) {
        if(INSTANCE == null) {
            throw new NullPointerException("RateThisApp not initialized. Call RateThisApp.initalize(Context, Config) from your application class.");
        }
        INSTANCE.mFragmentActivity = fragmentActivity;

        return RateThisApp.INSTANCE;
    }



    /**
     * Set callback INSTANCE.
     * The callback will receive yes/no/later events.
     * @param callback
     */
    public void setCallback(Callback callback) {
        sCallback = callback;
    }

    /**
     * Call this API when the launcher activity is launched.<br>
     * It is best to call this API in onCreate() of the launcher activity.
     */
    private void setup() {
        SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Editor editor = pref.edit();
        // If it is the first launch, save the date in shared preference.
        if (pref.getLong(KEY_INSTALL_DATE, 0) == 0L) {
            storeInstallDate(editor);
        }
        // Increment launch times
        int launchTimes = pref.getInt(KEY_LAUNCH_TIMES, 0);
        launchTimes++;
        editor.putInt(KEY_LAUNCH_TIMES, launchTimes);

        editor.apply();

        setObjects(pref);

        if(BuildConfig.DEBUG)
            printStatus();
    }

    private void setObjects(SharedPreferences pref) {
        mInstallDate = new Date(pref.getLong(KEY_INSTALL_DATE, 0));
        mLaunchTimes = pref.getInt(KEY_LAUNCH_TIMES, 0);
        mOptOut = pref.getBoolean(KEY_OPT_OUT, false);
        mAskLaterDate = new Date(pref.getLong(KEY_ASK_LATER_DATE, 0));
    }

    /**
     * Show the rate dialog if the criteria is satisfied.
     * @return true if shown, false otherwise.
     */
    public boolean showRateDialogIfNeeded(boolean forceDialog) {
        return showRateDialogIfNeeded(0, forceDialog);
    }

    /**
     * Show the rate dialog if the criteria is satisfied.
     * @param themeId Theme ID
     * @return true if shown, false otherwise.
     */
    public boolean showRateDialogIfNeeded(int themeId, boolean forceDialog) {
        if(forceDialog) {
            showRateDialog(mFragmentActivity, themeId);
            return true;
        } else if (shouldShowRateDialog(sConfig.getmOperator())) {
            showRateDialog(mFragmentActivity, themeId);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check whether the rate dialog should be shown or not.
     * Developers may call this method directly if they want to show their own view instead of
     * dialog provided by this library.
     * @return
     * @param operator
     * GISSEL Add an Operator enum ADD/OR for launchTimes and launchDate.
     */
    private boolean shouldShowRateDialog(Config.Operator operator) {
        if (mOptOut) {
            return false;
        } else {
            boolean launchTimes = getLaunchCount() >= sConfig.getmCriteriaLaunchTimes();
            long threshold = TimeUnit.DAYS.toMillis(sConfig.getmCriteriaInstallDays());   // msec
            boolean launchDate =
                    new Date().getTime() - mInstallDate.getTime() >= threshold &&
                            new Date().getTime() - mAskLaterDate.getTime() >= threshold;
            log("launchTimes: "+launchTimes+". launchDate: "+launchDate);

            if (operator.equals(Config.Operator.OR)) {
                return launchTimes || launchDate;
            } else
                return launchTimes && launchDate;
        }
    }

    /**
     * Show the rate dialog
     * @param activity
     * @param themeId
     */
    private void showRateDialog(final FragmentActivity activity, int themeId) {
        DialogFragment newFragment = DialogFragment.newInstance(
                sConfig, themeId);
        newFragment.setCallback(this);
        newFragment.show(activity.getSupportFragmentManager(), "dialog");
    }

    /**
     * Stop showing the rate dialog
     */
    public void stopRateDialog(){
        setOptOut(true);
    }

    /**
     * Get count number of the rate dialog launches
     * @return Time the app is launched
     */
    public int getLaunchCount(){
        return mLaunchTimes;
    }
    /**
     * Get the number of times the app is launched more than needed<br />
     * to show the rating dialog.
     * @return Number of times launched more than needed to show dialog.
     */
    public int getLaunchCountOverexeedsBy() {
        return getLaunchCount() - sConfig.getmCriteriaLaunchTimes();
    }

    /**
     * Get the number of days the app is launched more than needed<br />
     * to show the rating dialog.
     * @return Number of days more than needed to show dialog.
     */
    public int getLaunchDaysOverexeedsBy(){
        return Integer.parseInt(String.valueOf(TimeUnit.MILLISECONDS.toDays(new Date().getTime() - mInstallDate.getTime())-sConfig.getmCriteriaInstallDays()));
    }

    /**
     * Did user opted out on the dialog.
     * @return true if optOut, false if not optOut.
     */
    public boolean getOptOut() {
        return mOptOut;
    }

    /**
     * Was the dialog shown to the user.
     * @return true if shown, false if not shown.
     */
    public boolean getDialogShown() {
        return getOptOut();
    }

    public Config getConfig() {
        return sConfig;
    }

    /**
     * Clear data in shared preferences.<br>
     * This API is called when the "Later" is pressed or canceled.
     */
    private void clearSharedPreferences() {
        SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Editor editor = pref.edit();
        editor.remove(KEY_INSTALL_DATE);
        editor.remove(KEY_LAUNCH_TIMES);

        if(editor.commit())
            setObjects(pref);
    }

    /**
     * Set opt out flag.
     * If it is true, the rate dialog will never shown unless app data is cleared.
     * This method is called when Yes or No is pressed.
     * @param optOut
     */
    private void setOptOut(boolean optOut) {
        SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Editor editor = pref.edit();
        editor.putBoolean(KEY_OPT_OUT, optOut);
        editor.apply();
        mOptOut = optOut;
    }

    /**
     * Store install date.
     * Install date is retrieved from package manager if possible.
     * @param editor
     */
    private void storeInstallDate(SharedPreferences.Editor editor) {
        Date installDate = new Date();
        PackageManager packMan = mContext.getPackageManager();
        try {
            PackageInfo pkgInfo = packMan.getPackageInfo(mContext.getPackageName(), 0);
            installDate = new Date(pkgInfo.firstInstallTime);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        editor.putLong(KEY_INSTALL_DATE, installDate.getTime());
    }

    /**
     * Store the date the user asked for being asked again later.
     */
    private void storeAskLaterDate() {
        SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Editor editor = pref.edit();
        editor.putLong(KEY_ASK_LATER_DATE, System.currentTimeMillis());
        editor.apply();
    }

    /**
     * Print values in SharedPreferences (used for debug)
     */
    private void printStatus() {
        SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        log("*** RateThisApp Status ***");
        log("*** Install Date: " + new Date(pref.getLong(KEY_INSTALL_DATE, 0))+". Needed: "+sConfig.getmCriteriaInstallDays());
        log("*** Ask Later Date: " + new Date(pref.getLong(KEY_ASK_LATER_DATE, 0)));
        log("*** Launch Times: " + pref.getInt(KEY_LAUNCH_TIMES, 0)+". Needed: "+sConfig.getmCriteriaLaunchTimes());
        log("*** Install Date & Launch Times Operator: "+sConfig.getmOperator());
        log("*** Opt out: " + getOptOut());
        log("*** Criteria match? "+shouldShowRateDialog(sConfig.getmOperator()));
        log("*** getLaunchCountOverexeedsBy "+ getLaunchCountOverexeedsBy());
        log("*** getLaunchDaysOverexeedsBy "+ getLaunchDaysOverexeedsBy());
    }

    /**
     * Print log if enabled
     * @param message
     */
    private void log(String message) {
        if (BuildConfig.DEBUG) {
            Log.v(TAG, message);
        }
    }

    @Override
    public void onYesClicked() {
        String appPackage = mFragmentActivity.getPackageName();
        String url = getMarketURL(mMarket, appPackage);
        if (!TextUtils.isEmpty(sConfig.getmUrl())) {
            url = sConfig.getmUrl();
        }

        if(mMarket.equals(Market.GOOGLE)) {
            ReviewManager manager = ReviewManagerFactory.create(mContext);
            Task<ReviewInfo> request = manager.requestReviewFlow();
            String finalUrl = url;
            request.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // We can get the ReviewInfo object
                    mReviewInfo = task.getResult();
                    Log.d("ReviewManager", "onYesClicked. ReviewInfo: " + mReviewInfo);
                    Task<Void> flow = mReviewManager.launchReviewFlow(mFragmentActivity, mReviewInfo);

                    flow.addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            // The flow has finished. The API does not indicate whether the user
                            // reviewed or not, or even whether the review dialog was shown. Thus, no
                            // matter the result, we continue our app flow.
                            Log.d("ReviewManager", "Flow Completed.");
                        }
                    });

                    flow.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            Log.e("ReviewManager", "Flow failed. Exception: " + e.getMessage());
                        }
                    });


                    if (sCallback != null) {
                        sCallback.onYesClicked();
                    }

                } else {
                    // There was some problem, log or handle the error code.
                    launchIntent(finalUrl, appPackage);
                }
            });
        }
        else {
            launchIntent(url, appPackage);
            if (sCallback != null) {
                sCallback.onYesClicked();
            }
        }
        setOptOut(true);
    }

    private void launchIntent(String url, String appPackage) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mFragmentActivity.startActivity(intent);

        } catch (android.content.ActivityNotFoundException anfe) {
            url = getWebURL(mMarket, appPackage);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mFragmentActivity.startActivity(intent);
        }
    }

    private String getWebURL(Market mMarket, String appPackage) {
        String URL = "";
        switch (mMarket) {
            case AMAZON:
                URL = WEB_AMAZON_URL.concat(appPackage);
                break;
            case GOOGLE:
                URL = WEB_GOOGLE_URL.concat(appPackage);
                break;
            case HUAWEI:
                throw new UnableToFindMarketException("Huawei does not support web rating by packagename");
            case SAMSUNG:
                URL = WEB_SAMSUNG_URL.concat(appPackage);
                break;
        }
        return URL;
    }

    private String getMarketURL(Market mMarket, String appPackage) {
        String URL = "";
        switch (mMarket) {
            case AMAZON:
                URL = MARKET_AMAZON_URL.concat(appPackage);
                break;
            case GOOGLE:
                URL = MARKET_GOOGLE_URL.concat(appPackage);
                break;
            case HUAWEI:
                URL = MARKET_HUAWEI_URL.concat(appPackage);
                break;
            case SAMSUNG:
                URL = MARKET_SAMSUNG_URL.concat(appPackage);
                break;
        }
        return URL;
    }

    @Override
    public void onNoClicked() {
        if (sCallback != null) {
            sCallback.onNoClicked();
        }
        setOptOut(true);
    }

    @Override
    public void onCancelClicked() {
        if (sCallback != null) {
            sCallback.onCancelClicked();
        }
        clearSharedPreferences();
        storeAskLaterDate();
    }


}
