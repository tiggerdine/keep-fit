package com.example.keepfit;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.pnikosis.materialishprogress.ProgressWheel;

public class StatusFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_status, container, false);

        FloatingActionButton fab = view.findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getContext(), "My milkshake brings all the boys to the yard.", Toast.LENGTH_SHORT).show();
            }
        });

        ProgressWheel wheel = (ProgressWheel) view.findViewById(R.id.progress_wheel);

        wheel.setProgress(0.7f);

        wheel.setBarColor(Color.parseColor("#78BE49"));

        return view;
    }
}
