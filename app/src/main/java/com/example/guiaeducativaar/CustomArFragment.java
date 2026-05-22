package com.example.guiaeducativaar;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.IOException;
import java.io.InputStream;

public class CustomArFragment extends ArFragment {

    private static final String TAG = "AR_IMAGENES";

    @Override
    protected Config onCreateSessionConfig(Session session) {

        Config config = new Config(session);

        config.setLightEstimationMode(Config.LightEstimationMode.DISABLED);
        config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL);
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);

        AugmentedImageDatabase database = new AugmentedImageDatabase(session);

        agregarImagen(database, "mercurio", "mercurio.jpeg");
        agregarImagen(database, "venus", "venus.jpeg");
        agregarImagen(database, "tierra", "tierra.jpeg");
        agregarImagen(database, "marte", "marte.jpeg");
        agregarImagen(database, "jupiter", "jupiter.jpeg");
        agregarImagen(database, "saturno", "saturno.jpeg");
        agregarImagen(database, "urano", "urano.jpeg");
        agregarImagen(database, "neptuno", "neptuno.jpeg");
        agregarImagen(database, "sol", "sol.jpeg");

        config.setAugmentedImageDatabase(database);
        session.configure(config);

        Log.d(TAG, "Base de imágenes de planetas cargada correctamente");

        return config;
    }

    private void agregarImagen(AugmentedImageDatabase database, String nombre, String archivoAsset) {
        try {
            InputStream inputStream = requireContext().getAssets().open(archivoAsset);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap != null) {
                database.addImage(nombre, bitmap);
                Log.d(TAG, "Imagen agregada: " + nombre + " / " + archivoAsset);
            } else {
                Log.e(TAG, "No se pudo leer la imagen: " + archivoAsset);
            }

            inputStream.close();

        } catch (IOException e) {
            Log.e(TAG, "No existe en assets: " + archivoAsset, e);
        }
    }
}