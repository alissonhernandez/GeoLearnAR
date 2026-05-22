package com.example.guiaeducativaar.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.guiaeducativaar.R;
import com.example.guiaeducativaar.activities.DetallePuntoActivity;
import com.example.guiaeducativaar.activities.FormularioPuntoActivity;
import com.example.guiaeducativaar.adapters.PuntoEducativoAdapter;
import com.example.guiaeducativaar.firebase.FirebaseHelper;
import com.example.guiaeducativaar.models.PuntoEducativo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ListaPuntosFragment extends Fragment {

    private RecyclerView recyclerPuntos;
    private TextView txtEstadoLista;
    private Button btnAgregarPunto;

    private ArrayList<PuntoEducativo> listaPuntos;
    private PuntoEducativoAdapter adapter;

    public ListaPuntosFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_lista_puntos, container, false);

        recyclerPuntos = view.findViewById(R.id.recyclerPuntos);
        txtEstadoLista = view.findViewById(R.id.txtEstadoLista);
        btnAgregarPunto = view.findViewById(R.id.btnAgregarPunto);

        listaPuntos = new ArrayList<>();

        adapter = new PuntoEducativoAdapter(listaPuntos, new PuntoEducativoAdapter.OnPuntoClickListener() {
            @Override
            public void onPuntoClick(PuntoEducativo punto) {
                abrirDetalle(punto);
            }

            @Override
            public void onPuntoLongClick(PuntoEducativo punto) {
                abrirFormularioEditar(punto);
            }
        });

        recyclerPuntos.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerPuntos.setAdapter(adapter);

        btnAgregarPunto.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), FormularioPuntoActivity.class);
            startActivity(intent);
        });

        cargarPuntosDesdeFirebase();

        return view;
    }

    private void cargarPuntosDesdeFirebase() {
        txtEstadoLista.setText("Cargando estaciones...");

        FirebaseHelper.getPuntosReference().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                listaPuntos.clear();

                for (DataSnapshot dato : snapshot.getChildren()) {
                    PuntoEducativo punto = dato.getValue(PuntoEducativo.class);

                    if (punto != null) {
                        listaPuntos.add(punto);
                    }
                }

                adapter.notifyDataSetChanged();

                if (listaPuntos.isEmpty()) {
                    txtEstadoLista.setText("No hay estaciones registradas.");
                } else {
                    txtEstadoLista.setText("Estaciones encontradas: " + listaPuntos.size());
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(getContext(),
                        "Error al cargar estaciones: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();

                txtEstadoLista.setText("Error al cargar datos.");
            }
        });
    }

    private void abrirDetalle(PuntoEducativo punto) {
        Intent intent = new Intent(getActivity(), DetallePuntoActivity.class);

        intent.putExtra("id", punto.getId());
        intent.putExtra("nombre", punto.getNombre());
        intent.putExtra("descripcion", punto.getDescripcion());
        intent.putExtra("latitud", punto.getLatitud());
        intent.putExtra("longitud", punto.getLongitud());
        intent.putExtra("imagenReferencia", punto.getImagenReferencia());
        intent.putExtra("modelo3D", punto.getModelo3D());

        startActivity(intent);
    }

    private void abrirFormularioEditar(PuntoEducativo punto) {
        Intent intent = new Intent(getActivity(), FormularioPuntoActivity.class);

        intent.putExtra("id", punto.getId());
        intent.putExtra("nombre", punto.getNombre());
        intent.putExtra("descripcion", punto.getDescripcion());
        intent.putExtra("latitud", punto.getLatitud());
        intent.putExtra("longitud", punto.getLongitud());
        intent.putExtra("imagenReferencia", punto.getImagenReferencia());
        intent.putExtra("modelo3D", punto.getModelo3D());

        startActivity(intent);
    }
}