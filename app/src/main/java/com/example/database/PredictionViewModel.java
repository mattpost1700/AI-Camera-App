package com.example.database;

import android.app.Application;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aicameraapp.MainActivity;

import java.util.List;

import static android.os.Build.VERSION_CODES.R;

public class PredictionViewModel extends AndroidViewModel {
    private final LiveData<List<Prediction>> predictions;

    public PredictionViewModel(Application application) {
        super(application);
        predictions = PredictionDatabase.getDatabase(getApplication()).predictionDAO().getAll();
    }

    public LiveData<List<Prediction>> getAllPredictions() {
        return predictions;
    }
}
