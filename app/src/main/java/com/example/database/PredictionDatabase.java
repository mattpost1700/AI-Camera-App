package com.example.database;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;


@Database(entities = {Prediction.class}, version = 1, exportSchema = false)
public abstract class PredictionDatabase extends RoomDatabase {

    public interface PredictionListener {
        void onPredictionReturned(Prediction prediction);
    }

    public abstract PredictionDAO predictionDAO();

    private static PredictionDatabase INSTANCE;

    public static PredictionDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (PredictionDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), PredictionDatabase.class, "joke_database").addCallback(createPredictionDatabaseCallback).build();
                }
            }
        }
        return INSTANCE;
    }

    private static RoomDatabase.Callback createPredictionDatabaseCallback = new RoomDatabase.Callback() {
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            insert(new Prediction(99, "Not a prediction", new byte[5])); // for testing
        }
    };

    // varies from notes, types and returns
    public static void insert(Prediction prediction) {
        new AsyncTask<Prediction, Void, Void>() {
            protected Void doInBackground(Prediction... predictions) { // Background Thread
                INSTANCE.predictionDAO().insert(predictions[0]);
                Log.d("database", "Prediction added to db... id: " + predictions[0].predictionId + " prediction string: " + predictions[0].prediction_string + " bytearr: " + predictions[0].image);
                return null;
            }
        }.execute(prediction);
    }

    public static void getPrediction(int id, final PredictionListener listener) {
        new AsyncTask<Integer, Void, Prediction>() {
            @Override
            protected Prediction doInBackground(Integer... integers) { // Background Thread
                return INSTANCE.predictionDAO().getById(integers[0]);
            }

            @Override
            protected void onPostExecute(Prediction prediction) { // UI Thread
                super.onPostExecute(prediction);
                listener.onPredictionReturned(prediction);
            }
        }.execute(id);
    }

    // NOTE: Varys from notes but parsing Integers doeesn't line up with DAO, nor does it make sense.
    public static void delete(Prediction prediction) {
        new AsyncTask<Prediction, Void, Void>() {
            protected Void doInBackground(Prediction... predictions) {
                INSTANCE.predictionDAO().delete(predictions[0]);
                return null;
            }
        }.execute(prediction);
    }
//    From notes (and WRONG)
//    public static void delete(int predictionId) {
//        new AsyncTask<Integer, Void, Void>() {
//            protected Void doInBackground(Integer... integers) {
//                INSTANCE.predictionDAO().delete(integers[0]);
//                return null;
//            }
//        }.execute(predictionId);
//    }

    public static void update(Prediction prediction) {
        new AsyncTask<Prediction, Void, Void>() {
            protected Void doInBackground(Prediction... predictions) {
                INSTANCE.predictionDAO().update(predictions);
                return null;
            }
        }.execute(prediction);
    }
}
