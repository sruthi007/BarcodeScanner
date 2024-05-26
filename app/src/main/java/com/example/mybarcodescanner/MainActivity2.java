package com.example.mybarcodescanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mybarcodescanner.databinding.ActivityMainBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.List;

import com.example.mybarcodescanner.R;

public class MainActivity2 extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseFirestore db;
    private String currentAction;
    private List<Item> itemList = new ArrayList<>();
    private ItemAdapter itemAdapter;

    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    showCamera();
                } else {
                    Toast.makeText(this, "Camera permission is needed to scan barcodes", Toast.LENGTH_SHORT).show();
                }
            });

    private ActivityResultLauncher<ScanOptions> qrCodeLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() == null) {
            Toast.makeText(this, "Closed", Toast.LENGTH_SHORT).show();
        } else {
            setResult(result.getContents());
        }
    });

    private void setResult(String contents) {
        binding.textResult.setText(contents);
        binding.recyclerView.setVisibility(View.GONE);
        binding.layoutResult.setVisibility(View.VISIBLE);
    }

    private void showCamera() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);
        options.setPrompt("Scan a barcode");
        options.setCameraId(0);
        options.setBeepEnabled(false);
        options.setBarcodeImageEnabled(true);
        options.setOrientationLocked(false);

        qrCodeLauncher.launch(options);
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initBinding();
        initViews();

        db = FirebaseFirestore.getInstance();

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        itemAdapter = new ItemAdapter(itemList);
        recyclerView.setAdapter(itemAdapter);
    }

    private void initViews() {
        binding.fab.setOnClickListener(view -> showPopupMenu(view));
    }

    private void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_main, popup.getMenu());
        popup.setOnMenuItemClickListener(this::onMenuItemClick);
        popup.show();
    }

    private boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_add) {
            currentAction = "add";
            checkPermissionAndActivity(this);
            return true;
        } else if (itemId == R.id.action_delete) {
            currentAction = "delete";
            checkPermissionAndActivity(this);
            return true;
        } else if (itemId == R.id.action_show) {
            loadItems();
            return true;
        } else {
            return false;
        }
    }

    private void checkPermissionAndActivity(Context context) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {
            showCamera();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                Toast.makeText(context, "Camera Permission Needed", Toast.LENGTH_SHORT).show();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        }
    }

    private void initBinding() {
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    private void addItem(String barcode) {
        String itemName = "Item " + barcode;
        Item newItem = new Item(barcode, itemName);
        db.collection("items").document(barcode).set(newItem)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Item added", Toast.LENGTH_SHORT).show();
                    loadItems(); // Optionally refresh the list immediately after adding an item
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error adding item", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteItem(String barcode) {
        db.collection("items").document(barcode).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show();
                    loadItems(); // Optionally refresh the list immediately after deleting an item
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting item", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadItems() {
        binding.recyclerView.setVisibility(View.VISIBLE);
        binding.layoutResult.setVisibility(View.GONE);

        db.collection("items").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        itemList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Item item = document.toObject(Item.class);
                            itemList.add(item);
                        }
                        itemAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Error getting items", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
