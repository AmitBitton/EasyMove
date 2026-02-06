package com.example.easymove.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.easymove.model.repository.InventoryRepository;
import com.example.easymove.model.repository.UserRepository;

/**
 * Custom ViewModel Factory to handle Dependency Injection.
 * It ensures that ViewModels are created with the necessary Repositories.
 * * This allows Repositories to be Singletons within the scope of this Factory,
 * rather than creating a new Repository instance inside every ViewModel.
 */
public class AppViewModelFactory implements ViewModelProvider.Factory {

    // Initialize Repositories once (Singletons)
    private final InventoryRepository inventoryRepo = new InventoryRepository();
    private final UserRepository userRepo = new UserRepository();
    // private final MoveRepository moveRepo = new MoveRepository(); // Ready for future use

    @NonNull
    @Override
    @SuppressWarnings("unchecked") // Suppress unchecked cast warning
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {

        // 1. InventoryViewModel - Injects InventoryRepository
        if (modelClass.isAssignableFrom(InventoryViewModel.class)) {
            return (T) new InventoryViewModel(inventoryRepo);
        }

        // 2. UserViewModel
        if (modelClass.isAssignableFrom(UserViewModel.class)) {
            // Currently, UserViewModel instantiates its own repository internally.
            // TODO: Update UserViewModel constructor to accept 'userRepo' for better testing/efficiency.
            return (T) new UserViewModel();
        }

        // --- Future Extensions ---
        // if (modelClass.isAssignableFrom(MyMoveViewModel.class)) {
        //     return (T) new MyMoveViewModel(moveRepo);
        // }

        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}