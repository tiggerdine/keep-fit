package com.example.keepfit;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.keepfit.db.AppDatabase;
import com.example.keepfit.db.entity.Day;
import com.example.keepfit.db.entity.Goal;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.pnikosis.materialishprogress.ProgressWheel;

import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import nl.dionsegijn.konfetti.KonfettiView;
import nl.dionsegijn.konfetti.models.Shape;
import nl.dionsegijn.konfetti.models.Size;

public class StatusFragment extends Fragment {

    private static StatusFragment instance = null;

    private TextView statusTv;
    private TextView progressTextView;
    private KonfettiView viewKonfetti;
    private Spinner spinner;
    private ProgressWheel wheel;

    private AppDatabase db;
    private List<Goal> goals;
    private ArrayAdapter spinnerAdapter;
    private Goal activeGoal;

    static StatusFragment getInstance() {
        return instance;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_status, container, false);

        // Get the database.
        db = AppDatabase.getAppDatabase(getContext());

        // Query the database for goals.
        goals = db.goalDao().loadAllVisibleGoals();

        // Find the spinner.
        spinner = view.findViewById(R.id.spinner);

        // Create an adapter.
        spinnerAdapter = new ArrayAdapter<>(getContext(), R.layout.goal_spinner_item, goals);

        // Connect the spinner and the adapter.
        spinner.setAdapter(spinnerAdapter);

        // When the user selects an goal...
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected goal.
                Goal selectedGoal = (Goal) spinner.getSelectedItem();

                // Update the active goal.
                SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt(getString(R.string.active_goal_id_key), selectedGoal.goalId);
                editor.apply();
//                Log.v("StatusFragment", "putting id " + activeGoal.goalId);

                // Update today's entry in the database.
                Day today = today();
                today.goalId = selectedGoal.goalId;
                db.dayDao().update(today);

                // Refresh the views.
                refresh();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing.
            }
        });

        // Update the active goal.
        activeGoal = (Goal) spinner.getSelectedItem();

        // Find the floating action button.
        FloatingActionButton fab = view.findViewById(R.id.fab);

        // When the user clicks it...
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ... let them add steps.
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.dialog_steps, null);
                TextView tv = dialogView.findViewById(R.id.dialog_steps_tv);
                tv.setText("How many steps?");
                final EditText et = dialogView.findViewById(R.id.et);
                builder.setView(dialogView)
                        .setPositiveButton("Add Steps", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                Keyboard.hide(getContext());
                                int steps = Integer.parseInt(et.getText().toString());
                                if(steps > 0) {
                                    record(steps);
                                }
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                Keyboard.hide(getContext());
                            }
                        });
                final AlertDialog alertDialog = builder.create();

                // Map the enter button of the keyboard to the positive button of the dialog.
                alertDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (event.getAction() == KeyEvent.ACTION_DOWN &&
                                      keyCode == KeyEvent.KEYCODE_ENTER) {
                            alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                            return true;
                        }
                        return false;
                    }
                });

                alertDialog.show();
                Keyboard.show(getContext());
            }
        });

        // Find the views that get refreshed...
        progressTextView = view.findViewById(R.id.progress_text_view);
        statusTv = view.findViewById(R.id.status_tv);
        wheel = view.findViewById(R.id.progress_wheel);
        viewKonfetti = view.findViewById(R.id.viewKonfetti);

        // ... and refresh them.
        refresh();

        return view;
    }

    /**
     * Records some activity.
     *
     * @param steps the number of steps
     */
    public void record(int steps) {
        Date date = Utils.getDay();
        Day today = db.dayDao().findDayWithDate(date);
        boolean armNotification = false;
        boolean armConfetti = false;
        if (today.steps < (float) activeGoal.steps / 2) {
            armNotification = true;
        }
        if (today.steps < activeGoal.steps) {
            armConfetti = true;
        }
        if (today == null) {
            db.dayDao().insert(new Day(date, steps));
        } else {
            today.steps += steps;
            db.dayDao().update(today);
        }
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        Boolean notifications = sharedPrefs.getBoolean(getString(R.string.settings_notifications_key), false);
        if (notifications && armNotification && today.steps >= (float) activeGoal.steps / 2) {
            showNotification();
//            Toast.makeText(getContext(), "50%", Toast.LENGTH_SHORT).show();
        }
        if (armConfetti && today.steps >= activeGoal.steps) {
            fireConfetti();
        }
        refresh();
    }

    /**
     * Shows a notification.
     */
    private void showNotification() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("0", name, importance);
            channel.setDescription(description);
            // Enable vibration to make the notification heads-up.
            channel.enableVibration(true);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getActivity().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), "0")
                .setSmallIcon(R.drawable.ic_walk)
                .setContentTitle(getText(R.string.notification_title))
                .setContentText(getText(R.string.notification_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // Set vibrate default to make the notification heads-up.
                .setDefaults(Notification.DEFAULT_VIBRATE);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getContext());

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(0, builder.build());
    }

    /**
     * Fires some celebratory confetti.
     */
    private void fireConfetti() {
        viewKonfetti.build()
                .addColors(0xa864fd, 0x29cdff, 0x78ff44, 0xff718d, 0xfdff6a)
                .setDirection(0.0, 359.0)
                .setSpeed(1f, 5f)
                .setFadeOutEnabled(true)
                .setTimeToLive(2000L)
                .addShapes(Shape.RECT, Shape.CIRCLE)
                .addSizes(new Size(10, 5))
                .setPosition(-50f, viewKonfetti.getWidth() + 50f,
                             -50f, -50f)
                .streamFor(300, 5000L);
    }

    /**
     * Tries to find today's entry in the database.
     *
     * @return today's entry in the database or null
     */
    private Day today() {
        Date date = Utils.getDay();
        Day today = db.dayDao().findDayWithDate(date);
        if (today == null) {
            db.dayDao().insert(new Day(date, 0));
        }
        return db.dayDao().findDayWithDate(date);
    }

    /**
     * Queries the database for goals and refreshes the views.
     */
    void refresh() {
        goals.clear();

        // Query the database for goals.
        goals.addAll(db.goalDao().loadAllVisibleGoals());

        // Notify the adapter.
        spinnerAdapter.notifyDataSetChanged();

        // Get the id of the active goal.
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        int activeGoalId = sharedPref.getInt(getString(R.string.active_goal_id_key), 0);
//        Log.v("StatusFragment", "activeGoalId = " + activeGoalId);

        // Get the active goal.
        activeGoal = db.goalDao().findGoalWithId(activeGoalId);

        // If there is no active goal ...
        if (activeGoal == null) {

            // ... say so.
            statusTv.setText(getString(R.string.no_goals));

        // If there is an active goal...
        } else {

            // ... select it.
            spinner.setSelection(spinnerAdapter.getPosition(activeGoal));

            // Get today's date.
            Date date = Utils.getDay();

            // Try to find today's entry in the database.
            Day today = db.dayDao().findDayWithDate(date);

            int steps;

            // If there is no such entry...
            if (today == null) {
                steps = 0;

            // If there is...
            } else {
                steps = today.steps;
            }

            // Calculate the user's progress...
            float progress = (float) steps / activeGoal.steps;

            // ... and cap it at 100%.
            if (progress >= 1) {
                progress = 1;
            }

            // Refresh the views.
            String statusText = steps + "/" + activeGoal.steps;
            statusTv.setText(statusText);
            String progressText = (int) (progress * 100) + "%";
            progressTextView.setText(progressText);
            setBarColor(progress);
            wheel.setProgress(progress);
        }
    }

    /**
     * Sets the colour of the progress wheel based on the user's progress.
     *
     * @param progress the user's progress
     */
    private void setBarColor(float progress) {
        switch ((int) Math.floor(progress * 10)) {
            case 0:
                wheel.setBarColor(getResources().getColor(R.color.colorGradient1));
                break;
            case 1:
                wheel.setBarColor(getResources().getColor(R.color.colorGradient2));
                break;
            case 2:
                wheel.setBarColor(getResources().getColor(R.color.colorGradient3));
                break;
            case 3:
                wheel.setBarColor(getResources().getColor(R.color.colorGradient4));
                break;
            case 4:
                wheel.setBarColor(getResources().getColor(R.color.colorGradient5));
                break;
            case 5:
                wheel.setBarColor(getResources().getColor(R.color.colorGradient6));
                break;
            case 6:
                wheel.setBarColor(getResources().getColor(R.color.colorGradient7));
                break;
            case 7:
                wheel.setBarColor(getResources().getColor(R.color.colorGradient8));
                break;
            case 8:
                wheel.setBarColor(getResources().getColor(R.color.colorGradient9));
                break;
            default:
                wheel.setBarColor(getResources().getColor(R.color.colorGradient10));
                break;
        }
    }
}
