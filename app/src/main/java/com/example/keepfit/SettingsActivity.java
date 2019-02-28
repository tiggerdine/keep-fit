package com.example.keepfit;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.keepfit.db.AppDatabase;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
    }

    public static class FitPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings_main);
            Preference clearHistoryButton = findPreference(getString(R.string.settings_clear_history_key));
            clearHistoryButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    TextView confirmTextView = new TextView(getActivity());
                    confirmTextView.setText("Clear history?");
                    builder.setView(confirmTextView)
                            .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AppDatabase db = AppDatabase.getAppDatabase(getActivity());
                            db.dayDao().nuke();
                            // TODO delete invisible goals
                        }
                    })
                            .setNegativeButton("Cancel", null);
                    builder.show();
                    return true;
                }
            });
        }
    }
}