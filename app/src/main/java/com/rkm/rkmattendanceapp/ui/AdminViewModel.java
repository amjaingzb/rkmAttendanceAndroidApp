// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/AdminViewModel.java
package com.rkm.rkmattendanceapp.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * A ViewModel shared between AdminMainActivity and its child fragments.
 * It holds the persistent state of the user's logged-in privilege level.
 */
public class AdminViewModel extends ViewModel {

    // Use MutableLiveData to hold the privilege. It's private so only the ViewModel can change it.
    private final MutableLiveData<Privilege> _currentPrivilege = new MutableLiveData<>();

    // Expose an immutable LiveData to the UI for observation.
    public final LiveData<Privilege> currentPrivilege = _currentPrivilege;

    /**
     * Called by AdminMainActivity on its initial creation to set the user's role.
     * This is only called once, as the ViewModel will survive configuration changes.
     * @param privilege The user's logged-in privilege level.
     */
    public void setPrivilege(Privilege privilege) {
        // Only set the value if it hasn't been set before.
        if (_currentPrivilege.getValue() == null) {
            _currentPrivilege.setValue(privilege);
        }
    }
}