package com.example.lab2mobile;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Прив'язуємо елементи інтерфейсу
        ImageView developerPhoto = findViewById(R.id.developerPhoto);
        TextView developerInfo = findViewById(R.id.developerInfo);

        developerPhoto.setImageResource(R.drawable.my_photo);
        developerInfo.setText("Розробила: Семйон Наталія\nГрупа: ТТП-41\nКонтакти: @enn_sem");
    }
}
