package com.example.criminalIntent

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import java.io.Serializable
import java.util.*

private const val ARG_TIME = "time"




data class Time(
    var hour: Int,
    var minute: Int
) : Serializable


class TimePickerFragment : DialogFragment() {
    interface Callbacks {
        fun onTimeSelected(time: Time)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        val timeListener = TimePickerDialog.OnTimeSetListener { _: TimePicker, hour: Int, minute: Int ->

            targetFragment?.let {
                (it as Callbacks).onTimeSelected(Time(hour, minute))
            }
        }
        val cal = Calendar.getInstance()
        return TimePickerDialog(
            requireContext(), timeListener,
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE), true
        )

    }

    companion object {
        fun newInstance(time: Time): TimePickerFragment {
            val args = Bundle().apply { putSerializable(ARG_TIME, time) }
            return TimePickerFragment().apply { arguments = args }
        }
    }
}


