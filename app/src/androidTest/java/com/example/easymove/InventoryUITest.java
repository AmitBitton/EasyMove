package com.example.easymove;

import android.Manifest;
import android.view.View;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import com.example.easymove.view.activities.MainActivity;

import org.hamcrest.Matcher;
import static org.hamcrest.CoreMatchers.containsString;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class InventoryUITest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(
            Manifest.permission.POST_NOTIFICATIONS,   // For Android 13+
            Manifest.permission.ACCESS_FINE_LOCATION, // Common for move apps
            Manifest.permission.ACCESS_COARSE_LOCATION
    );

    @Before
    public void setUp() {
        // Check if we are on the login screen and log in if necessary
        try {
            // Try to find the email field. If it exists, we are on the Login Screen.
            onView(withId(R.id.editEmail)).check(matches(isDisplayed()));

            // PERFORM LOGIN
            onView(withId(R.id.editEmail)).perform(typeText("alaasalah142@gmail.com"), closeSoftKeyboard()); // REPLACE WITH REAL EMAIL
            onView(withId(R.id.editPassword)).perform(typeText("03062001"), closeSoftKeyboard());    // REPLACE WITH REAL PASSWORD
            onView(withId(R.id.buttonAction)).perform(click());

            // Wait for the login to complete and Main Screen to load.
            // In real tests, IdlingResource is better, but a simple sleep works for assignments.
            try { Thread.sleep(3000); } catch (InterruptedException e) { e.printStackTrace(); }

        } catch (NoMatchingViewException e) {
            // If editEmail is not found, we are already logged in. Continue.
        }
    }

    @Test
    public void testAddItemToInventory() {
        // Generate a unique name for this test run
        String uniqueName = "Box " + System.currentTimeMillis();

        // 1. Navigate to Inventory
        onView(withId(R.id.btnViewItems)).perform(click());

        // 2. Click Add Button
        onView(withId(R.id.fabAddItem)).perform(click());

        // 3. Fill Form
        onView(withId(R.id.editItemName))
                .perform(typeText(uniqueName), closeSoftKeyboard());

        onView(withId(R.id.editItemDescription))
                .perform(typeText("Automated Test Description"), closeSoftKeyboard());

        onView(withId(R.id.editItemQuantity))
                .perform(replaceText("10"), closeSoftKeyboard());

        // 4. Click Save
        onView(withId(R.id.btnSaveItem)).perform(click());

        // Wait for list to refresh
        try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }

        // 5. Verify Item exists in list
        onView(withId(R.id.recyclerInventory))
                .check(matches(hasDescendant(withText(uniqueName))));
    }

    @Test
    public void testViewItemDetails() {
        // Generate a unique name so we don't clash with previous failed tests
        String uniqueItemName = "Item " + System.currentTimeMillis();
        String uniqueDesc = "Secret " + System.currentTimeMillis();

        // 1. Navigate & Setup
        onView(withId(R.id.btnViewItems)).perform(click());

        // Add a specific item
        onView(withId(R.id.fabAddItem)).perform(click());
        onView(withId(R.id.editItemName)).perform(typeText(uniqueItemName), closeSoftKeyboard());
        onView(withId(R.id.editItemDescription)).perform(typeText(uniqueDesc), closeSoftKeyboard());
        onView(withId(R.id.btnSaveItem)).perform(click());

        // Wait for UI to update (crucial for RecyclerView)
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 2. Click "Show Details" inside the SPECIFIC card we just created
        onView(withId(R.id.recyclerInventory))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(uniqueItemName)), // Match only this specific item
                        clickChildViewWithId(R.id.btnItemDetails)));

        // 3. Assert popup appears with correct description
        onView(withText(containsString(uniqueDesc))).check(matches(isDisplayed()));
    }

    public static ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() { return null; }

            @Override
            public String getDescription() { return "Click on a child view with specified id."; }

            @Override
            public void perform(UiController uiController, View view) {
                View v = view.findViewById(id);
                if (v != null) v.performClick();
            }
        };
    }
}