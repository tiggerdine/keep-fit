package com.example.keepfit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.example.keepfit.db.AppDatabase;
import com.example.keepfit.db.entity.Goal;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import androidx.fragment.app.Fragment;

public class GoalFragment extends Fragment {

    private AppDatabase db;
    private List<Goal> goals;
    private InputMethodManager imm;
    private View view;
    private GoalAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_goal, container, false);

        db = AppDatabase.getAppDatabase(getContext());
        populate(db);
        goals = db.goalDao().loadAllVisibleGoals();

        adapter = new GoalAdapter(getActivity(), goals);

        imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        createListView();

        createFloatingActionButton();

        return view;
    }

    private void createFloatingActionButton() {
        FloatingActionButton fab = view.findViewById(R.id.goal_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View view1 = inflater.inflate(R.layout.dialog_goal, null);
                TextView goalTv = view1.findViewById(R.id.goal_tv);
                goalTv.setText("New Goal");
                final EditText nameEt = view1.findViewById(R.id.goal_name_et);
                final EditText stepsEt = view1.findViewById(R.id.goal_steps_et);
                builder.setView(view1)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // hide keyboard
                                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                                Goal goal = new Goal(nameEt.getText().toString(), Integer.parseInt(stepsEt.getText().toString()));
                                db.goalDao().insert(goal);
                                adapter.clear();
                                adapter.addAll(db.goalDao().loadAllGoals());
                                adapter.notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // hide keyboard
                                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                            }
                        });
                final AlertDialog alertDialog = builder.create();

                // TODO this is meant to control the behaviour of pressing enter on keyboard
                alertDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        // TODO this doesn't work right :(
                        // Log.v("GoalFragment", "keyCode" + keyCode + ", name has focus? " + nameEt.hasFocus() + ", steps has focus? " + stepsEt.hasFocus());
                        if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER && stepsEt.hasFocus()) {
                            alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                            return true;
                        }

                        return false;
                    }
                });
                alertDialog.show();
                nameEt.requestFocus();
                // show keyboard
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }
        });
    }

    private void createListView() {
        final ListView listView = view.findViewById(R.id.list);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                final Goal clickedGoal = (Goal) adapterView.getItemAtPosition(position);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View view1 = inflater.inflate(R.layout.dialog_goal, null);
                TextView goalTv = view1.findViewById(R.id.goal_tv);
                goalTv.setText("Edit Goal");
                final EditText nameEt = view1.findViewById(R.id.goal_name_et);
                nameEt.setText(clickedGoal.name);
                final EditText stepsEt = view1.findViewById(R.id.goal_steps_et);
                stepsEt.setText("" + clickedGoal.steps);
                builder.setView(view1)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // hide keyboard
                                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                                clickedGoal.name = nameEt.getText().toString();
                                clickedGoal.steps = Integer.parseInt(stepsEt.getText().toString());
                                db.goalDao().update(clickedGoal);
                                adapter.clear();
                                adapter.addAll(db.goalDao().loadAllGoals());
                                adapter.notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // hide keyboard
                                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                            }
                        });
                final AlertDialog alertDialog = builder.create();
                // TODO keyboard stuff
                alertDialog.show();
            }
        });
    }

    private void populate(AppDatabase db) {
        db.goalDao().nuke();
        Goal goal1 = new Goal("Goal 1", 10000);
        Goal goal2 = new Goal("Goal 2", 8000);
        Goal goal3 = new Goal("Goal 3", 12500);
        Goal goal4 = new Goal("Goal 4", 6500);
        db.goalDao().insert(goal1);
        db.goalDao().insert(goal2);
        db.goalDao().insert(goal3);
        db.goalDao().insert(goal4);
    }
}
