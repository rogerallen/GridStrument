package com.gmail.rallen.gridstrument;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import java.util.ArrayList;

// found a template for this at
// http://www.i-programmer.info/programming/android/7647-android-adventures-a-numberpicker-dialogfragment-project.html
// which was nice
public class TuningDialogFragment extends DialogFragment {
    private static final String        ARG_values = "values";
    private ArrayList<Integer> values = new ArrayList<>();
    private NumberPicker[]             numPickers;
    private OnTuningDialogDoneListener mListener;

    public interface OnTuningDialogDoneListener {
        void onTuningDialogDone(ArrayList<Integer> values);
    }

    public TuningDialogFragment() {
        // empty public constructor
    }

    public static TuningDialogFragment newInstance(ArrayList<Integer> initValues) {
        TuningDialogFragment theDialog = new TuningDialogFragment();
        Bundle args = new Bundle(); // TODO really necessary?
        args.putIntegerArrayList(ARG_values, initValues);
        theDialog.setArguments(args);
        return theDialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            values = getArguments().getIntegerArrayList(ARG_values);
        }
        // Android may cause you to save/restore state
        if (savedInstanceState != null) {
            values = savedInstanceState.getIntegerArrayList("CurValues");
        }
        numPickers = new NumberPicker[values.size()];
    }

    // Android may cause you to save/restore state
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntegerArrayList("CurValues", values);
    }

    private void getValuesFromPickers() {
        for (int i = 0; i < values.size(); i++) {
            values.set(i, numPickers[i].getValue());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        LinearLayout linLayoutH = new LinearLayout(getActivity());
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        linLayoutH.setLayoutParams(params);

        for (int i = 0 ; i < values.size(); i++){
            numPickers[i] = new NumberPicker(getActivity());
            // turn off the keyboard entry
            numPickers[i].setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
            numPickers[i].setMaxValue(127);
            numPickers[i].setMinValue(0);
            numPickers[i].setValue(values.get(i));
            linLayoutH.addView(numPickers[i]);
        }

        LinearLayout linLayoutV = new LinearLayout(getActivity());
        linLayoutV.setOrientation(LinearLayout.VERTICAL);
        linLayoutV.addView(linLayoutH);

        Button okButton = new Button(getActivity());
        okButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getValuesFromPickers();
                        if (mListener != null) {
                            mListener.onTuningDialogDone(values);
                        }
                        dismiss();
                    }
                });
        params.gravity = Gravity.CENTER_HORIZONTAL;
        okButton.setLayoutParams(params);
        okButton.setText("Done");
        // TODO - Cancel button?
        linLayoutV.addView(okButton);

        return linLayoutV;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnTuningDialogDoneListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnTuningDialogDoneListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
