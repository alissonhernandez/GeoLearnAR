package com.example.guiaeducativaar;

import android.graphics.Color;
import android.os.Bundle;
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
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.util.Collection;

public class ARActivity extends AppCompatActivity {

    private String nombrePunto = "Punto Educativo";
    private String descripcionPunto = "Contenido educativo AR";

    private CustomArFragment arFragment;
    private ModelRenderable modeloCubo;
    private AnchorNode anchorActual;
    private Button btnReiniciarAR;
    private boolean imagenDetectada = false;

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

        crearModeloBasico();

        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            if (!imagenDetectada) {
                colocarContenidoEnPlano(hitResult.createAnchor());
            }
        });

        arFragment.getArSceneView()
                .getScene()
                .addOnUpdateListener(frameTime -> detectarImagen());

        btnReiniciarAR.setOnClickListener(v -> reiniciarAR());
    }

    private void recibirDatosDelPunto() {
        if (getIntent() != null) {
            String nombre = getIntent().getStringExtra("nombre");
            String descripcion = getIntent().getStringExtra("descripcion");

            if (nombre != null && !nombre.isEmpty()) {
                nombrePunto = nombre;
            }

            if (descripcion != null && !descripcion.isEmpty()) {
                descripcionPunto = descripcion;
            }
        }
    }

    private void detectarImagen() {
        Frame frame = arFragment.getArSceneView().getArFrame();

        if (frame == null || modeloCubo == null || imagenDetectada) {
            return;
        }

        Collection<AugmentedImage> imagenes =
                frame.getUpdatedTrackables(AugmentedImage.class);

        for (AugmentedImage imagen : imagenes) {
            if (imagen.getTrackingState() == TrackingState.TRACKING
                    && "cuaderno".equals(imagen.getName())) {

                Anchor anchor = imagen.createAnchor(imagen.getCenterPose());
                colocarContenidoDeImagen(anchor);

                imagenDetectada = true;

                Toast.makeText(this, "Imagen detectada: cuaderno", Toast.LENGTH_LONG).show();
                break;
            }
        }
    }

    private void colocarContenidoEnPlano(Anchor anchor) {
        if (modeloCubo == null) {
            Toast.makeText(this, "Cargando modelo 3D...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (anchorActual != null) {
            Toast.makeText(this, "Presiona Reiniciar para colocar otro objeto", Toast.LENGTH_SHORT).show();
            return;
        }

        anchorActual = new AnchorNode(anchor);
        anchorActual.setParent(arFragment.getArSceneView().getScene());

        Node nodo3D = new Node();
        nodo3D.setParent(anchorActual);
        nodo3D.setRenderable(modeloCubo);
        nodo3D.setLocalPosition(new Vector3(0f, 0.1f, 0f));

        crearEtiqueta(anchorActual, nombrePunto + "\n" + descripcionPunto);

        Toast.makeText(this, "Contenido colocado en superficie", Toast.LENGTH_SHORT).show();
    }

    private void colocarContenidoDeImagen(Anchor anchor) {
        if (anchorActual != null) {
            return;
        }

        anchorActual = new AnchorNode(anchor);
        anchorActual.setParent(arFragment.getArSceneView().getScene());

        Node nodo3D = new Node();
        nodo3D.setParent(anchorActual);
        nodo3D.setRenderable(modeloCubo);
        nodo3D.setLocalPosition(new Vector3(0f, 0.05f, 0f));

        crearEtiqueta(anchorActual, "Imagen detectada\n" + nombrePunto);
    }

    private void crearModeloBasico() {
        MaterialFactory.makeOpaqueWithColor(
                this,
                new com.google.ar.sceneform.rendering.Color(Color.rgb(0, 150, 136))
        ).thenAccept(material -> {
            modeloCubo = ShapeFactory.makeCube(
                    new Vector3(0.20f, 0.20f, 0.20f),
                    new Vector3(0f, 0.10f, 0f),
                    material
            );
        }).exceptionally(throwable -> {
            Toast.makeText(this, "Error cargando modelo 3D", Toast.LENGTH_LONG).show();
            return null;
        });
    }

    private void crearEtiqueta(AnchorNode anchorNode, String texto) {
        TextView textView = new TextView(this);
        textView.setText(texto);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(10);
        textView.setBackgroundColor(Color.argb(190, 0, 11, 88));
        textView.setPadding(14, 10, 14, 10);

        ViewRenderable.builder()
                .setView(this, textView)
                .build()
                .thenAccept(renderable -> {
                    Node textoNode = new Node();
                    textoNode.setParent(anchorNode);
                    textoNode.setRenderable(renderable);
                    textoNode.setLocalPosition(new Vector3(0f, 0.35f, 0f));
                    textoNode.setLocalScale(new Vector3(0.35f, 0.35f, 0.35f));
                });
    }

    private void reiniciarAR() {
        if (anchorActual != null) {
            arFragment.getArSceneView().getScene().removeChild(anchorActual);

            if (anchorActual.getAnchor() != null) {
                anchorActual.getAnchor().detach();
            }

            anchorActual = null;
        }

        imagenDetectada = false;

        Toast.makeText(this, "Contenido AR reiniciado", Toast.LENGTH_SHORT).show();
    }
}