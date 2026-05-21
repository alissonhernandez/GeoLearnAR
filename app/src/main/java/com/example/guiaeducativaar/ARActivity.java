package com.example.guiaeducativaar;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.util.Collection;

public class ARActivity extends AppCompatActivity {

    private String nombrePunto = "Punto Educativo";
    private String descripcionPunto = "Contenido educativo AR";
    private String modelo3D = "cuaderno.glb";

    private CustomArFragment arFragment;
    private ModelRenderable modeloRenderable;
    private AnchorNode anchorActual;
    private Button btnReiniciarAR;

    private boolean imagenDetectada = false;
    private boolean puedeDetectar = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aractivity);

        recibirDatosDelPunto();

        arFragment = (CustomArFragment) getSupportFragmentManager()
                .findFragmentById(R.id.arFragment);

        btnReiniciarAR = findViewById(R.id.btnReiniciarAR);

        if (arFragment == null) {
            Toast.makeText(this, "Error al iniciar AR", Toast.LENGTH_LONG).show();
            return;
        }

        cargarModeloGLB();

        arFragment.getArSceneView()
                .getScene()
                .addOnUpdateListener(frameTime -> detectarImagen());

        btnReiniciarAR.setOnClickListener(v -> reiniciarAR());
    }

    private void recibirDatosDelPunto() {
        if (getIntent() != null) {
            String nombre = getIntent().getStringExtra("nombre");
            String descripcion = getIntent().getStringExtra("descripcion");
            String modelo = getIntent().getStringExtra("modelo3D");

            if (nombre != null && !nombre.isEmpty()) {
                nombrePunto = nombre;
            }

            if (descripcion != null && !descripcion.isEmpty()) {
                descripcionPunto = descripcion;
            }

            if (modelo != null && !modelo.isEmpty()) {
                modelo3D = modelo;
            }
        }
    }

    private void cargarModeloGLB() {
        Uri uriModelo = Uri.parse(modelo3D);

        ModelRenderable.builder()
                .setSource(this, uriModelo)
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(renderable -> {
                    modeloRenderable = renderable;
                    Toast.makeText(this, "Modelo 3D listo", Toast.LENGTH_SHORT).show();
                })
                .exceptionally(throwable -> {
                    Toast.makeText(this,
                            "Error cargando modelo 3D: " + modelo3D,
                            Toast.LENGTH_LONG).show();
                    return null;
                });
    }

    private void detectarImagen() {
        Frame frame = arFragment.getArSceneView().getArFrame();

        if (frame == null || modeloRenderable == null || imagenDetectada || !puedeDetectar) {
            return;
        }

        Collection<AugmentedImage> imagenes =
                frame.getUpdatedTrackables(AugmentedImage.class);

        for (AugmentedImage imagen : imagenes) {
            if (imagen.getTrackingState() == TrackingState.TRACKING) {

                Anchor anchor = imagen.createAnchor(imagen.getCenterPose());
                colocarModeloSobreImagen(anchor);

                imagenDetectada = true;

                Toast.makeText(this,
                        "Imagen detectada: " + imagen.getName(),
                        Toast.LENGTH_LONG).show();
                break;
            }
        }
    }

    private void colocarModeloSobreImagen(Anchor anchor) {
        if (anchorActual != null) {
            return;
        }

        anchorActual = new AnchorNode(anchor);
        anchorActual.setParent(arFragment.getArSceneView().getScene());

        Node nodo3D = new Node();
        nodo3D.setParent(anchorActual);
        nodo3D.setRenderable(modeloRenderable);

        nodo3D.setLocalPosition(new Vector3(0f, 0.01f, 0f));
        nodo3D.setLocalScale(new Vector3(0.10f, 0.10f, 0.10f));

        crearEtiqueta(anchorActual, "Imagen detectada\n" + nombrePunto);
    }

    private void crearEtiqueta(AnchorNode anchorNode, String texto) {
        TextView textView = new TextView(this);
        textView.setText(texto);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(10);
        textView.setBackgroundColor(Color.argb(190, 126, 87, 194));
        textView.setPadding(14, 10, 14, 10);

        ViewRenderable.builder()
                .setView(this, textView)
                .build()
                .thenAccept(renderable -> {
                    Node textoNode = new Node();
                    textoNode.setParent(anchorNode);
                    textoNode.setRenderable(renderable);
                    textoNode.setLocalPosition(new Vector3(0f, 0.30f, 0f));
                    textoNode.setLocalScale(new Vector3(0.30f, 0.30f, 0.30f));
                });
    }

    private void reiniciarAR() {
        puedeDetectar = false;

        if (anchorActual != null) {
            arFragment.getArSceneView().getScene().removeChild(anchorActual);

            if (anchorActual.getAnchor() != null) {
                anchorActual.getAnchor().detach();
            }

            anchorActual = null;
        }

        imagenDetectada = false;

        Toast.makeText(this, "AR reiniciado. Vuelve a escanear la imagen.", Toast.LENGTH_SHORT).show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> puedeDetectar = true, 2000);
    }
}