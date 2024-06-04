package com.example.kurs;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private RadioGroup radioGroup;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailEditText = findViewById(R.id.nameEmail);
        passwordEditText = findViewById(R.id.namePassword);
        radioGroup = findViewById(R.id.radioGroup);
        Button loginButton = findViewById(R.id.login_button);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("users");

        loginButton.setOnClickListener(v -> onLoginClicked());
    }

    private void onLoginClicked() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Введите email и пароль", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedId = radioGroup.getCheckedRadioButtonId();

        if (selectedId == -1) {
            Toast.makeText(this, "Выберите пользователя или администратора", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selectedRadioButton = findViewById(selectedId);
        String role = selectedRadioButton.getText().toString();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Вход успешен
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d("LoginActivity", "User signed in: " + user.getUid());
                        if (user != null) {
                            mDatabase.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    boolean userExists = snapshot.exists();
                                    Log.d("LoginActivity", "DataSnapshot exists: " + userExists);
                                    if (userExists) {
                                        String userRole = snapshot.child("role").getValue(String.class);
                                        if (role.equals("пользователь")) {
                                            Intent userIntent = new Intent(LoginActivity.this, UserActivity.class);
                                            userIntent.putExtra("email", email);
                                            startActivity(userIntent);
                                        } else if (role.equals("администратор")) {
                                            if ("admin".equals(userRole)) {
                                                Intent adminIntent = new Intent(LoginActivity.this, AdminActivity.class);
                                                adminIntent.putExtra("email", email);
                                                startActivity(adminIntent);
                                            } else {
                                                Toast.makeText(LoginActivity.this, "Вы не имеете прав администратора", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    } else {
                                        Log.d("LoginActivity", "Snapshot does not exist for user: " + user.getUid());
                                        Toast.makeText(LoginActivity.this, "Пользователь не найден", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(LoginActivity.this, "Ошибка базы данных: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        Log.e("LoginActivity", "Login failed: " + task.getException().getMessage());
                        Toast.makeText(LoginActivity.this, "Ошибка входа: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    // Метод для обработки нажатия на кнопку "Зарегистрироваться"
    public void onSignUpClicked(View view) {
        Intent signUpIntent = new Intent(LoginActivity.this, SignUpActivity.class);
        startActivity(signUpIntent);
    }
}
