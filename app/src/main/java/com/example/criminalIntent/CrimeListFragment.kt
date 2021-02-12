package com.example.criminalIntent

import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.util.*

const val CRIME_LIST_TAG = "CrimeListFragment"

//extend Boolean
val Boolean.int
    get() = if (this) 1 else 0

interface Callbacks {
    fun onCrimeSelected(crimeId: UUID)
}


class CrimeListFragment : Fragment() {

    private var callbacks: Callbacks? = null
    //private var crimesLiveData: LiveData<List<Crime>>

    private lateinit var crimeRecyclerView: RecyclerView
    private lateinit var emptyListButton: Button

    private val clvm: CrimeListViewModel by lazy {
        ViewModelProvider(this).get(CrimeListViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?):
            View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_crime_list, container, false)
        crimeRecyclerView = view.findViewById(R.id.crime_recycler_view)
        emptyListButton = view.findViewById(R.id.button_add_first_crime)
        crimeRecyclerView.layoutManager = LinearLayoutManager(context)
        crimeRecyclerView.adapter = CrimeListAdapter()
        emptyListButton.setOnClickListener {
            val crime = Crime()
            clvm.addCrime(crime)
            callbacks?.onCrimeSelected(crime.id)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        clvm.crimesLiveData.observe(
            viewLifecycleOwner,
            { crimes ->
                crimes?.let {
                    Log.i(CRIME_LIST_TAG, "Got crimes ${crimes.size}")
                    updateUI(crimes)
                }
            })

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_crime_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_crime -> {
                val crime = Crime()
                clvm.addCrime(crime)
                callbacks?.onCrimeSelected(crime.id)
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun updateUI(crimes: List<Crime>) {
        (crimeRecyclerView.adapter as CrimeListAdapter).submitList(crimes)

        val empty = clvm.crimesLiveData.value?.isEmpty() ?: true
        if (empty) {
            crimeRecyclerView.visibility = View.GONE
            emptyListButton.visibility = View.VISIBLE
        } else {
            crimeRecyclerView.visibility = View.VISIBLE
            emptyListButton.visibility = View.GONE
        }

        Log.d(CRIME_LIST_TAG, "submitted list: $crimes")
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }


    private inner class CrimeHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        private lateinit var crime: Crime
        private val titleTextView: TextView = itemView.findViewById(R.id.crime_title)
        private val dateTextView: TextView = itemView.findViewById(R.id.crime_date)
        private val contactPoliceButton: Button? = itemView.findViewById(R.id.contact_police_button)
        private val solvedIndicator: ImageView = itemView.findViewById(R.id.crime_solved)

        init {
            itemView.setOnClickListener(this)
            contactPoliceButton?.setOnClickListener {
                Toast.makeText(context, "WEE WEE POLICE IS ON DA WAY: ${crime.title}", Toast.LENGTH_SHORT).show()
            }
        }


        fun bind(crime: Crime) {
            this.crime = crime
            titleTextView.text = this.crime.title
            dateTextView.text = DateFormat.format(CRIME_DATETIME_FORMAT, this.crime.date)
            solvedIndicator.visibility = if (crime.isSolved) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }
        }

        override fun onClick(v: View?) {
            callbacks?.onCrimeSelected(crime.id)
        }

    }

    private inner class CrimeListAdapter : ListAdapter<Crime, CrimeHolder>(CrimeDiffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
            val viewId = if (viewType == 0) //no police
                R.layout.list_item_crime
            else
                R.layout.list_item_crime_police

            val view = layoutInflater.inflate(viewId, parent, false)
            return CrimeHolder(view)
        }

        override fun getItemViewType(position: Int): Int {
            return getItem(position).requiresPolice.int //two types
        }

        override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
            holder.bind(getItem(position))
        }

    }

    companion object {
        fun newInstance(): CrimeListFragment {
            return CrimeListFragment()
        }
    }
}


object CrimeDiffCallback : DiffUtil.ItemCallback<Crime>() {
    override fun areItemsTheSame(oldItem: Crime, newItem: Crime): Boolean {
        Log.d(CRIME_LIST_TAG, "called are ItemsTheSame, res: ${oldItem.id == newItem.id}")
        return oldItem.id == newItem.id

    }

    override fun areContentsTheSame(oldItem: Crime, newItem: Crime): Boolean {
        return oldItem == newItem
    }
}
