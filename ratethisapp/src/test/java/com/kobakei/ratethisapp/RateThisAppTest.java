package com.kobakei.ratethisapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcel;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = 36)
public class RateThisAppTest {

    private static final String PREF_NAME = "RateThisApp";
    private static final String KEY_INSTALL_DATE = "rta_install_date";
    private static final String KEY_LAUNCH_TIMES = "rta_launch_times";
    private static final String KEY_OPT_OUT = "rta_opt_out";
    private static final String KEY_ASK_LATER_DATE = "rta_ask_later_date";

    private Context context;
    private ActivityController<TestActivity> controller;
    private RecordingCallback callback;

    public static class TestActivity extends FragmentActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light);
            super.onCreate(savedInstanceState);
        }
    }

    static class RecordingCallback implements Callback {
        int yes, no, later;

        @Override public void onYesClicked() { yes++; }
        @Override public void onNoClicked() { no++; }
        @Override public void onLaterClicked() { later++; }
    }

    @Before
    public void setUp() throws Exception {
        resetSingleton();
        context = RuntimeEnvironment.getApplication();
        prefs().edit().clear().commit();
        callback = new RecordingCallback();
    }

    @After
    public void tearDown() throws Exception {
        if (controller != null) {
            controller.pause().stop().destroy();
        }
        resetSingleton();
    }

    // region helpers

    private static void resetSingleton() throws Exception {
        Field field = RateThisApp.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        field.set(null, null);
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /** Pretend the app was installed the given number of days ago and launched the given times before. */
    private void givenState(int installedDaysAgo, int previousLaunches) {
        prefs().edit()
                .putLong(KEY_INSTALL_DATE, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(installedDaysAgo))
                .putInt(KEY_LAUNCH_TIMES, previousLaunches)
                .commit();
    }

    private RateThisApp init(Config config, Market market) {
        RateThisApp.initialize(context, config, market);
        controller = Robolectric.buildActivity(TestActivity.class).setup();
        RateThisApp rateThisApp = RateThisApp.getInstance(controller.get());
        rateThisApp.setCallback(callback);
        return rateThisApp;
    }

    private RateThisApp init(Config config) {
        return init(config, Market.AMAZON);
    }

    /** Simulates the next process start. */
    private RateThisApp relaunch(Config config) throws Exception {
        if (controller != null) {
            controller.pause().stop().destroy();
            controller = null;
        }
        resetSingleton();
        return init(config);
    }

    private static void idle() {
        shadowOf(Looper.getMainLooper()).idle();
    }

    private RateDialogFragment dialogFragment() {
        return (RateDialogFragment) controller.get().getSupportFragmentManager()
                .findFragmentByTag(RateDialogFragment.TAG);
    }

    private AlertDialog shownDialog() {
        idle();
        RateDialogFragment fragment = dialogFragment();
        assertNotNull("dialog fragment not shown", fragment);
        AlertDialog dialog = (AlertDialog) fragment.getDialog();
        assertNotNull(dialog);
        return dialog;
    }

    private void click(int whichButton) {
        shownDialog().getButton(whichButton).performClick();
        idle();
    }

    // endregion

    // region launch counting and criteria

    @Test
    public void initializeCountsLaunchesAndStoresInstallDate() throws Exception {
        RateThisApp rateThisApp = init(new Config());
        assertEquals(1, rateThisApp.getLaunchCount());
        assertTrue(prefs().contains(KEY_INSTALL_DATE));

        rateThisApp = relaunch(new Config());
        assertEquals(2, rateThisApp.getLaunchCount());
    }

    @Test
    public void secondInitializeIsIgnored() {
        Config first = new Config(1, 1, Config.Operator.OR);
        RateThisApp rateThisApp = RateThisApp.initialize(context, first, Market.GOOGLE);
        RateThisApp again = RateThisApp.initialize(context, new Config(), Market.AMAZON);

        assertTrue(rateThisApp == again);
        assertTrue(first == again.getConfig());
        assertEquals(1, again.getLaunchCount());
    }

    @Test
    public void getInstanceWithoutInitializeThrows() {
        controller = Robolectric.buildActivity(TestActivity.class).setup();
        assertThrows(IllegalStateException.class, () -> RateThisApp.getInstance(controller.get()));
    }

    @Test
    public void orOperator_noCriteriaMet_notShown() {
        givenState(0, 0);
        RateThisApp rateThisApp = init(new Config(7, 10, Config.Operator.OR));

        assertFalse(rateThisApp.showRateDialogIfNeeded(false));
        idle();
        assertNull(dialogFragment());
    }

    @Test
    public void orOperator_launchesMet_shown() {
        givenState(0, 9);
        RateThisApp rateThisApp = init(new Config(7, 10, Config.Operator.OR));

        assertTrue(rateThisApp.showRateDialogIfNeeded(false));
        shownDialog();
    }

    @Test
    public void orOperator_daysMet_shown() {
        givenState(7, 0);
        RateThisApp rateThisApp = init(new Config(7, 10, Config.Operator.OR));

        assertTrue(rateThisApp.showRateDialogIfNeeded(false));
    }

    @Test
    public void andOperator_onlyLaunchesMet_notShown() {
        givenState(0, 20);
        RateThisApp rateThisApp = init(new Config(7, 10, Config.Operator.AND));

        assertFalse(rateThisApp.showRateDialogIfNeeded(false));
    }

    @Test
    public void andOperator_onlyDaysMet_notShown() {
        givenState(30, 0);
        RateThisApp rateThisApp = init(new Config(7, 10, Config.Operator.AND));

        assertFalse(rateThisApp.showRateDialogIfNeeded(false));
    }

    @Test
    public void andOperator_allMet_shown() {
        givenState(7, 9);
        RateThisApp rateThisApp = init(new Config(7, 10, Config.Operator.AND));

        assertTrue(rateThisApp.showRateDialogIfNeeded(false));
    }

    @Test
    public void overexceedsByValues() {
        givenState(10, 14);
        RateThisApp rateThisApp = init(new Config(7, 10, Config.Operator.OR));

        assertEquals(5, rateThisApp.getLaunchCountOverexeedsBy());
        assertEquals(3, rateThisApp.getLaunchDaysOverexeedsBy());
    }

    // endregion

    // region showing the dialog

    @Test
    public void forceDialog_shownEvenIfOptedOut() {
        givenState(0, 0);
        RateThisApp rateThisApp = init(new Config());
        rateThisApp.stopRateDialog();

        assertTrue(rateThisApp.showRateDialogIfNeeded(true));
        shownDialog();
    }

    @Test
    public void dialogIsNotShownTwice() {
        givenState(0, 20);
        RateThisApp rateThisApp = init(new Config());

        assertTrue(rateThisApp.showRateDialogIfNeeded(false));
        idle();
        assertFalse(rateThisApp.showRateDialogIfNeeded(false));
        assertFalse(rateThisApp.showRateDialogIfNeeded(true));
        idle();
        assertEquals(1, controller.get().getSupportFragmentManager().getFragments().size());
    }

    @Test
    public void dialogIsNotShownAfterStateSaved() {
        givenState(0, 20);
        RateThisApp rateThisApp = init(new Config());
        controller.pause().saveInstanceState(new Bundle()).stop();

        assertFalse(rateThisApp.showRateDialogIfNeeded(true));
    }

    @Test
    public void getDialogShown_trueOnlyAfterShowing_andPersisted() throws Exception {
        givenState(0, 0);
        RateThisApp rateThisApp = init(new Config());
        assertFalse(rateThisApp.getDialogShown());

        rateThisApp.showRateDialogIfNeeded(true);
        assertTrue(rateThisApp.getDialogShown());
        assertFalse(rateThisApp.getOptOut());

        assertTrue(relaunch(new Config()).getDialogShown());
    }

    @Test
    public void showWithoutActivity_returnsFalse() {
        RateThisApp rateThisApp = RateThisApp.initialize(context, new Config(), Market.GOOGLE);
        assertFalse(rateThisApp.showRateDialogIfNeeded(true));
    }

    // endregion

    // region buttons

    @Test
    public void noButton_optsOutPermanently() throws Exception {
        givenState(0, 20);
        RateThisApp rateThisApp = init(new Config());
        rateThisApp.showRateDialogIfNeeded(false);

        click(DialogInterface.BUTTON_NEGATIVE);

        assertEquals(1, callback.no);
        assertTrue(rateThisApp.getOptOut());
        assertTrue(prefs().getBoolean(KEY_OPT_OUT, false));
        assertFalse(rateThisApp.showRateDialogIfNeeded(false));
        assertFalse(relaunch(new Config()).showRateDialogIfNeeded(false));
    }

    @Test
    public void stopRateDialog_optsOut() {
        givenState(0, 20);
        RateThisApp rateThisApp = init(new Config());
        rateThisApp.stopRateDialog();

        assertTrue(rateThisApp.getOptOut());
        assertFalse(rateThisApp.showRateDialogIfNeeded(false));
    }

    @Test
    public void laterButton_resetsLaunchesAndDoesNotShowAgainInSameSession() {
        givenState(30, 20);
        RateThisApp rateThisApp = init(new Config(7, 10, Config.Operator.OR));
        rateThisApp.showRateDialogIfNeeded(false);

        click(DialogInterface.BUTTON_NEUTRAL);

        assertEquals(1, callback.later);
        assertFalse(rateThisApp.getOptOut());
        assertEquals(0, rateThisApp.getLaunchCount());
        assertEquals(0, prefs().getInt(KEY_LAUNCH_TIMES, -1));
        assertTrue(prefs().getLong(KEY_ASK_LATER_DATE, 0) > 0);
        // Regression: the dialog was shown again immediately after "Later".
        assertFalse(rateThisApp.showRateDialogIfNeeded(false));
    }

    @Test
    public void laterButton_keepsInstallDate() {
        givenState(30, 20);
        long installDate = prefs().getLong(KEY_INSTALL_DATE, 0);
        RateThisApp rateThisApp = init(new Config());
        rateThisApp.showRateDialogIfNeeded(false);

        click(DialogInterface.BUTTON_NEUTRAL);

        assertEquals(installDate, prefs().getLong(KEY_INSTALL_DATE, 0));
    }

    @Test
    public void afterLater_shownAgainWhenLaunchesReachedAgain() throws Exception {
        givenState(30, 20);
        Config config = new Config(7, 3, Config.Operator.OR);
        RateThisApp rateThisApp = init(config);
        rateThisApp.showRateDialogIfNeeded(false);
        click(DialogInterface.BUTTON_NEUTRAL);

        assertFalse(relaunch(config).showRateDialogIfNeeded(false)); // 1 launch
        assertFalse(relaunch(config).showRateDialogIfNeeded(false)); // 2 launches
        assertTrue(relaunch(config).showRateDialogIfNeeded(false));  // 3 launches
    }

    @Test
    public void afterLater_shownAgainWhenDaysPassedAgain() {
        givenState(30, 0);
        prefs().edit().putLong(KEY_ASK_LATER_DATE,
                System.currentTimeMillis() - TimeUnit.DAYS.toMillis(8)).commit();
        RateThisApp rateThisApp = init(new Config(7, 10, Config.Operator.OR));

        assertTrue(rateThisApp.showRateDialogIfNeeded(false));
    }

    @Test
    public void afterLater_notShownBeforeDaysPassedAgain() {
        givenState(30, 0);
        prefs().edit().putLong(KEY_ASK_LATER_DATE,
                System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)).commit();
        RateThisApp rateThisApp = init(new Config(7, 10, Config.Operator.OR));

        assertFalse(rateThisApp.showRateDialogIfNeeded(false));
    }

    @Test
    public void cancel_countsAsLater() {
        givenState(0, 20);
        RateThisApp rateThisApp = init(new Config());
        rateThisApp.showRateDialogIfNeeded(false);

        shownDialog().cancel();
        idle();

        assertEquals(1, callback.later);
        assertEquals(0, rateThisApp.getLaunchCount());
    }

    @Test
    public void cancelModes() {
        givenState(0, 20);
        Config config = new Config();
        RateThisApp rateThisApp = init(config);

        config.setCancelMode(Config.CANCEL_MODE_NONE);
        rateThisApp.showRateDialogIfNeeded(true);
        idle();
        assertFalse(dialogFragment().isCancelable());
        dialogFragment().dismissNow();

        config.setCancelMode(Config.CANCEL_MODE_BACK_KEY);
        rateThisApp.showRateDialogIfNeeded(true);
        idle();
        assertTrue(dialogFragment().isCancelable());
        assertFalse(shadowOf(shownDialog()).isCancelableOnTouchOutside());
        dialogFragment().dismissNow();

        config.setCancelMode(Config.CANCEL_MODE_BACK_KEY_OR_TOUCH_OUTSIDE);
        rateThisApp.showRateDialogIfNeeded(true);
        idle();
        assertTrue(dialogFragment().isCancelable());
        assertTrue(shadowOf(shownDialog()).isCancelableOnTouchOutside());
    }

    @Test
    public void buttonsStillWorkAfterRecreation() {
        givenState(0, 20);
        RateThisApp rateThisApp = init(new Config());
        rateThisApp.showRateDialogIfNeeded(false);
        idle();

        controller.recreate();
        RateThisApp.getInstance(controller.get());

        click(DialogInterface.BUTTON_NEGATIVE);

        assertEquals(1, callback.no);
        assertTrue(rateThisApp.getOptOut());
    }

    @Test
    public void yesButton_amazon_opensMarketUrlAndOptsOut() {
        givenState(0, 20);
        RateThisApp rateThisApp = init(new Config(), Market.AMAZON);
        rateThisApp.showRateDialogIfNeeded(false);

        click(DialogInterface.BUTTON_POSITIVE);

        Intent intent = shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity();
        assertNotNull(intent);
        assertEquals(RateThisApp.MARKET_AMAZON_URL + context.getPackageName(), intent.getDataString());
        assertEquals(1, callback.yes);
        assertTrue(rateThisApp.getOptOut());
    }

    @Test
    public void yesButton_customUrl_isOpened() {
        givenState(0, 20);
        Config config = new Config();
        config.setUrl("https://example.com/rate");
        RateThisApp rateThisApp = init(config, Market.SAMSUNG);
        rateThisApp.showRateDialogIfNeeded(false);

        click(DialogInterface.BUTTON_POSITIVE);

        Intent intent = shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity();
        assertEquals("https://example.com/rate", intent.getDataString());
    }

    @Test
    public void yesButton_huaweiWithoutAppGallery_doesNotCrash() {
        givenState(0, 20);
        RateThisApp rateThisApp = init(new Config(), Market.HUAWEI);
        // Throw ActivityNotFoundException for intents nobody can handle.
        shadowOf(RuntimeEnvironment.getApplication()).checkActivities(true);
        rateThisApp.showRateDialogIfNeeded(false);

        click(DialogInterface.BUTTON_POSITIVE);

        assertEquals(1, callback.yes);
        assertTrue(rateThisApp.getOptOut());
    }

    @Test
    public void yesButton_google_callsCallbackOnce() {
        givenState(0, 20);
        Config config = new Config();
        config.setUseFakeReviewManager(true);
        RateThisApp rateThisApp = init(config, Market.GOOGLE);
        rateThisApp.showRateDialogIfNeeded(false);

        click(DialogInterface.BUTTON_POSITIVE);
        idle();

        assertEquals(1, callback.yes);
        assertTrue(rateThisApp.getOptOut());
    }

    @Test
    public void withoutCallback_buttonsDoNotCrash() {
        givenState(0, 20);
        RateThisApp rateThisApp = init(new Config());
        rateThisApp.setCallback(null);
        rateThisApp.showRateDialogIfNeeded(false);

        click(DialogInterface.BUTTON_NEUTRAL);

        assertEquals(0, rateThisApp.getLaunchCount());
    }

    // endregion

    // region Config and Market

    @Test
    public void config_parcelRoundTrip_keepsAllValues() {
        Config config = new Config(3, 5, Config.Operator.AND);
        config.setUrl("https://example.com");
        config.setCancelMode(Config.CANCEL_MODE_NONE);
        config.setUseFakeReviewManager(true);

        Parcel parcel = Parcel.obtain();
        config.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Config copy = Config.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertEquals(3, copy.getmCriteriaInstallDays());
        assertEquals(5, copy.getmCriteriaLaunchTimes());
        assertEquals(Config.Operator.AND, copy.getmOperator());
        assertEquals("https://example.com", copy.getmUrl());
        assertEquals(Config.CANCEL_MODE_NONE, copy.getmCancelMode());
        assertTrue(copy.isUseFakeReviewManager());
    }

    @Test
    public void config_defaults() {
        Config config = new Config();
        assertEquals(7, config.getmCriteriaInstallDays());
        assertEquals(10, config.getmCriteriaLaunchTimes());
        assertEquals(Config.Operator.OR, config.getmOperator());
        assertEquals(Config.CANCEL_MODE_BACK_KEY_OR_TOUCH_OUTSIDE, config.getmCancelMode());
        assertNull(config.getmUrl());
        assertFalse(config.isUseFakeReviewManager());
        assertEquals(Config.Operator.OR, new Config(1, 1, null).getmOperator());
    }

    @Test
    public void market_fromName() {
        assertEquals(Market.AMAZON, Market.fromName("AMAZON"));
        assertEquals(Market.SAMSUNG, Market.fromName("samsung"));
        assertEquals(Market.GOOGLE, Market.fromName(null));
        assertEquals(Market.GOOGLE, Market.fromName(""));
        assertEquals(Market.GOOGLE, Market.fromName("unknown"));
    }

    @Test
    public void market_fromName_isLocaleIndependent() {
        Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            assertEquals(Market.HUAWEI, Market.fromName("huawei"));
            assertEquals(Market.AMAZON, Market.fromName("amazon"));
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    // endregion
}
