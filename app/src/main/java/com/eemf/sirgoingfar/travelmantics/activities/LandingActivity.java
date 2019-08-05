package com.eemf.sirgoingfar.travelmantics.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.eemf.sirgoingfar.travelmantics.R;

public class LandingActivity extends AppCompatActivity {

    private Button mBtnOtherMailClient;
    private Button mBtnGoogleMailClient;
    private TextView mBtnSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        mBtnOtherMailClient = findViewById(R.id.btn_sign_in_email);
        mBtnGoogleMailClient = findViewById(R.id.btn_sign_in_google);
        mBtnSignUp = findViewById(R.id.tv_sign_up);

        mBtnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LandingActivity.this, SignUpActivity.class));
            }
        });

        mBtnOtherMailClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LandingActivity.this, SignInActivity.class);
                intent.putExtra(SignInActivity.KEY_SIGN_IN_TYPE, SignInActivity.TYPE_OTHER_MAIL_CLIENT);
                startActivity(intent);
            }
        });

        mBtnGoogleMailClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LandingActivity.this, SignInActivity.class);
                intent.putExtra(SignInActivity.KEY_SIGN_IN_TYPE, SignInActivity.TYPE_GOOGLE_MAIL_CLIENT);
                startActivity(intent);
            }
        });
    }
}
