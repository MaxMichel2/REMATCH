package com.rematch.aroundme;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.zip.Inflater;

import javax.net.ssl.HttpsURLConnection;

public class APImapsActivity extends AppCompatActivity {
    // Initialise variable
    Spinner spType;
    //Button btFind;
    Button btStore;
    Button btBank;
    Button btATM;
    Button btHospital;
    Button btMovie;
    Button btRestaurant;


    //TextView textView;
    SupportMapFragment supportMapFragment;
    AlertDialog.Builder builder;

    // Partie Places
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    double currentLat = 0, currentLong = 0;
    List<HashMap<String, String>> possiblePlaces = null;
    int LOCATION_REQUEST_CODE = 10001;
    private static final String TAG = "InterfaceActivity";
    LocationCallback locationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if(locationResult == null){
                return;
            }
            for(Location location: locationResult.getLocations()){
                Log.d(TAG, "onLocationResults: " + location.toString());
                currentLat = location.getLatitude();
                currentLong = location.getLongitude();
            }
        }
    };

    private TextToSpeech tts;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_a_p_imaps);

        btStore = findViewById(R.id.buttonStore);
        btStore.setBackgroundColor(Color.parseColor("#5A3B5D"));
        btBank = findViewById(R.id.buttonBank);
        btBank.setBackgroundColor(Color.parseColor("#5A3B5D"));
        btATM = findViewById(R.id.buttonATM);
        btATM.setBackgroundColor(Color.parseColor("#FFDD33"));
        btHospital = findViewById(R.id.buttonHospital);
        btHospital.setBackgroundColor(Color.parseColor("#FFDD33"));
        btMovie = findViewById(R.id.buttonMovieTheater);
        btMovie.setBackgroundColor(Color.parseColor("#5A3B5D"));
        btRestaurant = findViewById(R.id.buttonRestaurant);
        btRestaurant.setBackgroundColor(Color.parseColor("#FFDD33"));

        // Initialise fused location provider client
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(4000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        builder= new AlertDialog.Builder(this);

        btATM.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // Get selected position of spinner
                // Initialize url
                String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" + //Url
                        "?location=" + currentLat + "," + currentLong + //Location latitude and longitude
                        "&radius=500" + //Nearby radius
                        "&types=" + "ATM" +
                        "&sensor=true" + //sensor
                        "&key=" + getResources().getString(R.string.google_map_key); //Google map key

                // Execute place task method to download json data
                new APImapsActivity.PlaceTask().execute(url);

                tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status != TextToSpeech.ERROR) {
                            tts.setLanguage(Locale.FRANCE);
                        }
                    }
                });
            }
        });
        btStore.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // Get selected position of spinner
                // Initialize url
                String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" + //Url
                        "?location=" + currentLat + "," + currentLong + //Location latitude and longitude
                        "&radius=500" + //Nearby radius
                        "&types=" + "store" +
                        "&sensor=true" + //sensor
                        "&key=" + getResources().getString(R.string.google_map_key); //Google map key

                // Execute place task method to download json data
                new APImapsActivity.PlaceTask().execute(url);

                tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status != TextToSpeech.ERROR) {
                            tts.setLanguage(Locale.FRANCE);
                        }
                    }
                });
            }
        });
        btHospital.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // Get selected position of spinner
                // Initialize url
                String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" + //Url
                        "?location=" + currentLat + "," + currentLong + //Location latitude and longitude
                        "&radius=500" + //Nearby radius
                        "&types=" + "hospital" +
                        "&sensor=true" + //sensor
                        "&key=" + getResources().getString(R.string.google_map_key); //Google map key

                // Execute place task method to download json data
                new APImapsActivity.PlaceTask().execute(url);

                tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status != TextToSpeech.ERROR) {
                            tts.setLanguage(Locale.FRANCE);
                        }
                    }
                });
            }
        });
        btRestaurant.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // Get selected position of spinner
                // Initialize url
                String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" + //Url
                        "?location=" + currentLat + "," + currentLong + //Location latitude and longitude
                        "&radius=500" + //Nearby radius
                        "&types=" + "restaurant" +
                        "&sensor=true" + //sensor
                        "&key=" + getResources().getString(R.string.google_map_key); //Google map key

                // Execute place task method to download json data
                new APImapsActivity.PlaceTask().execute(url);

                tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status != TextToSpeech.ERROR) {
                            tts.setLanguage(Locale.FRANCE);
                        }
                    }
                });
            }
        });
        btMovie.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // Get selected position of spinner
                // Initialize url
                String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" + //Url
                        "?location=" + currentLat + "," + currentLong + //Location latitude and longitude
                        "&radius=500" + //Nearby radius
                        "&types=" + "movie_theater" +
                        "&sensor=true" + //sensor
                        "&key=" + getResources().getString(R.string.google_map_key); //Google map key

                // Execute place task method to download json data
                new APImapsActivity.PlaceTask().execute(url);

                tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status != TextToSpeech.ERROR) {
                            tts.setLanguage(Locale.FRANCE);
                        }
                    }
                });
            }
        });
        btBank.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // Get selected position of spinner
                // Initialize url
                String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" + //Url
                        "?location=" + currentLat + "," + currentLong + //Location latitude and longitude
                        "&radius=500" + //Nearby radius
                        "&types=" + "bank" +
                        "&sensor=true" + //sensor
                        "&key=" + getResources().getString(R.string.google_map_key); //Google map key

                // Execute place task method to download json data
                new APImapsActivity.PlaceTask().execute(url);

                tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status != TextToSpeech.ERROR) {
                            tts.setLanguage(Locale.FRANCE);
                        }
                    }
                });
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check permissions
        if (ActivityCompat.checkSelfPermission(APImapsActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // When permission is granted
            // Call checkSettingsAndStartLocationUpdates
            checkSettingsAndStartLocationUpdates();
        } else {
            // When permission is denied
            // Request permission
            askLocationPermission();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLocationUpdates();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            finish();
            tts.stop();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void checkSettingsAndStartLocationUpdates(){
        LocationSettingsRequest request = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest).build();
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> locationSettingsResponseTask = client.checkLocationSettings(request);
        locationSettingsResponseTask.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // Settings of device are satisfied and we can start location updates
                startLocationUpdates();
            }
        });
        locationSettingsResponseTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Settings of devide are unsatisfied and we need to check exception
                if(e instanceof ResolvableApiException){
                    ResolvableApiException apiException = (ResolvableApiException) e;
                    try {
                        apiException.startResolutionForResult(APImapsActivity.this, 1001);
                    } catch (IntentSender.SendIntentException sendIntentException) {
                        sendIntentException.printStackTrace();
                    }
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates(){
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates(){
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void askLocationPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager
                .PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                Log.d(TAG, "askLocationPermission: you should show an alert dialog...");
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_REQUEST_CODE);
            }
        }
    }

    // Task to download the JSON data from google and return it as a String
    private class PlaceTask extends AsyncTask<String,Integer,String> {
        @Override
        protected String doInBackground(String... strings) {
            String data = null;
            try {
                // Initialize data
                data = downloadUrl(strings[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return data;
        }

        @Override
        protected void onPostExecute(String s) {
            // Execute parser task
            new APImapsActivity.ParserTask().execute(s);

        }

        private String downloadUrl(String string) throws IOException {
            // Initialize url
            URL url = new URL(string);
            // Initialize connection
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            // Connect connection
            connection.connect();
            // Initialize input stream
            InputStream stream = connection.getInputStream();
            // Initialize buffer reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            // Initialize  string builder
            StringBuilder builder = new StringBuilder();
            // Initialize string variable
            String line = "";
            // Use while loop
            while ((line = reader.readLine()) != null){
                // Append line
                builder.append(line);
            }
            // Get append data
            String data = builder.toString();
            // Close reader
            reader.close();
            // Return data
            return data;

        }
    }

    // Task to parse the data String to a List<HashMap<String,String>>
    private class ParserTask extends AsyncTask<String,Integer, List<HashMap<String,String>>> {
        @Override
        protected List<HashMap<String, String>> doInBackground(String... strings) {
            // Create Json parser class
            JsonParser jsonParser = new JsonParser();
            // Initialize hash map list
            List<HashMap<String,String>> mapList = null;
            JSONObject object = null;
            try {
                // Initialize JSON object
                object = new JSONObject(strings[0]);
                // Parse json object
                mapList = jsonParser.parseResult(object);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Display data on textview and convert text to speech
            String list_values = "";
            for (int i = 0; i < mapList.size();i++)
            {
                List values = new ArrayList(mapList.get(i).values());
                String text = values.get(1).toString();
                list_values = list_values + "\n" + text;
            }

            //textView.setText(list_values);
            tts.speak(list_values, TextToSpeech.QUEUE_FLUSH, null);

            // Return map list
            return mapList;
        }

        @Override
        protected void onPostExecute(List<HashMap<String, String>> hashMaps) {
            // Use for loop
            String results = "";
            for(int i=0; i<hashMaps.size(); i++){
//                // Initialize hash map
                HashMap<String,String> hashMapList = hashMaps.get(i);
//                //Get latitude
                double lat = Double.parseDouble(hashMapList.get("lat"));
//                //Get longitude
                double lng = Double.parseDouble(hashMapList.get("lng"));
//                //Get name
                String name = hashMapList.get("name");
//                //Concat latitude and longitude
                LatLng latLng = new LatLng(lat, lng);

                results += name + "\n" + latLng.toString() +"\n\n";
            }
        }
    }

}