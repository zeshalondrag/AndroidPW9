package com.example.firebasepw;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmployeeActivity extends AppCompatActivity {

    protected EditText serviceIdField, serviceNameField, serviceCategoryField, serviceDescriptionField;
    protected Button addServiceButton, updateServiceButton, deleteServiceButton;
    protected FirebaseFirestore firestore;

    protected EditText searchServiceField;
    protected Spinner categorySpinner;
    protected RecyclerView servicesRecyclerView;
    protected ServiceAdapter serviceAdapter;
    protected List<Map<String, Object>> servicesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee);

        firestore = FirebaseFirestore.getInstance();

        Toast.makeText(this, "Firestore initialized: " + (firestore != null), Toast.LENGTH_SHORT).show();

        serviceIdField = findViewById(R.id.serviceIdField);
        serviceNameField = findViewById(R.id.serviceNameField);
        serviceCategoryField = findViewById(R.id.serviceCategoryField);
        serviceDescriptionField = findViewById(R.id.serviceDescriptionField);
        addServiceButton = findViewById(R.id.addServiceButton);
        updateServiceButton = findViewById(R.id.updateServiceButton);
        deleteServiceButton = findViewById(R.id.deleteServiceButton);

        searchServiceField = findViewById(R.id.searchServiceField);
        categorySpinner = findViewById(R.id.categorySpinner);
        servicesRecyclerView = findViewById(R.id.servicesRecyclerView);

        servicesList = new ArrayList<>();
        serviceAdapter = new ServiceAdapter(servicesList, this::onServiceClick);
        servicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        servicesRecyclerView.setAdapter(serviceAdapter);

        setupCategorySpinner();

        searchServiceField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterServices();
            }
        });

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterServices();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        loadServices();

        addServiceButton.setOnClickListener(v -> addService());
        updateServiceButton.setOnClickListener(v -> updateService());
        deleteServiceButton.setOnClickListener(v -> deleteService());
    }

    protected void setupCategorySpinner() {
        List<String> categories = new ArrayList<>();
        categories.add("Все категории");
        categories.add("Категория 1");
        categories.add("Категория 2");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    protected void loadServices() {
        firestore.collection("services")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    servicesList.clear();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Map<String, Object> service = document.getData();
                        service.put("id", document.getId());
                        servicesList.add(service);
                    }
                    serviceAdapter.notifyDataSetChanged();
                    filterServices();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка загрузки услуг: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    protected void filterServices() {
        String searchText = searchServiceField.getText().toString().trim().toLowerCase();
        String selectedCategory = categorySpinner.getSelectedItem().toString();

        List<Map<String, Object>> filteredList = new ArrayList<>();
        for (Map<String, Object> service : new ArrayList<>(servicesList)) {
            String name = service.get("name").toString().toLowerCase();
            String category = service.get("category").toString();

            boolean matchesSearch = name.contains(searchText);
            boolean matchesCategory = "Все категории".equals(selectedCategory) || category.equals(selectedCategory);

            if (matchesSearch && matchesCategory) {
                filteredList.add(service);
            }
        }
        serviceAdapter.updateList(filteredList);
    }

    protected void onServiceClick(Map<String, Object> service) {
        String serviceId = service.get("id").toString();
        serviceIdField.setText(serviceId);
        serviceNameField.setText(service.get("name").toString());
        serviceCategoryField.setText(service.get("category").toString());
        serviceDescriptionField.setText(service.get("description").toString());
    }

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
                        loadServices();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка добавления услуги: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Заполните название и категорию", Toast.LENGTH_SHORT).show();
        }
    }

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
                        loadServices();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка обновления услуги: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Заполните ID, название и категорию", Toast.LENGTH_SHORT).show();
        }
    }

    protected void deleteService() {
        String serviceId = serviceIdField.getText().toString().trim();
        if (!serviceId.isEmpty()) {
            firestore.collection("services").document(serviceId).delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Услуга удалена", Toast.LENGTH_SHORT).show();
                        clearFields();
                        loadServices();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка удаления услуги: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Введите ID услуги", Toast.LENGTH_SHORT).show();
        }
    }

    protected void clearFields() {
        serviceIdField.setText("");
        serviceNameField.setText("");
        serviceCategoryField.setText("");
        serviceDescriptionField.setText("");
    }

    protected static class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder> {

        private List<Map<String, Object>> services;
        private final OnServiceClickListener listener;

        interface OnServiceClickListener {
            void onServiceClick(Map<String, Object> service);
        }

        ServiceAdapter(List<Map<String, Object>> services, OnServiceClickListener listener) {
            this.services = services;
            this.listener = listener;
        }

        void updateList(List<Map<String, Object>> newList) {
            this.services = newList;
            notifyDataSetChanged();
        }

        @Override
        public ServiceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ServiceViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ServiceViewHolder holder, int position) {
            Map<String, Object> service = services.get(position);
            String serviceName = service.get("name").toString();
            holder.itemView.setOnClickListener(v -> listener.onServiceClick(service));
            ((TextView) holder.itemView).setText(serviceName);
        }

        @Override
        public int getItemCount() {
            return services.size();
        }

        static class ServiceViewHolder extends RecyclerView.ViewHolder {
            ServiceViewHolder(View itemView) {
                super(itemView);
            }
        }
    }
}