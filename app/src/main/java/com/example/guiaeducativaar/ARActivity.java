package com.example.guiaeducativaar;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;

public class ARActivity extends AppCompatActivity {

    private CustomArFragment arFragment;
    private ModelRenderable modeloCubo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aractivity);

        arFragment = (CustomArFragment) getSupportFragmentManager()
                .findFragmentById(R.id.arFragment);

        if (arFragment == null) {
            Toast.makeText(this, "Error: no se encontró ArFragment", Toast.LENGTH_LONG).show();
            return;
        }

        crearModeloBasico();

        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            if (modeloCubo == null) {
                Toast.makeText(this, "El modelo aún está cargando", Toast.LENGTH_SHORT).show();
                return;
            }

            Anchor anchor = hitResult.createAnchor();

            AnchorNode anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(arFragment.getArSceneView().getScene());

            Node nodo = new Node();
            nodo.setParent(anchorNode);
            nodo.setRenderable(modeloCubo);

            Toast.makeText(this, "Objeto educativo colocado en RA", Toast.LENGTH_SHORT).show();
        });
    }

    private void crearModeloBasico() {
        MaterialFactory.makeOpaqueWithColor(
                this,
                new com.google.ar.sceneform.rendering.Color(Color.rgb(0, 150, 136))
        ).thenAccept(material -> {
            modeloCubo = ShapeFactory.makeCube(
                    new Vector3(0.2f, 0.2f, 0.2f),
                    new Vector3(0f, 0.1f, 0f),
                    material
            );
        }).exceptionally(throwable -> {
            Toast.makeText(this, "Error cargando modelo 3D", Toast.LENGTH_LONG).show();
            return null;
        });
    }
}