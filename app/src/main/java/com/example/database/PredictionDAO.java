package com.example.database;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface PredictionDAO {

    @Query("SELECT * FROM predictions ORDER BY rowid DESC")
    LiveData<List<Prediction>> getAll();

    @Query("SELECT * FROM predictions WHERE rowid = :rowId")
    Prediction getById(int rowId);

    @Query("DELETE FROM predictions")
    void resetData();

    @Insert
    void insert(Prediction... predictions);

    @Update
    void update(Prediction... prediction);

    @Delete
    void delete(Prediction... prediction);

}
