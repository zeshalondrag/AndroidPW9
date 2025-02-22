package com.example.firebasepw;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserActivity extends AppCompatActivity {

    private RecyclerView servicesRecyclerView;
    private ServiceAdapter serviceAdapter;
    private List<Map<String, Object>> servicesList;
    private TextView selectedDateTimeText;
    private Button selectDateButton, selectTimeButton, bookAppointmentButton;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private String selectedServiceId = "";
    private String selectedServiceName = "";
    private Calendar selectedDateTime = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        servicesRecyclerView = findViewById(R.id.servicesRecyclerView);
        selectedDateTimeText = findViewById(R.id.selectedDateTimeText);
        selectDateButton = findViewById(R.id.selectDateButton);
        selectTimeButton = findViewById(R.id.selectTimeButton);
        bookAppointmentButton = findViewById(R.id.bookAppointmentButton);

        servicesList = new ArrayList<>();
        serviceAdapter = new ServiceAdapter(servicesList, this::onServiceClick);
        servicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        servicesRecyclerView.setAdapter(serviceAdapter);

        loadServices();

        selectDateButton.setOnClickListener(v -> showDatePickerDialog());
        selectTimeButton.setOnClickListener(v -> showTimePickerDialog());
        bookAppointmentButton.setOnClickListener(v -> bookAppointment());

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadServices() {
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
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка загрузки услуг: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, month1, dayOfMonth) -> {
                    selectedDateTime.set(year1, month1, dayOfMonth);
                    updateDateTimeText();
                }, year, month, day);
        datePickerDialog.show();
    }

    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDateTime.set(Calendar.MINUTE, minute1);
                    updateDateTimeText();
                }, hour, minute, true);
        timePickerDialog.show();
    }

    private void updateDateTimeText() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        String date = dateFormat.format(selectedDateTime.getTime());
        String time = timeFormat.format(selectedDateTime.getTime());
        selectedDateTimeText.setText("Дата: " + date + ", Время: " + time);
    }

    private void onServiceClick(Map<String, Object> service) {
        selectedServiceId = service.get("id").toString();
        selectedServiceName = service.get("name").toString();
        Toast.makeText(this, "Выбрана услуга: " + selectedServiceName, Toast.LENGTH_SHORT).show();
    }

    private void bookAppointment() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedServiceId.isEmpty()) {
            Toast.makeText(this, "Выберите услугу", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDateTime.getTimeInMillis() <= Calendar.getInstance().getTimeInMillis()) {
            Toast.makeText(this, "Выберите будущую дату и время", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        String date = dateFormat.format(selectedDateTime.getTime());
        String time = timeFormat.format(selectedDateTime.getTime());

        String appointmentId = firestore.collection("appointments").document().getId();
        DocumentReference appointmentRef = firestore.collection("appointments").document(appointmentId);
        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("clientId", user.getUid());
        appointmentData.put("clientName", user.getEmail());
        appointmentData.put("serviceId", selectedServiceId);
        appointmentData.put("serviceName", selectedServiceName);
        appointmentData.put("date", date);
        appointmentData.put("time", time);

        appointmentRef.set(appointmentData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Запись успешно создана", Toast.LENGTH_SHORT).show();
                    selectedServiceId = "";
                    selectedServiceName = "";
                    selectedDateTime = Calendar.getInstance();
                    updateDateTimeText();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка создания записи: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private static class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder> {

        private List<Map<String, Object>> services;
        private final OnServiceClickListener listener;

        interface OnServiceClickListener {
            void onServiceClick(Map<String, Object> service);
        }

        ServiceAdapter(List<Map<String, Object>> services, OnServiceClickListener listener) {
            this.services = services;
            this.listener = listener;
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