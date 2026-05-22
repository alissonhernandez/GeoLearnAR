package com.example.guiaeducativaar.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.guiaeducativaar.R;
import com.example.guiaeducativaar.activities.DetallePuntoActivity;
import com.example.guiaeducativaar.firebase.FirebaseHelper;
import com.example.guiaeducativaar.models.PuntoEducativo;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private TextView txtEstadoMapa;
    private FusedLocationProviderClient fusedLocationClient;
    private Location ubicacionActual;

    private final ArrayList<PuntoEducativo> listaPuntos = new ArrayList<>();

    private static final int CODIGO_PERMISO_UBICACION = 200;
    private static final float RADIO_DESBLOQUEO = 80f;

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        txtEstadoMapa = view.findViewById(R.id.txtEstadoMapa);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapaHome);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        pedirUbicacion();
        cargarPuntosFirebase();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        obtenerUbicacionActual();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        LatLng sanMiguel = new LatLng(13.4833, -88.1833);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sanMiguel, 15));

        activarMiUbicacionEnMapa();

        googleMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();

            if (tag instanceof PuntoEducativo) {
                PuntoEducativo punto = (PuntoEducativo) tag;
                abrirDetallePunto(punto);
            }

            return true;
        });

        dibujarPines();
    }

    private void pedirUbicacion() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionActual();
        } else {
            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    CODIGO_PERMISO_UBICACION
            );
        }
    }

    private void obtenerUbicacionActual() {
        if (!isAdded()) return;

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        actualizarUbicacionSiConviene(location);
                    }

                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                            .addOnSuccessListener(nuevaLocation -> {
                                if (nuevaLocation != null) {
                                    actualizarUbicacionSiConviene(nuevaLocation);
                                }
                            });
                });
    }

    private void actualizarUbicacionSiConviene(Location nuevaLocation) {
        if (nuevaLocation == null) return;

        if (ubicacionActual == null) {
            ubicacionActual = nuevaLocation;
        } else {
            float accuracyNueva = nuevaLocation.getAccuracy();
            float accuracyActual = ubicacionActual.getAccuracy();

            boolean esMasPrecisa = accuracyNueva <= accuracyActual + 20;
            boolean noEsDemasiadoVieja = nuevaLocation.getTime() >= ubicacionActual.getTime();

            if (esMasPrecisa || noEsDemasiadoVieja) {
                ubicacionActual = nuevaLocation;
            }
        }

        if (googleMap != null) {
            activarMiUbicacionEnMapa();

            LatLng miPosicion = new LatLng(
                    ubicacionActual.getLatitude(),
                    ubicacionActual.getLongitude()
            );

            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(miPosicion, 17));
            dibujarPines();
        }
    }

    private void activarMiUbicacionEnMapa() {
        if (googleMap == null || !isAdded()) return;

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
    }

    private void cargarPuntosFirebase() {
        FirebaseHelper.getPuntosReference().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaPuntos.clear();

                for (DataSnapshot item : snapshot.getChildren()) {
                    PuntoEducativo punto = item.getValue(PuntoEducativo.class);

                    if (punto != null && coordenadasValidas(punto)) {
                        listaPuntos.add(punto);
                    }
                }

                dibujarPines();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),
                        "Error Firebase: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean coordenadasValidas(PuntoEducativo punto) {
        return punto.getLatitud() != 0 && punto.getLongitud() != 0;
    }

    private void dibujarPines() {
        if (googleMap == null) return;

        googleMap.clear();

        int verdes = 0;
        int rojos = 0;

        for (PuntoEducativo punto : listaPuntos) {
            boolean cerca = estaCerca(punto);

            if (cerca) {
                verdes++;
            } else {
                rojos++;
            }

            agregarPin(punto, cerca);
        }

        if (txtEstadoMapa != null) {
            txtEstadoMapa.setText("Estaciones: " + listaPuntos.size()
                    + " | Cerca: " + verdes
                    + " | Lejos: " + rojos);
        }
    }

    private void agregarPin(PuntoEducativo punto, boolean cerca) {
        LatLng posicion = new LatLng(punto.getLatitud(), punto.getLongitud());

        float colorPin = cerca
                ? BitmapDescriptorFactory.HUE_GREEN
                : BitmapDescriptorFactory.HUE_RED;

        com.google.android.gms.maps.model.Marker marker = googleMap.addMarker(
                new MarkerOptions()
                        .position(posicion)
                        .icon(BitmapDescriptorFactory.defaultMarker(colorPin))
        );

        if (marker != null) {
            marker.setTag(punto);
        }
    }

    private boolean estaCerca(PuntoEducativo punto) {
        if (ubicacionActual == null) {
            return false;
        }

        Location ubicacionPunto = new Location("punto");
        ubicacionPunto.setLatitude(punto.getLatitud());
        ubicacionPunto.setLongitude(punto.getLongitud());

        float distancia = ubicacionActual.distanceTo(ubicacionPunto);

        return distancia <= RADIO_DESBLOQUEO;
    }

    private void abrirDetallePunto(PuntoEducativo punto) {
        Intent intent = new Intent(requireContext(), DetallePuntoActivity.class);

        intent.putExtra("id", punto.getId());
        intent.putExtra("nombre", punto.getNombre());
        intent.putExtra("descripcion", punto.getDescripcion());

        intent.putExtra("latitud", punto.getLatitud());
        intent.putExtra("longitud", punto.getLongitud());

        intent.putExtra("imagenReferencia", punto.getImagenReferencia());
        intent.putExtra("modelo3D", punto.getModelo3D());

        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CODIGO_PERMISO_UBICACION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacionActual();
            } else {
                Toast.makeText(getContext(),
                        "Permiso de ubicación denegado. Los pines aparecerán rojos.",
                        Toast.LENGTH_SHORT).show();
                dibujarPines();
            }
        }
    }
}