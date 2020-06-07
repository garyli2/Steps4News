package com.steps4news.ui.track;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.steps4news.R;
import com.steps4news.TrackData;
import com.steps4news.services.PlayNewsService;

public class TrackFragment extends Fragment {
    private TrackViewModel trackViewModel;

    public TrackFragment() {}

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        trackViewModel = ViewModelProviders.of(this).get(TrackViewModel.class);
        View root = inflater.inflate(R.layout.fragment_track, container, false);

        Log.e("TrackFragment", "In track fragment oncreateview");

        // determine state of our app
        if (TrackData.getInstance().isWorkoutActive) {
            Log.e("TrackFragment", "workout rn. global var mSteps: "+TrackData.getInstance().mSteps);
            ((TextView)(root.findViewById(R.id.stepcounter))).setVisibility(View.VISIBLE);
            ((TextView)(root.findViewById(R.id.stepcounter))).setText(""+ TrackData.getInstance().mSteps);
            ((ImageView)(root.findViewById(R.id.imageView))).setVisibility(View.VISIBLE);
            root.findViewById(R.id.workoutNotStarted).setVisibility(View.INVISIBLE);
            ((Button)(root.findViewById(R.id.button))).setText("Stop Workout and Stop News");
        } else {
            Log.e("TrackFragment", "no workout. global var mSteps: "+TrackData.getInstance().mSteps);
            ((TextView)(root.findViewById(R.id.stepcounter))).setVisibility(View.INVISIBLE);
            ((ImageView)(root.findViewById(R.id.imageView))).setVisibility(View.INVISIBLE);
            ((Button)(root.findViewById(R.id.button))).setText("Start Workout and Play News");
        }

        return root;
    }





}