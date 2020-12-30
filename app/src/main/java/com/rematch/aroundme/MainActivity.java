package com.rematch.aroundme;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.location.Location;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.renderscript.ScriptGroup;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.URL;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    private Button scanButton;
    private Button listButton;
    private Bitmap imageBitmap;
    private Boolean firsttime=true;
    private ArrayList<String> listalreadySaid= new ArrayList<String>();
    private TextToSpeech tts;
    private ArrayList<String> nearbyPlaces = new ArrayList<String>();

    // Partie Places
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    double currentLat = 0, currentLong = 0;
    int LOCATION_REQUEST_CODE = 10001;
    private static final String TAG = "MainActivity";
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
                // Initialize url
                String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" + //Url
                        "?location=" + currentLat + "," + currentLong + //Location latitude and longitude
                        "&radius=50" + //Nearby radius
                        "&sensor=true" + //sensor
                        "&key=" + getResources().getString(R.string.google_map_key); //Google map key
                // Execute place task method to download json data
                new MainActivity.PlaceTask().execute(url);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(MainActivity.this);

        // Initialise fused location provider client
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(4000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        CameraView camera = findViewById(R.id.camera);
        listalreadySaid.add("");

        scanButton = findViewById(R.id.button);
        scanButton.setBackgroundColor(Color.parseColor("#5A3B5D"));
        scanButton.setText("SCAN");
        scanButton.setTextColor(Color.parseColor("#FFDD33"));

        listButton = findViewById(R.id.button2);
        listButton.setBackgroundColor(Color.parseColor("#FFDD33"));
        listButton.setText("LISTE");
        listButton.setTextColor(Color.parseColor("#5A3B5D"));
        listButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent= new Intent(MainActivity.this, APImapsActivity.class);
                startActivity(intent);
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
        camera.setLifecycleOwner(this);

        camera.addFrameProcessor(new FrameProcessor() {
            @Override
            @WorkerThread
            public void process(@NonNull Frame frame) {

                if (frame.getDataClass() == byte[].class) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    YuvImage yuvImage = new YuvImage((byte[]) frame.getData(), ImageFormat.NV21, frame.getSize().getWidth(), frame.getSize().getHeight(), null);
                    yuvImage.compressToJpeg(new Rect(0, 0, frame.getSize().getWidth(), frame.getSize().getHeight()), 90, out);
                    byte[] data = out.toByteArray();

                    imageBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    imageBitmap = RotateBitmap(imageBitmap, 90);
                    SystemClock.sleep(3000); // Sleep 5 seconds
                    detectTextFromImage();
                    scanButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            detectTextFromImage();
                        }
                    });
                } else if (frame.getDataClass() == Image.class) {
                    Image data = frame.getData();
                    System.out.println("Data Bonjour");
                    // Process android.media.Image...
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check permissions
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
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
                        apiException.startResolutionForResult(MainActivity.this, 1001);
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


    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
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
                Toast.makeText(MainActivity.this, "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.d("Error: ", e.getMessage());

            }
        });
    }

    private void displayTextFromImage(FirebaseVisionText firebaseVisionText)
    {
        List<FirebaseVisionText.Block> blockList = firebaseVisionText.getBlocks();
        if (blockList.size()==0){
            Toast.makeText(this, "No text found in Image.", Toast.LENGTH_SHORT).show();
        }
        else {
            for (FirebaseVisionText.Block block : firebaseVisionText.getBlocks()) {
                String text = block.getText();
                Log.d("TEXTE TROUVE", text);
//                String matchingPlace;
//                // If the app found nearby points of interests :
//                if(!nearbyPlaces.isEmpty()){
//                    // Changer le texte à dire avec le nom du lieu trouvé
//                    text = matchNearbyPlace(nearbyPlaces, text);
//                }

                // if text is like the last text dont speak
                if (!listalreadySaid.contains(text)) {
                    listalreadySaid.add(text);
                    Log.d("listal", String.valueOf(listalreadySaid));
                    tts.speak(text, TextToSpeech.QUEUE_ADD, null);
                }
            }

        }
    }

    private String matchNearbyPlace(ArrayList<String> nearby, String detected)
    {
        int i = 0;
        double maxScore = 0;
        int maxScoreIndex = 0;
        detected = detected.toLowerCase();
        // Pour chaque nom de lieu aux alentours
        for(String name:nearby) {
            name = name.toLowerCase();
            double score = 0.0;
            // Compter le nombre de lettres similaires
            for (char c : detected.toCharArray()) {
                if (name.contains(String.valueOf(c))) {
                    score += 1;
                }
            }
            // On pondère par la longueur du nom
            score = score / Math.sqrt(name.length());
            // Reconnaissance de mots français
            try {
                // On lit le dico fr
                ArrayList<String> dictionnaireFr = readWordList();
                // On récupère les mots détectés
                String[] detectedWords = detected.split(" ");
                // Pour chaque mot détecté, regarder s'il existe vraiment
                for(String wordInDetected:detectedWords){
                    // peut-être, ici, assouplir avec une acceptation du mot si la distance de levenstein est < 1 ou 2
                    if(dictionnaireFr.contains(wordInDetected)){
                        // gestion des mots qui existent
                        // Si le mot est francais et apparait dans
                        for(String wordInName:name.split(" ")){
                            if(wordInDetected.equals(wordInName)){
                                // Formule pour limiter l'impact des tous petits mots (de, par, a, etc)
                                score += 5 + wordInDetected.length();
                            }
                        }
                        // requete sur API google avec mot clef
                        // Boost du score si ça match avec le nom d'un magasin
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(name + " " + score);
            if(score > maxScore){
                maxScore = score;
                maxScoreIndex = i;
                System.out.println("New best name : " + name + " " + String.valueOf(score));
            }
            i++;
        }
        // Si au moins une lettre a été trouvée on renvoie le nom du lieu qui a le meilleur score
        // Peut-être : remplacer par <5 pour ne pas dire des conneries
        if(maxScore != 0){
            return nearby.get(maxScoreIndex);
        } else {
            return "";
        }
    }

    private ArrayList<String> readWordList() throws IOException{
        ArrayList<String> list = new ArrayList<String>();
        String word = "";
        InputStream file = this.getResources().openRawResource(R.raw.liste_francais_utf8);
        BufferedReader reader = new BufferedReader(new InputStreamReader(file));
        while(true) {
            try {
                if ((word = reader.readLine()) == null) break;
            } catch (IOException e) {
                e.printStackTrace();
            }
            list.add(word);
        }
        file.close();
        return list;
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
            new MainActivity.ParserTask().execute(s);
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

            // Return map list
            return mapList;
        }

        @Override
        protected void onPostExecute(List<HashMap<String, String>> hashMaps) {
            // Use for loop
            for(int i=0; i<hashMaps.size(); i++){
//                // Initialize hash map
                HashMap<String,String> hashMapList = hashMaps.get(i);
//                //Get name
                String name = hashMapList.get("name");

                nearbyPlaces.add(name);
            }
        }
    }
}