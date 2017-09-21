package com.example.shinogekai.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class AboutActivity extends AppCompatActivity {

    FloatingActionButton mFibMail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        mFibMail = findViewById(R.id.fab_email);

        mFibMail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "bojansaric1993@gmail.com", null)); //intent koji se korist za slanje mejla
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Track Time Tracker"); //subject maila
                startActivity(Intent.createChooser(emailIntent, ""));
            }
        });


    }
}
