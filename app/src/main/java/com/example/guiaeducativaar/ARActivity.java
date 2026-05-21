package com.example.guiaeducativaar;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.util.Collection;
import java.util.List;

public class ARActivity extends AppCompatActivity {

    private String nombrePunto = "Estación educativa";
    private String descripcionPunto = "Contenido educativo en realidad aumentada.";
    private String imagenReferencia = "";
    private String modelo3D = "";

    private boolean modoLibre = false;
    private boolean imagenYaDetectada = false;

    private CustomArFragment arFragment;
    private ModelRenderable modeloRenderable;
    private AnchorNode anchorActual;
    private Button btnReiniciarAR;
    private TextView txtInstruccionAR;

    private boolean contenidoColocado = false;
    private boolean puedeDetectar = true;
    private boolean cargandoModelo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aractivity);

        recibirDatosDelPunto();

        arFragment = (CustomArFragment) getSupportFragmentManager()
                .findFragmentById(R.id.arFragment);

        btnReiniciarAR = findViewById(R.id.btnReiniciarAR);
        txtInstruccionAR = findViewById(R.id.txtInstruccionAR);

        if (arFragment == null) {
            Toast.makeText(this, "Error al iniciar AR", Toast.LENGTH_LONG).show();
            return;
        }

        if (modoLibre) {
            txtInstruccionAR.setText("Modo libre: escanea una imagen registrada para cargar su modelo.");
        } else {
            txtInstruccionAR.setText("Escanea la imagen: " + imagenReferencia);
            cargarModeloGLB(null);
        }

        arFragment.getArSceneView()
                .getScene()
                .addOnUpdateListener(frameTime -> detectarImagen());

        arFragment.getArSceneView().getScene().setOnTouchListener((hitTestResult, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                colocarModeloConToque(motionEvent);
            }
            return true;
        });

        btnReiniciarAR.setOnClickListener(v -> reiniciarAR());
    }

    private void recibirDatosDelPunto() {
        if (getIntent() != null) {
            modoLibre = getIntent().getBooleanExtra("modoLibre", false);

            String nombre = getIntent().getStringExtra("nombre");
            String descripcion = getIntent().getStringExtra("descripcion");
            String imagen = getIntent().getStringExtra("imagenReferencia");
            String modelo = getIntent().getStringExtra("modelo3D");

            if (nombre != null && !nombre.trim().isEmpty()) {
                nombrePunto = nombre.trim();
            }

            if (descripcion != null && !descripcion.trim().isEmpty()) {
                descripcionPunto = descripcion.trim();
            }

            if (imagen != null && !imagen.trim().isEmpty()) {
                imagenReferencia = limpiarNombreImagen(imagen);
            }

            if (modelo != null && !modelo.trim().isEmpty()) {
                modelo3D = limpiarNombreModelo(modelo);
            }
        }

        if (!modoLibre) {
            if (imagenReferencia == null || imagenReferencia.trim().isEmpty()) {
                imagenReferencia = "arduino";
            }

            if (modelo3D == null || modelo3D.trim().isEmpty()) {
                modelo3D = imagenReferencia + ".glb";
            }
        }
    }

    private String limpiarNombreImagen(String valor) {
        valor = valor.trim();

        if (valor.contains("/")) {
            valor = valor.substring(valor.lastIndexOf("/") + 1);
        }

        valor = valor.replace(".png", "")
                .replace(".jpg", "")
                .replace(".jpeg", "")
                .replace(".webp", "");

        return valor.toLowerCase();
    }

    private String limpiarNombreModelo(String valor) {
        valor = valor.trim();

        if (valor.contains("/")) {
            valor = valor.substring(valor.lastIndexOf("/") + 1);
        }

        return valor;
    }

    private void cargarModeloGLB(Runnable alCargar) {
        if (modelo3D == null || modelo3D.trim().isEmpty()) {
            txtInstruccionAR.setText("Primero escanea una imagen registrada para saber qué modelo cargar.");
            return;
        }

        cargandoModelo = true;

        ModelRenderable.builder()
                .setSource(this, Uri.parse(modelo3D))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(renderable -> {
                    modeloRenderable = renderable;
                    cargandoModelo = false;

                    if (alCargar != null) {
                        alCargar.run();
                    } else {
                        txtInstruccionAR.setText("Modelo listo. Ahora apunta a una mesa/superficie y tócala.");
                    }
                })
                .exceptionally(throwable -> {
                    cargandoModelo = false;
                    txtInstruccionAR.setText("No se pudo cargar " + modelo3D + ". Revisa que exista en assets.");
                    Toast.makeText(this, "Error cargando modelo 3D", Toast.LENGTH_LONG).show();
                    return null;
                });
    }

    private void detectarImagen() {
        Frame frame = arFragment.getArSceneView().getArFrame();

        if (frame == null || contenidoColocado || !puedeDetectar || cargandoModelo || imagenYaDetectada) {
            return;
        }

        Collection<AugmentedImage> imagenes = frame.getUpdatedTrackables(AugmentedImage.class);

        for (AugmentedImage imagen : imagenes) {
            if (imagen.getTrackingState() == TrackingState.TRACKING) {

                String imagenDetectada = imagen.getName();

                if (!modoLibre && !imagenDetectada.equalsIgnoreCase(imagenReferencia)) {
                    txtInstruccionAR.setText("Detecté " + imagenDetectada + ", pero esta estación necesita " + imagenReferencia + ".");
                    return;
                }

                if (modoLibre) {
                    imagenReferencia = imagenDetectada;
                    modelo3D = imagenDetectada + ".glb";
                    configurarTextoSegunImagen(imagenDetectada);
                }

                imagenYaDetectada = true;

                cargarModeloGLB(() -> {
                    txtInstruccionAR.setText("Imagen detectada: " + imagenDetectada + ". Ahora toca una superficie para colocar el modelo.");
                    Toast.makeText(this, imagenDetectada + " detectado", Toast.LENGTH_LONG).show();
                });

                break;
            }
        }
    }

    private void configurarTextoSegunImagen(String imagenDetectada) {
        if (imagenDetectada.equalsIgnoreCase("arduino")) {
            nombrePunto = "Estación Arduino";
            descripcionPunto = "Aprende sobre microcontroladores, sensores y salidas digitales.";
        } else if (imagenDetectada.equalsIgnoreCase("android")) {
            nombrePunto = "Estación Programación Móvil";
            descripcionPunto = "Explora el desarrollo de aplicaciones Android con Java y Firebase.";
        } else if (imagenDetectada.equalsIgnoreCase("protoboard")) {
            nombrePunto = "Estación Protoboard";
            descripcionPunto = "Identifica conexiones básicas para circuitos electrónicos.";
        } else {
            nombrePunto = "Estación " + imagenDetectada;
            descripcionPunto = "Contenido educativo en realidad aumentada.";
        }
    }

    private void colocarModeloConToque(MotionEvent motionEvent) {
        if (contenidoColocado) {
            return;
        }

        if (modeloRenderable == null) {
            if (modoLibre) {
                txtInstruccionAR.setText("Primero escanea una imagen registrada. Luego toca una superficie.");
            } else {
                txtInstruccionAR.setText("El modelo todavía se está cargando.");
            }
            return;
        }

        Frame frame = arFragment.getArSceneView().getArFrame();

        if (frame == null) {
            txtInstruccionAR.setText("Mueve el celular lentamente para detectar superficies.");
            return;
        }

        List<HitResult> resultados = frame.hitTest(motionEvent);

        for (HitResult hit : resultados) {
            if (hit.getTrackable() instanceof Plane) {
                Plane plane = (Plane) hit.getTrackable();

                if (plane.isPoseInPolygon(hit.getHitPose())) {
                    Anchor anchor = hit.createAnchor();

                    colocarContenidoAR(anchor, "Contenido educativo:\n" + nombrePunto);

                    contenidoColocado = true;

                    txtInstruccionAR.setText("Modelo colocado con hitTest y anchor sobre una superficie.");
                    Toast.makeText(this, "Modelo colocado en superficie", Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        }
    }

    private void colocarContenidoAR(Anchor anchor, String textoEtiqueta) {
        if (anchorActual != null) {
            return;
        }

        anchorActual = new AnchorNode(anchor);
        anchorActual.setParent(arFragment.getArSceneView().getScene());

        Node nodo3D = new Node();
        nodo3D.setParent(anchorActual);
        nodo3D.setRenderable(modeloRenderable);

        aplicarEscalaYRotacion(nodo3D);

        crearEtiqueta(anchorActual, textoEtiqueta + "\n" + descripcionPunto);
    }

    private void aplicarEscalaYRotacion(Node nodo3D) {

        if (imagenReferencia.equalsIgnoreCase("arduino")) {
            nodo3D.setLocalScale(new Vector3(0.18f, 0.18f, 0.18f));
            nodo3D.setLocalPosition(new Vector3(0f, 0.02f, 0f));
            nodo3D.setLocalRotation(Quaternion.axisAngle(new Vector3(0f, 1f, 0f), 0f));

        } else if (imagenReferencia.equalsIgnoreCase("android")) {
            nodo3D.setLocalScale(new Vector3(0.22f, 0.22f, 0.22f));
            nodo3D.setLocalPosition(new Vector3(0f, 0.02f, 0f));
            nodo3D.setLocalRotation(Quaternion.axisAngle(new Vector3(0f, 1f, 0f), 0f));

        } else if (imagenReferencia.equalsIgnoreCase("protoboard")) {
            nodo3D.setLocalScale(new Vector3(0.20f, 0.20f, 0.20f));
            nodo3D.setLocalPosition(new Vector3(0f, 0.02f, 0f));
            nodo3D.setLocalRotation(Quaternion.axisAngle(new Vector3(0f, 1f, 0f), 0f));

        } else {
            nodo3D.setLocalScale(new Vector3(0.18f, 0.18f, 0.18f));
            nodo3D.setLocalPosition(new Vector3(0f, 0.02f, 0f));
        }
    }

    private void crearEtiqueta(AnchorNode anchorNode, String texto) {
        TextView textView = new TextView(this);
        textView.setText(texto);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(7);
        textView.setBackgroundColor(Color.argb(210, 70, 50, 130));
        textView.setPadding(12, 8, 12, 8);
        textView.setMaxWidth(420);

        ViewRenderable.builder()
                .setView(this, textView)
                .build()
                .thenAccept(renderable -> {
                    Node textoNode = new Node();
                    textoNode.setParent(anchorNode);
                    textoNode.setRenderable(renderable);
                    textoNode.setLocalPosition(new Vector3(0f, 0.08f, 0f));
                    textoNode.setLocalScale(new Vector3(0.13f, 0.13f, 0.13f));
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

        contenidoColocado = false;
        imagenYaDetectada = false;
        modeloRenderable = null;
        cargandoModelo = false;

        if (modoLibre) {
            imagenReferencia = "";
            modelo3D = "";
            txtInstruccionAR.setText("Reiniciando... aparta la cámara de la imagen anterior.");
        } else {
            cargarModeloGLB(null);
            txtInstruccionAR.setText("AR reiniciado. Escanea: " + imagenReferencia);
        }

        Toast.makeText(this, "AR reiniciado", Toast.LENGTH_SHORT).show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            puedeDetectar = true;
            if (modoLibre) {
                txtInstruccionAR.setText("Reiniciado. Apunta nuevamente a una imagen registrada.");
            }
        }, 3000);
    }
}