package com.example.lab2mobile;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private EditText nameInput, grade1Input, grade2Input, addressInput;
    private TextView resultText;
    private DBHelper dbHelper;
    private MapHandler mapHandler;
    private Spinner contactSpinner;
    private List<Pair<String, String>> contactList = new ArrayList<>();

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contactSpinner = findViewById(R.id.contactSpinner);

        checkPermissions(false);

        nameInput = findViewById(R.id.nameInput);
        grade1Input = findViewById(R.id.grade1Input);
        grade2Input = findViewById(R.id.grade2Input);
        addressInput = findViewById(R.id.addressInput);
        resultText = findViewById(R.id.resultText);

        dbHelper = new DBHelper(this);

        // Initialize the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapHandler = new MapHandler(mapFragment, this);
        }
    }

    private void loadContacts() {
        String[] projection = {
                ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS
        };

        Cursor cursor = getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                projection,
                ContactsContract.Data.MIMETYPE + " = ?",
                new String[]{ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE},
                null
        );

        List<String> spinnerItems = new ArrayList<>();
        contactList.clear();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));
                String address = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS));

                if (address != null && !address.isEmpty()) {
                    contactList.add(new Pair<>(name, address));
                    spinnerItems.add(name + " (" + address + ")");
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        contactSpinner.setAdapter(adapter);
    }

    public void showRoute(View view) {
        int selectedIndex = contactSpinner.getSelectedItemPosition();
        if (selectedIndex >= 0 && selectedIndex < contactList.size()) {
            String destination = contactList.get(selectedIndex).second;
            mapHandler.showRoute(destination);
        } else {
            Toast.makeText(this, "No contact selected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Додаємо меню в ActionBar
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void addStudent(View view) {
        try {
            String fullName = nameInput.getText().toString();
            int grade1 = Integer.parseInt(grade1Input.getText().toString());
            int grade2 = Integer.parseInt(grade2Input.getText().toString());
            String address = addressInput.getText().toString();

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("full_name", fullName);
            values.put("subject1_grade", grade1);
            values.put("subject2_grade", grade2);
            values.put("address", address);

            long newRowId = db.insert("students", null, values);
            if (newRowId != -1) {
                Toast.makeText(this, "Student added successfully", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();

        }
    }

    public void showHighAverageStudents(View view) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery("SELECT full_name FROM students WHERE (subject1_grade + subject2_grade) / 2 > 60", null);

            StringBuilder result = new StringBuilder();
            if (cursor.moveToFirst()) {
                do {
                    String fullName = cursor.getString(cursor.getColumnIndexOrThrow("full_name"));
                    result.append(fullName).append("\n");
                } while (cursor.moveToNext());
            }
            resultText.setText(result.toString());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void calculateHighAveragePercentage(View view) {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor totalCursor = db.rawQuery("SELECT COUNT(*) AS total FROM students", null);
            Cursor selectedCursor = db.rawQuery("SELECT COUNT(*) AS selected FROM students WHERE (subject1_grade + subject2_grade) / 2 > 60", null);

            if (totalCursor.moveToFirst() && selectedCursor.moveToFirst()) {
                int total = totalCursor.getInt(totalCursor.getColumnIndexOrThrow("total"));
                int selected = selectedCursor.getInt(selectedCursor.getColumnIndexOrThrow("selected"));

                double percentage = (double) selected / total * 100;
                resultText.setText("Selected Percentage: " + percentage + "%");
            }
            totalCursor.close();
            selectedCursor.close();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();

        }
    }

    public void showFilteredContacts() {
        try {
            String selection = ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME + " LIKE ?";
            String[] selectionArgs = new String[]{"%Іван%"};

            Cursor cursor = getContentResolver().query(
                    ContactsContract.Data.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID, ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME},
                    selection + " AND " + ContactsContract.Data.MIMETYPE + " = ?",
                    new String[]{selectionArgs[0], ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE},
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                StringBuilder result = new StringBuilder();
                do {
                    String contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));

                    Cursor phoneCursor = getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{contactId},
                            null
                    );

                    if (phoneCursor != null && phoneCursor.moveToFirst()) {
                        do {
                            String phone = phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            result.append(name).append(" : ").append(phone).append("\n");
                        } while (phoneCursor.moveToNext());
                        phoneCursor.close();
                    }

                } while (cursor.moveToNext());

                resultText.setText(result.toString());
                cursor.close();
            } else {
                resultText.setText("No contacts found.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();

        }
    }

    public void showFilteredContacts(View view) {
        checkPermissions(true);
    }

    private void checkPermissions(boolean show) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            if (show) {
                showFilteredContacts();
            } else {
                loadContacts();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showFilteredContacts();
            loadContacts();
        }
    }
}
