package com.example.criminalIntent

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.lang.IllegalStateException
import java.util.*


class MainActivity : AppCompatActivity(), Callbacks, CrimeFragmentCallbacks {

    private val TAG = "MainActivity"
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //register the callback to use that will be invoked after launch() of the Launcher
        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                val currentFragment =
                    supportFragmentManager.findFragmentByTag(CRIME_FRAGMENT_TAG) ?: return@registerForActivityResult
                if (isGranted) {
                    (currentFragment as CrimeFragment).onContactsPermissionGranted()
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("Permission needed")
                        .setMessage("For this feature to work, the app needs a permission to read your contacts.")
                        .setPositiveButton(android.R.string.ok) { _, _ ->

                        }.setIcon(android.R.drawable.ic_dialog_alert).show()
                    (currentFragment as CrimeFragment).onContactsPermissionDenied()
                    Log.w(TAG,"ActivityResult returned permission is not granted")
                }
            }

        val currentFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment == null) {
            val fragment = CrimeListFragment.newInstance()
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, fragment, CRIME_LIST_TAG)
                .commit()
        }
    }

    override fun onCrimeSelected(crimeId: UUID) {
        val fragment = CrimeFragment.newInstance(crimeId)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment, CRIME_FRAGMENT_TAG)
            .addToBackStack(null)
            .commit()
    }

    override fun getContactsPermissionState() {
        var mayRequest = false
        val currentFragment =
            supportFragmentManager.findFragmentByTag(CRIME_FRAGMENT_TAG) ?: return

        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                (currentFragment as CrimeFragment).onContactsPermissionGranted()
                return
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) -> {
                AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("For this feature to work, the app needs a permission to read your contacts. Grant?")
                    .setPositiveButton(R.string.yes) { _, _ ->
                        mayRequest = true
                    }
                    .setNegativeButton(R.string.no) { _, _ ->
                        mayRequest = false
                    }.setIcon(android.R.drawable.ic_dialog_alert).show()
            }
            else -> {
                mayRequest = true
            }
        }
        if (mayRequest) {
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            (currentFragment as CrimeFragment).onContactsPermissionDenied()
        }
    }

    override fun deleteCrimePressed() {
        val currentFragment =
            supportFragmentManager.findFragmentByTag(CRIME_FRAGMENT_TAG) ?: throw IllegalStateException("The " +
                    "crime fragment wants to remove itself but is not on top of the stack")
        supportFragmentManager.beginTransaction().remove(currentFragment).commit()
        supportFragmentManager.popBackStack()
    }
}
