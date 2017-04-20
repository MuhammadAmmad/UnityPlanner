package com.nbdeg.unityplanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.nbdeg.unityplanner.utils.Database;

import java.util.Arrays;

public class loginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 145;
    private RelativeLayout loginView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginView = (RelativeLayout) findViewById(R.id.activity_login);
        Button signInButton = (Button) findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

        // Checks if user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            // User is logged in - direct them to MainActivity
            startActivity(new Intent(loginActivity.this, MainActivity.class));
        } else {
            // User is not logged in - log them in.
            signIn();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            // Successfully signed in
            if (resultCode == ResultCodes.OK) {

                Database.refreshDatabase();

                //  Declare a new thread to do a preference check
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //  Initialize SharedPreferences
                        SharedPreferences getPrefs = PreferenceManager
                                .getDefaultSharedPreferences(getBaseContext());

                        //  Create a new boolean and preference and set it to true
                        boolean isFirstStart = getPrefs.getBoolean("firstStart", true);

                        //  If the activity has never started before...
                        if (isFirstStart) {

                            //  Launch app intro
                            Intent i = new Intent(loginActivity.this, IntroActivity.class);
                            startActivity(i);

                            //  Make a new preferences editor
                            SharedPreferences.Editor e = getPrefs.edit();

                            //  Edit preference to make it false because we don't want this to run again
                            e.putBoolean("firstStart", false);

                            //  Apply changes
                            e.apply();
                        }
                    }
                });

                // Start the thread
                t.start();

                FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.LOGIN, null);
                startActivity(new Intent(loginActivity.this, MainActivity.class));
                finish();
                return;
            } else {
                // Sign in failed
                if (response == null) {
                    // User pressed back button
                    return;
                }

                if (response.getErrorCode() == ErrorCodes.NO_NETWORK) {
                    showSnackbar(R.string.no_internet_connection);
                    return;
                }

                if (response.getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                    showSnackbar(R.string.unknown_error);
                    return;
                }
            }

            showSnackbar(R.string.unknown_error);
        }
    }

    private void signIn() {
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setProviders(Arrays.asList(
                                new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                        .setLogo(R.mipmap.ic_logo_white)
                        .setTheme(R.style.LoginTheme)
                        .build(),
                RC_SIGN_IN);
    }

    private void showSnackbar(int stringResource) {
        Snackbar snackbar = Snackbar.make(loginView, stringResource, Snackbar.LENGTH_LONG);
        snackbar.show();
    }
}
