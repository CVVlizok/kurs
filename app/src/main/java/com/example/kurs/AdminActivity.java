package com.example.kurs;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AdminActivity extends AppCompatActivity {

    private Button[] expandableButtons = new Button[6];
    private LinearLayout[] expandableLayouts = new LinearLayout[6];
    private boolean[] isExpanded = new boolean[6];
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        EditText searchEditText = findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterButtons(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

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

        Button deleteDataButton = findViewById(R.id.deleteDataButton);
        deleteDataButton.setOnClickListener(view -> showDeleteDialog());

        Button helpButton = findViewById(R.id.helpButton);
        helpButton.setOnClickListener(view -> showHelpDialog());

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

    // Метод для отображения окна с инструкцией
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
                "4. Для удаления данных введите почту пользователя, выберите параметр, дату и время записи, затем нажмите 'Удалить данные'.\n" +
                "5. Используйте кнопку 'Как пользоваться приложением', чтобы снова увидеть эту инструкцию.";

        helpTextView.setText(helpText);

        AlertDialog dialog = builder.create(); // Объявляем dialog здесь

        okButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show(); // Используем dialog здесь
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
                    Toast.makeText(AdminActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                }
            });

            saveButton.setOnClickListener(v -> {
                String data = input.getText().toString();
                if (!TextUtils.isEmpty(data)) {
                    saveData(index, data);
                    dialog.dismiss();
                } else {
                    Toast.makeText(AdminActivity.this, "Please enter data", Toast.LENGTH_SHORT).show();
                }
            });
        }

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

    private void showDeleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.delete_dialog, null);
        builder.setView(dialogView);

        EditText emailInput = dialogView.findViewById(R.id.emailInput);
        Spinner healthParameterSpinner = dialogView.findViewById(R.id.healthParameterSpinner);
        Button datePickerButton = dialogView.findViewById(R.id.datePickerButton);
        Button timePickerButton = dialogView.findViewById(R.id.timePickerButton);
        Button deleteButton = dialogView.findViewById(R.id.deleteButton);

        AlertDialog dialog = builder.create();

        // Initialize the spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.health_parameters, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        healthParameterSpinner.setAdapter(adapter);

        final Calendar calendar = Calendar.getInstance();

        datePickerButton.setOnClickListener(v -> {
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(AdminActivity.this, (view, year1, monthOfYear, dayOfMonth) -> {
                String date = String.format("%04d-%02d-%02d", year1, monthOfYear + 1, dayOfMonth);
                datePickerButton.setText(date);
                calendar.set(year1, monthOfYear, dayOfMonth);
            }, year, month, day);
            datePickerDialog.show();
        });

        timePickerButton.setOnClickListener(v -> showCustomTimePickerDialog(timePickerButton, calendar));

        deleteButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String healthParameter = healthParameterSpinner.getSelectedItem().toString();
            String date = datePickerButton.getText().toString();
            String time = timePickerButton.getText().toString();
            String dateTime = date + " " + time;

            if (!TextUtils.isEmpty(email) && !date.equals("Выберите дату") && !time.equals("Выберите время")) {
                deleteData(email, healthParameter, dateTime);
                dialog.dismiss();
            } else {
                Toast.makeText(AdminActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }








    private void showCustomTimePickerDialog(Button timePickerButton, Calendar calendar) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.time_picker_dialog, null);
        builder.setView(dialogView);

        NumberPicker hourPicker = dialogView.findViewById(R.id.hourPicker);
        NumberPicker minutePicker = dialogView.findViewById(R.id.minutePicker);
        NumberPicker secondPicker = dialogView.findViewById(R.id.secondPicker);
        Button okButton = dialogView.findViewById(R.id.okButton);

        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        secondPicker.setMinValue(0);
        secondPicker.setMaxValue(59);

        AlertDialog dialog = builder.create();

        okButton.setOnClickListener(v -> {
            int hour = hourPicker.getValue();
            int minute = minutePicker.getValue();
            int second = secondPicker.getValue();
            String time = String.format(Locale.getDefault(), "%02d:%02d:%02d", hour, minute, second);
            timePickerButton.setText(time);
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, second);
            dialog.dismiss();
        });

        dialog.show();
    }






    private void deleteData(String email, String healthParameter, String dateTime) {
        String path = getPathFromHealthParameter(healthParameter);
        DatabaseReference usersRef = mDatabase.child("users");

        Log.d("AdminActivity", "Пытаемся найти пользователя по email: " + email);

        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String uid = userSnapshot.getKey();
                        if (uid != null) {
                            Log.d("AdminActivity", "Найден пользователь с uid: " + uid);
                            DatabaseReference dataRef = mDatabase.child("users").child(uid).child(path);
                            dataRef.orderByChild("timestamp").equalTo(dateTime).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        Log.d("AdminActivity", "Найдены данные с timestamp: " + dateTime);
                                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                            snapshot.getRef().removeValue().addOnCompleteListener(task -> {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(AdminActivity.this, "Data deleted successfully", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(AdminActivity.this, "Failed to delete data", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    } else {
                                        Log.d("AdminActivity", "Данные не найдены с timestamp: " + dateTime);
                                        Toast.makeText(AdminActivity.this, "Данные не найдены с timestamp: " + dateTime, Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    Toast.makeText(AdminActivity.this, "Failed to delete data", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                } else {
                    Log.d("AdminActivity", "Не удалось найти пользователя по email: " + email);
                    Toast.makeText(AdminActivity.this, "Не удалось найти пользователя по email: " + email, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AdminActivity.this, "Failed to find user", Toast.LENGTH_SHORT).show();
            }
        });
    }









    private String getPathFromHealthParameter(String healthParameter) {
        switch (healthParameter) {
            case "Пульс":
                return "pulses";
            case "Давление":
                return "pressures";
            case "Кислород в крови":
                return "oxygen_levels";
            case "Глюкоза в крови":
                return "glucose_levels";
            case "Температура":
                return "temperatures";
            case "Частота дыхания":
                return "respiratory_rates";
            default:
                return "";
        }
    }
}
