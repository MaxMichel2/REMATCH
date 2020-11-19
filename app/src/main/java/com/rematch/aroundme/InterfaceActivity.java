package com.rematch.aroundme;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.AsyncQueryHandler;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.shapes.PathShape;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

public class InterfaceActivity extends AppCompatActivity {
    private ImageView imageView;
    private TextView textView;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private Bitmap imageBitmap;
    private Button reloadButton;

    // Partie Places
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    double currentLat = 0, currentLong = 0;
    List<HashMap<String, String>> possiblePlaces = null;
    int LOCATION_REQUEST_CODE = 10001;
    private static final String TAG = "InterfaceActivity";
    boolean shouldStartActivityFlag = true;
    // Function called at every location update
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
                // create the request url for Google Places API
                String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" + //Url
                        "?location=" + currentLat + "," + currentLong + //Location latitude and longitude
                        "&radius=100" + //Nearby radius
                        "&sensor=true" + //sensor
                        "&key=" + getResources().getString(R.string.google_map_key); //Google map key
                // Execute place task method to download json data
                new InterfaceActivity.PlaceTask().execute(url);
                if(shouldStartActivityFlag == true){
                    // If the flag is true and we got a location value : open camera
                    shouldStartActivityFlag = false;
                    dispatchTakePictureIntent();
                }
            }
        }
    };


    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interface);
        FirebaseApp.initializeApp(InterfaceActivity.this);

        imageView = findViewById(R.id.image_view);
        textView = findViewById(R.id.text_display);
        textView.setMovementMethod(new ScrollingMovementMethod());
        reloadButton = findViewById(R.id.reloadButton);
        reloadButton.setVisibility(View.GONE);
        reloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    shouldStartActivityFlag = true;
                    Intent intent = new Intent(InterfaceActivity.this, InterfaceActivity.class);
                    startActivity(intent);
                }
            }
        );
//        dispatchTakePictureIntent();
        textView.setText("");

        // Initialise fused location provider client
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(4000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Check GPS permissions
        if (ActivityCompat.checkSelfPermission(InterfaceActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // When permission is granted
            // Call checkSettingsAndStartLocationUpdates
            checkSettingsAndStartLocationUpdates();
        } else {
            // When permission is denied
            // Request permission
            askLocationPermission();
        }

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.FRANCE);
                }
            }
        });
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

    private void dispatchTakePictureIntent()
    {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null){
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
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
                        apiException.startResolutionForResult(InterfaceActivity.this, 1001);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            reloadButton.setVisibility(View.VISIBLE);
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            imageView.setImageBitmap(imageBitmap);
            detectTextFromImage();
        }
    }

    private void detectTextFromImage()
    {
        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(imageBitmap);
        FirebaseVisionTextDetector firebaseVisionTextDetector = FirebaseVision.getInstance().getVisionTextDetector();
        firebaseVisionTextDetector.detectInImage(firebaseVisionImage).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
            @Override
            public void onSuccess(FirebaseVisionText firebaseVisionText)
            {
                displayTextFromImage(firebaseVisionText );

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e)
            {
                Toast.makeText(InterfaceActivity.this, "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.d("Error: ", e.getMessage());

            }
        });
    }

    private void displayTextFromImage(FirebaseVisionText firebaseVisionText)
    {
        List<FirebaseVisionText.Block> blockList = firebaseVisionText.getBlocks();
        String fullText = "";
        if (blockList.size()==0){
            Toast.makeText(this, "No text found in Image.", Toast.LENGTH_SHORT).show();
            tts.speak("Pas de texte trouvé. Veuillez reprendre la photo.", TextToSpeech.QUEUE_FLUSH, null);
        }
        else {
            for (FirebaseVisionText.Block block : firebaseVisionText.getBlocks()){
                String text = block.getText();
                fullText = fullText + text;
//                textView.setText(placesToSay);
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }

            String debug = "";
            // Initiate Levenshtein Distance calculator
            LevenshteinCalculator levenshteinCalculator = new LevenshteinCalculator();
            Integer minDistance = Integer.MAX_VALUE;
            List<HashMap<String,String>> bestPlaces = null;
            // Compute all the Levenshtein distances
            for (int i = 0; i < possiblePlaces.size(); i++)
            {
                HashMap<String,String> currentPlace = possiblePlaces.get(i);
                int distance = levenshteinCalculator.levenshteinDistance(fullText, currentPlace.get("name"));
                currentPlace.put("LevenshteinDistance", Integer.toString(distance));
                debug = debug + currentPlace.get("name") + " : " + currentPlace.get("LevenshteinDistance") + "\n";
            }
            textView.setText(debug);
            // Search the place with the lowest distance
            for (int i = 0; i < possiblePlaces.size(); i++)
            {
                HashMap<String,String> currentPlace = possiblePlaces.get(i);
                Integer currentDistance = Integer.parseInt(currentPlace.get("LevenshteinDistance"));
                if(currentDistance < minDistance){
                    minDistance = currentDistance;
                    bestPlaces = new ArrayList<HashMap<String, String>>();
                    bestPlaces.add(currentPlace);
                } else if(currentDistance == minDistance){
                    bestPlaces.add(currentPlace);
                }
            }
            String placesToSay = "";
            // Create the string that will be said
            for (int i = 0; i < bestPlaces.size(); i++)
            {
                placesToSay = placesToSay + bestPlaces.get(i).get("name") + " : " + bestPlaces.get(i).get("LevenshteinDistance") + "\n";
            }
            textView.setText(placesToSay);
            tts.speak(placesToSay, TextToSpeech.QUEUE_ADD, null);
        }
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == LOCATION_REQUEST_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                // When permission is granted
                // Call getCurrentLocation
                checkSettingsAndStartLocationUpdates();
            } else {
                // WHen permission is not granted
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
            new InterfaceActivity.ParserTask().execute(s);
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
            List<HashMap<String, String>> mapList = null;
            JSONObject object = null;
            try {
                // Initialize JSON object
                object = new JSONObject(strings[0]);
                // Parse json object
                mapList = jsonParser.parseResult(object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return mapList;
        }

        @Override
        protected void onPostExecute(List<HashMap<String, String>> hashMaps) {
            possiblePlaces = hashMaps;
        }
    }

}