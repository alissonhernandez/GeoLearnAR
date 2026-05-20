package com.example.guiaeducativaar.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.guiaeducativaar.R;
import com.example.guiaeducativaar.firebase.FirebaseHelper;
import com.example.guiaeducativaar.models.PuntoEducativo;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class FormularioPuntoActivity extends AppCompatActivity {

    private TextView txtTituloFormulario;
    private TextInputEditText edtNombre, edtDescripcion, edtLatitud, edtLongitud;
    private TextView txtImagenSeleccionada, txtModeloSeleccionado;
    private Button btnGuardar, btnEliminar;

    private String idPunto;
    private boolean modoEditar = false;

    private Uri imagenUri;
    private Uri modeloUri;

    private String imagenUrlActual = "";
    private String modeloUrlActual = "";

    private ActivityResultLauncher<String> seleccionarImagenLauncher;
    private ActivityResultLauncher<String> seleccionarModeloLauncher;

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

        Button btnSeleccionarImagen = findViewById(R.id.btnSeleccionarImagen);
        Button btnSeleccionarModelo = findViewById(R.id.btnSeleccionarModelo);

        btnGuardar = findViewById(R.id.btnGuardar);
        btnEliminar = findViewById(R.id.btnEliminar);

        configurarSelectoresArchivos();
        recibirDatos();

        ImageButton btnRegresar = findViewById(R.id.btnRegresar);
        btnRegresar.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        btnSeleccionarImagen.setOnClickListener(v -> seleccionarImagenLauncher.launch("image/*"));

        // IMPORTANTE:
        // Usamos application/octet-stream y */* para que Android deje elegir archivos .glb.
        btnSeleccionarModelo.setOnClickListener(v -> seleccionarModeloLauncher.launch("*/*"));

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

        if (latitudTexto.isEmpty()) {
            edtLatitud.setError("Debes obtener la ubicación actual");
            hayError = true;
        }

        if (longitudTexto.isEmpty()) {
            edtLongitud.setError("Debes obtener la ubicación actual");
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
            Toast.makeText(this, "Revisa los campos antes de guardar", Toast.LENGTH_LONG).show();
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

        if (latitud < -90 || latitud > 90) {
            edtLatitud.setError("La latitud debe estar entre -90 y 90");
            Toast.makeText(this, "Latitud inválida", Toast.LENGTH_LONG).show();
            return;
        }

        if (longitud < -180 || longitud > 180) {
            edtLongitud.setError("La longitud debe estar entre -180 y 180");
            Toast.makeText(this, "Longitud inválida", Toast.LENGTH_LONG).show();
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
        String nombreArchivo = carpeta + "_" + System.currentTimeMillis();

        if (carpeta.equals("imagenes")) {
            nombreArchivo = nombreArchivo + ".jpg";
        } else if (carpeta.equals("modelos3d")) {
            nombreArchivo = nombreArchivo + ".glb";
        }

        StorageReference referencia = FirebaseStorage.getInstance()
                .getReference()
                .child(carpeta)
                .child(nombreArchivo);

        referencia.putFile(uri)
                .addOnSuccessListener(taskSnapshot ->
                        referencia.getDownloadUrl()
                                .addOnSuccessListener(downloadUri ->
                                        listener.onSubido(downloadUri.toString())
                                )
                                .addOnFailureListener(e -> {
                                    restaurarBotonGuardar();
                                    Toast.makeText(this, "Error obteniendo URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                })
                )
                .addOnFailureListener(e -> {
                    restaurarBotonGuardar();
                    Toast.makeText(this, "Error subiendo archivo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
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