package com.kobakei.ratethisapp;

/**
 * Created by Sönke Gissel on 22.10.2019.
 */
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
    void onLaterClicked();
}
