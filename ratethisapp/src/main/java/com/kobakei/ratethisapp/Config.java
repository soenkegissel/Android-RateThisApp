package com.kobakei.ratethisapp;

/**
 * Created by Sönke Gissel on 10.09.2019.
 */

import android.os.Parcel;
import android.os.Parcelable;

/**
 * RateThisApp configuration.
 */
public class Config implements Parcelable {
    public static final int CANCEL_MODE_BACK_KEY_OR_TOUCH_OUTSIDE = 0;
    public static final int CANCEL_MODE_BACK_KEY                  = 1;
    public static final int CANCEL_MODE_NONE                      = 2;

    public enum Operator {
        AND,
        OR
    }

    private String mUrl = null;

    private int mCriteriaInstallDays;
    private int mCriteriaLaunchTimes;
    private int mCancelMode = CANCEL_MODE_BACK_KEY_OR_TOUCH_OUTSIDE;
    private Operator mOperator;

    /**
     * Constructor with default criteria.
     */
    public Config() {
        this(7, 10, Operator.OR);
    }

    /**
     * Constructor.
     * @param criteriaInstallDays
     * @param criteriaLaunchTimes
     * @param operator
     */
    public Config(int criteriaInstallDays, int criteriaLaunchTimes, Operator operator) {
        this.mCriteriaInstallDays = criteriaInstallDays;
        this.mCriteriaLaunchTimes = criteriaLaunchTimes;
        this.mOperator = operator;
    }

    /**
     * Set navigation url when user clicks rate button.
     * Typically, url will be https://play.google.com/store/apps/details?id=PACKAGE_NAME for Google Play.
     * @param url
     */
    public void setUrl(String url) {
        this.mUrl = url;
    }

    /**
     * Set the cancel mode; namely, which ways the user can cancel the dialog.
     * @param cancelMode
     */
    public void setCancelMode(int cancelMode) {
        this.mCancelMode = cancelMode;
    }

    public int getmCriteriaInstallDays() {
        return mCriteriaInstallDays;
    }

    public int getmCriteriaLaunchTimes() {
        return mCriteriaLaunchTimes;
    }

    public int getmCancelMode() {
        return mCancelMode;
    }

    public String getmUrl() {
        return mUrl;
    }

    public Operator getmOperator() {
        return mOperator;
    }

    // Parcelling part
    protected Config(Parcel in){
        mCancelMode = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mCancelMode);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Config> CREATOR = new Parcelable.Creator<Config>() {
        public Config createFromParcel(Parcel in) {
            return new Config(in);
        }

        public Config[] newArray(int size) {
            return new Config[size];
        }
    };
}
