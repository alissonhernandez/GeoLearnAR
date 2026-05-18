package com.example.guiaeducativaar.firebase;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseHelper {

    private static final String NODO_PUNTOS = "puntosEducativos";

    public static DatabaseReference getPuntosReference() {
        return FirebaseDatabase.getInstance().getReference(NODO_PUNTOS);
    }
}