package com.example.mybarcodescanner;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

        // Handle button clicks
        binding.btnAddItem.setOnClickListener(v -> {
            currentAction = "add";
            checkPermissionAndActivity();
        });

        binding.btnDeleteItem.setOnClickListener(v -> {
            currentAction = "delete";
            checkPermissionAndActivity();
        });

        binding.btnShowItems.setOnClickListener(v -> loadItems());

        // Set up the Floating Action Button
        binding.fab.setOnClickListener(v -> showExpiringItemsDialog());

        // Initialize badge drawable
        badgeDrawable = BadgeDrawable.create(this);
        badgeDrawable.setBadgeGravity(BadgeDrawable.TOP_END);
        badgeDrawable.setMaxCharacterCount(3); // Adjust as needed for larger numbers
        badgeDrawable.setHorizontalOffset(20);
        badgeDrawable.setVerticalOffset(20);
        badgeDrawable.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
        binding.fab.post(() -> BadgeUtils.attachBadgeDrawable(badgeDrawable, binding.fab));

        // Update the notification badge
        updateNotificationBadge();
    }

    private void initBinding() {
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    private void initViews() {
        // You can remove the FAB if not needed
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
                    updateNotificationBadge(); // Update the notification badge
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
                    updateNotificationBadge(); // Update the notification badge
                    loadItems(); // Optionally refresh the list immediately after deleting an item
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
                                if (expiryDate.isBefore(today) || expiryDate.equals(today) || expiryDate.isBefore(today.plusDays(7))) {
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
            Toast.makeText(this, "No items expiring soon.", Toast.LENGTH_SHORT).show();
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
                .setTitle("Expiring Items")
                .setMessage(message.toString())
                .setPositiveButton("OK", (dialog, which) -> {
                    badgeDrawable.setVisible(false); // Clear the badge after reading the message
                    expiringItems.clear(); // Clear the list to avoid re-triggering the badge
                })
                .show();
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

    // Handle back button press to navigate to home page
    @Override
    public void onBackPressed() {
        if (binding.recyclerView.getVisibility() == View.VISIBLE) {
            // If the RecyclerView is visible, return to the home screen (initial state)
            binding.recyclerView.setVisibility(View.GONE);
            binding.layoutResult.setVisibility(View.VISIBLE);

            // Show action buttons and text again
            binding.layoutAddItem.setVisibility(View.VISIBLE);
            binding.layoutDeleteItem.setVisibility(View.VISIBLE);
            binding.layoutShowItems.setVisibility(View.VISIBLE);
        } else {
            // Otherwise, call the superclass method to handle the default back press
            super.onBackPressed();
        }
    }

    // Interface for date set listener
    private interface OnDateSetListener {
        void onDateSet(LocalDate date);
    }
}
