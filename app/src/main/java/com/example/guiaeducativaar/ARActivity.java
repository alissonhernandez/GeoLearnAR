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

    private String nombrePunto = "";
    private String descripcionPunto = "";
    private String imagenReferencia = "";
    private String modelo3D = "";

    private boolean modoLibre = false;
    private boolean imagenYaDetectada = false;
    private boolean contenidoColocado = false;
    private boolean puedeDetectar = true;
    private boolean cargandoModelo = false;

    private CustomArFragment arFragment;
    private ModelRenderable modeloRenderable;
    private AnchorNode anchorActual;

    private Button btnReiniciarAR;
    private TextView txtInstruccionAR;

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
            Toast.makeText(this, "No se pudo iniciar la cámara AR.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mostrarInstruccionInicial();

        arFragment.getArSceneView()
                .getScene()
                .addOnUpdateListener(frameTime -> detectarImagen());

        arFragment.getArSceneView()
                .getScene()
                .setOnTouchListener((hitTestResult, motionEvent) -> {
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        colocarModeloConToque(motionEvent);
                    }
                    return true;
                });

        btnReiniciarAR.setOnClickListener(v -> reiniciarAR());
    }

    private void recibirDatosDelPunto() {
        if (getIntent() == null) {
            Toast.makeText(this, "No se recibieron datos del punto educativo.", Toast.LENGTH_LONG).show();
            return;
        }

        modoLibre = getIntent().getBooleanExtra("modoLibre", false);

        String nombre = getIntent().getStringExtra("nombre");
        String descripcion = getIntent().getStringExtra("descripcion");
        String imagen = getIntent().getStringExtra("imagenReferencia");
        String modelo = getIntent().getStringExtra("modelo3D");

        if (nombre != null && !nombre.trim().isEmpty()) {
            nombrePunto = nombre.trim();
        } else {
            nombrePunto = "Punto educativo sin nombre";
        }

        if (descripcion != null && !descripcion.trim().isEmpty()) {
            descripcionPunto = descripcion.trim();
        } else {
            descripcionPunto = "Descripción no disponible en Firebase.";
        }

        if (imagen != null && !imagen.trim().isEmpty()) {
            imagenReferencia = limpiarNombreImagen(imagen);
        } else {
            imagenReferencia = "";
        }

        if (modelo != null && !modelo.trim().isEmpty()) {
            modelo3D = limpiarNombreModelo(modelo);
        } else if (!imagenReferencia.isEmpty()) {
            modelo3D = imagenReferencia + ".glb";
        } else {
            modelo3D = "";
        }
    }

    private void mostrarInstruccionInicial() {
        if (modoLibre) {
            txtInstruccionAR.setText(
                    "Modo libre:\n1. Apunta a una imagen registrada.\n2. Espera detección.\n3. Toca una superficie."
            );
            return;
        }

        if (imagenReferencia == null || imagenReferencia.trim().isEmpty()) {
            txtInstruccionAR.setText(
                    "No se recibió imagen de referencia desde Firebase.\nRevisa que el punto educativo tenga el campo imagenReferencia."
            );
            return;
        }

        if (modelo3D == null || modelo3D.trim().isEmpty()) {
            txtInstruccionAR.setText(
                    "No se recibió modelo 3D desde Firebase.\nRevisa que el punto educativo tenga el campo modelo3D."
            );
            return;
        }

        txtInstruccionAR.setText(
                "Escanea la imagen: " + imagenReferencia +
                        "\nLuego toca una mesa o superficie para colocar el modelo."
        );

        cargarModeloGLB(null);
    }

    private String limpiarNombreImagen(String valor) {
        if (valor == null) return "";

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
        if (valor == null) return "";

        valor = valor.trim();

        if (valor.contains("/")) {
            valor = valor.substring(valor.lastIndexOf("/") + 1);
        }

        if (!valor.toLowerCase().endsWith(".glb")
                && !valor.toLowerCase().endsWith(".gltf")) {
            if (!imagenReferencia.isEmpty()) {
                return imagenReferencia + ".glb";
            }
        }

        return valor;
    }

    private void cargarModeloGLB(Runnable alCargar) {
        if (modelo3D == null || modelo3D.trim().isEmpty()) {
            txtInstruccionAR.setText(
                    "No hay modelo 3D configurado para este punto educativo."
            );
            return;
        }

        if (cargandoModelo) return;

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
                        txtInstruccionAR.setText(
                                "Modelo cargado: " + modelo3D +
                                        "\nEscanea la imagen registrada y toca una superficie detectada."
                        );
                    }
                })
                .exceptionally(throwable -> {
                    cargandoModelo = false;
                    modeloRenderable = null;

                    txtInstruccionAR.setText(
                            "No se pudo cargar el modelo: " + modelo3D +
                                    "\nVerifica que exista en app/src/main/assets/"
                    );

                    Toast.makeText(this, "Error cargando modelo 3D", Toast.LENGTH_LONG).show();
                    return null;
                });
    }

    private void detectarImagen() {
        if (arFragment == null) return;

        Frame frame = arFragment.getArSceneView().getArFrame();

        if (frame == null || contenidoColocado || !puedeDetectar || cargandoModelo || imagenYaDetectada) {
            return;
        }

        Collection<AugmentedImage> imagenes =
                frame.getUpdatedTrackables(AugmentedImage.class);

        for (AugmentedImage imagen : imagenes) {
            if (imagen.getTrackingState() == TrackingState.TRACKING) {

                String imagenDetectada = imagen.getName();

                if (!modoLibre && !imagenDetectada.equalsIgnoreCase(imagenReferencia)) {
                    txtInstruccionAR.setText(
                            "Imagen detectada: " + imagenDetectada +
                                    "\nPero este punto educativo necesita: " + imagenReferencia
                    );
                    return;
                }

                if (modoLibre) {
                    imagenReferencia = imagenDetectada;
                    modelo3D = imagenDetectada + ".glb";
                    configurarTextoSegunImagen(imagenDetectada);
                }

                imagenYaDetectada = true;

                cargarModeloGLB(() -> {
                    txtInstruccionAR.setText(
                            "Imagen detectada: " + imagenDetectada +
                                    "\nAhora toca una superficie plana para colocar el modelo."
                    );

                    Toast.makeText(this, imagenDetectada + " detectado", Toast.LENGTH_SHORT).show();
                });

                break;
            }
        }
    }

    private void configurarTextoSegunImagen(String imagenDetectada) {
        if (imagenDetectada.equalsIgnoreCase("arduino")) {
            nombrePunto = "Estación Arduino";
            descripcionPunto = "Microcontrolador utilizado para crear proyectos electrónicos con sensores, luces y motores.";
        } else if (imagenDetectada.equalsIgnoreCase("android")) {
            nombrePunto = "Estación Android";
            descripcionPunto = "Sistema operativo móvil usado para desarrollar aplicaciones educativas con Java y Firebase.";
        } else if (imagenDetectada.equalsIgnoreCase("protoboard")) {
            nombrePunto = "Estación Protoboard";
            descripcionPunto = "Placa de pruebas usada para armar circuitos electrónicos sin soldar.";
        } else {
            nombrePunto = "Estación " + imagenDetectada;
            descripcionPunto = "Contenido educativo en realidad aumentada.";
        }
    }

    private void colocarModeloConToque(MotionEvent motionEvent) {
        if (contenidoColocado) {
            txtInstruccionAR.setText("El modelo ya fue colocado. Usa Reiniciar para volver a intentarlo.");
            return;
        }

        if (!imagenYaDetectada && !modoLibre) {
            txtInstruccionAR.setText("Primero escanea la imagen: " + imagenReferencia);
            return;
        }

        if (modeloRenderable == null) {
            txtInstruccionAR.setText("El modelo aún se está cargando. Espera unos segundos.");
            return;
        }

        Frame frame = arFragment.getArSceneView().getArFrame();

        if (frame == null) {
            txtInstruccionAR.setText("Mueve lentamente el celular para detectar superficies.");
            return;
        }

        List<HitResult> resultados = frame.hitTest(motionEvent);

        for (HitResult hit : resultados) {
            if (hit.getTrackable() instanceof Plane) {
                Plane plane = (Plane) hit.getTrackable();

                if (plane.isPoseInPolygon(hit.getHitPose())) {
                    Anchor anchor = hit.createAnchor();

                    colocarContenidoAR(anchor);

                    contenidoColocado = true;

                    txtInstruccionAR.setText(
                            "Modelo colocado correctamente usando hitTest y anchor.\nPuedes observarlo moviendo el teléfono."
                    );

                    Toast.makeText(this, "Modelo colocado", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        txtInstruccionAR.setText(
                "No se detectó una superficie válida.\nApunta a una mesa o piso iluminado y toca de nuevo."
        );
    }

    private void colocarContenidoAR(Anchor anchor) {
        if (anchorActual != null) return;

        anchorActual = new AnchorNode(anchor);
        anchorActual.setParent(arFragment.getArSceneView().getScene());

        Node nodo3D = new Node();
        nodo3D.setParent(anchorActual);
        nodo3D.setRenderable(modeloRenderable);

        aplicarEscalaYRotacion(nodo3D);
        crearEtiqueta(anchorActual);
    }

    private void aplicarEscalaYRotacion(Node nodo3D) {
        String clave = imagenReferencia == null ? "" : imagenReferencia.toLowerCase();

        if (clave.contains("arduino")) {
            nodo3D.setLocalScale(new Vector3(0.25f, 0.25f, 0.25f));
            nodo3D.setLocalPosition(new Vector3(0f, 0.02f, 0f));
            nodo3D.setLocalRotation(Quaternion.axisAngle(new Vector3(0f, 1f, 0f), 0f));

        } else if (clave.contains("android")) {
            nodo3D.setLocalScale(new Vector3(0.28f, 0.28f, 0.28f));
            nodo3D.setLocalPosition(new Vector3(0f, 0.02f, 0f));
            nodo3D.setLocalRotation(Quaternion.axisAngle(new Vector3(0f, 1f, 0f), 0f));

        } else if (clave.contains("protoboard")) {
            nodo3D.setLocalScale(new Vector3(0.24f, 0.24f, 0.24f));
            nodo3D.setLocalPosition(new Vector3(0f, 0.02f, 0f));
            nodo3D.setLocalRotation(Quaternion.axisAngle(new Vector3(0f, 1f, 0f), 0f));

        } else {
            nodo3D.setLocalScale(new Vector3(0.24f, 0.24f, 0.24f));
            nodo3D.setLocalPosition(new Vector3(0f, 0.02f, 0f));
        }
    }

    private void crearEtiqueta(AnchorNode anchorNode) {
        TextView textView = new TextView(this);

        String texto = nombrePunto + "\n\n" + descripcionPunto;

        textView.setText(texto);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(8);
        textView.setBackgroundColor(Color.argb(225, 70, 50, 130));
        textView.setPadding(18, 12, 18, 12);
        textView.setMaxWidth(520);

        ViewRenderable.builder()
                .setView(this, textView)
                .build()
                .thenAccept(renderable -> {
                    Node etiquetaNode = new Node();
                    etiquetaNode.setParent(anchorNode);
                    etiquetaNode.setRenderable(renderable);

                    etiquetaNode.setLocalPosition(new Vector3(0f, 0.15f, 0f));
                    etiquetaNode.setLocalScale(new Vector3(0.15f, 0.15f, 0.15f));
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

        txtInstruccionAR.setText(
                "Reiniciando AR...\nAparta la cámara de la imagen anterior unos segundos."
        );

        Toast.makeText(this, "AR reiniciado", Toast.LENGTH_SHORT).show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            puedeDetectar = true;

            if (modoLibre) {
                imagenReferencia = "";
                modelo3D = "";

                txtInstruccionAR.setText(
                        "Modo libre reiniciado.\nApunta nuevamente a una imagen registrada."
                );

            } else {
                if (imagenReferencia == null || imagenReferencia.trim().isEmpty()) {
                    txtInstruccionAR.setText(
                            "No hay imagen de referencia configurada para esta estación."
                    );
                    return;
                }

                if (modelo3D == null || modelo3D.trim().isEmpty()) {
                    txtInstruccionAR.setText(
                            "No hay modelo 3D configurado para esta estación."
                    );
                    return;
                }

                cargarModeloGLB(null);

                txtInstruccionAR.setText(
                        "AR reiniciado.\nEscanea nuevamente la imagen: " + imagenReferencia
                );
            }
        }, 4000);
    }
}