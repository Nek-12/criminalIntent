package com.example.criminalIntent

import androidx.lifecycle.ViewModel

class CrimeListViewModel : ViewModel() {
    private val crimeRepository: CrimeRepository = CrimeRepository.get()
    val crimesLiveData = crimeRepository.getCrimes()

    init {

    }

}
