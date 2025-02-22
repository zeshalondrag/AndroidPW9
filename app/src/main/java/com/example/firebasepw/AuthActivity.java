package com.example.firebasepw;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class AuthActivity extends AppCompatActivity {

    private EditText emailField, passwordField;
    private Button loginButton, registerButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // Инициализация Firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Проверка подключения к Firestore
        Toast.makeText(this, "Firestore initialized: " + (firestore != null), Toast.LENGTH_SHORT).show();

        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);

        loginButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();
            if (!email.isEmpty() && !password.isEmpty()) {
                loginUser(email, password);
            } else {
                Toast.makeText(AuthActivity.this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            }
        });

        registerButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();
            if (!email.isEmpty() && !password.isEmpty()) {
                registerUser(email, password);
            } else {
                Toast.makeText(AuthActivity.this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(AuthActivity.this, "Вход успешен, UID: " + (user != null ? user.getUid() : "null"), Toast.LENGTH_SHORT).show();
                        checkUserRole(user);
                    } else {
                        Toast.makeText(AuthActivity.this, "Ошибка входа: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void registerUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();
                            DocumentReference userRef = firestore.collection("users").document(uid);
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("email", email);
                            userData.put("role", "user"); // Роль по умолчанию "user"
                            userRef.set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(AuthActivity.this, "Регистрация успешна, роль: user", Toast.LENGTH_SHORT).show();
                                        checkUserRole(user);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(AuthActivity.this, "Ошибка записи данных в Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(AuthActivity.this, "Пользователь не создан", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(AuthActivity.this, "Ошибка регистрации: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserRole(FirebaseUser user) {
        if (user == null) {
            Toast.makeText(AuthActivity.this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }
        firestore.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        Toast.makeText(AuthActivity.this, "Найдена роль: " + (role != null ? role : "null"), Toast.LENGTH_SHORT).show();
                        try {
                            if ("admin".equals(role)) {
                                startActivity(new Intent(AuthActivity.this, AdminActivity.class));
                                finish();
                            } else if ("employee".equals(role)) {
                                startActivity(new Intent(AuthActivity.this, EmployeeActivity.class));
                                finish();
                            } else if ("user".equals(role)) {
                                startActivity(new Intent(AuthActivity.this, UserActivity.class)); // Предполагаем, что есть UserActivity
                                finish();
                            } else {
                                Toast.makeText(AuthActivity.this, "Неизвестная роль пользователя", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(AuthActivity.this, "Ошибка при переходе: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(AuthActivity.this, "Данные пользователя не найдены в Firestore", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AuthActivity.this, "Ошибка чтения данных из Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}