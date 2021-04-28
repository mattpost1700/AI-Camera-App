package com.example.database;

import android.graphics.Color;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "predictions")
public class Prediction {

    public Prediction(int id, String prediction_string, byte[] image_byte_arr) {
        this.id = id;
        this.prediction_string = prediction_string;
        this.image_byte_arr = image_byte_arr;
    }
    public Prediction(Prediction p){
        this.id = p.id;
        this.prediction_string = p.prediction_string;
        this.image_byte_arr = p.image_byte_arr;
    }
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "rowid")
    public int id;

    @ColumnInfo(name = "prediction_string")
    public String prediction_string;

    // from https://stackoverflow.com/questions/46337519/how-insert-image-in-room-persistence-library
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    public byte[] image_byte_arr;

    public byte[] getImage_byte_arr() {
        return image_byte_arr;
    }
}
