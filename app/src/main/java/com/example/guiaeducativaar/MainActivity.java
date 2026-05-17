package com.example.guiaeducativaar;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.guiaeducativaar.fragments.AyudaFragment;
import com.example.guiaeducativaar.fragments.HomeFragment;
import com.example.guiaeducativaar.fragments.ListaPuntosFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigation = findViewById(R.id.bottomNavigation);

        cargarFragment(new HomeFragment());

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                cargarFragment(new HomeFragment());
                return true;
            } else if (id == R.id.nav_puntos) {
                cargarFragment(new ListaPuntosFragment());
                return true;
            } else if (id == R.id.nav_ayuda) {
                cargarFragment(new AyudaFragment());
                return true;
            }

            return false;
        });
    }

    private void cargarFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contenedorFragments, fragment)
                .commit();
    }
}