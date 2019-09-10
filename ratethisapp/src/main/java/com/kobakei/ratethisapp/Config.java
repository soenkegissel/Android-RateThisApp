package com.kobakei.ratethisapp;

/**
 * Created by Sönke Gissel on 10.09.2019.
 */

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.StringRes;

/**
 * RateThisApp configuration.
 */
public class Config implements Parcelable {
    public static final int CANCEL_MODE_BACK_KEY_OR_TOUCH_OUTSIDE = 0;
    public static final int CANCEL_MODE_BACK_KEY                  = 1;
    public static final int CANCEL_MODE_NONE                      = 2;

    private String mUrl = null;

    private int mCriteriaInstallDays;
    private int mCriteriaLaunchTimes;
    private int mTitleId = 0;
    private int mMessageId = 0;
    private int mYesButtonId = 0;
    private int mNoButtonId = 0;
    private int mCancelButton = 0;
    private int mCancelMode = CANCEL_MODE_BACK_KEY_OR_TOUCH_OUTSIDE;

    /**
     * Constructor with default criteria.
     */
    public Config() {
        this(7, 10);
    }

    /**
     * Constructor.
     * @param criteriaInstallDays
     * @param criteriaLaunchTimes
     */
    public Config(int criteriaInstallDays, int criteriaLaunchTimes) {
        this.mCriteriaInstallDays = criteriaInstallDays;
        this.mCriteriaLaunchTimes = criteriaLaunchTimes;
    }

    /**
     * Set title string ID.
     * @param stringId
     */
    public void setTitle(@StringRes int stringId) {
        this.mTitleId = stringId;
    }

    /**
     * Set message string ID.
     * @param stringId
     */
    public void setMessage(@StringRes int stringId) {
        this.mMessageId = stringId;
    }

    /**
     * Set rate now string ID.
     * @param stringId
     */
    public void setYesButtonText(@StringRes int stringId) {
        this.mYesButtonId = stringId;
    }

    /**
     * Set no thanks string ID.
     * @param stringId
     */
    public void setNoButtonText(@StringRes int stringId) {
        this.mNoButtonId = stringId;
    }

    /**
     * Set cancel string ID.
     * @param stringId
     */
    public void setCancelButtonText(@StringRes int stringId) {
        this.mCancelButton = stringId;
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

    public int getmTitleId() {
        return mTitleId;
    }

    public int getmMessageId() {
        return mMessageId;
    }

    public int getmYesButtonId() {
        return mYesButtonId;
    }

    public int getmNoButtonId() {
        return mNoButtonId;
    }

    public int getmCancelButton() {
        return mCancelButton;
    }

    public int getmCancelMode() {
        return mCancelMode;
    }

    public String getmUrl() {
        return mUrl;
    }

    // Parcelling part
    public Config(Parcel in){
        int[] data = new int[5];

        in.readIntArray(data);
        // the order needs to be the same as in writeToParcel() method
        this.mTitleId = data[0];
        this.mMessageId = data[1];
        this.mYesButtonId = data[2];
        this.mNoButtonId = data[3];
        this.mCancelButton = data[4];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeIntArray(new int[] {this.mTitleId,
                this.mMessageId,
                this.mYesButtonId,
                this.mNoButtonId,
                this.mCancelButton});
    }
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Config createFromParcel(Parcel in) {
            return new Config(in);
        }

        public Config[] newArray(int size) {
            return new Config[size];
        }
    };
}
