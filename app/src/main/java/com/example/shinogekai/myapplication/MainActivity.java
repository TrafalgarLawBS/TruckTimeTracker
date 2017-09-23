package com.example.shinogekai.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.shinogekai.myapplication.dataobject.DriverDataDAO;
import com.example.shinogekai.myapplication.model.DriverData;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import es.dmoral.toasty.Toasty;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    static DriverDataDAO driverDataDAO;
    private Chronometer mEngineTimeRunning;
    private Button mStartEngineBtn, mPauseEngineBtn, mMakeReport;
    private FloatingActionButton mAddDayBtn;
    private long totalDrivingTime;
    private long lastPause;
    private Date timeStartedEngine;
    private Date timeFinishedEngine;
    private boolean firstStart;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location lctn;
    private String weatherCity, weatherDescription, weatherMainDescription;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private long lastClick;
    private Intent intent;
    private String userName, userEmail, userImageURL, userID;
    private DriverData driverData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        driverDataDAO = DriverDataDAO.getInstance();
        driverDataDAO.init();

        driverData = new DriverData();

        /*referenciranje dugmica i chronometra*/
        mStartEngineBtn = findViewById(R.id.start_engine);
        mPauseEngineBtn = findViewById(R.id.pause_engine);
        mAddDayBtn = findViewById(R.id.add_day);
        mMakeReport = findViewById(R.id.make_report);

////////////////////////////////////////////////////////////////////////////////////////////////////////
        //PODACI SA PRETHODNOG AKTIVITIJA
        intent = getIntent();
        userName = intent.getStringExtra("user_name");
        userEmail = intent.getStringExtra("user_email");
        userImageURL = intent.getStringExtra("user_image");
        userID = intent.getStringExtra("user_uid");

        Log.d(TAG, "userName: " + userName + "   userEmail: " + userEmail + "   userImageURL: " + userImageURL);

        if (userName != null) {
            Toasty.info(this, "Welcome " + userName, Toast.LENGTH_LONG, true).show();
        }
////////////////////////////////////////////////////////////////////////////////////////////////////////
        //LOKALIZACIJA
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                if (location == null) {
                    Log.d(TAG, "location == null");

                } else {
                    lctn.setLatitude(location.getLatitude());
                    lctn.setLongitude(location.getLongitude());

                    Log.d(TAG, "\nLat: " + location.getLatitude() + " Long: " + location.getLongitude());
                }

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);

            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET
                }, 10);
            }
            return;
        } else {
            initActivity();
        }


//////////////////////////////////////////////////////////////////////////////////////////////////////
        //AUTENTIFIKACIJA
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() == null) {

                    userName = null;
                    userEmail = null;
                    userImageURL = null;

                    startActivity(new Intent(MainActivity.this, LoginActivity.class));

                }
            }
        };

        mAddDayBtn.setEnabled(false);
        mPauseEngineBtn.setEnabled(false);

        mEngineTimeRunning = findViewById(R.id.time_running);


        /*Liseneri klika na dugmice*/
        //Lisener klika na dugme start an engine i trazenje lokacije

        mStartEngineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.d("START onClickListener", "Kliknuto na START!");

                if (!firstStart) {
                    Log.d("START onClickListener", "Prvi start!");
                    //TODO vrijeme promjeni u novi format "23. Sep 2017. 19:49:20"
                    timeStartedEngine = Calendar.getInstance().getTime();

                    firstStart = true;

                    new MyWeatherTask().execute();

                    if (lctn != null)
                        Log.d(TAG, "onClick: Lat: " + lctn.getLatitude() + " Long:" + lctn.getLongitude());

                }

                if (lastPause != 0) {
                    Log.d("START onClickListener", "lastPause razlicit od nule!");
                    mEngineTimeRunning.setBase(mEngineTimeRunning.getBase() + SystemClock.elapsedRealtime() - lastPause);
                } else {
                    Log.d("START onClickListener", "lastPause je nula!");
                    mEngineTimeRunning.setBase(SystemClock.elapsedRealtime());
                }
                mEngineTimeRunning.start();

                mAddDayBtn.setEnabled(true);
                mStartEngineBtn.setEnabled(false);
                mPauseEngineBtn.setEnabled(true);
            }
        });


        /*lisener klika na dugme pause an engine*/

        mPauseEngineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("PAUSE onClickListener", "Kliknuto na PAUSE!");
                lastPause = SystemClock.elapsedRealtime();
                mEngineTimeRunning.stop();
                mPauseEngineBtn.setEnabled(false);
                mStartEngineBtn.setEnabled(true);
            }
        });

        /*lisener klika na dugme addDay*/
        mAddDayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (isNetworkAvailable()) {

                    Log.d("ADD onClickListener", "Kliknuto na ADD!");
                    mAddDayBtn.setEnabled(false);

                    firstStart = false;
                    totalDrivingTime = SystemClock.elapsedRealtime() - mEngineTimeRunning.getBase();
                    String total = getHours(totalDrivingTime);


                    timeFinishedEngine = Calendar.getInstance().getTime();

                    mEngineTimeRunning.stop();
                    mEngineTimeRunning.setBase(SystemClock.elapsedRealtime());
                    lastPause = 0;

                    mStartEngineBtn.setEnabled(true);
                    mPauseEngineBtn.setEnabled(false);

                    // setuj podatke za DriverData
                    String dfTimeStart = DateFormat.getTimeInstance().format(timeStartedEngine);
                    String dfTimeFinish = DateFormat.getTimeInstance().format(timeFinishedEngine);

                    //TODO mozda ubaciti i datum u driverData da bi se moglo bolje sortirati
                    driverData.setTotalDrivingTime(total);
                    Log.d(TAG, "totalDrivingTime: " + total);
                    driverData.setUserId(userID);
                    Log.d(TAG, "userID: " + userID);
                    driverData.setTimeStarted(dfTimeStart);
                    Log.d(TAG, "timeStartedEngine: " + dfTimeStart);
                    driverData.setTimeFinished(dfTimeFinish);
                    Log.d(TAG, "timeFinishedEngine: " + dfTimeFinish);
                    driverData.setWeatherInfo(weatherDescription);
                    Log.d(TAG, "weatherMainDescription: " + weatherDescription);
                    driverData.setCity(weatherCity);
                    Log.d(TAG, "weatherCity: " + weatherCity);

                    driverDataDAO.write(driverData);

                    Toasty.success(MainActivity.this, "Record added to database", Toast.LENGTH_SHORT, true).show();
                } else {
                    Toasty.info(MainActivity.this, "Check internet connection!", Toast.LENGTH_LONG, true).show();
                }

            }
        });

        mMakeReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (driverDataDAO.getDriverDatasByUserID(userID).size() != 0) {
                    Intent intent = new Intent(MainActivity.this, DriverDataListActivity.class);
                    intent.putExtra("userId", userID);
                    startActivity(intent);
                } else {
                    Toasty.error(MainActivity.this, "No data acquired from internet", Toast.LENGTH_LONG, true).show();
                }
            }
        });

    }

    private String getHours(long totalDrivingTime) {

        int seconds = (int) (totalDrivingTime / 1000) % 60;
        int minutes = (int) ((totalDrivingTime / (1000 * 60)) % 60);
        int hours = (int) ((totalDrivingTime / (1000 * 60 * 60)) % 24);

        return hours + ":" + minutes + ":" + seconds;
    }

    private void initActivity() {

        //mPrepareActivity.setVisibility(View.GONE);
        mMakeReport.setVisibility(View.VISIBLE);

        locationManager.requestLocationUpdates("gps", 4000, 0, locationListener);


    }

    public void getWeatherInfo(double lat, double lon) {

        String lata = String.format("%.2f", lat);
        String lona = String.format("%.2f", lon);
        Log.d(TAG, "getWeatherInfo: Usao u metodu sa argumentima lat: " + lata + " long: " + lona);

        String url = "http://api.openweathermap.org/data/2.5/weather?lat=" + lata + "&lon=" + lona + "&appid=0b705ebf55d7bb01cb45bb6c1aa6ca89&units=imperial";
        Log.d(TAG, "URL: " + url);

        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {

                    JSONArray arrayWeather = response.getJSONArray("weather");
                    JSONObject object = arrayWeather.getJSONObject(0);
                    weatherMainDescription = object.getString("main");
                    weatherDescription = object.getString("description");
                    weatherCity = response.getString("name");

                    Log.d(TAG, "onResponse: main: " + weatherMainDescription + " description: " + weatherDescription + " city: " + weatherCity);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toasty.error(MainActivity.this, "Weather data not acquired!", Toast.LENGTH_LONG, true).show();
            }
        });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jor);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 10:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    initActivity();
                break;
            default:
                break;
        }
    }

    private Location getLastKnownLocation() { //vraca najprecizniju poznatu lokaciju uredjaja
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;

        for (String provider : providers) {

            Location l = locationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /* ubaci menu*/
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /* koja opcija iz menua je izabrana*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int menuItemThatWasSelected = item.getItemId();

        if (menuItemThatWasSelected == R.id.menu_log_out) {
            Log.d(TAG, "onOptionsItemSelected: menu_log_out");

            userName = null;
            userEmail = null;
            userImageURL = null;
            Log.d(TAG, "userName: " + userName + "userEmail: " + userEmail + "userImageURL: " + userImageURL);
            mAuth.signOut();

            //Toast.makeText(MainActivity.this, "Log Out clicked", Toast.LENGTH_SHORT).show();

        } else if (menuItemThatWasSelected == R.id.menu_about) {

            startActivity(new Intent(MainActivity.this, AboutActivity.class));
            Log.d(TAG, "onOptionsItemSelected: menu_about clicked");
            //Toast.makeText(MainActivity.this, "About clicked", Toast.LENGTH_SHORT).show();

        } else if (menuItemThatWasSelected == R.id.menu_driver_info) {
            Log.d(TAG, "onOptionsItemSelected: menu_driver_info");

            Intent intent = new Intent(MainActivity.this, DriverDetailActivity.class);
            intent.putExtra("user_name", userName);
            intent.putExtra("user_email", userEmail);
            intent.putExtra("user_image", userImageURL);
            startActivity(intent);
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

    /*izlazak iz aplikacije*/
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
            Toasty.info(MainActivity.this, "Press back again to exit", Toast.LENGTH_SHORT, true).show();
            lastClick = now;
        }
    }


    public class MyWeatherTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG, "onPreExecute: Uzimamo Lokaciju");
            lctn = getLastKnownLocation();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            getWeatherInfo(lctn.getLatitude(), lctn.getLongitude());
            Log.d(TAG, "doInBackground: WeatherInfo");
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.d(TAG, "onPostExecute: Imamo Weather info");
        }
    }

}
