package com.example.easymove.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.easymove.model.repository.InventoryRepository;
// import com.example.easymove.model.repository.MoveRepository; // נשתמש בזה בעתיד
import com.example.easymove.model.repository.UserRepository;

public class AppViewModelFactory implements ViewModelProvider.Factory {

    // יוצרים את הריפוזיטוריז פעם אחת
    private final InventoryRepository inventoryRepo = new InventoryRepository();
    private final UserRepository userRepo = new UserRepository();
    // private final MoveRepository moveRepo = new MoveRepository();

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {

        // אם מבקשים InventoryViewModel -> נחזיר אותו עם הריפוזיטורי המתאים
        if (modelClass.isAssignableFrom(InventoryViewModel.class)) {
            return (T) new InventoryViewModel(inventoryRepo);
        }

        // אם מבקשים UserViewModel -> נחזיר אותו (אם הוא צריך ריפוזיטורי בעתיד, נזריק כאן)
        if (modelClass.isAssignableFrom(UserViewModel.class)) {
            // כרגע UserViewModel שלך כנראה יוצר ריפוזיטורי בתוכו, אבל בעתיד נעביר לו מכאן:
            // return (T) new UserViewModel(userRepo);
            return (T) new UserViewModel();
        }

        // כאן נוסיף בעתיד: MyMoveViewModel, ChatViewModel...

        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}