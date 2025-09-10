// In: ui/AddEditEventActivity.java
package com.rkm.rkmattendanceapp.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.rkm.attendance.model.Event;
import com.rkm.rkmattendanceapp.R;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

public class AddEditEventActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "com.rkm.rkmattendanceapp.ui.EXTRA_EVENT_ID";
    public static final long NEW_EVENT_ID = -1;

    private AddEditEventViewModel viewModel;
    private TextInputEditText nameEditText, dateEditText, fromTimeEditText, untilTimeEditText, remarkEditText;
    private long currentEventId = NEW_EVENT_ID;
    private LocalDate selectedDate = LocalDate.now();
    private LocalTime selectedFromTime = LocalTime.of(6, 0);
    private LocalTime selectedUntilTime = LocalTime.of(22, 0);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_event);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);
        }

        bindViews();
        setupClickListeners();

        viewModel = new ViewModelProvider(this).get(AddEditEventViewModel.class);

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_EVENT_ID)) {
            currentEventId = intent.getLongExtra(EXTRA_EVENT_ID, NEW_EVENT_ID);
        }

        if (currentEventId == NEW_EVENT_ID) {
            setTitle("New Event");
            updateDateAndTimeViews(); // Show defaults for a new event
        } else {
            setTitle("Edit Event");
            viewModel.loadEvent(currentEventId);
        }

        observeViewModel();
    }

    private void bindViews() {
        nameEditText = findViewById(R.id.edit_text_event_name);
        dateEditText = findViewById(R.id.edit_text_event_date);
        fromTimeEditText = findViewById(R.id.edit_text_active_from);
        untilTimeEditText = findViewById(R.id.edit_text_active_until);
        remarkEditText = findViewById(R.id.edit_text_event_remark);
    }

    private void setupClickListeners() {
        dateEditText.setOnClickListener(v -> showDatePicker());
        fromTimeEditText.setOnClickListener(v -> showTimePicker(true));
        untilTimeEditText.setOnClickListener(v -> showTimePicker(false));
    }
    
    private void observeViewModel() {
        viewModel.getEvent().observe(this, this::populateUI);
        viewModel.getSaveFinished().observe(this, finished -> {
            if (finished != null && finished) {
                Toast.makeText(this, "Event saved", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        });
        viewModel.getErrorMessage().observe(this, message -> {
            if (!TextUtils.isEmpty(message)) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void populateUI(Event event) {
        if (event == null) return;
        nameEditText.setText(event.getEventName());
        remarkEditText.setText(event.getRemark());

        try {
            selectedDate = LocalDate.parse(event.getEventDate(), DATE_FORMATTER);
            LocalDateTime fromDateTime = LocalDateTime.parse(event.getActiveFromTs(), DATETIME_FORMATTER);
            LocalDateTime untilDateTime = LocalDateTime.parse(event.getActiveUntilTs(), DATETIME_FORMATTER);
            selectedFromTime = fromDateTime.toLocalTime();
            selectedUntilTime = untilDateTime.toLocalTime();
        } catch (Exception e) {
            // Fallback to defaults if parsing fails
            e.printStackTrace();
        }
        updateDateAndTimeViews();
    }

    private void saveEvent() {
        String name = nameEditText.getText().toString().trim();
        String date = dateEditText.getText().toString().trim();
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(date)) {
            Toast.makeText(this, "Event Name and Date are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        String remark = remarkEditText.getText().toString().trim();
        String activeFrom = LocalDateTime.of(selectedDate, selectedFromTime).format(DATETIME_FORMATTER);
        String activeUntil = LocalDateTime.of(selectedDate, selectedUntilTime).format(DATETIME_FORMATTER);

        Event eventToSave = new Event(
                currentEventId == NEW_EVENT_ID ? null : currentEventId,
                null, name, date, activeFrom, activeUntil, remark
        );
        viewModel.saveEvent(eventToSave);
    }

    private void updateDateAndTimeViews() {
        dateEditText.setText(selectedDate.format(DATE_FORMATTER));
        fromTimeEditText.setText(selectedFromTime.format(TIME_FORMATTER));
        untilTimeEditText.setText(selectedUntilTime.format(TIME_FORMATTER));
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                    updateDateAndTimeViews();
                },
                selectedDate.getYear(), selectedDate.getMonthValue() - 1, selectedDate.getDayOfMonth());
        dialog.show();
    }
    
    private void showTimePicker(boolean isFromTime) {
        LocalTime timeToShow = isFromTime ? selectedFromTime : selectedUntilTime;
        TimePickerDialog dialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    if (isFromTime) {
                        selectedFromTime = LocalTime.of(hourOfDay, minute);
                    } else {
                        selectedUntilTime = LocalTime.of(hourOfDay, minute);
                    }
                    updateDateAndTimeViews();
                },
                timeToShow.getHour(), timeToShow.getMinute(), true // 24-hour format
        );
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_edit_devotee_menu, menu); // Re-use the save menu
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_save_devotee) {
            saveEvent();
            return true;
        } else if (itemId == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}