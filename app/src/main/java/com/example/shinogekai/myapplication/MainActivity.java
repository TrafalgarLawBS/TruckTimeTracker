package com.example.shinogekai.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private Chronometer mEngineTimeRunning;
    private Button mStartEngineBtn, mPauseEngineBtn;
    private FloatingActionButton mAddDayBtn;

    private long lastPause;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private long lastClick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Inicijalizacija dugmica i chronometra
        mStartEngineBtn = findViewById(R.id.start_engine);
        mPauseEngineBtn = findViewById(R.id.pause_engine);
        mAddDayBtn = (FloatingActionButton) findViewById(R.id.add_day);

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() == null) {

                    startActivity(new Intent(MainActivity.this, LoginActivity.class));

                }
            }
        };


        mAddDayBtn.setEnabled(false);
        mPauseEngineBtn.setEnabled(false);

        mEngineTimeRunning = findViewById(R.id.time_running);


        //Liseneri klika na dugmice
        //Lisener klika na dugme start an engine
        mStartEngineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAddDayBtn.setEnabled(true);
                if (lastPause != 0) {
                    mEngineTimeRunning.setBase(mEngineTimeRunning.getBase() + SystemClock.elapsedRealtime() - lastPause);
                } else {
                    mEngineTimeRunning.setBase(SystemClock.elapsedRealtime());
                }
                mEngineTimeRunning.start();
                mStartEngineBtn.setEnabled(false);
                mPauseEngineBtn.setEnabled(true);
            }
        });

        //lisener klika na dugme pause an engine
        mPauseEngineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lastPause = SystemClock.elapsedRealtime();
                mEngineTimeRunning.stop();
                mPauseEngineBtn.setEnabled(false);
                mStartEngineBtn.setEnabled(true);
            }
        });

        //lisener klika na dugme addDay
        mAddDayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAddDayBtn.setEnabled(false);
                Context context = MainActivity.this;
                Toast.makeText(context, "Record added to database!", Toast.LENGTH_LONG).show();
                mEngineTimeRunning.stop();
                mEngineTimeRunning.setBase(SystemClock.elapsedRealtime());
                lastPause = 0;
                mStartEngineBtn.setEnabled(true);
                mPauseEngineBtn.setEnabled(false);
            }
        });

    }

    // ubaci menu
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // koja opcija iz menua je izabrana
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menuItemThatWasSelected = item.getItemId();
        Context context = MainActivity.this;
        if (menuItemThatWasSelected == R.id.menu_log_out) {

            mAuth.signOut();

            Toast.makeText(context, "Log Out clicked", Toast.LENGTH_SHORT).show();
        } else if (menuItemThatWasSelected == R.id.menu_about) {

            startActivity(new Intent(MainActivity.this, AboutActivity.class));

            Toast.makeText(context, "About clicked", Toast.LENGTH_SHORT).show();
        } else if (menuItemThatWasSelected == R.id.menu_settings) {
            Toast.makeText(context, "Settings clicked", Toast.LENGTH_SHORT).show();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();

        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    public void onBackPressed() {
        long now = System.currentTimeMillis();
        if (now - lastClick < 3000) {
            //super.onBackPressed();
            Intent a = new Intent(Intent.ACTION_MAIN);
            a.addCategory(Intent.CATEGORY_HOME);
            a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(a);
            finish();
            System.exit(0);
        } else {
            Toast.makeText(MainActivity.this, "Press back again to exit", Toast.LENGTH_SHORT).show();
            lastClick = now;
        }
    }
}
