package com.example.guiaeducativaar;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ux.ArFragment;

public class CustomArFragment extends ArFragment {

    @Override
    protected Config onCreateSessionConfig(Session session) {

        Config config = new Config(session);

        config.setLightEstimationMode(
                Config.LightEstimationMode.DISABLED
        );

        session.configure(config);

        return config;
    }
}