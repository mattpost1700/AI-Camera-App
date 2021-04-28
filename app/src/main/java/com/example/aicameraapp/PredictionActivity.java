package com.example.aicameraapp;

import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.database.Prediction;
import com.example.database.PredictionDatabase;
import com.google.android.material.textfield.TextInputEditText;

import java.nio.ByteBuffer;

public class PredictionActivity  extends AppCompatActivity {

    private ImageView lastEntry;
    private EditText chooseEntry;
    private Button getPrediction;
    private Prediction somePrediction;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.predictions_activity);
        Toolbar toolbar = findViewById(R.id.predictionTB);
        setSupportActionBar(toolbar);
        lastEntry = findViewById(R.id.imageView);
        chooseEntry = findViewById(R.id.chooseEntry);
        getPrediction = findViewById(R.id.confirmChoice);
        getPrediction.setOnClickListener(v -> processSetLastEntry());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sub, menu);
        return true;
    }

    //menu item logic to go back to main
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.toMain) {
            Log.e("info", "toMain clicked");
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    public void processSetLastEntry(){
        Integer choice = Integer.parseInt(chooseEntry.getText().toString());
        if(choice != null){

            PredictionDatabase.getById(choice, new PredictionDatabase.PredictionListener() {
                @Override
                public void onPredictionReturned(Prediction prediction) {
                    if(prediction != null){
                        somePrediction = prediction;
                        setLastEntry(somePrediction.getImage_byte_arr());
                    }
                    else{
                        //Toast.makeText(PredictionActivity, "Prediction is null", Toast.LENGTH_LONG).show();
                    }
                }
            });

        }
        else{
            Toast.makeText(this, "Choice is null", Toast.LENGTH_LONG).show();
        }

    }
    public Bitmap convertByteArrayToBitmap(byte[] bitmapdata){
        Bitmap bmp = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888);
        ByteBuffer buffer = ByteBuffer.wrap(bitmapdata);
        bmp.copyPixelsFromBuffer(buffer);
        return bmp;
    }
    public void setLastEntry(byte[] bytes){
        Bitmap bitmapForLastEntry = convertByteArrayToBitmap(bytes);
        lastEntry.setImageBitmap(bitmapForLastEntry);
    }
}
