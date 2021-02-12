package com.example.criminalIntent

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.criminalIntent.databinding.FragmentCrimeBinding
import java.util.*

private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "DialogDate"
private const val DIALOG_TIME = "DialogTime"
private const val REQUEST_CODE_DATE = 0
private const val REQUEST_CODE_CONTACT = 1
private const val REQUEST_CODE_TIME = 2
const val CRIME_DATE_FORMAT = "MMM dd, EEEE, yyyy"
const val CRIME_TIME_FORMAT = "HH:MM"
const val CRIME_DATETIME_FORMAT = "$CRIME_DATE_FORMAT $CRIME_TIME_FORMAT"
const val CRIME_FRAGMENT_TAG = "CrimeFragment"

 interface CrimeFragmentCallbacks {
    fun getContactsPermissionState()
 }

class CrimeFragment : Fragment(), DatePickerFragment.Callbacks, TimePickerFragment.Callbacks {



    private var callbacks: Callbacks? = null
    private var _binding: FragmentCrimeBinding? = null
    private val b get() = _binding!!
    private lateinit var crime: Crime

    private val cdvm: CrimeDetailViewModel by lazy {
        ViewModelProvider(this).get(CrimeDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        cdvm.loadCrime(crimeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentCrimeBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cdvm.crimeLiveData.observe(viewLifecycleOwner,
            { crime ->
                crime?.let {
                    this.crime = crime
                    updateUI()
                }
            })
    }

    override fun onStart() {
        super.onStart()

        b.crimeSolved.apply {
            setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
            }
        }

        b.requiresPolice.apply {
            setOnCheckedChangeListener { _,
                                         isChecked ->
                crime.requiresPolice = isChecked
            }
        }

        b.crimeDate.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_CODE_DATE)
                show(this@CrimeFragment.parentFragmentManager, DIALOG_DATE)
            }
        }
        b.crimeTime.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.time = crime.date
            val t = Time(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            TimePickerFragment.newInstance(t).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_CODE_TIME)
                show(this@CrimeFragment.parentFragmentManager, DIALOG_TIME)
            }
        }

        b.crimeSuspect.apply {
            val pickContactIntent =
                Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            setOnClickListener {
                startActivityForResult(pickContactIntent, REQUEST_CODE_CONTACT)
            }
            val packageManager: PackageManager = requireActivity().packageManager
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(pickContactIntent,
                    PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity == null) {
                isEnabled = false //FIXME: For some reason always returns null
                Log.w(CRIME_FRAGMENT_TAG,"Couldn't resolve activity for contacts app")
            }
        }

        b.callSuspect.setOnClickListener {
            //on completion, calls the necessary methods asynchronously
            (context as CrimeFragmentCallbacks).getContactsPermissionState()
        }


        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //unneeded
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                crime.title = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {
                //unneeded
            }
        }
        b.crimeTitle.addTextChangedListener(titleWatcher)

        b.crimeReport.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    getString(R.string.crime_report_subject)
                )
            }.also { intent ->
                val chooserIntent = Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    private fun updateUI() {
        b.crimeTitle.setText(crime.title)
        b.crimeDate.text = DateFormat.format(CRIME_DATE_FORMAT, crime.date)
        b.crimeTime.text = DateFormat.format(CRIME_TIME_FORMAT,crime.date)
        b.crimeSolved.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }
        b.requiresPolice.apply {
            isChecked = crime.requiresPolice
            jumpDrawablesToCurrentState()
        }
        if (crime.suspect.isNotEmpty()) {
            b.crimeSuspect.text = crime.suspect
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return
            requestCode == REQUEST_CODE_CONTACT && data != null -> {
                val contactUri: Uri = data.data!!
// Specify which fields you want your query to return values for
                val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
// Perform your query - the contactUri is like a "where" clause here
                val cursor = requireActivity().contentResolver
                    .query(contactUri, queryFields, null, null, null)
                cursor?.use {
// Verify cursor contains at least one result
                    if (it.count == 0) {
                        return
                    }
                    // Pull out the first column of the first row of data -
// that is your suspect's name
                    it.moveToFirst()
                    val suspect = it.getString(0)
                    crime.suspect = suspect
                    cdvm.saveCrime(crime)
                    b.crimeSuspect.text = suspect

                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        cdvm.saveCrime(crime)
    }

    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }
        val dateString = DateFormat.format(CRIME_DATE_FORMAT, crime.date).toString()
        val suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }
        return getString(
            R.string.crime_report,
            crime.title, dateString, solvedString, suspect
        )
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param id - Crime UUID
         * @return A new instance of fragment CrimeFragment.
         */
        @JvmStatic
        fun newInstance(id: UUID) =
            CrimeFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CRIME_ID, id)
                }
            }
    }

    override fun onDateSelected(date: Date) {
        crime.date = date
        updateUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onTimeSelected(time: Time) {
        val cal = Calendar.getInstance()
        cal.time = crime.date
        cal.set(Calendar.HOUR_OF_DAY, time.hour)
        cal.set(Calendar.MINUTE, time.minute)
        crime.date = cal.time
        updateUI()
    }

    fun onContactsPermissionDenied() {
        b.callSuspect.isEnabled = false
        b.callSuspect.setTextColor(Color.RED) //TODO: Test
    }

    fun onContactsPermissionGranted() {
        val contactUri = Uri.parse(crime.suspect)
        val queryFields: Array<String> = arrayOf(ContactsContract.Contacts._ID)
        //first, find the contact ID by name
        val cursor = requireActivity().contentResolver
            .query(contactUri, queryFields, null, null, null)
        cursor?.use {
            if (it.count == 0) {
                Log.w(CRIME_FRAGMENT_TAG, "found 0 results for crime.suspect name(uri): $contactUri")
                return
            }
            it.moveToFirst() //the first contact with this name
            val idUri = Uri.parse(it.getString(0))

            //now query for the phone number
            val queryFields2: Array<String> = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val cursor2 = requireActivity().contentResolver
                .query(idUri, queryFields2, null, null, null)
            cursor2?.use { phoneCursor ->
                if (phoneCursor.count == 0) {
                    Log.w(CRIME_FRAGMENT_TAG, "found 0 results for crime.suspect phone: $idUri")
                    return
                }
                phoneCursor.moveToFirst() //the only phone number that should have been left
                //finally, call the suspect
                val callIntent = Intent(Intent.ACTION_DIAL)
                callIntent.data = Uri.parse(phoneCursor.getString(0))
                startActivity(callIntent)
            }
        }
    }


}
