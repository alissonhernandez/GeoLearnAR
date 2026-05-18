package com.example.guiaeducativaar.models;

public class PuntoEducativo {

    private String id;
    private String nombre;
    private String descripcion;
    private double latitud;
    private double longitud;
    private String imagenReferencia;
    private String modelo3D;

    public PuntoEducativo() {
    }

    public PuntoEducativo(String id, String nombre, String descripcion, double latitud, double longitud,
                          String imagenReferencia, String modelo3D) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.latitud = latitud;
        this.longitud = longitud;
        this.imagenReferencia = imagenReferencia;
        this.modelo3D = modelo3D;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public double getLatitud() {
        return latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public String getImagenReferencia() {
        return imagenReferencia;
    }

    public String getModelo3D() {
        return modelo3D;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }

    public void setImagenReferencia(String imagenReferencia) {
        this.imagenReferencia = imagenReferencia;
    }

    public void setModelo3D(String modelo3D) {
        this.modelo3D = modelo3D;
    }
}