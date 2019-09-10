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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * RateThisApp<br>
 * A library to show the app rate dialog
 * @author Keisuke Kobayashi (k.kobayashi.122@gmail.com)
 *
 */
public class RateThisApp {

    private final String TAG = RateThisApp.class.getSimpleName();

    private static final String PREF_NAME = "RateThisApp";
    private static final String KEY_INSTALL_DATE = "rta_install_date";
    private static final String KEY_LAUNCH_TIMES = "rta_launch_times";
    private static final String KEY_OPT_OUT = "rta_opt_out";
    private static final String KEY_ASK_LATER_DATE = "rta_ask_later_date";

    private Date mInstallDate;
    private Date mAskLaterDate;
    private int mLaunchTimes;
    private boolean mOptOut;

    private Config sConfig;
    private Callback sCallback;

    private FragmentActivity fragmentActivity;

    public RateThisApp(FragmentActivity activity, Config config) {
        this.fragmentActivity = activity;
        this.sConfig = config;

        setup();
    }

    /**
     * Initialize RateThisApp configuration.
     * @param config Configuration object.
     */
    public void setConfig(Config config) {
        sConfig = config;
    }

    /**
     * Set callback instance.
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
        SharedPreferences pref = fragmentActivity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Editor editor = pref.edit();
        // If it is the first launch, save the date in shared preference.
        if (pref.getLong(KEY_INSTALL_DATE, 0) == 0L) {
            storeInstallDate(fragmentActivity, editor);
        }
        // Increment launch times
        int launchTimes = pref.getInt(KEY_LAUNCH_TIMES, 0);
        launchTimes++;
        editor.putInt(KEY_LAUNCH_TIMES, launchTimes);

        editor.apply();

        mInstallDate = new Date(pref.getLong(KEY_INSTALL_DATE, 0));
        mLaunchTimes = pref.getInt(KEY_LAUNCH_TIMES, 0);
        mOptOut = pref.getBoolean(KEY_OPT_OUT, false);
        mAskLaterDate = new Date(pref.getLong(KEY_ASK_LATER_DATE, 0));

        if(BuildConfig.DEBUG)
            printStatus();
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
            showRateDialog(fragmentActivity, themeId);
            return true;
        } else if (shouldShowRateDialog(sConfig.getmOperator())) {
            showRateDialog(fragmentActivity, themeId);
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
            boolean launchTimes = mLaunchTimes >= sConfig.getmCriteriaLaunchTimes();
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
        DialogFragmentThreeButtons newFragment = DialogFragmentThreeButtons.newInstance(
                sConfig, themeId);
        newFragment.setCallback(callback);
        newFragment.show(activity.getSupportFragmentManager(), "dialog");
    }

    private Callback callback = new Callback() {
        @Override
        public void onYesClicked() {
            if (sCallback != null) {
                sCallback.onYesClicked();
            }
            String appPackage = fragmentActivity.getPackageName();
            String url = "market://details?id=" + appPackage;
            if (!TextUtils.isEmpty(sConfig.getmUrl())) {
                url = sConfig.getmUrl();
            }
            try {
                fragmentActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (android.content.ActivityNotFoundException anfe) {
                fragmentActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + fragmentActivity.getPackageName())));
            }
            setOptOut(true);
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
            storeAskLaterDate(fragmentActivity);
        }
    };

    /**
     * Stop showing the rate dialog
     */
    public void stopRateDialog(){
        setOptOut(true);
    }

    /**
     * Get count number of the rate dialog launches
     * @return
     */
    public int getLaunchCount(){
        SharedPreferences pref = fragmentActivity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pref.getInt(KEY_LAUNCH_TIMES, 0);
    }

    /**
     * Clear data in shared preferences.<br>
     * This API is called when the "Later" is pressed or canceled.
     */
    private void clearSharedPreferences() {
        SharedPreferences pref = fragmentActivity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Editor editor = pref.edit();
        editor.remove(KEY_INSTALL_DATE);
        editor.remove(KEY_LAUNCH_TIMES);
        editor.apply();
    }

    /**
     * Set opt out flag.
     * If it is true, the rate dialog will never shown unless app data is cleared.
     * This method is called when Yes or No is pressed.
     * @param optOut
     */
    private void setOptOut(boolean optOut) {
        SharedPreferences pref = fragmentActivity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Editor editor = pref.edit();
        editor.putBoolean(KEY_OPT_OUT, optOut);
        editor.apply();
        mOptOut = optOut;
    }

    /**
     * Store install date.
     * Install date is retrieved from package manager if possible.
     * @param context
     * @param editor
     */
    private void storeInstallDate(final Context context, SharedPreferences.Editor editor) {
        Date installDate = new Date();
        PackageManager packMan = context.getPackageManager();
        try {
            PackageInfo pkgInfo = packMan.getPackageInfo(context.getPackageName(), 0);
            installDate = new Date(pkgInfo.firstInstallTime);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        editor.putLong(KEY_INSTALL_DATE, installDate.getTime());
        log("First install: " + installDate.toString());
    }

    /**
     * Store the date the user asked for being asked again later.
     * @param context
     */
    private void storeAskLaterDate(final Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Editor editor = pref.edit();
        editor.putLong(KEY_ASK_LATER_DATE, System.currentTimeMillis());
        editor.apply();
    }

    /**
     * Print values in SharedPreferences (used for debug)
     */
    public void printStatus() {
        SharedPreferences pref = fragmentActivity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        log("*** RateThisApp Status ***");
        log("Install Date: " + new Date(pref.getLong(KEY_INSTALL_DATE, 0))+". Needed: "+sConfig.getmCriteriaInstallDays());
        log("Launch Times: " + pref.getInt(KEY_LAUNCH_TIMES, 0)+". Needed: "+sConfig.getmCriteriaLaunchTimes());
        log("Install Date & Launch Times Operator: "+sConfig.getmOperator());
        log("Opt out: " + pref.getBoolean(KEY_OPT_OUT, false));
        log("Criteria match? "+shouldShowRateDialog(sConfig.getmOperator()));
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

    /**
     * Callback of dialog click event
     */
    public interface Callback {
        /**
         * "Rate now" event
         */
        void onYesClicked();

        /**
         * "No, thanks" event
         */
        void onNoClicked();

        /**
         * "Later" event
         */
        void onCancelClicked();
    }
}
