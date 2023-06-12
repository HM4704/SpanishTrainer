package com.hmgmbh.spanishtrainer.ui.train;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hmgmbh.spanishtrainer.R;

public class TrainFragment extends Fragment {

    private TrainViewModel mViewModel;

    public static TrainFragment newInstance() {
        return new TrainFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.train_fragment, container, false);


        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(TrainViewModel.class);
        // TODO: Use the ViewModel
        mViewModel.getGermanText().observe(getViewLifecycleOwner(), text -> {
            // update UI
            TextView germanText = (TextView) getView().findViewById(R.id.germanText);
            germanText.setText(text);
        });

        mViewModel.readData(getContext());
    }

    @Override
    public void onStop() {
        super.onStop();
        mViewModel.stop();
    }
}