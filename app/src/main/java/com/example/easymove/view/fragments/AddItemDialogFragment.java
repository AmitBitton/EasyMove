package com.example.easymove.view.fragments;

import android.Manifest;
import android.app.Dialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.easymove.R;
import com.example.easymove.viewmodel.InventoryViewModel;
import com.example.easymove.viewmodel.ViewModelFactoryProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog Fragment for adding a new item to the user's moving inventory.
 * Supports:
 * 1. Text input (Name, Description, Quantity).
 * 2. Room selection via Spinner.
 * 3. Fragile toggle.
 * 4. Image attachment via Camera or Gallery (System Chooser).
 */
public class AddItemDialogFragment extends DialogFragment {

    private static final String TAG = "AddItemDialog";

    private InventoryViewModel viewModel;

    // UI Components
    private EditText editName, editDesc, editQuantity;
    private Spinner spinnerRoom;
    private CheckBox checkFragile;
    private ImageView imagePreview;

    // Image Handling
    private Uri selectedImageUri = null;
    private Uri cameraImageUri = null;

    private final String[] rooms = {"סלון", "מטבח", "חדר שינה", "חדר ילדים", "אמבטיה", "מחסן", "אחר"};

    // ------------------------------------------------------------------------
    // Activity Result Launchers
    // ------------------------------------------------------------------------

    /**
     * Unified Launcher for picking an image (Gallery) or capturing one (Camera).
     */
    private final ActivityResultLauncher<Intent> imageChooserLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != android.app.Activity.RESULT_OK) return;

                Uri uri = null;
                Intent data = result.getData();

                // Case 1: Image selected from Gallery (data contains URI)
                if (data != null && data.getData() != null) {
                    uri = data.getData();
                }
                // Case 2: Image captured via Camera (data is null, use pre-saved URI)
                else if (cameraImageUri != null) {
                    uri = cameraImageUri;
                }

                if (uri != null) {
                    selectedImageUri = uri;
                    imagePreview.setImageURI(selectedImageUri);
                    imagePreview.setVisibility(View.VISIBLE);
                }
            });

    /**
     * Launcher for requesting Camera permission.
     */
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (Boolean.TRUE.equals(granted)) {
                    openSystemChooserWithCamera();
                } else {
                    Toast.makeText(getContext(), "האפליקציה צריכה גישה למצלמה", Toast.LENGTH_SHORT).show();
                }
            });

    // ------------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------------

    @Override
    public void onStart() {
        super.onStart();
        // Expand dialog to full width
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_item, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel (Shared with Parent Fragment if possible, or Factory driven)
        // Using requireParentFragment() ensures we share state or events if needed.
        // If ViewModelFactoryProvider.factory is not available globally, ensure it is passed correctly.
        try {
            viewModel = new ViewModelProvider(requireParentFragment(), ViewModelFactoryProvider.getFactory())
                    .get(InventoryViewModel.class);
        } catch (Exception e) {
            // Fallback for testing/preview
            viewModel = new ViewModelProvider(this).get(InventoryViewModel.class);
        }

        initViews(view);
        setupListeners(view);
        observeViewModel();
    }

    private void initViews(View view) {
        editName = view.findViewById(R.id.editItemName);
        editDesc = view.findViewById(R.id.editItemDescription);
        editQuantity = view.findViewById(R.id.editItemQuantity);
        spinnerRoom = view.findViewById(R.id.spinnerRoomType);
        checkFragile = view.findViewById(R.id.checkFragile);
        imagePreview = view.findViewById(R.id.imagePreview);

        // Setup Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, rooms);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRoom.setAdapter(adapter);
    }

    private void setupListeners(View view) {
        Button btnAddImage = view.findViewById(R.id.btnAddImage);
        Button btnSave = view.findViewById(R.id.btnSaveItem);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        btnAddImage.setOnClickListener(v -> openChooserWithPermissionIfNeeded());
        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> saveItem());
    }

    private void observeViewModel() {
        viewModel.getAddSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                dismiss();
                viewModel.resetAddSuccess();
            }
        });
    }

    // ------------------------------------------------------------------------
    // Logic: Image Picking
    // ------------------------------------------------------------------------

    /**
     * Checks for Camera permission before opening the chooser.
     * This ensures the "Camera" option inside the system chooser works immediately.
     */
    private void openChooserWithPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openSystemChooserWithCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * Opens the System Chooser (Bottom Sheet) allowing the user to pick between:
     * 1. Existing Gallery Apps (Photos, Gallery).
     * 2. Camera (New Photo).
     */
    private void openSystemChooserWithCamera() {
        cameraImageUri = null;

        // Intent A: Pick Image from Gallery
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");

        // Intent B: Capture Image with Camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        boolean canTakePhoto = cameraIntent.resolveActivity(requireActivity().getPackageManager()) != null;

        if (canTakePhoto) {
            try {
                File photoFile = createTempImageFile();
                String authority = requireContext().getPackageName() + ".fileprovider";
                cameraImageUri = FileProvider.getUriForFile(requireContext(), authority, photoFile);

                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                // Grant permissions for the camera app to write to this URI
                cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                cameraIntent.setClipData(ClipData.newRawUri("camera", cameraImageUri));
            } catch (Exception e) {
                Log.e(TAG, "Failed to create temp file for camera", e);
                cameraIntent = null; // Disable camera option if file creation fails
            }
        } else {
            cameraIntent = null;
        }

        // Combine Intents into a Chooser
        Intent chooser = Intent.createChooser(pickIntent, "בחר תמונה או צלם");

        List<Intent> extraIntents = new ArrayList<>();
        if (cameraIntent != null) {
            extraIntents.add(cameraIntent);
        }

        if (!extraIntents.isEmpty()) {
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents.toArray(new Intent[0]));
        }

        imageChooserLauncher.launch(chooser);
    }

    private File createTempImageFile() throws IOException {
        File dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (dir == null) dir = requireContext().getCacheDir();
        return File.createTempFile("camera_", ".jpg", dir);
    }

    // ------------------------------------------------------------------------
    // Logic: Save Item
    // ------------------------------------------------------------------------

    private void saveItem() {
        String name = editName.getText().toString().trim();
        String desc = editDesc.getText().toString().trim();
        String quantityStr = editQuantity.getText().toString().trim();
        String room = "";

        if (spinnerRoom.getSelectedItem() != null) {
            room = spinnerRoom.getSelectedItem().toString();
        }

        boolean isFragile = checkFragile.isChecked();

        if (name.isEmpty()) {
            editName.setError("חובה להזין שם פריט");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) quantity = 1;
        } catch (NumberFormatException ignored) {
            quantity = 1;
        }

        viewModel.addItem(name, desc, room, isFragile, quantity, selectedImageUri);
    }
}