package com.example.easymove.viewmodel;

import androidx.lifecycle.ViewModelProvider;

public class ViewModelFactoryProvider {
    // משתנה סטטי יחיד שמחזיק את המפעל
    public static final ViewModelProvider.Factory factory = new AppViewModelFactory();
}