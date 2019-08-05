package com.eemf.sirgoingfar.travelmantics.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.eemf.sirgoingfar.travelmantics.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private Button mEmailSignInButton;
    private TextView mOnboardingSwitcher;
    private CheckBox mPasswordToggler;

    private boolean isRegistration;
    private int MIN_PASSWORD_LENGTH = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Set up the login form.
        mEmailView = findViewById(R.id.et_email);
        mPasswordView = findViewById(R.id.et_password);
        mEmailSignInButton = findViewById(R.id.btn_sign_in_or_register);
        mEmailSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isRegistration)
                    attemptSignUp();
                else
                    attemptSignIn();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
        mOnboardingSwitcher = findViewById(R.id.tv_sign_in_or_register);
        mOnboardingSwitcher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRegistration = !isRegistration;
                setupScreen();
            }
        });

        mPasswordToggler = findViewById(R.id.cb_password_toggle);
        mPasswordToggler.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mPasswordToggler.setText(isChecked ? getString(R.string.keyword_hide) : getString(R.string.keyword_show));

                //change the type of the input field
                mPasswordView.setInputType(isChecked ? InputType.TYPE_TEXT_VARIATION_PASSWORD :
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                mPasswordView.setSelection(mPasswordView.getText().toString().length());
            }
        });

        //instantiate the FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        isRegistration = false;
    }

    private void setupScreen() {
        //change view contents - Registration
        if(isRegistration) {
            mEmailSignInButton.setText(getString(R.string.sign_up));
            mOnboardingSwitcher.setText(R.string.or_sign_in);

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null)
                actionBar.setTitle(R.string.sign_up);
        }
        //change view contents - Sign in
        else {
            mEmailSignInButton.setText(getString(R.string.sign_in));
            mOnboardingSwitcher.setText(R.string.or_sign_up);

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null)
                actionBar.setTitle(R.string.sign_in);
        }
    }

    /**
     * Attempts to sign into the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptSignIn() {

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        final String email = mEmailView.getText().toString().trim();
        final String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        } else if(password == null || password.isEmpty()){
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off the registration
            // perform the user login attempt.
            showProgress(true);
            mExecutor.networkIO().execute(new Runnable() {
                @Override
                public void run() {
                    signInUser(email, password);
                }
            });
        }
    }

    private void signInUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    //prepare local Db for new sign in
                    fetchUserDocumentFromFirebase();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mPasswordView.setText("");
                            showProgress(false);
                        }
                    });
                }
                else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mPasswordView.setText("");
                            showProgress(false);
                            Snackbar.make(mEmailView, getString(R.string.login_error_message), Snackbar.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    /**
     * Attempts to register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptSignUp() {
        //check if there's an internet connection
        if(!NetworkUtils.isOnline(this)){
            Snackbar.make(mPasswordView, R.string.poor_connectivitiy, Snackbar.LENGTH_LONG).show();
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        final String email = mEmailView.getText().toString().trim();
        final String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if(TextUtils.isEmpty(password)){
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        } else if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off the registration
            // perform the user login attempt.
            showProgress(true);
            mExecutor.networkIO().execute(new Runnable() {
                @Override
                public void run() {
                    createUserOnFirebase(email, password);
                }
            });
        }
    }

    private void createUserOnFirebase(String email, String password) {

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull final Task<AuthResult> task) {
                if(task.isSuccessful())
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            processResponse(task.isSuccessful(), null);
                        }
                    });
                else {
                    if(task.getException() instanceof FirebaseAuthUserCollisionException)
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                processResponse(task.isSuccessful(), getString(R.string.email_already_registered));
                            }
                        });
                    else
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                processResponse(false, null);
                            }
                        });
                }
            }
        });
    }

    private void processResponse(boolean success, String response){
        if (success) {

            //navigate to the Catalog Activity
            Intent intent = new Intent(LoginActivity.this, CatalogActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

            showProgress(false);
        } else {
            showProgress(false);

            if (response == null)
                Snackbar.make(mPasswordView,(getString(R.string.pls_retry)),Snackbar.LENGTH_LONG).show();
            else
                Snackbar.make(mPasswordView,response,Snackbar.LENGTH_LONG).show();

            mLoginFormView.setVisibility(View.VISIBLE);
            mPasswordView.setText("");
        }
    }

    private void fetchUserDocumentFromFirebase() {
        if(!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, getString(R.string.poor_connectivitiy),Toast.LENGTH_LONG).show();
            return;
        }

        //Fetch user data and populate the db using Service
        AppExecutors.getInstance().networkIO().execute(new Runnable() {
            @Override
            public void run() {
                new FirebaseTransactionTasks().execute(
                        LoginActivity.this,
                        FirebaseTransactionTasks.START_CATALOG_ACTIVITY,
                        null);
            }
        });
    }

    private boolean isEmailValid(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPasswordValid(String password) {
        if(isRegistration)
            return password.length() >= MIN_PASSWORD_LENGTH;
        else
            return password != null && !password.isEmpty();
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }

        //set ActionBar title as appropriate
        ActionBar bar = getSupportActionBar();
        if(bar != null){
            if(show){
                if(isRegistration)
                    bar.setTitle(R.string.signing_up);
                else
                    bar.setTitle(R.string.signing_in);
            }else {
                if(isRegistration)
                    bar.setTitle(getString(R.string.sign_up));
                else
                    bar.setTitle(getString(R.string.sign_in));
            }
        }
    }


    /*
     * on @value KEYCODE_BACK is pressed, @link onKeyDown(keyCode, event) is called
     * @param keyCode is the key code for the key pressed
     * @param event is the key event
     * @return true or @return the event*/

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        switch(keyCode){
            case KeyEvent.KEYCODE_BACK:
                Intent minimize = new Intent(Intent.ACTION_MAIN);
                minimize.addCategory(Intent.CATEGORY_HOME);
                minimize.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(minimize);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
