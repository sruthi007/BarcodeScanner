package com.example.mybarcodescanner;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mybarcodescanner.databinding.ActivityMainBinding;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
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
    private BadgeDrawable badgeDrawable;
    private List<Item> expiringItems = new ArrayList<>();
    private String currentFilter = "All";

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

        binding.btnAddItem.setOnClickListener(v -> {
            currentAction = "add";
            checkPermissionAndActivity();
        });

        binding.btnDeleteItem.setOnClickListener(v -> {
            currentAction = "delete";
            checkPermissionAndActivity();
        });

        binding.btnShowItems.setOnClickListener(v -> {
            showFilterLayout(true);
            loadItems();
        });

        
        binding.fab.setOnClickListener(v -> showExpiringItemsDialog());

        badgeDrawable = BadgeDrawable.create(this);
        badgeDrawable.setBadgeGravity(BadgeDrawable.TOP_END);
        badgeDrawable.setMaxCharacterCount(3); 
        badgeDrawable.setHorizontalOffset(20);
        badgeDrawable.setVerticalOffset(20);
        badgeDrawable.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
        binding.fab.post(() -> BadgeUtils.attachBadgeDrawable(badgeDrawable, binding.fab));

        // Update the notification badge
        updateNotificationBadge();

        // Handle filter actions
        binding.checkBoxExpired.setOnClickListener(v -> loadItems());
        binding.checkBoxExpiringSoon.setOnClickListener(v -> loadItems());
        binding.checkBoxValid.setOnClickListener(v -> loadItems());

        // Search bar filter
        binding.searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadItems();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void initBinding() {
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    private void initViews() {
        binding.fab.setOnClickListener(v -> showCamera());
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
        Log.d(TAG, "Barcode scanned: " + contents);
        if ("add".equals(currentAction)) {
            promptForItemDetails(contents);
        } else if ("delete".equals(currentAction)) {
            deleteItem(contents);
        }
    }

    private void promptForItemDetails(String barcode) {
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

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Enter Item Details")
                .setView(dialogView)
                .setPositiveButton("Add", (dialogInterface, which) -> {
                    String itemName = itemNameInput.getText().toString();
                    addItem(barcode, itemName, mfdDate.toString(), expiryDate.toString());
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            itemNameInput.setEnabled(true); 
            mfdDateInput.setEnabled(true);  
            expiryDateInput.setEnabled(true);  
            itemNameInput.requestFocus();
            showKeyboard(itemNameInput);
        });

        dialog.show();
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
                    updateNotificationBadge(); 
                    loadItems(); 
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error adding item", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteItem(String barcode) {
        db.collection("items").document(barcode).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show();
                    updateNotificationBadge(); 
                    loadItems(); 
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting item", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadItems() {
        // Hide action buttons and text when showing items
        binding.layoutAddItem.setVisibility(View.GONE);
        binding.layoutDeleteItem.setVisibility(View.GONE);
        binding.layoutShowItems.setVisibility(View.GONE);
        binding.layoutFilter.setVisibility(View.VISIBLE);

        binding.recyclerView.setVisibility(View.VISIBLE);
        binding.layoutResult.setVisibility(View.GONE);

        db.collection("items").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        itemList.clear();
                        LocalDate today = LocalDate.now();
                        String searchText = binding.searchBar.getText().toString().toLowerCase();
                        boolean filterExpired = binding.checkBoxExpired.isChecked();
                        boolean filterExpiringSoon = binding.checkBoxExpiringSoon.isChecked();
                        boolean filterValid = binding.checkBoxValid.isChecked();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Item item = document.toObject(Item.class);
                            if (item != null && item.getBarcode() != null) {
                                LocalDate expiryDate = LocalDate.parse(item.getExpiryDate());
                                boolean matchesFilter = false;

                                if (filterExpired && expiryDate.isBefore(today)) {
                                    matchesFilter = true;
                                } else if (filterExpiringSoon && (expiryDate.isEqual(today) || expiryDate.isAfter(today) && expiryDate.isBefore(today.plusDays(7)))) {
                                    matchesFilter = true;
                                } else if (filterValid && expiryDate.isAfter(today.plusDays(7))) {
                                    matchesFilter = true;
                                }

                                if (!filterExpired && !filterExpiringSoon && !filterValid) {
                                    matchesFilter = true;
                                }

                                if (matchesFilter && (item.getName().toLowerCase().contains(searchText) || item.getBarcode().toLowerCase().contains(searchText))) {
                                    itemList.add(item);
                                }
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

    private void updateNotificationBadge() {
        db.collection("items").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        expiringItems.clear();
                        int expiryCount = 0;
                        LocalDate today = LocalDate.now();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Item item = document.toObject(Item.class);
                            if (item != null && item.getExpiryDate() != null) {
                                LocalDate expiryDate = LocalDate.parse(item.getExpiryDate());
                                if (expiryDate.isBefore(today)) {
                                    expiringItems.add(item);
                                    expiryCount++;
                                }
                            }
                        }
                        if (expiryCount > 0) {
                            badgeDrawable.setNumber(expiryCount);
                            badgeDrawable.setVisible(true);
                        } else {
                            badgeDrawable.setVisible(false);
                        }
                    }
                });
    }

    private void showExpiringItemsDialog() {
        if (expiringItems.isEmpty()) {
            Toast.makeText(this, "No New Notifications.", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder message = new StringBuilder();
        for (Item item : expiringItems) {
            message.append("Name: ").append(item.getName())
                    .append("\nBarcode: ").append(item.getBarcode())
                    .append("\nExpiry Date: ").append(item.getExpiryDate())
                    .append("\n\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Expired Items")
                .setMessage(message.toString())
                .setPositiveButton("OK", (dialog, which) -> {
                    badgeDrawable.setVisible(false); 
                    expiringItems.clear();
                })
                .show();
    }

    private void showFilterLayout(boolean show) {
        binding.layoutFilter.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void onPermissionResult(boolean isGranted) {
        if (isGranted) {
            showCamera();
        } else {
            Toast.makeText(this, "Camera permission is needed to scan barcodes", Toast.LENGTH_SHORT).show();
        }
    }

    private void onScanResult(ScanIntentResult result) {
        if (result.getContents() == null) {
            Toast.makeText(this, "Closed", Toast.LENGTH_SHORT).show();
        } else {
            handleBarcodeScan(result.getContents());
        }
    }

    // Handle back button press to navigate to home page
    @Override
    public void onBackPressed() {
        if (binding.recyclerView.getVisibility() == View.VISIBLE) {

            binding.recyclerView.setVisibility(View.GONE);
            binding.layoutResult.setVisibility(View.VISIBLE);
            binding.layoutAddItem.setVisibility(View.VISIBLE);
            binding.layoutDeleteItem.setVisibility(View.VISIBLE);
            binding.layoutShowItems.setVisibility(View.VISIBLE);
            showFilterLayout(false);
        } else {
            super.onBackPressed();
        }
    }

    private void showKeyboard(View view) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private interface OnDateSetListener {
        void onDateSet(LocalDate date);
    }
}
