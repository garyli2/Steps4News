package com.steps4news;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.steps4news.services.PlayNewsService;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    SensorManager sensorManager;
    Sensor sSensor;
    TextView stepCounter;


    private FirebaseAuth mAuth;
    private Timestamp startTime;
    static int RC_SIGN_IN = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // initialize firebase auth
        mAuth = FirebaseAuth.getInstance();
        boolean isSignedIn = mAuth.getCurrentUser() != null;

        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(), new AuthUI.IdpConfig.AnonymousBuilder().build());

        if (!isSignedIn) {
            // send them to the sign in page
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .setTheme(R.style.LoginTheme)
                            .setLogo(R.drawable.appicon)
                            .build(),
                    RC_SIGN_IN);
        } else {
            showHomePage();

        }
    }

    // When the sign-in flow is complete, you will receive the result in onActivityResult:
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                // show the actual app
                showHomePage();
            }
        }
    }

    private final SensorEventListener mListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
                Log.e("MainActivity", "SENSOR ACTIVE");
                if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {

                if (TrackData.getInstance().initialCounterStepsWhenRegistered < 1) {
                    // initial value
                    TrackData.getInstance().initialCounterStepsWhenRegistered = (int) event.values[0];
                }

                    TrackData.getInstance().mSteps = (int) event.values[0] - TrackData.getInstance().initialCounterStepsWhenRegistered;

                // do not let user rotate app under any circumstances
                    TextView stepCounter = findViewById(R.id.stepcounter);
                    if (stepCounter != null) { // in case the user clicks away while its updating steps
                        stepCounter.setText(""+TrackData.getInstance().mSteps);
                    }

            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    void showHomePage() {
        stepCounter = findViewById(R.id.stepcounter);

        // check if they have necessary permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions( this, new String[] {  Manifest.permission.ACTIVITY_RECOGNITION  },
                    1 );
        }
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_track, R.id.navigation_data, R.id.navigation_settings)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);


    }

    public void onActionButtonClick(View view) {
        String searchTerm = ((EditText)findViewById(R.id.newsSearchTerm)).getText().toString();
        TextView workoutNotStarted = findViewById(R.id.workoutNotStarted);

        Log.e("MainActivity", "Just at workoutnotstarted, "+workoutNotStarted);
        if (((Button)view.findViewById(R.id.button)).getText().equals("Start Workout and Play News")) {
            Log.e("MainActivity", "in starting block");
            startTrackingStepsAndShowData();
            // start tracking
            startTime = Timestamp.now(); // update start time instance variable

            Intent it = new Intent(getApplicationContext(), PlayNewsService.class);
            it.putExtra("searchTerm", searchTerm);
            TrackData.getInstance().isWorkoutActive = true;
            startService(it);
            // set text to stop
            ((Button)view.findViewById(R.id.button)).setText("Stop Workout and Stop News");
            workoutNotStarted.setVisibility(View.INVISIBLE);
        } else {
            // user clicks on stop button
            Log.e("MainActivity", "in stopping block");
            stopService(new Intent(this, PlayNewsService.class));
            TrackData.getInstance().isWorkoutActive = false;
            recordStepsToDatabase();

            // set back to start tracking, UI changes
            ((Button)view.findViewById(R.id.button)).setText("Start Workout and Play News");
            workoutNotStarted.setVisibility(View.VISIBLE);
        }

    }

    void recordStepsToDatabase() {
        final String uid = mAuth.getCurrentUser().getUid();
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        final Timestamp endTime = Timestamp.now();

        // field that we want to overwrite

        db.collection("users").document(uid).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Long steps = (Long) documentSnapshot.get("total_steps");
                long longSteps = steps.longValue();
                longSteps += (int) TrackData.getInstance().mSteps;
                Log.e("MainActivity", "Previous: "+steps+", newTotal: "+longSteps);
                final Map<String, Object> newTotalSteps = new HashMap<>();
                newTotalSteps.put("total_steps", longSteps);

                db.collection("users").document(uid).set(newTotalSteps, SetOptions.merge()).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.e("MainActivity", "total steps data uploaded!");
                    }
                });

                final Map<String, Object> sessionData = new HashMap<>();
                sessionData.put("steps", TrackData.getInstance().mSteps);
                sessionData.put("start", startTime);
                sessionData.put("end", endTime);

                db.collection("users").document(uid).collection("sessions").document().set(sessionData).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getApplicationContext(),"Sessions Data Uploaded!",Toast.LENGTH_SHORT).show();
                        Log.e("MainActivity", "sessions data uploaded!");
                    }
                });

                // after we get all the necessary step data, then we clear
                stopTrackingStepsAndHideData();
            }
        });
    }

    void startTrackingStepsAndShowData() {
        ((TextView)findViewById(R.id.stepcounter)).setVisibility(View.VISIBLE);
        ((ImageView)findViewById(R.id.imageView)).setVisibility(View.VISIBLE);
        ((ImageView)findViewById(R.id.imageView)).setVisibility(View.VISIBLE);

        sensorManager = (SensorManager) getSystemService(Activity.SENSOR_SERVICE);
        sSensor= sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        final boolean batchMode = sensorManager.registerListener(mListener, sSensor, SensorManager.SENSOR_DELAY_FASTEST, 0);
    }

    void stopTrackingStepsAndHideData() {
        TrackData.getInstance().mSteps = 0;
        TrackData.getInstance().initialCounterStepsWhenRegistered = 0;
        ((TextView)findViewById(R.id.stepcounter)).setVisibility(View.INVISIBLE);
        ((ImageView)findViewById(R.id.imageView)).setVisibility(View.INVISIBLE);
        sensorManager.unregisterListener(mListener);
    }
}