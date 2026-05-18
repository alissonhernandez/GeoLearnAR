package com.example.guiaeducativaar.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.guiaeducativaar.R;
import com.example.guiaeducativaar.firebase.FirebaseHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class HomeFragment extends Fragment {

    private Button btnProbarFirebase;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        btnProbarFirebase = view.findViewById(R.id.btnProbarFirebase);

        btnProbarFirebase.setOnClickListener(v -> probarConexionFirebase());

        return view;
    }

    private void probarConexionFirebase() {
        FirebaseHelper.getPuntosReference().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                long cantidad = snapshot.getChildrenCount();

                Toast.makeText(getContext(),
                        "Firebase conectado. Puntos encontrados: " + cantidad,
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(getContext(),
                        "Error Firebase: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}