package com.example.aicameraapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.database.Prediction;
import com.example.database.PredictionDatabase;
import com.example.database.PredictionViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class PredictionActivity extends AppCompatActivity {
    private ImageView lastEntry;
    private PredictionViewModel predictionViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.predictions_activity);
        Toolbar toolbar = findViewById(R.id.predictionTB);
        setSupportActionBar(toolbar);
        lastEntry = findViewById(R.id.imageView);

        RecyclerView recyclerView = findViewById(R.id.predictionsRecyclerView);
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));
        PredictionListAdapter adapter = new PredictionListAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        predictionViewModel = new ViewModelProvider(this).get(PredictionViewModel.class);
        predictionViewModel.getAllPredictions().observe(this, adapter::setPredictions);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sub, menu);
        return true;
    }

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

    public Bitmap convertByteArrayToBitmap(byte[] bitmapdata) {
        Bitmap bmp = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888);
        ByteBuffer buffer = ByteBuffer.wrap(bitmapdata);
        bmp.copyPixelsFromBuffer(buffer);
        return bmp;
    }


    public class PredictionListAdapter extends RecyclerView.Adapter<PredictionListAdapter.PredictionViewHolder> {
        class PredictionViewHolder extends RecyclerView.ViewHolder {
            private TextView predictionGuessView; // notes says final??

            private Prediction prediction;

            private PredictionViewHolder(View itemView) {
                super(itemView);

                predictionGuessView = itemView.findViewById(R.id.txtTitle);

                itemView.setOnClickListener(view -> {
                    Bitmap bitmapForLastEntry = convertByteArrayToBitmap(prediction.image_byte_arr);
                    lastEntry.setImageBitmap(bitmapForLastEntry);

                });
            }
        }

        private List<Prediction> predictions;
        private final LayoutInflater layoutInflater;

        PredictionListAdapter(Context context) {
            layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public int getItemCount() {
            if (predictions != null)
                return predictions.size();
            return 0;
        }

        @NonNull
        @Override
        public PredictionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = layoutInflater.inflate(R.layout.list_item, parent, false);
            return new PredictionViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull PredictionViewHolder holder, int position) {
            if (predictions != null) {
                Prediction currentPrediction = predictions.get(position);

                holder.prediction = currentPrediction;


                String resultString = currentPrediction.prediction_string;
                String outputString = "";

                resultString = resultString.substring(1); // removes first [
                String[] percents = resultString.split(",");

                String prediction = percents[0];
                String digit = prediction.substring(prediction.indexOf('[') + 1, prediction.indexOf(']') + 1);
                String percent = prediction.substring(prediction.indexOf('(') + 1, prediction.indexOf(')'));
                outputString += percent + " chance the number is " + digit;

                outputString = outputString.substring(0, outputString.length() - 1); // removes last new line character


                holder.predictionGuessView.setText(currentPrediction.id + ") " + outputString);

            } else {
                holder.predictionGuessView.setText("initializing...");
            }
        }

        void setPredictions(List<Prediction> predictions) {
            this.predictions = predictions;
            notifyDataSetChanged(); // causes any updates to be displayed
        }
    }
}
