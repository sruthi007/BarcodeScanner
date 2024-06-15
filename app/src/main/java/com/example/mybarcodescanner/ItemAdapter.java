package com.example.mybarcodescanner;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    private List<Item> items;

    public ItemAdapter(List<Item> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = items.get(position);
        holder.textName.setText(item.getName().toUpperCase());

        LocalDate expiryDate = LocalDate.parse(item.getExpiryDate(), DateTimeFormatter.ISO_DATE);
        LocalDate today = LocalDate.now();

        GradientDrawable background = (GradientDrawable) holder.textName.getBackground();
        if (expiryDate.isAfter(today.plusDays(7))) {
            background.setColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.green));
        } else {
            background.setColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.red));
        }

        holder.itemView.setOnClickListener(v -> showItemDetails(v.getContext(), item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void showItemDetails(Context context, Item item) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_item_details, null);
        TextView textViewBarcode = dialogView.findViewById(R.id.textViewBarcode);
        EditText itemNameInput = dialogView.findViewById(R.id.editTextItemName);
        EditText mfdDateInput = dialogView.findViewById(R.id.editTextMfdDate);
        EditText expiryDateInput = dialogView.findViewById(R.id.editTextExpiryDate);

        textViewBarcode.setText("Barcode: " + item.getBarcode());
        itemNameInput.setText(item.getName());
        mfdDateInput.setText(item.getMfdDate());
        expiryDateInput.setText(item.getExpiryDate());

        new AlertDialog.Builder(context)
                .setTitle("Item Details")
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .show();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textItemName);
        }
    }
}
