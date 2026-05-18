package com.example.guiaeducativaar.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import com.example.guiaeducativaar.R;
import com.example.guiaeducativaar.activities.DetallePuntoActivity;

public class ListaPuntosFragment extends Fragment {

    private Button btnPuntoDemo;

    public ListaPuntosFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_lista_puntos, container, false);

        btnPuntoDemo = view.findViewById(R.id.btnPuntoDemo);

        btnPuntoDemo.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DetallePuntoActivity.class);
            intent.putExtra("nombre", "Estación demo: Anatomía humana");
            intent.putExtra("descripcion", "Contenido educativo interactivo sobre el cuerpo humano usando realidad aumentada.");
            intent.putExtra("latitud", "13.7000");
            intent.putExtra("longitud", "-88.1000");
            startActivity(intent);
        });

        return view;
    }
}