package com.urbanairship.actions;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.urbanairship.Autopilot;
import com.urbanairship.Logger;
import com.urbanairship.R;
import com.urbanairship.UAirship;
import com.urbanairship.google.PlayServicesUtils;
import com.urbanairship.messagecenter.ThemedActivity;

import com.urbanairship.util.ManifestUtils;

import static com.urbanairship.UAirship.getPackageName;

/**
 * An activity that displays a Rate App prompt that links to an app store.
 */
public class RateAppActivity extends ThemedActivity {

    AlertDialog dialog;

    @SuppressLint("NewApi")
    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Autopilot.automaticTakeOff(getApplication());

        if (!UAirship.isTakingOff() && !UAirship.isFlying()) {
            Logger.error("RateAppActivity - unable to create activity, takeOff not called.");
            finish();
            return;
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        Logger.debug("RateAppActivity - New intent received for rate app activity");
        restartActivity(intent.getData(), intent.getExtras());
    }

    @SuppressLint("NewApi")
    @Override
    public void onResume() {
        super.onResume();
        displayDialog();
    }

    @SuppressLint("NewApi")
    @Override
    public void onPause() {
        super.onPause();
    }

    /**
     * Finishes the activity.
     *
     * @param view The view that was clicked.
     */
    public void onCloseButtonClick(View view) {
        this.finish();
    }

    /**
     * Displays the Rate App prompt that links to an app store.
     */
    private void displayDialog() {
        if (dialog != null && dialog.isShowing()) {
            return;
        }

        Intent intent = getIntent();
        if (intent == null) {
            Logger.error("RateAppActivity - Started activity with null intent.");
            finish();
            return;
        }

        final Uri storeUri = intent.getParcelableExtra(RateAppAction.STORE_URI_KEY);

        if (storeUri == null) {
            Logger.error("RateAppActivity - Missing store URI.");
            finish();
            return;
        }



        AlertDialog.Builder builder;
        Context context = this;
        builder = new AlertDialog.Builder(context);

        if (intent.getStringExtra(RateAppAction.TITLE_KEY) != null) {
            builder.setTitle(intent.getStringExtra(RateAppAction.TITLE_KEY));
        } else {
            String title = context.getString(R.string.ua_rate_app_action_default_title, getAppName());
            builder.setTitle(title);
        }

        if (intent.getStringExtra(RateAppAction.BODY_KEY) != null) {
            builder.setMessage(intent.getStringExtra(RateAppAction.BODY_KEY));
        } else {
            String positiveButtonTitle = context.getString(R.string.ua_rate_app_action_default_rate_positive_button);
            String body = context.getString(R.string.ua_rate_app_action_default_body, positiveButtonTitle);
            builder.setMessage(body);
        }

        builder.setPositiveButton(
                context.getString(R.string.ua_rate_app_action_default_rate_positive_button),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            Intent openLinkIntent = new Intent(Intent.ACTION_VIEW, storeUri);
                            startActivity(openLinkIntent);
                        } catch (ActivityNotFoundException e) {
                            Logger.error("No web browser available to handle request to open the store link.", e);
                        }

                        dialog.cancel();
                        finish();
                    }
                });


        builder.setNegativeButton(
                context.getString(R.string.ua_rate_app_action_default_rate_negative_button),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        finish();
                    }
                });

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.cancel();
                finish();
            }
        });

        dialog = builder.create();
        dialog.setCancelable(true);
        dialog.show();
    }

    /**
     * Relaunches the activity.
     *
     * @param uri The URI of the intent.
     * @param extras The extras bundle.
     */
    private void restartActivity(Uri uri, Bundle extras) {
        Logger.debug("Relaunching activity");

        finish();

        Intent restartIntent = new Intent()
                .setClass(this, this.getClass())
                .setData(uri)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (extras != null) {
            restartIntent.putExtras(extras);
        }

        this.startActivity(restartIntent);
    }

    private String getAppName() {
        String packageName = UAirship.getApplicationContext().getPackageName();
        PackageManager packageManager = UAirship.getApplicationContext().getPackageManager();

        try {
            ApplicationInfo info = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            String appName = (String)packageManager.getApplicationLabel(info);
            return appName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }
}