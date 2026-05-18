package com.example.guiaeducativaar;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;

public class ARActivity extends AppCompatActivity {

    private ArFragment arFragment;
    private boolean objetoColocado = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aractivity);

        //arFragment = (ArFragment) getSupportFragmentManager()
                //.findFragmentById(R.id.arFragment);

        if (arFragment == null) {
            Toast.makeText(this, "No se pudo iniciar ARCore", Toast.LENGTH_SHORT).show();
            return;
        }

        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            if (!objetoColocado) {
                crearObjeto3D(hitResult);
                objetoColocado = true;
            } else {
                Toast.makeText(this, "Objeto AR ya colocado", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void crearObjeto3D(HitResult hitResult) {
        Anchor anchor = hitResult.createAnchor();

        MaterialFactory.makeOpaqueWithColor(
                this,
                new Color(android.graphics.Color.rgb(0, 106, 103))
        ).thenAccept(material -> {

            AnchorNode anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(arFragment.getArSceneView().getScene());

            anchorNode.setRenderable(
                    ShapeFactory.makeCube(
                            new com.google.ar.sceneform.math.Vector3(0.25f, 0.25f, 0.25f),
                            new com.google.ar.sceneform.math.Vector3(0f, 0.12f, 0f),
                            material
                    )
            );

            Toast.makeText(this, "Objeto 3D colocado", Toast.LENGTH_SHORT).show();
        });
    }
}
