package com.example.kurs;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserActivity extends AppCompatActivity {

    private Button[] expandableButtons = new Button[6];
    private LinearLayout[] expandableLayouts = new LinearLayout[6];
    private boolean[] isExpanded = new boolean[6];
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        for (int i = 1; i <= 6; i++) {
            int buttonId = getResources().getIdentifier("expandableButton" + i, "id", getPackageName());
            int layoutId = getResources().getIdentifier("expandableLayout" + i, "id", getPackageName());
            int additionalButtonId = getResources().getIdentifier("additionalButton" + i, "id", getPackageName());

            expandableButtons[i - 1] = findViewById(buttonId);
            expandableLayouts[i - 1] = findViewById(layoutId);

            final int index = i - 1;

            expandableButtons[index].setOnClickListener(view -> onExpandableButtonClick(index));

            Button additionalButton = findViewById(additionalButtonId);
            additionalButton.setOnClickListener(view -> showInputDialog(index));
        }

        // Инициализация кнопки помощи
        Button helpButton = findViewById(R.id.helpButton);
        helpButton.setOnClickListener(v -> showHelpDialog());
    }

    private void filterButtons(String query) {
        for (int i = 0; i < expandableButtons.length; i++) {
            String buttonText = expandableButtons[i].getText().toString().toLowerCase();
            if (buttonText.contains(query.toLowerCase())) {
                expandableButtons[i].setVisibility(View.VISIBLE);
            } else {
                expandableButtons[i].setVisibility(View.GONE);
            }
        }
    }


    private void onExpandableButtonClick(int index) {
        if (isExpanded[index]) {
            collapse(expandableLayouts[index]);
            expandableButtons[index].setText(getButtonText(index, false));
        } else {
            expand(expandableLayouts[index]);
            expandableButtons[index].setText(getButtonText(index, true));
        }
        isExpanded[index] = !isExpanded[index];
    }


    private void showInputDialog(int index) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.input_dialog, null);
        builder.setView(dialogView);

        EditText input = dialogView.findViewById(R.id.input);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        TextView savedDataTextView = dialogView.findViewById(R.id.savedDataTextView);

        AlertDialog dialog = builder.create();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            String path = getPath(index);
            DatabaseReference userRef = mDatabase.child("users").child(uid).child(path);

            // Load saved data and display it
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    StringBuilder savedData = new StringBuilder();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String value = snapshot.child("value").getValue(String.class);
                        String timestamp = snapshot.child("timestamp").getValue(String.class);
                        savedData.append(timestamp).append(": ").append(value).append("\n");
                    }
                    savedDataTextView.setText(savedData.toString());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(UserActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                }
            });

            saveButton.setOnClickListener(v -> {
                String data = input.getText().toString();
                if (!TextUtils.isEmpty(data)) {
                    saveData(index, data);
                    dialog.dismiss();
                } else {
                    Toast.makeText(UserActivity.this, "Please enter data", Toast.LENGTH_SHORT).show();
                }
            });
        }

        dialog.show();
    }

    private void showHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.help_dialog, null);
        builder.setView(dialogView);

        TextView helpTextView = dialogView.findViewById(R.id.helpTextView);
        Button okButton = dialogView.findViewById(R.id.okButton);

        String helpText = "Инструкция по использованию приложения:\n\n" +
                "1. Используйте строку поиска, чтобы найти необходимый параметр здоровья.\n" +
                "2. Нажмите на название параметра, чтобы раскрыть дополнительную информацию.\n" +
                "3. Используйте кнопку 'Ввести данные', чтобы добавить новую запись.\n" +
                "4. Используйте кнопку 'Как пользоваться приложением', чтобы снова увидеть эту инструкцию.";

        helpTextView.setText(helpText);

        AlertDialog dialog = builder.create();

        okButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }


    private void saveData(int index, String data) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            String path = getPath(index);

            DatabaseReference userRef = mDatabase.child("users").child(uid).child(path).push();
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            userRef.child("value").setValue(data);
            userRef.child("timestamp").setValue(timestamp);
        }
    }

    private String getPath(int index) {
        switch (index) {
            case 0:
                return "pulses";
            case 1:
                return "pressures";
            case 2:
                return "oxygen_levels";
            case 3:
                return "glucose_levels";
            case 4:
                return "temperatures";
            case 5:
                return "respiratory_rates";
            default:
                return "";
        }
    }

    private void expand(final View v) {
        v.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();

        v.getLayoutParams().height = 0;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? ViewGroup.LayoutParams.WRAP_CONTENT
                        : (int) (targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration((int) (targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    private void collapse(final View v) {
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    v.setVisibility(View.GONE);
                } else {
                    v.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration((int) (initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    private String getButtonText(int index, boolean expanded) {
        String[] titles = getResources().getStringArray(R.array.expandable_titles);
        return titles[index] + (expanded ? " ▲" : " ▼");
    }
}
