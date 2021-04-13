package com.example.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "predictions")
public class Prediction {
    int predictionId;
    String predictionsAsString;
    byte[] image;

    public Prediction(int predictionId, String predictionsAsString, byte[] image) {
        this.predictionId = predictionId;
        this.predictionsAsString = predictionsAsString;
        this.image = image;
    }

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "rowid")
    public int id;

    @ColumnInfo(name = "prediction_string")
    public String prediction_string;

    @ColumnInfo(name = "image_byte_arr")
    public String image_byte_arr;
}
