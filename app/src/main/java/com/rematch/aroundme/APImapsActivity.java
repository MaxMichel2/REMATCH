package com.rematch.aroundme;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
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
    Button btFind;
    TextView textView;
    SupportMapFragment supportMapFragment;
    GoogleMap map;
    FusedLocationProviderClient client;
    double currentLat = 0, currentLong = 0;

    private TextToSpeech tts;

    @Override
    protected void onResume() {
        super.onResume();

        // Check permissions
        if (ActivityCompat.checkSelfPermission(APImapsActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // When permission is granted
            // Call getCurrentLocation
            getCurrentLocation();
        } else {
            // When permission is denied
            // Request permission
            ActivityCompat.requestPermissions(APImapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_a_p_imaps);

        // Assign variable
        textView = findViewById(R.id.text_view);
        spType = findViewById(R.id.sp_type);
        btFind = findViewById(R.id.bt_find);
        supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);

        // Initialise fused location provider client
        client = LocationServices.getFusedLocationProviderClient(this);

        // Initialise array of place type
        final String[] placeTypeList = {"atm", "bank", "hospital", "movie_theater", "restaurant"};
        // Initialise array of place names
        String[] placeNameList = {"ATM", "Bank", "Hospital", "Movie Theater", "Restaurant"};

        // Set adapter for Spinner
        spType.setAdapter(new ArrayAdapter<String>(APImapsActivity.this
                , android.R.layout.simple_spinner_dropdown_item, placeNameList));

        btFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get selected position of spinner
                int i = spType.getSelectedItemPosition();
                // Initialize url
                String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" + //Url
                        "?location=" + currentLat + "," + currentLong + //Location latitude and longitude
                        "&radius=500" + //Nearby radius
                        "&types=" + placeTypeList[i] + //place type
                        "&sensor=true" + //sensor
                        "&key="; //Google map key

                // Execute place task method to download json data
                new APImapsActivity.PlaceTask().execute(url);
            }
        });

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.FRANCE);
                }
            }
        });

    }

    private void getCurrentLocation() {
        // Initialise Task<Location>
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        // TODO: Change the fused Location call to a continuous GPS call (subscribe to "requestLocationUpdates" method)
        //      For now, if the GPS has no fix at the opening of the app, we get 0.0, 0.0 as location.

        Task<Location> task = client.getLastLocation();

        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(final Location location) {
                // When successful
                if (location != null){
                    // Get current latitude
                    currentLat = location.getLatitude();
                    // Get current longitude
                    currentLong = location.getLongitude();
                    // Sync map
                    supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {
                            // When map is ready
                            map = googleMap;

                            // Initialise LatLng
                            LatLng latLng = new LatLng(currentLat, currentLong);

//                            // Initialize fake lat lng for development purpose
//                            currentLat = 45.763850;
//                            currentLong = 4.868139;
//                            LatLng latLng = new LatLng(currentLat, currentLong);

                            // Add marker
                            MarkerOptions options = new MarkerOptions().position(latLng)
                                    .title("I am here");

                            // Zoom map
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));

                            // Add marker on map
                            googleMap.addMarker(options);
                        }
                    });
                } else {
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 44){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                // When permission is granted
                // Call getCurrentLocation
                getCurrentLocation();
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

            textView.setText(list_values);
            tts.speak(list_values, TextToSpeech.QUEUE_FLUSH, null);

            // Return map list
            return mapList;
        }

        @Override
        protected void onPostExecute(List<HashMap<String, String>> hashMaps) {
            // Clear map
            map.clear();
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
                // Initialize marker options
                MarkerOptions options = new MarkerOptions();
                // Set position
                options.position(latLng);
                // Set title
                options.title(name);
                // Add marker on map
                map.addMarker(options);

                results += name + "\n" + latLng.toString() +"\n\n";
            }
        }
    }

}