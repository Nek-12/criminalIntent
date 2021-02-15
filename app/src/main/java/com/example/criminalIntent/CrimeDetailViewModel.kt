package com.example.criminalIntent

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import java.io.File
import java.util.*


private const val TAG = "CrimeDetailViewModel"
class CrimeDetailViewModel : ViewModel() {

    private val crimeRepository = CrimeRepository.get()
    private val crimeIdLiveData = MutableLiveData<UUID>()
    var crimeLiveData: LiveData<Crime?> =
        Transformations.switchMap(crimeIdLiveData) { crimeId ->
            crimeRepository.getCrime(crimeId)
        }

    fun loadCrime(crimeId: UUID) {
        crimeIdLiveData.value = crimeId
        Log.d(TAG,"loaded crime: $crimeId")
    }
    fun saveCrime(crime: Crime) {
        crimeRepository.updateCrime(crime)
        Log.d(TAG,"updated crime: $crime")
    }
    fun getPhotoFile(crime: Crime): File {
        return crimeRepository.getPhotoFile(crime)
    }
    fun deleteCrime(crime: Crime) {
        crimeRepository.deleteCrime(crime)
    }


}
