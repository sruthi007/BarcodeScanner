package com.example.mybarcodescanner;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
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
import com.journeyapps.barcodescanner.ScanIntentResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private FirebaseFirestore db;
    private String currentAction;
    private List<Item> itemList = new ArrayList<>();
    private ItemAdapter itemAdapter;
    private LocalDate mfdDate;
    private LocalDate expiryDate;

    // ActivityResultLauncher for requesting camera permission
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), this::onPermissionResult);

    // ActivityResultLauncher for scanning QR codes
    private final ActivityResultLauncher<ScanOptions> qrCodeLauncher =
            registerForActivityResult(new ScanContract(), this::onScanResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initBinding();
        initViews();
        initFirestore();
        initRecyclerView();
    }

    private void initBinding() {
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    private void initViews() {
        binding.fab.setOnClickListener(this::showPopupMenu);
    }

    private void initFirestore() {
        db = FirebaseFirestore.getInstance();
    }

    private void initRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        itemAdapter = new ItemAdapter(itemList);
        recyclerView.setAdapter(itemAdapter);
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
            checkPermissionAndActivity();
            return true;
        } else if (itemId == R.id.action_delete) {
            currentAction = "delete";
            checkPermissionAndActivity();
            return true;
        } else if (itemId == R.id.action_show) {
            loadItems();
            return true;
        }
        return false;
    }

    private void checkPermissionAndActivity() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            showCamera();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                Toast.makeText(this, "Camera Permission Needed", Toast.LENGTH_SHORT).show();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        }
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

    private void handleBarcodeScan(String contents) {
        if ("add".equals(currentAction)) {
            promptForItemDetails(contents);
        } else if ("delete".equals(currentAction)) {
            deleteItem(contents);
        }
    }

    private void promptForItemDetails(String barcode) {
        // Inflate a custom layout that includes EditText for item name and DatePickers for dates
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_item_details, null);
        EditText itemNameInput = dialogView.findViewById(R.id.editTextItemName);
        EditText mfdDateInput = dialogView.findViewById(R.id.editTextMfdDate);
        EditText expiryDateInput = dialogView.findViewById(R.id.editTextExpiryDate);

        mfdDateInput.setOnClickListener(v -> showDatePickerDialog(date -> {
            mfdDate = date;
            mfdDateInput.setText(date.toString());
        }));

        expiryDateInput.setOnClickListener(v -> showDatePickerDialog(date -> {
            expiryDate = date;
            expiryDateInput.setText(date.toString());
        }));

        // Show a dialog to get these details
        new AlertDialog.Builder(this)
                .setTitle("Enter Item Details")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String itemName = itemNameInput.getText().toString();
                    addItem(barcode, itemName, mfdDate.toString(), expiryDate.toString());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDatePickerDialog(OnDateSetListener listener) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> listener.onDateSet(LocalDate.of(year, month + 1, dayOfMonth)),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void addItem(String barcode, String name, String mfdDate, String expiryDate) {
        Item newItem = new Item(barcode, name, mfdDate, expiryDate);
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
                            if (item != null && item.getBarcode() != null) {
                                itemList.add(item);
                            } else {
                                Log.w(TAG, "Retrieved null or invalid item from Firestore");
                            }
                        }
                        itemAdapter.notifyDataSetChanged();
                    } else {
                        Log.e(TAG, "Error getting items: ", task.getException());
                        Toast.makeText(this, "Error getting items", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Permission result callback
    private void onPermissionResult(boolean isGranted) {
        if (isGranted) {
            showCamera();
        } else {
            Toast.makeText(this, "Camera permission is needed to scan barcodes", Toast.LENGTH_SHORT).show();
        }
    }

    // Scan result callback
    private void onScanResult(ScanIntentResult result) {
        if (result.getContents() == null) {
            Toast.makeText(this, "Closed", Toast.LENGTH_SHORT).show();
        } else {
            handleBarcodeScan(result.getContents());
        }
    }

    // Interface for date set listener
    private interface OnDateSetListener {
        void onDateSet(LocalDate date);
    }
}