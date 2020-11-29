package com.rematch.aroundme;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.AsyncQueryHandler;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.otaliastudios.cameraview.size.Size;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    // Initialise variable
    //private Button buttonImage;
    //private Button buttonMaps;


    /*
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonImage = findViewById(R.id.image_detection_btn);
        buttonMaps = findViewById(R.id.api_maps);
        buttonMaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMapsActivity();
            }
        });
        buttonImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageActivity();
            }
        });
    }

    public void openImageActivity()
    {
        Intent intent = new Intent(this, InterfaceActivity.class);
        startActivity(intent);
    }
    public void openMapsActivity()
    {
        Intent intent = new Intent(this, APImapsActivity.class);
        startActivity(intent);
    }

     */
    private Button scanButton;
    private Button listButton;
    private Bitmap imageBitmap;
    private Boolean firsttime=true;
    private ArrayList<String> listalreadySaid= new ArrayList<String>();
    private TextToSpeech tts;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(MainActivity.this);


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
                    detectTextFromImage();
                    SystemClock.sleep(3000); // Sleep 5 seconds
                    scanButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            detectTextFromImage();
                        }
                    });


/**                    if (firsttime==true) {
                        firsttime=false;
                        Log.d("test","on est dans le false");
                        detectTextFromImage();
                    }
                    else {
                        final Handler handler = new Handler();
                        Timer t = new Timer();
                        t.schedule(new TimerTask() {
                            public void run() {
                                handler.post(new Runnable() {
                                    public void run() {
                                        detectTextFromImage();
                                    }
                                });
                            }
                        }, 5000);
 }

**/


                } else if (frame.getDataClass() == Image.class) {
                    Image data = frame.getData();
                    System.out.println("Data Bonjour");
                    // Process android.media.Image...
                }
            }
        });

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
                // if text is like the last text dont speak
                if (listalreadySaid.get(listalreadySaid.size() - 1) != text) {
                    listalreadySaid.add(text);
                    Log.d("listal", String.valueOf(listalreadySaid));
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
                }
            }

        }
    }

    private String matchNearbyPlace(String[] nearby, String detected)
    {
        int i = 0;
        int maxScore = 0;
        int maxScoreIndex = 0;
        // Pour chaque nom de lieu aux alentours
        for(String name:nearby) {
            int score = 0;
            // Compter le nombre de lettres similaires
            for (char c : detected.toCharArray()) {
                if (name.contains(String.valueOf(c))) {
                    score += 1;
                }
            }
            if(score > maxScore){
                maxScore = score;
                maxScoreIndex = i;
            }
            i++;
        }
        // Si au moins une lettre a été trouvée on renvoie le nom du lieu qui a le meilleur score
        if(maxScore != 0){
            return nearby[maxScoreIndex];
        } else {
            return "";
        }
    }
}