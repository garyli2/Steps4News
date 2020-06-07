package com.steps4news.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.auth.User;
import com.steps4news.MainActivity;
import com.steps4news.R;

import java.util.Arrays;
import java.util.List;

import static com.firebase.ui.auth.AuthUI.getApplicationContext;

public class SettingsFragment extends Fragment {

    private SettingsViewModel dataViewModel;
    private EditText currentEmailInput, currentPasswordInput, newPasswordInput, newEmailInput;
    private Button reauthenticateButton, submitNewEmailButton, submitNewPasswordButton;
    private TextView authenticate_warning, password_prompt, email_prompt;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        dataViewModel = ViewModelProviders.of(this).get(SettingsViewModel.class);
        View root = inflater.inflate(R.layout.fragment_settings, container, false);
        //final TextView textView = root.findViewById(R.id.text_dashboard);
        final FirebaseUser curUser = FirebaseAuth.getInstance().getCurrentUser();

        currentEmailInput = (EditText)(root.findViewById(R.id.currentEmailInput));
        currentPasswordInput = (EditText)(root.findViewById(R.id.currentPasswordInput));
        newPasswordInput = (EditText)(root.findViewById(R.id.newPasswordInput));
        newEmailInput = (EditText)(root.findViewById(R.id.newEmailInput));
        submitNewEmailButton = (Button)(root.findViewById(R.id.submitNewEmail));
        submitNewPasswordButton = (Button)(root.findViewById(R.id.submitNewPassword));
        reauthenticateButton = (Button)(root.findViewById(R.id.reauthenticate_button));
        authenticate_warning = root.findViewById(R.id.authenticate_warning);
        password_prompt = root.findViewById(R.id.password_prompt);
        email_prompt = root.findViewById(R.id.email_prompt);

        if (!curUser.isAnonymous()) {
            root.findViewById(R.id.anonymousUserWarning).setVisibility(View.INVISIBLE);
            final Button reauthenticateButton = (Button) root.findViewById(R.id.reauthenticate_button);
            reauthenticateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e("SettingsFragment", "IN onclick!!");
                    String currentEmail = currentEmailInput.getText().toString();
                    String currentPassword = currentPasswordInput.getText().toString();

                    curUser.reauthenticate(EmailAuthProvider.getCredential(currentEmail, currentPassword))
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        // unlock the change password and change email buttons
                                        submitNewEmailButton.setEnabled(true);
                                        submitNewPasswordButton.setEnabled(true);
                                        reauthenticateButton.setEnabled(false);
                                        Toast.makeText(getContext(),"Re-authentication successful!",Toast.LENGTH_SHORT).show();
                                    } else {
                                        Log.d("SettingsFragment", "Error auth failed");
                                    }
                                }
                            });
                }
            });


            // -------- CHANGE PASSWORD ----------------------------
            // what happens if the submit new password button is pressed
            submitNewPasswordButton.setOnClickListener((new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e("SettingsFragment", "in submit new password onclick listener");
                    String newPassword = newPasswordInput.getText().toString();
                    curUser.updatePassword(newPassword).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(getContext(),"Password successfully changed!",Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("SettingsFragment", e.getMessage() + e.getCause());
                        }
                    });
                }
            }));


            // ----------- CHANGE EMAIL --------------------
            Button changeEmailButton = (Button) (root.findViewById(R.id.submitNewEmail));
            // what happens if the submit new password button is pressed
            changeEmailButton.setOnClickListener((new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String newEmail = newEmailInput.getText().toString();
                    curUser.updateEmail(newEmail).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(getContext(),"Email successfully changed!",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }));
        } else {
            // user is anonymous
            currentEmailInput.setVisibility(View.INVISIBLE);
            currentPasswordInput.setVisibility(View.INVISIBLE);
            newPasswordInput.setVisibility(View.INVISIBLE);
            newEmailInput.setVisibility(View.INVISIBLE);
            submitNewEmailButton.setVisibility(View.INVISIBLE);
            submitNewPasswordButton.setVisibility(View.INVISIBLE);
            reauthenticateButton.setVisibility(View.INVISIBLE);
            authenticate_warning.setVisibility(View.INVISIBLE);
            password_prompt.setVisibility(View.INVISIBLE);
            email_prompt.setVisibility(View.INVISIBLE);
        }

        // --------LOGOUT BUTTON
        Button logoutButton = (Button) root.findViewById(R.id.logout_button);

        logoutButton.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("SettingsFragment", "logging out");
                FirebaseAuth.getInstance().signOut();
                // Choose authentication providers
                List<AuthUI.IdpConfig> providers = Arrays.asList(
                        new AuthUI.IdpConfig.EmailBuilder().build(), new AuthUI.IdpConfig.AnonymousBuilder().build());
                // send them to the sign in page
                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setAvailableProviders(providers)
                                .setTheme(R.style.LoginTheme)
                                .setLogo(R.drawable.appicon)
                                .build(),
                        123);
                Toast.makeText(getContext(),"Logged out successfully",Toast.LENGTH_SHORT).show();
            }
        }));

        return root;
    }
}