package com.example.database;

import android.graphics.Color;

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

    // from https://stackoverflow.com/questions/46337519/how-insert-image-in-room-persistence-library
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    public byte[] image_byte_arr;
}
