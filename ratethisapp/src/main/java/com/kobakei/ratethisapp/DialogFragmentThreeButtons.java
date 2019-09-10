package com.kobakei.ratethisapp;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.KeyEvent;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

/**
 * Created by Sönke Gissel on 10.09.2019.
 */
public class DialogFragmentThreeButtons extends AppCompatDialogFragment {

    private AlertDialog.Builder builder;
    private Config sConfig;

    private RateThisApp.Callback sCallback;

    static DialogFragmentThreeButtons newInstance(Parcelable config, int themeId) {
        DialogFragmentThreeButtons frag = new DialogFragmentThreeButtons();
        Bundle args = new Bundle();
        args.putParcelable("config", config);
        args.putInt("themeId", themeId);
        frag.setArguments(args);
        return frag;
    }

    /**
     * Set callback instance.
     * The callback will receive yes/no/later events.
     * @param callback
     */
    void setCallback(RateThisApp.Callback callback) {
        sCallback = callback;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int themeId = getArguments().getInt("themeId");
        sConfig = getArguments().getParcelable("config");

        builder = new AlertDialog.Builder(getContext(), themeId);

        int titleId = sConfig.getmTitleId() != 0 ? sConfig.getmTitleId() : R.string.rta_dialog_title;
        int messageId = sConfig.getmMessageId() != 0 ? sConfig.getmMessageId() : R.string.rta_dialog_message;
        int cancelButtonID = sConfig.getmCancelButton() != 0 ? sConfig.getmCancelButton() : R.string.rta_dialog_cancel;
        int thanksButtonID = sConfig.getmNoButtonId() != 0 ? sConfig.getmNoButtonId() : R.string.rta_dialog_no;
        int rateButtonID = sConfig.getmYesButtonId() != 0 ? sConfig.getmYesButtonId() : R.string.rta_dialog_ok;
        builder.setTitle(titleId);
        builder.setMessage(messageId);
        switch (sConfig.getmCancelMode()) {
            case Config.CANCEL_MODE_BACK_KEY_OR_TOUCH_OUTSIDE:
                setCancelable(true); // It's the default anyway
                break;
            case Config.CANCEL_MODE_BACK_KEY:
                setCancelable(false);
                builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            dialog.cancel();
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
                break;
            case Config.CANCEL_MODE_NONE:
                setCancelable(false);
                break;
        }
        builder.setPositiveButton(rateButtonID, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sCallback.onYesClicked();
            }
        });
        builder.setNeutralButton(cancelButtonID, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sCallback.onCancelClicked();
            }
        });
        builder.setNegativeButton(thanksButtonID, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sCallback.onNoClicked();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                sCallback.onCancelClicked();
            }
        });
        return builder.create();
    }
}
