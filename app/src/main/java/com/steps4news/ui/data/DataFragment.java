package com.steps4news.ui.data;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.steps4news.CustomAdapter;
import com.steps4news.R;

import java.util.ArrayList;
import java.util.List;

public class DataFragment extends Fragment {
    String TAG = "DataFragment";


    private DataViewModel dataViewModel;
    private TextView totalSteps;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dataViewModel =
                ViewModelProviders.of(this).get(DataViewModel.class);
        final View root = inflater.inflate(R.layout.fragment_data, container, false);
        totalSteps = root.findViewById(R.id.total_steps_counter);

        return root;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        Log.e(TAG, "IN DAYA FRAGMENT");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users").document(uid).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                // set the total steps textview
                totalSteps.setText(totalSteps.getText()+""+(long)documentSnapshot.get("total_steps"));
            }
        });

        db.collection("users").document(uid).collection("sessions").orderBy("end", Query.Direction.DESCENDING).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    ArrayList<QueryDocumentSnapshot> data = new ArrayList<>();

                    // list of all sessions of this user
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        data.add(document);
                    }
                    if (data.size() != 0) {
                        CustomAdapter customAdapter = new CustomAdapter(getContext(), data);
                        if (getActivity() != null) { // needed
                            final ListView list = getActivity().findViewById(R.id.list);
                            if (list != null)
                                list.setAdapter(customAdapter);
                        }

                    }
                } else {
                    Log.d("DataFragment", "Error getting documents: ", task.getException());
                }
            }
        });

    }
}