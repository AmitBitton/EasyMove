package com.example.easymove; // שים לב שהחבילה תואמת לתיקייה שלך

import android.os.Looper;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.example.easymove.model.InventoryItem;
import com.example.easymove.model.repository.InventoryRepository;
import com.example.easymove.viewmodel.InventoryViewModel;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class InventoryViewModelUnitTests {

    // Rule שמבטיח ש-LiveData יעבוד בצורה סינכרונית בטסטים
    @Rule
    public InstantTaskExecutorRule instantRule = new InstantTaskExecutorRule();

    // Rule לאתחול אוטומטי של Mockito
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    InventoryRepository mockRepository; // ה-Mock שלנו

    private InventoryViewModel viewModel;

    @Before
    public void setUp() {
        // אתחול ה-ViewModel עם ה-Repository המזויף
        viewModel = new InventoryViewModel(mockRepository);
    }

    /**
     * בדיקה 1: הוספת פריט מוצלחת (Happy Path)
     */
    @Test
    public void testAddItem_Success() {
        // 1. Setting up conditions
        String userId = "testUser123";
        when(mockRepository.getCurrentUserId()).thenReturn(userId);
        when(mockRepository.getMyInventory()).thenReturn(Tasks.forResult(new ArrayList<>()));

        DocumentReference mockDocRef = mock(DocumentReference.class);
        Task<DocumentReference> successTask = Tasks.forResult(mockDocRef);

        when(mockRepository.addInventoryItem(any(InventoryItem.class), any())).thenReturn(successTask);

        // 2. Action
        viewModel.addItem("Sofa", "Big sofa", "Living Room", false, 1, null);

        // ביצוע משימות אסינכרוניות
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // 3. Assertions
        Boolean isSuccess = viewModel.getAddSuccess().getValue();
        assertNotNull("addSuccess LiveData should not be null", isSuccess);
        assertTrue("Expected addSuccess to be true", isSuccess);
        assertEquals("נוסף בהצלחה", viewModel.getToastMessage().getValue());

        verify(mockRepository, times(1)).addInventoryItem(any(InventoryItem.class), any());
    }

    /**
     * בדיקה 2: כישלון בהוספה (למשל שגיאת שרת/רשת)
     */
    @Test
    public void testAddItem_Failure() {
        // 1. Setting up conditions
        when(mockRepository.getCurrentUserId()).thenReturn("testUser123");

        Task<DocumentReference> failedTask = Tasks.forException(new Exception("Firebase Error"));
        when(mockRepository.addInventoryItem(any(InventoryItem.class), any())).thenReturn(failedTask);

        // 2. Action
        viewModel.addItem("Table", "Small table", "Kitchen", true, 2, null);

        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // 3. Assertions
        String message = viewModel.getToastMessage().getValue();
        assertNotNull(message);
        assertEquals("שגיאה: Firebase Error", message);
    }

    /**
     * בדיקה 3: ניסיון הוספה כשאין משתמש מחובר
     */
    @Test
    public void testAddItem_NoUserLoggedIn() {
        // 1. Setting up conditions
        when(mockRepository.getCurrentUserId()).thenReturn(null);

        // 2. Action
        viewModel.addItem("Chair", "Nice chair", "Bedroom", false, 1, null);

        // 3. Assertions
        verify(mockRepository, never()).addInventoryItem(any(InventoryItem.class), any());
        assertNull(viewModel.getAddSuccess().getValue());
    }
}