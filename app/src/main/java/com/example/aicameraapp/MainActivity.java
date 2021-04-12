package com.example.aicameraapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1001;
    public static final int MULTIPLE_PERMISSIONS = 100;
    public static final String[] permissions = {Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private String currentPhotoPath;
    private ImageView imageView;
    private Bitmap bitmapForAnalysis;
    private Button captureButton;
    private Button analyzeButton;
    private TextView prediction;
    private boolean checkPicture = false;

    /*I'll be adding other models that we can experiment with
      to the project. In order to use a different model, replace
      MODEL_PATH with the title of the correct model in the ml directory
     */

    /*A new and improved model has been added to the ml folder and it's path
      has replaced the old one in this variable.
     */
    private static final String MODEL_PATH = "model_for_digits_ver_0.3.tflite";
    /*There's a different type of model called Quant, however, it might
      be deprecated. Always keep false for now.
      */
    private static final boolean QUANT = false;
    private static final String LABEL_PATH = "labels.txt";
    private static final int INPUT_SIZE = 224;

    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.mainTB);
        setSupportActionBar(toolbar);
        getPermission();
        captureButton = findViewById(R.id.captureButton);
        analyzeButton = findViewById(R.id.analyzeButton);
        imageView = findViewById(R.id.imageCapture);
        prediction = findViewById(R.id.predictions);

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });
        analyzeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analyzeImage();
            }
        });
        initTensorFlowAndLoadModel();
    }

    //Oh pretty please...
    public void getPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(permissions,
                    MULTIPLE_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == MULTIPLE_PERMISSIONS){
            Toast.makeText(this, "Permissions Granted", Toast.LENGTH_SHORT).show();
        }
    }

    /*This is necessary into order to properly release resources,
      the garbage collector doesn't just do it on its own.
    */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                classifier.close();
            }
        });
    }

    private void displayImage(){
        if(currentPhotoPath != null){
            checkPicture = true;
            Bitmap temp = fixOrientation(BitmapFactory.decodeFile(currentPhotoPath));
            bitmapForAnalysis = temp;
            imageView.setImageBitmap(temp);
        }
        else{
            Toast.makeText(this, "Image Path is null", Toast.LENGTH_LONG).show();
        }
    }

    //This is necessary to make sure the photo is always oriented properly
    private Bitmap fixOrientation(Bitmap bitmap){
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(currentPhotoPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                break;
        }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return bitmap;
    }

    //This creates model from the TensorFlow lite file
    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            MODEL_PATH,
                            LABEL_PATH,
                            INPUT_SIZE,
                            QUANT);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

    //This method will launch the predictions_activity
    private void analyzeImage() {
        if (checkPicture) { //if user did select a picture
            bitmapForAnalysis = Bitmap.createScaledBitmap(bitmapForAnalysis, INPUT_SIZE, INPUT_SIZE, false);

            final List<Classifier.Recognition> results = classifier.recognizeImage(bitmapForAnalysis);

            //This toast has been made shorter.
            Toast.makeText(this, "Picture has been successfully analyzed", Toast.LENGTH_SHORT).show();
            prediction.setText(results.toString());
        }
        else //if user didn't select a picture, will just simply display a toast message
            Toast.makeText(this, "No image has been selected", Toast.LENGTH_LONG).show();
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_IMAGE_CAPTURE) {
            displayImage();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id){
            case R.id.toPrediction:
                Log.e("info", "toPrediction clicked");
                Activity activity = new PredictionActivity();
                Intent intent = new Intent(this, PredictionActivity.class);
                startActivity(intent);
                return true;
            case R.id.toSettings:
                Log.e("info", "toSettings clicked");
                Activity activity2 = new SettingsActivity();
                Intent intent2 = new Intent(this, SettingsActivity.class);
                startActivity(intent2);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}