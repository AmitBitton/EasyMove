package com.example.easymove.view.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.easymove.R;
import com.example.easymove.viewmodel.InventoryViewModel;
import com.example.easymove.viewmodel.ViewModelFactoryProvider;

public class AddItemDialogFragment extends DialogFragment {

    private InventoryViewModel viewModel;

    private EditText editName, editDesc, editQuantity;
    private Spinner spinnerRoom;
    private CheckBox checkFragile;
    private ImageView imagePreview;
    private Uri selectedImageUri = null;

    // רשימת חדרים לבחירה
    private final String[] rooms = {"סלון", "מטבח", "חדר שינה", "חדר ילדים", "אמבטיה", "מחסן", "אחר"};

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    imagePreview.setImageURI(selectedImageUri);
                    imagePreview.setVisibility(View.VISIBLE);
                }
            }
    );

    @Override
    public void onStart() {
        super.onStart();
        // הרחבת הדיאלוג לרוחב המסך
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

        // חיבור ל-ViewModel (משותף עם הפרגמנט שקרא לו כדי שיתעדכן)
        viewModel = new ViewModelProvider(requireParentFragment(), ViewModelFactoryProvider.factory).get(InventoryViewModel.class);

        // חיבור Views
        editName = view.findViewById(R.id.editItemName);
        editDesc = view.findViewById(R.id.editItemDescription);
        editQuantity = view.findViewById(R.id.editItemQuantity);
        spinnerRoom = view.findViewById(R.id.spinnerRoomType);
        checkFragile = view.findViewById(R.id.checkFragile);
        imagePreview = view.findViewById(R.id.imagePreview);
        Button btnAddImage = view.findViewById(R.id.btnAddImage);
        Button btnSave = view.findViewById(R.id.btnSaveItem);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        // הגדרת הספינר
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, rooms);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRoom.setAdapter(adapter);

        // כפתורים
        btnAddImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        btnCancel.setOnClickListener(v -> dismiss());

        btnSave.setOnClickListener(v -> saveItem());

        // האזנה להצלחה כדי לסגור את החלון
        viewModel.getAddSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                dismiss();
                viewModel.resetAddSuccess(); // איפוס
            }
        });
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

        int quantity = 1;
        try {
            quantity = Integer.parseInt(quantityStr);
        } catch (NumberFormatException e) {
            quantity = 1;
        }

        viewModel.addItem(name, desc, room, isFragile, quantity, selectedImageUri);
    }
}