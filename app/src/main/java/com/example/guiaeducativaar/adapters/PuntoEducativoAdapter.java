package com.example.guiaeducativaar.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.guiaeducativaar.R;
import com.example.guiaeducativaar.models.PuntoEducativo;

import java.util.List;

public class PuntoEducativoAdapter extends RecyclerView.Adapter<PuntoEducativoAdapter.PuntoVH> {

    private List<PuntoEducativo> listaPuntos;
    private OnPuntoClickListener listener;

    public interface OnPuntoClickListener {
        void onPuntoClick(PuntoEducativo punto);
        void onPuntoLongClick(PuntoEducativo punto);
    }

    public PuntoEducativoAdapter(List<PuntoEducativo> listaPuntos, OnPuntoClickListener listener) {
        this.listaPuntos = listaPuntos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PuntoVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_punto_educativo, parent, false);

        return new PuntoVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PuntoVH holder, int position) {
        PuntoEducativo punto = listaPuntos.get(position);

        holder.txtNombrePunto.setText(punto.getNombre());
        holder.txtDescripcionPunto.setText(punto.getDescripcion());
        holder.txtUbicacionPunto.setText(
                "Latitud: " + punto.getLatitud() + " | Longitud: " + punto.getLongitud()
        );

        holder.itemView.setOnClickListener(v -> listener.onPuntoClick(punto));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onPuntoLongClick(punto);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return listaPuntos.size();
    }

    public static class PuntoVH extends RecyclerView.ViewHolder {

        TextView txtNombrePunto, txtDescripcionPunto, txtUbicacionPunto;
        ImageView imgIconoEstacion;

        public PuntoVH(@NonNull View itemView) {
            super(itemView);

            txtNombrePunto = itemView.findViewById(R.id.txtNombrePunto);
            txtDescripcionPunto = itemView.findViewById(R.id.txtDescripcionPunto);
            txtUbicacionPunto = itemView.findViewById(R.id.txtUbicacionPunto);
            imgIconoEstacion = itemView.findViewById(R.id.imgIconoEstacion);
        }
    }
}