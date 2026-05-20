package com.example.guiaeducativaar.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.guiaeducativaar.R;
import com.example.guiaeducativaar.firebase.FirebaseHelper;
import com.example.guiaeducativaar.models.PuntoEducativo;

public class FormularioPuntoActivity extends AppCompatActivity {

    private TextView txtTituloFormulario;
    private EditText edtNombre, edtDescripcion, edtLatitud, edtLongitud, edtImagen, edtModelo;
    private Button btnGuardar, btnEliminar;

    private String idPunto;
    private boolean modoEditar = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_formulario_punto);

        txtTituloFormulario = findViewById(R.id.txtTituloFormulario);
        edtNombre = findViewById(R.id.edtNombre);
        edtDescripcion = findViewById(R.id.edtDescripcion);
        edtLatitud = findViewById(R.id.edtLatitud);
        edtLongitud = findViewById(R.id.edtLongitud);
        edtImagen = findViewById(R.id.edtImagen);
        edtModelo = findViewById(R.id.edtModelo);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnEliminar = findViewById(R.id.btnEliminar);

        recibirDatos();

        btnGuardar.setOnClickListener(v -> guardarPunto());
        btnEliminar.setOnClickListener(v -> eliminarPunto());
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
            edtImagen.setText(getIntent().getStringExtra("imagenReferencia"));
            edtModelo.setText(getIntent().getStringExtra("modelo3D"));
        }
    }

    private void guardarPunto() {
        String nombre = edtNombre.getText().toString().trim();
        String descripcion = edtDescripcion.getText().toString().trim();
        String latitudTexto = edtLatitud.getText().toString().trim();
        String longitudTexto = edtLongitud.getText().toString().trim();
        String imagen = edtImagen.getText().toString().trim();
        String modelo = edtModelo.getText().toString().trim();

        if (nombre.isEmpty() || descripcion.isEmpty() || latitudTexto.isEmpty() || longitudTexto.isEmpty()) {
            Toast.makeText(this, "Completa nombre, descripción, latitud y longitud", Toast.LENGTH_SHORT).show();
            return;
        }

        double latitud;
        double longitud;

        try {
            latitud = Double.parseDouble(latitudTexto);
            longitud = Double.parseDouble(longitudTexto);
        } catch (Exception e) {
            Toast.makeText(this, "Latitud o longitud inválida", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!modoEditar) {
            idPunto = FirebaseHelper.getPuntosReference().push().getKey();
        }

        PuntoEducativo punto = new PuntoEducativo(
                idPunto,
                nombre,
                descripcion,
                latitud,
                longitud,
                imagen,
                modelo
        );

        FirebaseHelper.getPuntosReference()
                .child(idPunto)
                .setValue(punto)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Estación guardada correctamente", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
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
}