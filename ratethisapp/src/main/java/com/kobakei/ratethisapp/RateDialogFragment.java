package com.kobakei.ratethisapp;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.core.os.BundleCompat;

import com.rucksack.ratethisapp.R;

/**
 * Created by Sönke Gissel on 10.09.2019.
 * The button events are forwarded to the RateThisApp instance, so they still work
 * after the fragment was recreated (e.g. rotation or process death).
 */
public class RateDialogFragment extends AppCompatDialogFragment {

    static final String TAG = "RateThisAppDialog";

    private static final String ARG_CONFIG = "config";
    private static final String ARG_THEME_ID = "themeId";

    static RateDialogFragment newInstance(Config config, int themeId) {
        RateDialogFragment frag = new RateDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_CONFIG, config);
        args.putInt(ARG_THEME_ID, themeId);
        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = requireArguments();
        int themeId = args.getInt(ARG_THEME_ID);
        Config config = BundleCompat.getParcelable(args, ARG_CONFIG, Config.class);
        int cancelMode = config != null ? config.getmCancelMode() : Config.CANCEL_MODE_BACK_KEY_OR_TOUCH_OUTSIDE;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), themeId);
        builder.setTitle(R.string.rta_dialog_title);
        builder.setMessage(R.string.rta_dialog_message);
        builder.setPositiveButton(R.string.rta_dialog_ok, (dialog, which) -> {
            RateThisApp rateThisApp = RateThisApp.getInstanceOrNull();
            if (rateThisApp != null)
                rateThisApp.onYesClicked(requireActivity());
        });
        builder.setNeutralButton(R.string.rta_dialog_cancel, (dialog, which) -> notifyLater());
        builder.setNegativeButton(R.string.rta_dialog_no, (dialog, which) -> {
            RateThisApp rateThisApp = RateThisApp.getInstanceOrNull();
            if (rateThisApp != null)
                rateThisApp.onNoClicked();
        });

        AlertDialog dialog = builder.create();
        switch (cancelMode) {
            case Config.CANCEL_MODE_BACK_KEY:
                setCancelable(true);
                dialog.setCanceledOnTouchOutside(false);
                break;
            case Config.CANCEL_MODE_NONE:
                setCancelable(false);
                break;
            case Config.CANCEL_MODE_BACK_KEY_OR_TOUCH_OUTSIDE:
            default:
                setCancelable(true); // It's the default anyway
                break;
        }
        return dialog;
    }

    /**
     * DialogFragment replaces the cancel listener of the dialog, so cancel events arrive here.
     */
    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        notifyLater();
    }

    private void notifyLater() {
        RateThisApp rateThisApp = RateThisApp.getInstanceOrNull();
        if (rateThisApp != null)
            rateThisApp.onLaterClicked();
    }
}
