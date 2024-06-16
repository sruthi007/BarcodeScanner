package com.example.mybarcodescanner;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    private List<Item> items;
    private FirebaseFirestore db;

    public ItemAdapter(List<Item> items) {
        this.items = items;
        this.db = FirebaseFirestore.getInstance();
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
        } else if (expiryDate.isAfter(today.plusDays(2))) {
            background.setColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.orange));
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

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Item Details")
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .setNegativeButton("Delete", (d, which) -> {
                    deleteItem(context, item.getBarcode());
                })
                .show();
    }

    private void deleteItem(Context context, String barcode) {
        db.collection("items").document(barcode).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show();
                    removeItemFromList(barcode);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error deleting item", Toast.LENGTH_SHORT).show();
                });
    }

    private void removeItemFromList(String barcode) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getBarcode().equals(barcode)) {
                items.remove(i);
                notifyItemRemoved(i);
                notifyItemRangeChanged(i, items.size());
                break;
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textItemName);
        }
    }
}
