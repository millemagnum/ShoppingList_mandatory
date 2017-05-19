package org.projects.shoppinglist;

/**
 * Created by camil on 04-05-2017.
 */
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// var MainActivity
public class EmailPasswordActivity extends BaseActivity implements View.OnClickListener {

    // laver string der hedder tag, der indeholder EmailPassword
    private static final String TAG = "EmailPassword";

    private TextView mTitleTextView;
    private TextView mStatusTextView;
    private TextView mDetailTextView;
    private EditText mEmailField;
    private EditText mPasswordField;


    // firebase authentication
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // sætter layoutet til activity
        setContentView(R.layout.activity_emailpassword);

        //getActionBar().setDisplayHomeAsUpEnabled(true);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        // finder textviews og edittexts
        mTitleTextView = (TextView) findViewById(R.id.titleText);
        mStatusTextView = (TextView) findViewById(R.id.status);
        mDetailTextView = (TextView) findViewById(R.id.detail);
        mEmailField = (EditText) findViewById(R.id.fieldEmail);
        mPasswordField = (EditText) findViewById(R.id.fieldPassword);

        // finder knapper og click listeners
        findViewById(R.id.emailSigninButton).setOnClickListener(this);
        findViewById(R.id.createAccountButton).setOnClickListener(this);
        findViewById(R.id.signOutButton).setOnClickListener(this);
        findViewById(R.id.verifyEmailButton).setOnClickListener(this);

        // får den delte instans af FirebaseAuth objektet
        mAuth = FirebaseAuth.getInstance();

    } // onCreate slutter


    @Override
    public void onStart() {
        super.onStart();

        // tjekker om brugeren er logget ind og opdatere UI
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        updateUI(currentUser);


//        if (user != null) {
//            // brugeren er logget ind
//            String name = user.getDisplayName();
//            String email = user.getEmail();
//            Uri photoUrl = user.getPhotoUrl();
//
//            // tjek om brugerens email er bekræftet
//            boolean emailVerified = user.isEmailVerified();
//
//            // brugerens id er unik til Firebase projektet
//            // skal ikke bruges til at authenticate med backend server
//            String uid = user.getUid();
//
//
//        } else {
//            // ingen bruger er logget ind
//        }





    } // onStart slutter


    // Opretter bruger med email og password
    private void createAccount(String email, String password) {
        Log.d(TAG, "createAccount:" + email);
        if (!validateForm()) {
            return;
        }

       // showProgessDialog();

        // click event listener, der tjekker om brugeren blev oprettet successfuldt og opdatere UI herefter, hvis ikke så kommer der en toast
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // login succes, opdaterer UI med brugerens info
                            Log.d(TAG, "createUserWithEmail:succes");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // hvis sign in fejler, vis en besked til brugeren
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(EmailPasswordActivity.this, "Error creating user",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                       // hideProgessDialog();
                    }
                });

    } // create account slutter

    // metode til at logge ind
    private void signIn(String email, String password) {
        Log.d(TAG, "signIn:" + email);
        if(!validateForm()) {
            return;
        }

        showProgressDialog();

        // click listener - ligesom ved createAccount - tjekker om det gik som planlagt, ellers vises toast
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // login success, opdater UI med brugerens info
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // hvis login fejler, vis en besked til brugeren
                            Log.w(TAG, "signinWithEmail:failure", task.getException());
                            Toast.makeText(EmailPasswordActivity.this, "Error signing in, try again",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                        // hvis det ikke lykkedes af en anden grund, skal teksten ændres
                        if (!task.isSuccessful()) {
                            mStatusTextView.setText(R.string.auth_failed);
                        }
                      // hideProgessDialog();
                    }
                });



    } // signIn metode slutter

    // metode for at logge ud
    private void signOut() {

        // logger brugeren ud
        mAuth.signOut();
        updateUI(null);
    } // signout slutter

    // metode for at sende bekræftelsesmail til brugeren
    private void sendEmailVerification() {

        // disabler verify knappen
        findViewById(R.id.verifyEmailButton).setEnabled(false);

        // sender bekræftelses email
        final FirebaseUser user = mAuth.getCurrentUser();
        user.sendEmailVerification()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        // enabler knappen igen
                        findViewById(R.id.verifyEmailButton).setEnabled(true);

                        if(task.isSuccessful()) {
                            // viser toast, hvis det lykkedes
                            Toast.makeText(EmailPasswordActivity.this,
                                    "Verification mail sent to " + user.getEmail(),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // viser en toast, hvis det fejlede
                            Log.e(TAG, "sendEmailVerification", task.getException());
                            Toast.makeText(EmailPasswordActivity.this,
                                    "Failed to send verfication mail.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });


    } // slutter sendEmailVerification


    // metode for at validerer formularen (login)
    private boolean validateForm() {

        // laver boolean
        boolean valid = true;

        // sætter email til at være den indtastede email
        String email = mEmailField.getText().toString();

        // hvis det ikke er udfyldt, så skal den vise fejl
        if (TextUtils.isEmpty(email)) {
            mEmailField.setError("Required");
            valid = false;
        } else {
            mEmailField.setError(null);
        }

        // sætter password til at være det indtastede password
        String password = mPasswordField.getText().toString();

        // hvis det ikke er udfyldt, skal den vise fejl
        if (TextUtils.isEmpty(password)) {
            mPasswordField.setError("Required");
            valid = false;
        } else {
            mPasswordField.setError(null);
        }

        return valid;
    } // validateForm slutter


    // metode til at opdatere UI med bruger informationer, så brugeren ved om han/hun er logget ind eller ej
    private void updateUI(FirebaseUser user) {

        hideProgressDialog();

        if (user != null) {
            // hvis brugeren er logget ind, sættes teksten til textviews - som viser emailen man er logget ind med og user ID fra Firebase
            mTitleTextView.setText("You are now logged in as:");
            mStatusTextView.setText("Email: " + user.getEmail());
            mDetailTextView.setText("Firebase User ID: " + user.getUid());

            // får inputfelterne for email og password samt "signin" og "create account" knapper til at forsvinde
            findViewById(R.id.emailPasswordButtons).setVisibility(View.GONE);
            findViewById(R.id.EmailPasswordFields).setVisibility(View.GONE);
            findViewById(R.id.emailSigninButton).setVisibility(View.VISIBLE);

            findViewById(R.id.verifyEmailButton).setEnabled(!user.isEmailVerified());
            findViewById(R.id.verifyEmailButton).setVisibility(View.VISIBLE);
            findViewById(R.id.signOutButton).setVisibility(View.VISIBLE);

            // fortsæt - https://github.com/firebase/quickstart-android/blob/master/auth/app/src/main/java/com/google/firebase/quickstart/auth/EmailPasswordActivity.java#L123-L147

        } else {

            // brugeren er ikke logget ind/logget ud, så derfor ændres teksten - inputfelterne + knapper bliver igen synlige

            mTitleTextView.setText("You are not logged in - please login below");
            mStatusTextView.setText(R.string.signed_out);
            mDetailTextView.setText(null);

            findViewById(R.id.emailPasswordButtons).setVisibility(View.VISIBLE);
            findViewById(R.id.EmailPasswordFields).setVisibility(View.VISIBLE);
            findViewById(R.id.emailSigninButton).setVisibility(View.VISIBLE);
            findViewById(R.id.verifyEmailButton).setVisibility(View.GONE);
            findViewById(R.id.signOutButton).setVisibility(View.GONE);
        }
    } // updateUI slutter




    // denne metode kan bruges, da jeg implementerer View.OnClickListener
    @Override
    public void onClick(View view) {

        int i = view.getId();

        // hvis der klikkes på create account knappen, kaldes metoden createAccount og opretter en bruger
        if (i == R.id.createAccountButton) {
            createAccount(mEmailField.getText().toString(), mPasswordField.getText().toString());

        // hvis der trykkes på signin, kaldes metoden Signin, der logger brugeren ind
        } else if (i == R.id.emailSigninButton) {
            signIn(mEmailField.getText().toString(), mPasswordField.getText().toString());

        // hvis der trykkes på signout, køres metoden, der logger brugeren ud
        } else if (i == R.id.signOutButton) {
            signOut();

        // hvis der trykkes på verify, køres metoden, der sender en bekræftelsesmail til brugeren
        } else if (i == R.id.verifyEmailButton) {
            sendEmailVerification();
        }

    } // Onclick slutter


}
