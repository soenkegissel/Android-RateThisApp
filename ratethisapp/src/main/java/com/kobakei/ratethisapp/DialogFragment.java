package com.kobakei.ratethisapp;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.KeyEvent;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.rucksack.ratethisapp.R;

/**
 * Created by Sönke Gissel on 10.09.2019.
 */
public class DialogFragment extends AppCompatDialogFragment {

    private AlertDialog.Builder builder;

    private Callback sCallback;

    static DialogFragment newInstance(Parcelable config, int themeId) {
        DialogFragment frag = new DialogFragment();
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
    void setCallback(Callback callback) {
        sCallback = callback;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int themeId = getArguments().getInt("themeId");
        Config sConfig = getArguments().getParcelable("config");

        builder = new AlertDialog.Builder(getContext(), themeId);
        builder.setTitle(R.string.rta_dialog_title);
        builder.setMessage(R.string.rta_dialog_message);
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
        builder.setPositiveButton(R.string.rta_dialog_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(sCallback != null)
                    sCallback.onYesClicked();
            }
        });
        builder.setNeutralButton(R.string.rta_dialog_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(sCallback != null)
                    sCallback.onLaterClicked();
            }
        });
        builder.setNegativeButton(R.string.rta_dialog_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(sCallback != null)
                    sCallback.onNoClicked();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if(sCallback != null)
                    sCallback.onLaterClicked();
            }
        });
        return builder.create();
    }
}
