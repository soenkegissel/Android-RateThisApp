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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
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

    private Date mInstallDate = new Date();
    private int mLaunchTimes = 0;
    private boolean mOptOut = false;
    private Date mAskLaterDate = new Date();

    private Config sConfig = new Config();
    private Callback sCallback = null;

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
     * It is better to call this API in onCreate() of the launcher activity.
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
        log("Launch times; " + launchTimes);

        editor.apply();

        mInstallDate = new Date(pref.getLong(KEY_INSTALL_DATE, 0));
        mLaunchTimes = pref.getInt(KEY_LAUNCH_TIMES, 0);
        mOptOut = pref.getBoolean(KEY_OPT_OUT, false);
        mAskLaterDate = new Date(pref.getLong(KEY_ASK_LATER_DATE, 0));

        printStatus(fragmentActivity);
    }

    /**
     * Show the rate dialog if the criteria is satisfied.
     * @return true if shown, false otherwise.
     */
    public boolean showRateDialogIfNeeded() {
        if (shouldShowRateDialog()) {
            showRateDialog(fragmentActivity);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Show the rate dialog if the criteria is satisfied.
     * @param themeId Theme ID
     * @return true if shown, false otherwise.
     */
    public boolean showRateDialogIfNeeded(int themeId) {
        if (shouldShowRateDialog()) {
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
     */
    private boolean shouldShowRateDialog() {
        if (mOptOut) {
            return false;
        } else {
            if (mLaunchTimes >= sConfig.getmCriteriaLaunchTimes()) {
                return true;
            }
            long threshold = TimeUnit.DAYS.toMillis(sConfig.getmCriteriaInstallDays());   // msec
            if (new Date().getTime() - mInstallDate.getTime() >= threshold &&
                new Date().getTime() - mAskLaterDate.getTime() >= threshold) {
                return true;
            }
            return false;
        }
    }

    /**
     * Show the rate dialog
     * @param activity
     */
    private void showRateDialog(final FragmentActivity activity) {
        DialogFragmentThreeButtons newFragment = DialogFragmentThreeButtons.newInstance(
                sConfig, 0);
        newFragment.show(activity.getSupportFragmentManager(), "dialog");
    }

    /**
     * Show the rate dialog
     * @param activity
     * @param themeId
     */
    private void showRateDialog(final FragmentActivity activity, int themeId) {
        DialogFragmentThreeButtons newFragment = DialogFragmentThreeButtons.newInstance(
                sConfig, themeId);
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
     * @return
     */
    public int getLaunchCount(){
        SharedPreferences pref = fragmentActivity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pref.getInt(KEY_LAUNCH_TIMES, 0);
    }

    private void doPositiveClick() {
        // Do stuff here.
        Log.i("FragmentAlertDialog", "Positive click!");
    }

    private void doNegativeClick() {
        // Do stuff here.
        Log.i("FragmentAlertDialog", "Negative click!");
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            PackageManager packMan = context.getPackageManager();
            try {
                PackageInfo pkgInfo = packMan.getPackageInfo(context.getPackageName(), 0);
                installDate = new Date(pkgInfo.firstInstallTime);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
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
     * @param context
     */
    private void printStatus(final Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        log("*** RateThisApp Status ***");
        log("Install Date: " + new Date(pref.getLong(KEY_INSTALL_DATE, 0)));
        log("Launch Times: " + pref.getInt(KEY_LAUNCH_TIMES, 0));
        log("Opt out: " + pref.getBoolean(KEY_OPT_OUT, false));
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
