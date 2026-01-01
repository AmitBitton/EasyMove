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

public class AddItemDialogFragment extends DialogFragment {

    private InventoryViewModel viewModel;

    private EditText editName, editDesc, editQuantity;
    private Spinner spinnerRoom;
    private CheckBox checkFragile;
    private ImageView imagePreview;

    private Uri selectedImageUri = null;
    private Uri cameraImageUri = null;

    private final String[] rooms = {"סלון", "מטבח", "חדר שינה", "חדר ילדים", "אמבטיה", "מחסן", "אחר"};

    // ✅ Chooser של המערכת (כמו “Photos / תמונות” מלמטה)
    private final ActivityResultLauncher<Intent> imageChooserLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != android.app.Activity.RESULT_OK) return;

                Uri uri = null;
                Intent data = result.getData();

                // 1) אם נבחרה תמונה מאפליקציה (Photos/תמונות) נקבל URI ב-data
                if (data != null && data.getData() != null) {
                    uri = data.getData();
                }
                // 2) אם זו מצלמה עם EXTRA_OUTPUT לרוב data יהיה null → משתמשים ב-cameraImageUri
                else if (cameraImageUri != null) {
                    uri = cameraImageUri;
                }

                if (uri != null) {
                    selectedImageUri = uri;
                    imagePreview.setImageURI(selectedImageUri);
                    imagePreview.setVisibility(View.VISIBLE);
                }
            });

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (Boolean.TRUE.equals(granted)) {
                    openSystemChooserWithCamera();
                } else {
                    Toast.makeText(getContext(), "אין הרשאת מצלמה", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public void onStart() {
        super.onStart();
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

        viewModel = new ViewModelProvider(requireParentFragment(), ViewModelFactoryProvider.factory)
                .get(InventoryViewModel.class);

        editName = view.findViewById(R.id.editItemName);
        editDesc = view.findViewById(R.id.editItemDescription);
        editQuantity = view.findViewById(R.id.editItemQuantity);
        spinnerRoom = view.findViewById(R.id.spinnerRoomType);
        checkFragile = view.findViewById(R.id.checkFragile);
        imagePreview = view.findViewById(R.id.imagePreview);

        Button btnAddImage = view.findViewById(R.id.btnAddImage);
        Button btnSave = view.findViewById(R.id.btnSaveItem);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, rooms);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRoom.setAdapter(adapter);

        btnAddImage.setOnClickListener(v -> openChooserWithPermissionIfNeeded());
        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> saveItem());

        viewModel.getAddSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                dismiss();
                viewModel.resetAddSuccess();
            }
        });
    }

    private void openChooserWithPermissionIfNeeded() {
        // אם אין הרשאת מצלמה, עדיין אפשר להציג chooser (Photos/תמונות),
        // אבל כדי שהמצלמה תעבוד מיד מתוך אותו chooser – נבקש הרשאה מראש.
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openSystemChooserWithCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openSystemChooserWithCamera() {
        cameraImageUri = null;

        // A) בסיס: בחירת תמונה (זה מה שפותח את ה-bottom sheet עם Photos/תמונות)
        Intent basePick = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        basePick.setType("image/*");

        // B) אופציה 1: מצלמה
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            try {
                File photoFile = createTempImageFile();
                String authority = requireContext().getPackageName() + ".fileprovider";
                cameraImageUri = FileProvider.getUriForFile(requireContext(), authority, photoFile);

                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                cameraIntent.setClipData(ClipData.newRawUri("camera", cameraImageUri));
            } catch (Exception e) {
                cameraIntent = null; // אם נכשל, נוותר על מצלמה
                cameraImageUri = null;
            }
        } else {
            cameraIntent = null;
        }

        // C) אופציה 2: Google Photos (אם מותקן) – רק “לדחוף” אותו שיופיע
        Intent googlePhotosIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        googlePhotosIntent.setType("image/*");
        googlePhotosIntent.setPackage("com.google.android.apps.photos");
        if (googlePhotosIntent.resolveActivity(requireActivity().getPackageManager()) == null) {
            googlePhotosIntent = null;
        }

        // בונים chooser של המערכת (ללא כותרת כדי שיהיה כמו פעם)
        Intent chooser = Intent.createChooser(basePick, null);

        ArrayList<Intent> initialIntents = new ArrayList<>();
        if (cameraIntent != null) initialIntents.add(cameraIntent);
        if (googlePhotosIntent != null) initialIntents.add(googlePhotosIntent);

        if (!initialIntents.isEmpty()) {
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, initialIntents.toArray(new Intent[0]));
        }

        imageChooserLauncher.launch(chooser);
    }

    private File createTempImageFile() throws IOException {
        File dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (dir == null) dir = requireContext().getCacheDir();
        return File.createTempFile("camera_", ".jpg", dir);
    }

    private void saveItem() {
        String name = editName.getText().toString().trim();
        String desc = editDesc.getText().toString().trim();
        String quantityStr = editQuantity.getText().toString().trim();
        String room = spinnerRoom.getSelectedItem().toString();
        boolean isFragile = checkFragile.isChecked();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "נא להזין שם פריט", Toast.LENGTH_SHORT).show();
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
