package com.example.guiaeducativaar.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.guiaeducativaar.R;
import com.example.guiaeducativaar.firebase.FirebaseHelper;
import com.example.guiaeducativaar.models.PuntoEducativo;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FormularioPuntoActivity extends AppCompatActivity {

    private static final String CLOUD_NAME = "dhvx694fe";
    private static final String UPLOAD_PRESET = "geolearnar_upload";

    private TextView txtTituloFormulario;
    private TextInputEditText edtNombre, edtDescripcion, edtLatitud, edtLongitud;
    private TextView txtImagenSeleccionada, txtModeloSeleccionado, txtDireccionActual;
    private Button btnGuardar, btnEliminar, btnUsarUbicacionActual, btnVerUbicacionMapa;

    private String idPunto;
    private boolean modoEditar = false;

    private Uri imagenUri;
    private Uri modeloUri;

    private String imagenUrlActual = "";
    private String modeloUrlActual = "";

    private ActivityResultLauncher<String> seleccionarImagenLauncher;
    private ActivityResultLauncher<String> seleccionarModeloLauncher;
    private ActivityResultLauncher<String> permisoUbicacionLauncher;

    private FusedLocationProviderClient fusedLocationClient;
    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_formulario_punto);

        txtTituloFormulario = findViewById(R.id.txtTituloFormulario);

        edtNombre = findViewById(R.id.edtNombre);
        edtDescripcion = findViewById(R.id.edtDescripcion);
        edtLatitud = findViewById(R.id.edtLatitud);
        edtLongitud = findViewById(R.id.edtLongitud);

        txtImagenSeleccionada = findViewById(R.id.txtImagenSeleccionada);
        txtModeloSeleccionado = findViewById(R.id.txtModeloSeleccionado);
        txtDireccionActual = findViewById(R.id.txtDireccionActual);

        Button btnSeleccionarImagen = findViewById(R.id.btnSeleccionarImagen);
        Button btnSeleccionarModelo = findViewById(R.id.btnSeleccionarModelo);

        btnUsarUbicacionActual = findViewById(R.id.btnUsarUbicacionActual);
        btnVerUbicacionMapa = findViewById(R.id.btnVerUbicacionMapa);

        btnGuardar = findViewById(R.id.btnGuardar);
        btnEliminar = findViewById(R.id.btnEliminar);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        configurarSelectoresArchivos();
        configurarPermisoUbicacion();
        recibirDatos();

        ImageButton btnRegresar = findViewById(R.id.btnRegresar);
        btnRegresar.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        btnSeleccionarImagen.setOnClickListener(v -> seleccionarImagenLauncher.launch("image/*"));
        btnSeleccionarModelo.setOnClickListener(v -> seleccionarModeloLauncher.launch("*/*"));

        btnUsarUbicacionActual.setOnClickListener(v -> verificarPermisoYObtenerUbicacion());
        btnVerUbicacionMapa.setOnClickListener(v -> abrirMapa());

        btnGuardar.setOnClickListener(v -> guardarPunto());
        btnEliminar.setOnClickListener(v -> eliminarPunto());
    }

    private void configurarSelectoresArchivos() {
        seleccionarImagenLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        imagenUri = uri;
                        txtImagenSeleccionada.setText("Imagen seleccionada correctamente");
                        Toast.makeText(this, "Imagen seleccionada", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        seleccionarModeloLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        modeloUri = uri;
                        txtModeloSeleccionado.setText("Modelo .glb seleccionado correctamente");
                        Toast.makeText(this, "Modelo seleccionado", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void configurarPermisoUbicacion() {
        permisoUbicacionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        obtenerUbicacionActual();
                    } else {
                        Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void verificarPermisoYObtenerUbicacion() {
        if (!gpsActivo()) {
            Toast.makeText(this, "Activa el GPS para tomar la ubicación actual", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionActual();
        } else {
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
            Toast.makeText(this, "No hay permiso de ubicación", Toast.LENGTH_LONG).show();
            return;
        }

        btnUsarUbicacionActual.setEnabled(false);
        btnUsarUbicacionActual.setText("Obteniendo ubicación...");
        txtDireccionActual.setText("Ubicación: buscando dirección...");

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    btnUsarUbicacionActual.setEnabled(true);
                    btnUsarUbicacionActual.setText("Usar mi ubicación actual");

                    if (location != null) {
                        colocarUbicacion(location);
                    } else {
                        txtDireccionActual.setText("Ubicación: no se pudo obtener");
                        Toast.makeText(this, "No se pudo obtener ubicación. Intenta de nuevo.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    btnUsarUbicacionActual.setEnabled(true);
                    btnUsarUbicacionActual.setText("Usar mi ubicación actual");
                    txtDireccionActual.setText("Ubicación: error al obtener ubicación");
                    Toast.makeText(this, "Error obteniendo ubicación: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void colocarUbicacion(Location location) {
        double latitud = location.getLatitude();
        double longitud = location.getLongitude();

        edtLatitud.setText(String.valueOf(latitud));
        edtLongitud.setText(String.valueOf(longitud));

        btnVerUbicacionMapa.setEnabled(true);
        obtenerDireccionDesdeCoordenadas(latitud, longitud);

        Toast.makeText(this, "Ubicación actual tomada correctamente", Toast.LENGTH_SHORT).show();
    }

    private void obtenerDireccionDesdeCoordenadas(double latitud, double longitud) {
        try {
            Geocoder geocoder = new Geocoder(this, new Locale("es", "SV"));
            List<Address> direcciones = geocoder.getFromLocation(latitud, longitud, 1);

            if (direcciones != null && !direcciones.isEmpty()) {
                Address direccion = direcciones.get(0);

                String lugar = direccion.getFeatureName();
                String colonia = direccion.getSubLocality();
                String ciudad = direccion.getLocality();
                String departamento = direccion.getAdminArea();
                String pais = direccion.getCountryName();

                String textoDireccion = "";

                if (lugar != null && !lugar.matches("^[A-Z0-9]+\\+[A-Z0-9]+.*")) {
                    textoDireccion = lugar;
                }

                if (colonia != null && !colonia.isEmpty()) {
                    textoDireccion += textoDireccion.isEmpty() ? colonia : ", " + colonia;
                }

                if (ciudad != null && !ciudad.isEmpty()) {
                    textoDireccion += textoDireccion.isEmpty() ? ciudad : ", " + ciudad;
                }

                if (departamento != null && !departamento.isEmpty()) {
                    textoDireccion += textoDireccion.isEmpty() ? departamento : ", " + departamento;
                }

                if (pais != null && !pais.isEmpty()) {
                    textoDireccion += textoDireccion.isEmpty() ? pais : ", " + pais;
                }

                if (!textoDireccion.isEmpty()) {
                    txtDireccionActual.setText("Ubicación: " + textoDireccion);
                } else {
                    txtDireccionActual.setText("Ubicación aproximada: " + latitud + ", " + longitud);
                }

            } else {
                txtDireccionActual.setText("Ubicación obtenida, sin dirección exacta");
            }

        } catch (Exception e) {
            txtDireccionActual.setText("Ubicación obtenida, sin dirección disponible");
        }
    }

    private void abrirMapa() {
        String latitudTexto = edtLatitud.getText().toString().trim();
        String longitudTexto = edtLongitud.getText().toString().trim();

        if (latitudTexto.isEmpty() || longitudTexto.isEmpty()) {
            Toast.makeText(this, "Primero toma la ubicación actual", Toast.LENGTH_LONG).show();
            return;
        }

        String uri = "geo:" + latitudTexto + "," + longitudTexto +
                "?q=" + latitudTexto + "," + longitudTexto + "(Estación educativa)";

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Intent navegador = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=" + latitudTexto + "," + longitudTexto));
            startActivity(navegador);
        }
    }

    private void recibirDatos() {
        idPunto = getIntent().getStringExtra("id");

        if (idPunto != null && !idPunto.isEmpty()) {
            modoEditar = true;

            txtTituloFormulario.setText("Editar estación");
            btnGuardar.setText("Actualizar");
            btnEliminar.setVisibility(View.VISIBLE);

            edtNombre.setText(getIntent().getStringExtra("nombre"));
            edtDescripcion.setText(getIntent().getStringExtra("descripcion"));
            edtLatitud.setText(getIntent().getStringExtra("latitud"));
            edtLongitud.setText(getIntent().getStringExtra("longitud"));

            imagenUrlActual = getIntent().getStringExtra("imagenReferencia");
            modeloUrlActual = getIntent().getStringExtra("modelo3D");

            if (imagenUrlActual != null && !imagenUrlActual.isEmpty()) {
                txtImagenSeleccionada.setText("Imagen actual cargada");
            }

            if (modeloUrlActual != null && !modeloUrlActual.isEmpty()) {
                txtModeloSeleccionado.setText("Modelo actual cargado");
            }

            if (edtLatitud.getText() != null && edtLongitud.getText() != null &&
                    !edtLatitud.getText().toString().isEmpty() &&
                    !edtLongitud.getText().toString().isEmpty()) {
                btnVerUbicacionMapa.setEnabled(true);

                try {
                    double lat = Double.parseDouble(edtLatitud.getText().toString());
                    double lon = Double.parseDouble(edtLongitud.getText().toString());
                    obtenerDireccionDesdeCoordenadas(lat, lon);
                } catch (Exception e) {
                    txtDireccionActual.setText("Ubicación guardada, sin dirección disponible");
                }
            }
        }
    }

    private void guardarPunto() {
        String nombre = edtNombre.getText().toString().trim();
        String descripcion = edtDescripcion.getText().toString().trim();
        String latitudTexto = edtLatitud.getText().toString().trim();
        String longitudTexto = edtLongitud.getText().toString().trim();

        edtNombre.setError(null);
        edtDescripcion.setError(null);
        edtLatitud.setError(null);
        edtLongitud.setError(null);

        boolean hayError = false;

        if (nombre.isEmpty()) {
            edtNombre.setError("Escribe el nombre de la estación");
            hayError = true;
        }

        if (descripcion.isEmpty()) {
            edtDescripcion.setError("Escribe una descripción");
            hayError = true;
        }

        if (latitudTexto.isEmpty() || longitudTexto.isEmpty()) {
            Toast.makeText(this, "Debes tomar la ubicación actual antes de guardar", Toast.LENGTH_LONG).show();
            hayError = true;
        }

        if (!modoEditar && imagenUri == null) {
            Toast.makeText(this, "Debes seleccionar una imagen", Toast.LENGTH_LONG).show();
            hayError = true;
        }

        if (!modoEditar && modeloUri == null) {
            Toast.makeText(this, "Debes seleccionar un modelo .glb", Toast.LENGTH_LONG).show();
            hayError = true;
        }

        if (hayError) {
            return;
        }

        double latitud;
        double longitud;

        try {
            latitud = Double.parseDouble(latitudTexto);
            longitud = Double.parseDouble(longitudTexto);
        } catch (Exception e) {
            Toast.makeText(this, "La ubicación no es válida", Toast.LENGTH_LONG).show();
            return;
        }

        if (!modoEditar) {
            idPunto = FirebaseHelper.getPuntosReference().push().getKey();
        }

        if (idPunto == null) {
            Toast.makeText(this, "No se pudo generar el ID", Toast.LENGTH_LONG).show();
            return;
        }

        btnGuardar.setEnabled(false);
        btnGuardar.setText("Subiendo archivos...");

        subirArchivosYGuardar(nombre, descripcion, latitud, longitud);
    }

    private void subirArchivosYGuardar(String nombre, String descripcion, double latitud, double longitud) {
        if (imagenUri != null) {
            subirArchivo(imagenUri, "imagenes", urlImagen -> {
                if (modeloUri != null) {
                    subirArchivo(modeloUri, "modelos3d", urlModelo ->
                            guardarEnFirebase(nombre, descripcion, latitud, longitud, urlImagen, urlModelo)
                    );
                } else {
                    guardarEnFirebase(nombre, descripcion, latitud, longitud, urlImagen, modeloUrlActual);
                }
            });
        } else if (modeloUri != null) {
            subirArchivo(modeloUri, "modelos3d", urlModelo ->
                    guardarEnFirebase(nombre, descripcion, latitud, longitud, imagenUrlActual, urlModelo)
            );
        } else {
            guardarEnFirebase(nombre, descripcion, latitud, longitud, imagenUrlActual, modeloUrlActual);
        }
    }

    private void subirArchivo(Uri uri, String carpeta, OnArchivoSubido listener) {
        try {
            String nombreArchivo = obtenerNombreArchivo(uri);

            if (nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
                nombreArchivo = carpeta + "_" + System.currentTimeMillis();
            }

            File archivoTemporal = copiarUriAArchivoTemporal(uri, nombreArchivo);

            String resourceType = carpeta.equals("modelos3d") ? "raw" : "image";

            String url = "https://api.cloudinary.com/v1_1/"
                    + CLOUD_NAME
                    + "/"
                    + resourceType
                    + "/upload";

            RequestBody archivoBody = RequestBody.create(
                    archivoTemporal,
                    MediaType.parse("application/octet-stream")
            );

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", nombreArchivo, archivoBody)
                    .addFormDataPart("upload_preset", UPLOAD_PRESET)
                    .addFormDataPart("folder", carpeta)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, java.io.IOException e) {
                    runOnUiThread(() -> {
                        restaurarBotonGuardar();
                        Toast.makeText(FormularioPuntoActivity.this,
                                "Error subiendo archivo: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws java.io.IOException {
                    String respuesta = response.body() != null ? response.body().string() : "";

                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> {
                            restaurarBotonGuardar();
                            Toast.makeText(FormularioPuntoActivity.this,
                                    "Cloudinary rechazó el archivo: " + respuesta,
                                    Toast.LENGTH_LONG).show();
                        });
                        return;
                    }

                    try {
                        JSONObject json = new JSONObject(respuesta);
                        String urlArchivo = json.getString("secure_url");

                        runOnUiThread(() -> listener.onSubido(urlArchivo));

                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            restaurarBotonGuardar();
                            Toast.makeText(FormularioPuntoActivity.this,
                                    "Error leyendo respuesta de Cloudinary",
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });

        } catch (Exception e) {
            restaurarBotonGuardar();
            Toast.makeText(this,
                    "Error preparando archivo: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private String obtenerNombreArchivo(Uri uri) {
        String nombre = null;

        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int indiceNombre = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (indiceNombre >= 0) {
                        nombre = cursor.getString(indiceNombre);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (nombre == null) {
            nombre = uri.getLastPathSegment();
        }

        return nombre;
    }

    private File copiarUriAArchivoTemporal(Uri uri, String nombreArchivo) throws Exception {
        InputStream inputStream = getContentResolver().openInputStream(uri);

        if (inputStream == null) {
            throw new Exception("No se pudo leer el archivo seleccionado");
        }

        File archivoTemporal = new File(getCacheDir(), nombreArchivo);
        FileOutputStream outputStream = new FileOutputStream(archivoTemporal);

        byte[] buffer = new byte[4096];
        int bytesLeidos;

        while ((bytesLeidos = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesLeidos);
        }

        outputStream.close();
        inputStream.close();

        return archivoTemporal;
    }

    private void guardarEnFirebase(String nombre, String descripcion, double latitud, double longitud,
                                   String imagenUrl, String modeloUrl) {

        PuntoEducativo punto = new PuntoEducativo(
                idPunto,
                nombre,
                descripcion,
                latitud,
                longitud,
                imagenUrl,
                modeloUrl
        );

        btnGuardar.setText("Guardando...");

        FirebaseHelper.getPuntosReference()
                .child(idPunto)
                .setValue(punto)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Estación guardada correctamente", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    restaurarBotonGuardar();
                    Toast.makeText(this, "No se pudo guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void restaurarBotonGuardar() {
        btnGuardar.setEnabled(true);
        btnGuardar.setText(modoEditar ? "Actualizar" : "Guardar");
    }

    private void eliminarPunto() {
        if (idPunto == null) {
            Toast.makeText(this, "No se puede eliminar esta estación", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseHelper.getPuntosReference()
                .child(idPunto)
                .removeValue()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Estación eliminada", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al eliminar: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    interface OnArchivoSubido {
        void onSubido(String url);
    }
}