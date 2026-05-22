package com.example.guiaeducativaar.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.guiaeducativaar.ARActivity;
import com.example.guiaeducativaar.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.InputStream;

public class DetallePuntoActivity extends AppCompatActivity {

    private TextView txtNombreDetalle, txtDescripcionDetalle, txtCoordenadas, txtMensajeEstado;
    private LinearLayout contenedorEstado;
    private Button btnVerificarUbicacion, btnAbrirAR;
    private ImageView imgReferencia;
    private ImageButton btnRegresar;

    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> permisoUbicacionLauncher;

    private double latitudPunto;
    private double longitudPunto;

    private final float RADIO_PERMITIDO_METROS = 50f;

    private String nombre = "";
    private String descripcion = "";
    private String imagenReferencia = "";
    private String modelo3D = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_punto);

        txtNombreDetalle = findViewById(R.id.txtNombreDetalle);
        txtDescripcionDetalle = findViewById(R.id.txtDescripcionDetalle);
        txtCoordenadas = findViewById(R.id.txtCoordenadas);
        txtMensajeEstado = findViewById(R.id.txtMensajeEstado);
        contenedorEstado = findViewById(R.id.contenedorEstado);

        btnVerificarUbicacion = findViewById(R.id.btnVerificarUbicacion);
        btnAbrirAR = findViewById(R.id.btnAbrirAR);
        imgReferencia = findViewById(R.id.imgReferencia);

        btnRegresar = findViewById(R.id.btnRegresar);

        btnRegresar.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        configurarPermisoUbicacion();
        recibirDatos();
        cargarImagenReferencia();

        btnAbrirAR.setEnabled(false);
        mostrarEstado("info", "Primero verifica tu ubicación. Si estás cerca, se desbloqueará la realidad aumentada.");

        btnVerificarUbicacion.setOnClickListener(v -> verificarPermisoYUbicacion());
        btnAbrirAR.setOnClickListener(v -> abrirRealidadAumentada());
    }

    private void recibirDatos() {
        nombre = getIntent().getStringExtra("nombre");
        descripcion = getIntent().getStringExtra("descripcion");
        imagenReferencia = getIntent().getStringExtra("imagenReferencia");
        modelo3D = getIntent().getStringExtra("modelo3D");

        String latitud = getIntent().getStringExtra("latitud");
        String longitud = getIntent().getStringExtra("longitud");

        if (nombre == null || nombre.trim().isEmpty()) {
            nombre = "Estación educativa sin nombre";
        }

        if (descripcion == null || descripcion.trim().isEmpty()) {
            descripcion = "Sin descripción disponible.";
        }

        imagenReferencia = limpiarNombreArchivo(imagenReferencia);

        if (modelo3D == null || modelo3D.trim().isEmpty()) {
            if (!imagenReferencia.isEmpty()) {
                modelo3D = imagenReferencia + ".glb";
            } else {
                modelo3D = "";
            }
        } else {
            modelo3D = limpiarModelo(modelo3D);
        }

        try {
            latitudPunto = Double.parseDouble(latitud);
            longitudPunto = Double.parseDouble(longitud);
        } catch (Exception e) {
            latitudPunto = 0;
            longitudPunto = 0;
        }

        txtNombreDetalle.setText(nombre);
        txtDescripcionDetalle.setText(descripcion);
        txtCoordenadas.setText("Latitud: " + latitudPunto + "\nLongitud: " + longitudPunto);
    }

    private void cargarImagenReferencia() {
        if (imagenReferencia == null || imagenReferencia.trim().isEmpty()) {
            imgReferencia.setImageResource(R.drawable.ic_launcher_background);
            return;
        }

        if (imagenReferencia.startsWith("http")) {
            Glide.with(this)
                    .load(imagenReferencia)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(imgReferencia);
            return;
        }

        if (cargarImagenDesdeAssets(imagenReferencia + ".png")) return;
        if (cargarImagenDesdeAssets(imagenReferencia + ".jpg")) return;
        if (cargarImagenDesdeAssets(imagenReferencia + ".jpeg")) return;
        if (cargarImagenDesdeAssets(imagenReferencia + ".webp")) return;

        imgReferencia.setImageResource(R.drawable.ic_launcher_background);
    }

    private boolean cargarImagenDesdeAssets(String nombreArchivo) {
        try {
            InputStream inputStream = getAssets().open(nombreArchivo);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (bitmap != null) {
                imgReferencia.setImageBitmap(bitmap);
                return true;
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    private void configurarPermisoUbicacion() {
        permisoUbicacionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        obtenerUbicacionActual();
                    } else {
                        mostrarEstado("error", "Permiso de ubicación denegado. No se puede desbloquear esta estación.");
                        btnAbrirAR.setEnabled(false);
                    }
                }
        );
    }

    private void verificarPermisoYUbicacion() {
        if (!gpsActivo()) {
            mostrarEstado("advertencia", "Activa el GPS del teléfono para verificar tu cercanía.");
            Toast.makeText(this, "Activa el GPS y vuelve a intentar", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionActual();
        } else {
            mostrarEstado("advertencia", "La app necesita permiso de ubicación para calcular la distancia.");
            permisoUbicacionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private boolean gpsActivo() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null &&
                (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                        || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    private void obtenerUbicacionActual() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            mostrarEstado("error", "No hay permiso de ubicación.");
            btnAbrirAR.setEnabled(false);
            return;
        }

        btnVerificarUbicacion.setEnabled(false);
        btnVerificarUbicacion.setText("Verificando ubicación...");
        mostrarEstado("info", "Buscando ubicación actual con GPS...");

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    btnVerificarUbicacion.setEnabled(true);
                    btnVerificarUbicacion.setText("Verificar ubicación");

                    if (location != null) {
                        calcularDistancia(location);
                    } else {
                        mostrarEstado("advertencia", "No se pudo obtener ubicación. Muévete a un lugar abierto e intenta otra vez.");
                        btnAbrirAR.setEnabled(false);
                    }
                })
                .addOnFailureListener(e -> {
                    btnVerificarUbicacion.setEnabled(true);
                    btnVerificarUbicacion.setText("Verificar ubicación");
                    mostrarEstado("error", "Error obteniendo ubicación: " + e.getMessage());
                    btnAbrirAR.setEnabled(false);
                });
    }

    private void calcularDistancia(Location ubicacionUsuario) {
        if (latitudPunto == 0 && longitudPunto == 0) {
            mostrarEstado("error", "Esta estación no tiene coordenadas válidas.");
            btnAbrirAR.setEnabled(false);
            return;
        }

        if (imagenReferencia == null || imagenReferencia.trim().isEmpty()) {
            mostrarEstado("error", "Esta estación no tiene imagen de referencia configurada.");
            btnAbrirAR.setEnabled(false);
            return;
        }

        if (modelo3D == null || modelo3D.trim().isEmpty()) {
            mostrarEstado("error", "Esta estación no tiene modelo 3D configurado.");
            btnAbrirAR.setEnabled(false);
            return;
        }

        Location ubicacionPunto = new Location("puntoEducativo");
        ubicacionPunto.setLatitude(latitudPunto);
        ubicacionPunto.setLongitude(longitudPunto);

        float distancia = ubicacionUsuario.distanceTo(ubicacionPunto);
        int distanciaRedondeada = Math.round(distancia);

        if (distancia <= RADIO_PERMITIDO_METROS) {
            mostrarEstado("correcto", "Estás cerca de esta estación. Distancia: " + distanciaRedondeada + " m. Contenido AR desbloqueado.");
            btnAbrirAR.setEnabled(true);
        } else {
            mostrarEstado("advertencia", "Estás lejos de esta estación. Distancia aproximada: " + distanciaRedondeada + " m. Acércate para desbloquear AR.");
            btnAbrirAR.setEnabled(false);
        }
    }

    private void abrirRealidadAumentada() {
        if (imagenReferencia == null || imagenReferencia.trim().isEmpty()) {
            mostrarEstado("error", "No se puede abrir AR porque falta la imagen de referencia.");
            return;
        }

        if (modelo3D == null || modelo3D.trim().isEmpty()) {
            mostrarEstado("error", "No se puede abrir AR porque falta el modelo 3D.");
            return;
        }

        Intent intent = new Intent(this, ARActivity.class);
        intent.putExtra("nombre", nombre);
        intent.putExtra("descripcion", descripcion);
        intent.putExtra("latitud", String.valueOf(latitudPunto));
        intent.putExtra("longitud", String.valueOf(longitudPunto));
        intent.putExtra("imagenReferencia", imagenReferencia);
        intent.putExtra("modelo3D", modelo3D);
        startActivity(intent);
    }

    private String limpiarNombreArchivo(String valor) {
        if (valor == null) return "";

        valor = valor.trim();

        if (valor.startsWith("http")) {
            return valor;
        }

        if (valor.contains("/")) {
            valor = valor.substring(valor.lastIndexOf("/") + 1);
        }

        valor = valor.replace(".png", "")
                .replace(".jpg", "")
                .replace(".jpeg", "")
                .replace(".webp", "");

        return valor.toLowerCase();
    }

    private String limpiarModelo(String valor) {
        if (valor == null) return "";

        valor = valor.trim();

        if (valor.contains("/")) {
            valor = valor.substring(valor.lastIndexOf("/") + 1);
        }

        if (!valor.toLowerCase().endsWith(".glb")
                && !valor.toLowerCase().endsWith(".gltf")) {
            if (!imagenReferencia.isEmpty() && !imagenReferencia.startsWith("http")) {
                return imagenReferencia + ".glb";
            }
        }

        return valor.toLowerCase();
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

            default:
                contenedorEstado.setBackgroundColor(Color.parseColor("#CFE2FF"));
                txtMensajeEstado.setTextColor(Color.parseColor("#084298"));
                break;
        }
    }
}