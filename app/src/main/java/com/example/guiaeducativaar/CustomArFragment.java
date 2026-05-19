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

    @Override
    protected Config onCreateSessionConfig(Session session) {

        Config config = new Config(session);

        config.setLightEstimationMode(
                Config.LightEstimationMode.DISABLED
        );

        try {

            InputStream inputStream = requireContext()
                    .getAssets()
                    .open("cuaderno.jpeg");

            Bitmap bitmap =
                    BitmapFactory.decodeStream(inputStream);

            AugmentedImageDatabase database =
                    new AugmentedImageDatabase(session);

            database.addImage(
                    "cuaderno",
                    bitmap
            );

            config.setAugmentedImageDatabase(database);

        } catch (IOException e) {

            Log.e(
                    "CustomArFragment",
                    "Error cargando cuaderno.jpeg",
                    e
            );
        }

        session.configure(config);

        return config;
    }
}