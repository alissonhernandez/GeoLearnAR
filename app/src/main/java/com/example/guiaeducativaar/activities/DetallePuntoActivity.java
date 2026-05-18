package com.example.guiaeducativaar.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.guiaeducativaar.ARActivity;
import com.example.guiaeducativaar.R;

public class DetallePuntoActivity extends AppCompatActivity {

    private TextView txtNombreDetalle, txtDescripcionDetalle, txtCoordenadas;
    private Button btnVerificarUbicacion, btnAbrirAR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_punto);

        txtNombreDetalle = findViewById(R.id.txtNombreDetalle);
        txtDescripcionDetalle = findViewById(R.id.txtDescripcionDetalle);
        txtCoordenadas = findViewById(R.id.txtCoordenadas);
        btnVerificarUbicacion = findViewById(R.id.btnVerificarUbicacion);
        btnAbrirAR = findViewById(R.id.btnAbrirAR);

        String nombre = getIntent().getStringExtra("nombre");
        String descripcion = getIntent().getStringExtra("descripcion");
        String latitud = getIntent().getStringExtra("latitud");
        String longitud = getIntent().getStringExtra("longitud");

        txtNombreDetalle.setText(nombre);
        txtDescripcionDetalle.setText(descripcion);
        txtCoordenadas.setText("Latitud: " + latitud + "\nLongitud: " + longitud);

        btnVerificarUbicacion.setOnClickListener(v ->
                Toast.makeText(this, "Función GPS pendiente para el Día 2", Toast.LENGTH_SHORT).show()
        );

        btnAbrirAR.setOnClickListener(v -> {
            Intent intent = new Intent(this, ARActivity.class);
            startActivity(intent);
        });
    }
}