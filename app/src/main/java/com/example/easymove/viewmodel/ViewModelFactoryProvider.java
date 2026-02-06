package com.example.easymove.viewmodel;

import androidx.lifecycle.ViewModelProvider;

/**
 * A Singleton provider for the AppViewModelFactory.
 * * Purpose:
 * This class ensures that the entire application shares a SINGLE instance of the ViewModelFactory.
 * Since the Factory holds the Repositories (InventoryRepo, UserRepo, etc.), this effectively
 * makes those repositories singletons as well, ensuring data consistency across the app.
 */
public class ViewModelFactoryProvider {

    // The single instance of the factory
    private static final ViewModelProvider.Factory INSTANCE = new AppViewModelFactory();

    // Private constructor to prevent instantiation of this utility class
    private ViewModelFactoryProvider() {}

    /**
     * @return The singleton instance of the ViewModelFactory.
     */
    public static ViewModelProvider.Factory getFactory() {
        return INSTANCE;
    }
}