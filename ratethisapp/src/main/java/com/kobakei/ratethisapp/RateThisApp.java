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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.review.testing.FakeReviewManager;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * RateThisApp<br>
 * A library to show the app rate dialog
 * @author Keisuke Kobayashi (k.kobayashi.122@gmail.com)
 * @editor Sönke Gissel (soenke.gissel@gmail.com)
 *
 */
public class RateThisApp implements Callback {

    private static final String TAG = RateThisApp.class.getSimpleName();

    public static final String MARKET_AMAZON_URL = "amzn://apps/android?p=";
    public static final String WEB_AMAZON_URL = "https://www.amazon.com/gp/mas/dl/android?p=";

    public static final String MARKET_HUAWEI_URL = "appmarket://details?id=";

    public static final String MARKET_GOOGLE_URL = "market://details?id=";
    public static final String WEB_GOOGLE_URL = "https://play.google.com/store/apps/details?id=";

    public static final String MARKET_SAMSUNG_URL = "samsungapps://ProductDetail/";
    public static final String WEB_SAMSUNG_URL = "https://galaxystore.samsung.com/detail/";


    private static final String PREF_NAME = "RateThisApp";
    private static final String KEY_INSTALL_DATE = "rta_install_date";
    private static final String KEY_LAUNCH_TIMES = "rta_launch_times";
    private static final String KEY_OPT_OUT = "rta_opt_out";
    private static final String KEY_ASK_LATER_DATE = "rta_ask_later_date";
    private static final String KEY_DIALOG_SHOWN = "rta_dialog_shown";

    private Date mInstallDate;
    private Date mAskLaterDate;
    private int mLaunchTimes;
    private boolean mOptOut;
    private boolean mDialogShown;

    private final Config sConfig;
    private Callback sCallback;

    private WeakReference<FragmentActivity> mActivityRef;
    private final Context mContext;

    private final Market mMarket;
    private final boolean mDebug;

    private final ReviewManager mReviewManager;

    //https://de.wikibooks.org/wiki/Muster:_Java:_Singleton
    @SuppressLint("StaticFieldLeak")
    private static volatile RateThisApp INSTANCE;

    private RateThisApp(Context context, Config config, Market market) {
        this.mContext = context;
        this.sConfig = config;
        this.mMarket = market;
        // BuildConfig.DEBUG of a library is always false in the published aar, so use the app's flag.
        this.mDebug = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;

        if (market == Market.GOOGLE && (mDebug || config.isUseFakeReviewManager())) {
            this.mReviewManager = new FakeReviewManager(mContext);
        } else if (market == Market.GOOGLE) {
            this.mReviewManager = ReviewManagerFactory.create(mContext);
        } else {
            this.mReviewManager = null;
        }

        setup();
    }

    /**
     * Initialize RateThisApp. Call this once from your Application class.
     * Subsequent calls return the existing instance and ignore the given config and market.
     */
    public static synchronized RateThisApp initialize(Context context, Config config, Market market) {
        if (INSTANCE == null) {
            Context applicationContext;
            if (context.getApplicationContext() == null) {
                applicationContext = context;
            } else {
                applicationContext = context.getApplicationContext();
            }
            INSTANCE = new RateThisApp(applicationContext, config, market);
        } else {
            Log.w(TAG, "RateThisApp is already initialized. Ignoring new config and market.");
        }
        return INSTANCE;
    }

    public static RateThisApp initialize(Context context, Market market) {
        return initialize(context, new Config(7, 10, Config.Operator.OR), market);
    }

    /**
     * Initialize with default config and Google Play as market.
     * @deprecated The library cannot read the BuildConfig.MARKET of your app, so this always uses
     * {@link Market#GOOGLE}. Use {@link #initialize(Context, Config, Market)} with
     * {@code Market.fromName(BuildConfig.MARKET)} instead.
     */
    @Deprecated
    public static RateThisApp initialize(Context context) {
        return initialize(context, Market.GOOGLE);
    }

    public static RateThisApp getInstance(FragmentActivity fragmentActivity) {
        if(INSTANCE == null) {
            throw new IllegalStateException("RateThisApp not initialized. Call RateThisApp.initialize(Context, Config, Market) from your application class.");
        }
        INSTANCE.mActivityRef = new WeakReference<>(fragmentActivity);

        return RateThisApp.INSTANCE;
    }

    static RateThisApp getInstanceOrNull() {
        return INSTANCE;
    }

    private FragmentActivity getActivity() {
        return mActivityRef != null ? mActivityRef.get() : null;
    }

    /**
     * Set callback INSTANCE.
     * The callback will receive yes/no/later events.
     * The callback is held by the singleton, so remove it with setCallback(null) in onDestroy()
     * if it references an Activity.
     * @param callback
     */
    public void setCallback(Callback callback) {
        sCallback = callback;
    }

    /**
     * Called once per process start from initialize().
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

        if(mDebug)
            printStatus();
    }

    private void setObjects(SharedPreferences pref) {
        mInstallDate = new Date(pref.getLong(KEY_INSTALL_DATE, 0));
        mLaunchTimes = pref.getInt(KEY_LAUNCH_TIMES, 0);
        mOptOut = pref.getBoolean(KEY_OPT_OUT, false);
        mAskLaterDate = new Date(pref.getLong(KEY_ASK_LATER_DATE, 0));
        mDialogShown = pref.getBoolean(KEY_DIALOG_SHOWN, false);
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
        FragmentActivity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "Activity is null. Cannot show dialog.");
            return false;
        }

        if(forceDialog || shouldShowRateDialog(sConfig.getmOperator())) {
            return showRateDialog(activity, themeId);
        } else {
            return false;
        }
    }

    /**
     * Check whether the rate dialog should be shown or not.
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
     * @return true if shown, false if already showing or the activity state is already saved.
     */
    private boolean showRateDialog(final FragmentActivity activity, int themeId) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        if (fragmentManager.isStateSaved()) {
            Log.w(TAG, "Activity state already saved. Cannot show dialog.");
            return false;
        }
        if (fragmentManager.findFragmentByTag(RateDialogFragment.TAG) != null) {
            return false;
        }
        RateDialogFragment.newInstance(sConfig, themeId).show(fragmentManager, RateDialogFragment.TAG);
        setDialogShown();
        return true;
    }

    /**
     * Stop showing the rate dialog
     */
    public void stopRateDialog(){
        setOptOut(true);
    }

    /**
     * Get the number of app launches (process starts) since install or the last "Later".
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
        return (int) (TimeUnit.MILLISECONDS.toDays(new Date().getTime() - mInstallDate.getTime()) - sConfig.getmCriteriaInstallDays());
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
        return mDialogShown;
    }

    public Config getConfig() {
        return sConfig;
    }

    /**
     * Reset the launch times and store the date the user asked for being asked again later.<br>
     * This API is called when the "Later" is pressed or canceled.
     */
    private void storeAskLater() {
        SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Editor editor = pref.edit();
        editor.putInt(KEY_LAUNCH_TIMES, 0);
        editor.putLong(KEY_ASK_LATER_DATE, System.currentTimeMillis());
        editor.apply();
        mLaunchTimes = 0;
        mAskLaterDate = new Date(pref.getLong(KEY_ASK_LATER_DATE, 0));
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

    private void setDialogShown() {
        SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pref.edit().putBoolean(KEY_DIALOG_SHOWN, true).apply();
        mDialogShown = true;
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
     * Print values in SharedPreferences (used for debug)
     */
    private void printStatus() {
        SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        log("*** RateThisApp Status ***");
        log("*** Market Config: " + mMarket);
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
     * Print log if the app is debuggable
     * @param message
     */
    private void log(String message) {
        if (mDebug) {
            Log.v(TAG, message);
        }
    }

    @Override
    public void onYesClicked() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "Activity is null. Cannot handle Yes click.");
            return;
        }
        onYesClicked(activity);
    }

    /**
     * Handle the "Rate now" click with the activity currently hosting the dialog.
     */
    void onYesClicked(final FragmentActivity activity) {
        final String appPackage = activity.getPackageName();
        final String url = sConfig.getmUrl() != null ? sConfig.getmUrl() : getMarketURL(mMarket, appPackage);

        if(mMarket.equals(Market.GOOGLE)) {
            mReviewManager.requestReviewFlow().addOnCompleteListener(requestTask -> {
                if (!requestTask.isSuccessful() || activity.isFinishing() || activity.isDestroyed()) {
                    // There was some problem, fallback to market URL.
                    log("Review flow not available, fallback to market url.");
                    launchIntent(activity, url, appPackage);
                    notifyYes();
                    return;
                }
                ReviewInfo reviewInfo = requestTask.getResult();
                log("onYesClicked. ReviewInfo: " + reviewInfo);
                mReviewManager.launchReviewFlow(activity, reviewInfo).addOnCompleteListener(launchTask -> {
                    if (!launchTask.isSuccessful()) {
                        log("Review flow failed, fallback to market url.");
                        launchIntent(activity, url, appPackage);
                    } else {
                        // The flow has finished. Google does not tell if the user actually rated.
                        log("Review flow completed.");
                    }
                    notifyYes();
                });
            });
        }
        else {
            launchIntent(activity, url, appPackage);
            notifyYes();
        }
        setOptOut(true);
    }

    private void notifyYes() {
        if (sCallback != null) {
            sCallback.onYesClicked();
        }
    }

    private void launchIntent(FragmentActivity activity, String url, String appPackage) {
        if (startViewIntent(activity, url)) {
            return;
        }
        // Market app not installed, fallback to web url if the market has one.
        String webUrl = getWebURL(mMarket, appPackage);
        if (webUrl == null || !startViewIntent(activity, webUrl)) {
            Log.e(TAG, "Unable to open market or web url for " + appPackage + " (" + mMarket + ")");
        }
    }

    private boolean startViewIntent(FragmentActivity activity, String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            return true;
        } catch (android.content.ActivityNotFoundException anfe) {
            return false;
        }
    }

    /**
     * @return web url of the market or null if the market does not support web rating by package name.
     */
    private String getWebURL(Market market, String appPackage) {
        switch (market) {
            case AMAZON:
                return WEB_AMAZON_URL + appPackage;
            case GOOGLE:
                return WEB_GOOGLE_URL + appPackage;
            case HUAWEI:
                // Huawei does not support web rating by packagename
                return null;
            case SAMSUNG:
                return WEB_SAMSUNG_URL + appPackage;
            default:
                return null;
        }
    }

    private String getMarketURL(Market market, String appPackage) {
        switch (market) {
            case AMAZON:
                return MARKET_AMAZON_URL + appPackage;
            case GOOGLE:
                return MARKET_GOOGLE_URL + appPackage;
            case HUAWEI:
                return MARKET_HUAWEI_URL + appPackage;
            case SAMSUNG:
                return MARKET_SAMSUNG_URL + appPackage;
            default:
                return "";
        }
    }

    @Override
    public void onNoClicked() {
        if (sCallback != null) {
            sCallback.onNoClicked();
        }
        setOptOut(true);
    }

    @Override
    public void onLaterClicked() {
        if (sCallback != null) {
            sCallback.onLaterClicked();
        }
        storeAskLater();
    }


}
