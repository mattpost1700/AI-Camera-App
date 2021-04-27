package com.example.aicameraapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class SavePredictionDialog extends DialogFragment {

    public interface ButtonClickListner {
        public void onSaveClick();
    }

    private ButtonClickListner listner;
    private String mTitle = "";
    private String mMessage = "";
    private String mPositiveButton = "";
    private String mNegativeButton = "";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Build the dialog and set up the button click handlers
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        mTitle = getArguments().getString("title");
        mMessage = getArguments().getString("message");
        mPositiveButton = getArguments().getString("positiveButton");
        mNegativeButton = getArguments().getString("negativeButton");

        builder.setTitle(mTitle)
                .setMessage(mMessage)
                .setPositiveButton(mPositiveButton, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listner.onSaveClick();
                    }
                })
                .setNegativeButton(mNegativeButton, new
                        DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // close
                            }
                        });
        return builder.create();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("title", mTitle);
        outState.putString("message", mMessage);
        outState.putString("positiveButton", mPositiveButton);
        outState.putString("negativeButton", mNegativeButton);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listner = (ButtonClickListner) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement ButtonClickListener()");
        }
    }
}