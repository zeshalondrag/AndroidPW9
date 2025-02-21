package com.example.firebasepw;

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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class AdminActivity extends EmployeeActivity {

    private EditText userIdField, userEmailField, userRoleField;
    private Button addUserButton, updateUserButton, deleteUserButton;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        Toast.makeText(this, "Firestore initialized: " + (firestore != null), Toast.LENGTH_SHORT).show();

        serviceIdField = findViewById(R.id.serviceIdField);
        serviceNameField = findViewById(R.id.serviceNameField);
        serviceCategoryField = findViewById(R.id.serviceCategoryField);
        serviceDescriptionField = findViewById(R.id.serviceDescriptionField);
        addServiceButton = findViewById(R.id.addServiceButton);
        updateServiceButton = findViewById(R.id.updateServiceButton);
        deleteServiceButton = findViewById(R.id.deleteServiceButton);

        userIdField = findViewById(R.id.userIdField);
        userEmailField = findViewById(R.id.userEmailField);
        userRoleField = findViewById(R.id.userRoleField);
        addUserButton = findViewById(R.id.addUserButton);
        updateUserButton = findViewById(R.id.updateUserButton);
        deleteUserButton = findViewById(R.id.deleteUserButton);

        addServiceButton.setOnClickListener(v -> addService());
        updateServiceButton.setOnClickListener(v -> updateService());
        deleteServiceButton.setOnClickListener(v -> deleteService());

        addUserButton.setOnClickListener(v -> {
            String email = userEmailField.getText().toString().trim();
            String role = userRoleField.getText().toString().trim();

            if (!email.isEmpty() && !role.isEmpty() && (role.equals("admin") || role.equals("employee"))) {
                String userId = firestore.collection("users").document().getId();
                DocumentReference userRef = firestore.collection("users").document(userId);
                Map<String, Object> userData = new HashMap<>();
                userData.put("email", email);
                userData.put("role", role);

                userRef.set(userData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Пользователь добавлен (ID: " + userId + ")", Toast.LENGTH_LONG).show();
                            clearUserFields();
                            logAction("add_user", userId, null);
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Ошибка добавления пользователя: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "Заполните email и корректную роль (admin/employee)", Toast.LENGTH_SHORT).show();
            }
        });

        updateUserButton.setOnClickListener(v -> {
            String userId = userIdField.getText().toString().trim();
            String email = userEmailField.getText().toString().trim();
            String role = userRoleField.getText().toString().trim();

            if (!userId.isEmpty() && !email.isEmpty() && !role.isEmpty() && (role.equals("admin") || role.equals("employee"))) {
                DocumentReference userRef = firestore.collection("users").document(userId);
                Map<String, Object> userData = new HashMap<>();
                userData.put("email", email);
                userData.put("role", role);

                userRef.set(userData, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Пользователь обновлен", Toast.LENGTH_SHORT).show();
                            clearUserFields();
                            logAction("update_user", userId, null);
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Ошибка обновления пользователя: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "Заполните ID, email и корректную роль", Toast.LENGTH_SHORT).show();
            }
        });

        deleteUserButton.setOnClickListener(v -> {
            String userId = userIdField.getText().toString().trim();
            if (!userId.isEmpty()) {
                firestore.collection("users").document(userId).delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Пользователь удален", Toast.LENGTH_SHORT).show();
                            clearUserFields();
                            logAction("delete_user", userId, null);
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Ошибка удаления пользователя: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "Введите ID пользователя", Toast.LENGTH_SHORT).show();
            }
        });

        loadServices();
    }

    private void logAction(String action, String targetId, String serviceId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String logId = firestore.collection("logs").document().getId();
            DocumentReference logRef = firestore.collection("logs").document(logId);
            Map<String, Object> logData = new HashMap<>();
            logData.put("action", action);
            logData.put("userId", user.getUid());
            logData.put("targetId", targetId);
            logData.put("serviceId", serviceId);
            logData.put("timestamp", System.currentTimeMillis());

            logRef.set(logData)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Действие залогировано", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка логирования: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void addService() {
        String name = serviceNameField.getText().toString().trim();
        String category = serviceCategoryField.getText().toString().trim();
        String description = serviceDescriptionField.getText().toString().trim();

        if (!name.isEmpty() && !category.isEmpty()) {
            String serviceId = firestore.collection("services").document().getId();
            DocumentReference serviceRef = firestore.collection("services").document(serviceId);
            Map<String, Object> serviceData = new HashMap<>();
            serviceData.put("name", name);
            serviceData.put("category", category);
            serviceData.put("description", description);

            serviceRef.set(serviceData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Услуга добавлена (ID: " + serviceId + ")", Toast.LENGTH_LONG).show();
                        clearFields();
                        loadServices(); // Обновляем список услуг
                        logAction("add_service", null, serviceId);
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка добавления услуги: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Заполните название и категорию", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void updateService() {
        String serviceId = serviceIdField.getText().toString().trim();
        String name = serviceNameField.getText().toString().trim();
        String category = serviceCategoryField.getText().toString().trim();
        String description = serviceDescriptionField.getText().toString().trim();

        if (!serviceId.isEmpty() && !name.isEmpty() && !category.isEmpty()) {
            DocumentReference serviceRef = firestore.collection("services").document(serviceId);
            Map<String, Object> serviceData = new HashMap<>();
            serviceData.put("name", name);
            serviceData.put("category", category);
            serviceData.put("description", description);

            serviceRef.set(serviceData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Услуга обновлена", Toast.LENGTH_SHORT).show();
                        clearFields();
                        loadServices(); // Обновляем список услуг
                        logAction("update_service", null, serviceId);
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка обновления услуги: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Заполните ID, название и категорию", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void deleteService() {
        String serviceId = serviceIdField.getText().toString().trim();
        if (!serviceId.isEmpty()) {
            firestore.collection("services").document(serviceId).delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Услуга удалена", Toast.LENGTH_SHORT).show();
                        clearFields();
                        loadServices(); // Обновляем список услуг
                        logAction("delete_service", null, serviceId);
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка удаления услуги: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Введите ID услуги", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearUserFields() {
        userIdField.setText("");
        userEmailField.setText("");
        userRoleField.setText("");
    }
}