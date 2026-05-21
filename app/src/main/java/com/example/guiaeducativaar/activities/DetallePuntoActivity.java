package com.example.guiaeducativaar.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.guiaeducativaar.ARActivity;
import com.example.guiaeducativaar.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class DetallePuntoActivity extends AppCompatActivity {

    private TextView txtNombreDetalle, txtDescripcionDetalle, txtCoordenadas;
    private TextView txtMensajeEstado;
    private LinearLayout contenedorEstado;

    private Button btnVerificarUbicacion, btnAbrirAR;
    private ImageView imgReferencia;

    private FusedLocationProviderClient fusedLocationClient;

    private double latitudPunto;
    private double longitudPunto;

    private final float RADIO_PERMITIDO_METROS = 50f;

    private ActivityResultLauncher<String> permisoUbicacionLauncher;

    private String nombre;
    private String descripcion;
    private String imagenReferencia;
    private String modelo3D;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_punto);

        txtNombreDetalle = findViewById(R.id.txtNombreDetalle);
        txtDescripcionDetalle = findViewById(R.id.txtDescripcionDetalle);
        txtCoordenadas = findViewById(R.id.txtCoordenadas);

        contenedorEstado = findViewById(R.id.contenedorEstado);
        txtMensajeEstado = findViewById(R.id.txtMensajeEstado);

        btnVerificarUbicacion = findViewById(R.id.btnVerificarUbicacion);
        btnAbrirAR = findViewById(R.id.btnAbrirAR);
        imgReferencia = findViewById(R.id.imgReferencia);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        configurarPermisoUbicacion();
        recibirDatos();
        cargarImagenReferencia();

        btnAbrirAR.setEnabled(false);
        mostrarEstado("info", "Presiona verificar ubicación para comprobar si estás cerca de esta estación.");

        btnVerificarUbicacion.setOnClickListener(v -> verificarPermisoYUbicacion());

        btnAbrirAR.setOnClickListener(v -> {
            Intent intent = new Intent(this, ARActivity.class);
            intent.putExtra("nombre", nombre);
            intent.putExtra("descripcion", descripcion);
            intent.putExtra("latitud", String.valueOf(latitudPunto));
            intent.putExtra("longitud", String.valueOf(longitudPunto));
            intent.putExtra("modelo3D", modelo3D);
            startActivity(intent);
        });
    }

    private void recibirDatos() {
        nombre = getIntent().getStringExtra("nombre");
        descripcion = getIntent().getStringExtra("descripcion");
        String latitud = getIntent().getStringExtra("latitud");
        String longitud = getIntent().getStringExtra("longitud");
        imagenReferencia = getIntent().getStringExtra("imagenReferencia");
        modelo3D = getIntent().getStringExtra("modelo3D");

        if (modelo3D == null || modelo3D.isEmpty()) {
            modelo3D = "cuaderno.glb";
        }

        txtNombreDetalle.setText(nombre != null ? nombre : "Estación educativa");
        txtDescripcionDetalle.setText(descripcion != null ? descripcion : "Sin descripción");

        try {
            latitudPunto = Double.parseDouble(latitud);
            longitudPunto = Double.parseDouble(longitud);
        } catch (Exception e) {
            latitudPunto = 0;
            longitudPunto = 0;
            mostrarEstado("error", "Las coordenadas de esta estación no son válidas.");
        }

        txtCoordenadas.setText(
                "Latitud: " + latitudPunto +
                        "\nLongitud: " + longitudPunto
        );
    }

    private void cargarImagenReferencia() {
        if (imagenReferencia == null || imagenReferencia.isEmpty()) {
            imgReferencia.setImageResource(R.drawable.ic_launcher_background);
            return;
        }

        if (imagenReferencia.startsWith("http")) {
            Glide.with(this)
                    .load(imagenReferencia)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(imgReferencia);

        } else if (imagenReferencia.contains("microprogramacion")) {
            imgReferencia.setImageResource(R.drawable.microprogramacion);

        } else if (imagenReferencia.contains("android")) {
            imgReferencia.setImageResource(R.drawable.android);

        } else {
            imgReferencia.setImageResource(R.drawable.ic_launcher_background);
        }
    }

    private void configurarPermisoUbicacion() {
        permisoUbicacionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        mostrarEstado("info", "Permiso concedido. Obteniendo ubicación actual...");
                        obtenerUbicacionActual();
                    } else {
                        mostrarEstado("error", "Permiso de ubicación denegado. No se puede verificar la cercanía.");
                        Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void verificarPermisoYUbicacion() {
        mostrarEstado("info", "Buscando tu ubicación actual...");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionActual();
        } else {
            mostrarEstado("advertencia", "La app necesita permiso de ubicación para verificar la distancia.");
            permisoUbicacionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void obtenerUbicacionActual() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            mostrarEstado("error", "No hay permiso de ubicación.");
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        calcularDistancia(location);
                    } else {
                        mostrarEstado("advertencia", "No se pudo obtener la ubicación. Activa el GPS y vuelve a intentar.");
                        Toast.makeText(this, "Activa el GPS e intenta de nuevo", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    mostrarEstado("error", "Error al obtener ubicación: " + e.getMessage());
                    Toast.makeText(this, "Error al obtener ubicación", Toast.LENGTH_LONG).show();
                });
    }

    private void calcularDistancia(Location ubicacionUsuario) {
        Location ubicacionPunto = new Location("puntoEducativo");
        ubicacionPunto.setLatitude(latitudPunto);
        ubicacionPunto.setLongitude(longitudPunto);

        float distancia = ubicacionUsuario.distanceTo(ubicacionPunto);
        int distanciaRedondeada = Math.round(distancia);

        if (distancia <= RADIO_PERMITIDO_METROS) {
            mostrarEstado("correcto", "Estás cerca de esta estación. Distancia: " + distanciaRedondeada + " m. Punto desbloqueado.");
            btnAbrirAR.setEnabled(true);
        } else {
            mostrarEstado("advertencia", "Estás lejos de esta estación. Distancia aproximada: " + distanciaRedondeada + " m.");
            btnAbrirAR.setEnabled(false);
        }
    }

    private void mostrarEstado(String tipo, String mensaje) {
        contenedorEstado.setVisibility(View.VISIBLE);
        txtMensajeEstado.setText(mensaje);

        switch (tipo) {
            case "error":
                contenedorEstado.setBackgroundColor(Color.parseColor("#F8D7DA"));
                txtMensajeEstado.setTextColor(Color.parseColor("#842029"));
                break;

            case "advertencia":
                contenedorEstado.setBackgroundColor(Color.parseColor("#FFF3CD"));
                txtMensajeEstado.setTextColor(Color.parseColor("#5C4300"));
                break;

            case "correcto":
                contenedorEstado.setBackgroundColor(Color.parseColor("#D1E7DD"));
                txtMensajeEstado.setTextColor(Color.parseColor("#0F5132"));
                break;

            case "info":
            default:
                contenedorEstado.setBackgroundColor(Color.parseColor("#CFE2FF"));
                txtMensajeEstado.setTextColor(Color.parseColor("#084298"));
                break;
        }
    }
}