package com.rematch.aroundme;

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

public class InterfaceActivity extends AppCompatActivity {
    private ImageView imageView;
    private TextView textView;
    private Bitmap imageBitmap;

    private TextToSpeech tts;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interface);
        FirebaseApp.initializeApp(InterfaceActivity.this);

        imageView = findViewById(R.id.image_view);
        CameraView camera = findViewById(R.id.camera);
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
                                             imageView.setImageBitmap(imageBitmap);
                                             detectTextFromImage();


                                         } else if (frame.getDataClass() == Image.class) {
                                             Image data = frame.getData();
                                             System.out.println("Data Bonjour");
                                                 // Process android.media.Image...
                                             }
                                         }
                                     });


        textView = findViewById(R.id.text_display);
        textView.setText("");

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.FRANCE);
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
                Toast.makeText(InterfaceActivity.this, "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.d("Error: ", e.getMessage());

            }
        });
    }

    private void displayTextFromImage(FirebaseVisionText firebaseVisionText)
    {
        List<FirebaseVisionText.Block> blockList = firebaseVisionText.getBlocks();
        if (blockList.size()==0){
            Toast.makeText(this, "No text found in Image.", Toast.LENGTH_SHORT).show();
            tts.speak("Pas de texte trouvé. Veuillez reprendre la photo.", TextToSpeech.QUEUE_FLUSH, null);
        }
        else {
            for (FirebaseVisionText.Block block : firebaseVisionText.getBlocks()){
                String text = block.getText();
                textView.setText(text);
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }

        }
    }

}