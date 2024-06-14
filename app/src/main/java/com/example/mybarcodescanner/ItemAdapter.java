package com.example.mybarcodescanner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    private List<Item> items;

    public ItemAdapter(List<Item> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = items.get(position);
        holder.textBarcode.setText("Barcode: " + item.getBarcode());
        holder.textName.setText("Name: " + item.getName());
        holder.textMfdDate.setText("MFD: " + item.getMfdDate());
        holder.textExpiryDate.setText("EXP: " + item.getExpiryDate());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textBarcode, textName, textMfdDate, textExpiryDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textBarcode = itemView.findViewById(R.id.textBarcode);
            textName = itemView.findViewById(R.id.textItemName);
            textMfdDate = itemView.findViewById(R.id.textMfdDate);
            textExpiryDate = itemView.findViewById(R.id.textExpiryDate);
        }
    }
}
