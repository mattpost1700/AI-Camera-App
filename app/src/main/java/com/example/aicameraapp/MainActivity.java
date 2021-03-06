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
import android.database.Cursor;
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
import android.widget.Toast;

import com.example.database.Prediction;
import com.example.database.PredictionDatabase;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.FormatFlagsConversionMismatchException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements SavePredictionDialog.ButtonClickListener {

    private static final int REQUEST_IMAGE_CAPTURE = 1001;
    private static final int REQUEST_IMAGE_SELECT = 1002;
    public static final int MULTIPLE_PERMISSIONS = 100;
    public static final String[] permissions = {Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private String currentPhotoPath;
    private ImageView imageView;
    private Bitmap bitmapForAnalysis;
    private String resultStringToSave;
    private byte[] byteArrayToSave;
    private Button captureButton;
    private Button analyzeButton;
    private Button importButton;

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
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.mainTB);
        setSupportActionBar(toolbar);
        getPermission();
        captureButton = findViewById(R.id.captureButton);
        analyzeButton = findViewById(R.id.analyzeButton);
        importButton = findViewById(R.id.importButton);
        imageView = findViewById(R.id.imageCapture);
        PredictionDatabase.getDatabase(getApplication());

        if (savedInstanceState != null) {
            currentPhotoPath = savedInstanceState.getString("imageFilePath");
            if(currentPhotoPath != null && !currentPhotoPath.equals(""))
                displayImage();
        }

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
        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });
        initTensorFlowAndLoadModel();
    }

    /**
     * Get's permission to use the phone's camera.
     */
    public void getPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(permissions,
                    MULTIPLE_PERMISSIONS);
        }
    }

    /**
     * Displays the image in the image view.
     */
    private void displayImage() {
        if (currentPhotoPath != null) {
            // checkPicture = true; depreciated, try catch to catch if photo exists
            Bitmap temp = fixOrientation(BitmapFactory.decodeFile(currentPhotoPath));
            bitmapForAnalysis = temp;
            imageView.setImageBitmap(temp);
        } else {
            Toast.makeText(this, "Image Path is null", Toast.LENGTH_LONG).show();
        }
    }

    //This is necessary to make sure the photo is always oriented properly
    /**
     * Keeps photo's orientation correct.
     *
     * @param bitmap The image bitmap.
     * @return The image bitmap with the correct orientation.
     */
    private Bitmap fixOrientation(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

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

    /**
     * This creates model from the TensorFlow lite file.
     */
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
    /**
     * Analyzes the image for digits using the TensorFlow model.
     */
    private void analyzeImage() { // replace check picture with try catch for robustness
        try {//if user did select a picture
            bitmapForAnalysis = Bitmap.createScaledBitmap(bitmapForAnalysis, INPUT_SIZE, INPUT_SIZE, false);

            final List<Classifier.Recognition> results = classifier.recognizeImage(bitmapForAnalysis);

            int size = bitmapForAnalysis.getRowBytes() * bitmapForAnalysis.getHeight();
            ByteBuffer byteBuffer = ByteBuffer.allocate(size);
            bitmapForAnalysis.copyPixelsToBuffer(byteBuffer);

            // These need to be saved to private member variables in order
            //  to prevent successively piping too much info down multiple methods

            byteArrayToSave = byteBuffer.array();
            resultStringToSave = results.toString();


            //This code has been moved to the onSaveClick Interface method
            /*
            Prediction p = new Prediction(0, results.toString(), byteArray);
            Log.d("database", "Prediction before adding to db... id: ? prediction string: " + results.toString() + " bytearr: " + byteArray);
            PredictionDatabase.insert(p);
            //PredictionDatabase.insert(new Prediction(1, "please", new byte[1]));
            */

            //This toast has been made shorter.
            Toast.makeText(this, "Picture has been successfully analyzed", Toast.LENGTH_SHORT).show();
            displayPredictionResult(results.toString());
        } catch (NullPointerException e) {//if user didn't select a picture, will just simply display a toast message
            Toast.makeText(this, "No image has been selected", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Displays the formatted result in predictionsTextView.
     *
     * @param resultString The AI's result output as a string.
     */
    private void displayPredictionResult(String resultString) {
        Log.d("displayPredictionResult", "result string: " + resultString);

        SavePredictionDialog dialog = new SavePredictionDialog();
        Bundle b = new Bundle();
        b.putString("result_string", resultString);
        b.putString("title", getResources().getString(R.string.save_dialog_title));
        b.putString("message", formatText(resultString));
        b.putString("positiveButton", getResources().getString(R.string.save_dialog_positive_button));
        b.putString("negativeButton", getResources().getString(R.string.save_dialog_negative_button));
        dialog.setArguments(b);
        dialog.show(getSupportFragmentManager(), "saveDialog");
    }

    /**
     * Formats text for dialog
     * @param resultString AI's output
     * @return formatted string
     */
    public String formatText(String resultString) {
        String outputString = "";

        resultString = resultString.substring(1); // removes first [
        String[] percents = resultString.split(",");

        for(int i = 0; i < percents.length && i < 3; i++) { // at most top 3 predictions
            String prediction = percents[i];
            Log.d("displayPredictionResult", "prediction @" + i + " " + prediction);

            String digit   = prediction.substring(prediction.indexOf('[') + 1, prediction.indexOf(']'));
            String percent = prediction.substring(prediction.indexOf('(') + 1, prediction.indexOf(')'));
            // outputString += "There is a " + percent + " chance the number is " + digit + '\n';
            outputString += percent + " chance the number is " + digit + '\n';
        }

        return outputString.substring(0, outputString.length() - 1); // removes last new line character
    }

    /**
     * Saves current prediction to db.
     */
    public void onSaveClick() {
        Log.d("database", "onSaveClick invoked.");
        Prediction p = new Prediction(0, resultStringToSave, byteArrayToSave);
        Log.d("database", "Prediction before adding to db... id: ? prediction string: " + resultStringToSave + " bytearr: " + byteArrayToSave);
        PredictionDatabase.insert(p);
        // save to db
    }

    /**
     * Creates the file from global variable currentPhotoPath
     * @return The image as a File object.
     * @throws IOException On input error
     */
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

    /**
     * Sends an intent to take a picture with the phone's camera.
     */
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

    /**
     * Sends an intent to get a picture from the phone's gallery.
     */
    private void openGallery() {
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, REQUEST_IMAGE_SELECT);
    }

    /**
     * Displays the selected image in the image view.
     *
     * @return A null value if there is not picture to display.
     * @param data The selected image.
     */
    private void displaySelectedImage(Intent data) {
        if (data == null) {
            return;
        }

        // checkPicture = true; depreciated, try catch to catch if photo exists
        Uri selectedImage = data.getData();
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(selectedImage,
                filePathColumn, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        currentPhotoPath = cursor.getString(columnIndex);
        cursor.close();
        Bitmap temp = fixOrientation(BitmapFactory.decodeFile(currentPhotoPath));
        bitmapForAnalysis = temp;
        imageView.setImageBitmap(temp);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);
        savedState.putString("imageFilePath", currentPhotoPath);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);
        savedState.putString("imageFilePath", currentPhotoPath);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            displayImage();
        } else if (requestCode == REQUEST_IMAGE_SELECT) {
            displaySelectedImage(data);
        }
    }

    //create menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //logic for menu item
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.toPrediction:
                Log.d("info", "toPrediction clicked");
                Activity activity = new PredictionActivity();
                Intent intent = new Intent(this, PredictionActivity.class);
                startActivity(intent);
                return true;
            case R.id.toSettings:
                Log.d("info", "toSettings clicked");
                Activity activity2 = new SettingsActivity();
                Intent intent2 = new Intent(this, SettingsActivity.class);
                startActivity(intent2);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MULTIPLE_PERMISSIONS) {
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


}