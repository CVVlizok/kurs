package com.example.kurs;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText usernameEditText;
    private EditText passwordEditText;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        emailEditText = findViewById(R.id.nameEmail);
        usernameEditText = findViewById(R.id.userName);
        passwordEditText = findViewById(R.id.namePassword);
        Button signUpButton = findViewById(R.id.sign_up_button);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("users");

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSignUpClicked(v);
            }
        });
    }

    public void onSignUpClicked(View view) {
        String email = emailEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Registration success
                        FirebaseUser user = mAuth.getCurrentUser();
                        writeNewUser(user.getUid(), username, email);

                        Toast.makeText(SignUpActivity.this, "Регистрация прошла успешно", Toast.LENGTH_SHORT).show();
                        Intent loginIntent = new Intent(SignUpActivity.this, LoginActivity.class);
                        startActivity(loginIntent);
                        finish();
                    } else {
                        // If registration fails
                        Toast.makeText(SignUpActivity.this, "Ошибка регистрации: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void writeNewUser(String userId, String username, String email) {
        User user = new User(username, email); // Создание объекта User с указанными именем и электронной почтой
        mDatabase.child(userId).setValue(user); // Сохранение пользователя в базу данных
    }
}
