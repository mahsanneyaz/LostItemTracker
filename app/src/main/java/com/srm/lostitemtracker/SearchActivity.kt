package com.srm.lostitemtracker

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

data class SearchResultItem(
    val id       : String,
    val itemName : String,
    val category : String,
    val location : String,
    val photoUrl : String,
    val type     : String
)

class SearchActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private val masterList   = mutableListOf<SearchResultItem>()
    private val filteredList = mutableListOf<SearchResultItem>()

    private lateinit var adapter     : SearchAdapter
    private lateinit var etSearch    : EditText        // was TextInputEditText — now EditText
    private lateinit var spinnerType : Spinner
    private lateinit var spinnerCat  : Spinner
    private lateinit var progress    : ProgressBar
    private lateinit var tvEmpty     : TextView
    private lateinit var tvCount     : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val tvBack    = findViewById<ImageView>(R.id.tvSearchBack)
        val rvResults = findViewById<RecyclerView>(R.id.rvSearchResults)
        etSearch      = findViewById(R.id.etSearchKeyword)
        spinnerType   = findViewById(R.id.spinnerType)
        spinnerCat    = findViewById(R.id.spinnerCategory)
        progress      = findViewById(R.id.searchProgress)
        tvEmpty       = findViewById(R.id.tvSearchEmpty)
        tvCount       = findViewById(R.id.tvResultCount)

        tvBack.setOnClickListener { finish() }

        adapter = SearchAdapter(filteredList)
        rvResults.layoutManager = LinearLayoutManager(this)
        rvResults.adapter = adapter

        spinnerType.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("All Types", "Lost", "Found"))

        spinnerCat.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("All Categories", "Electronics", "Documents", "Accessories",
                "Clothing", "Keys", "Wallet / Purse", "Books / Notes", "Other"))

        spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { applyFilters() }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        spinnerCat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { applyFilters() }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) { applyFilters() }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadAllItems()
    }

    private fun loadAllItems() {
        progress.visibility = View.VISIBLE
        tvEmpty.visibility  = View.GONE
        masterList.clear()
        var loadedCount = 0

        db.collection("lost_items").get()
            .addOnSuccessListener { snap ->
                for (doc in snap.documents) {
                    masterList.add(SearchResultItem(
                        id = doc.id,
                        itemName = doc.getString("itemName") ?: "",
                        category = doc.getString("category") ?: "",
                        location = doc.getString("location") ?: "",
                        photoUrl = doc.getString("photoUrl") ?: "",
                        type = "LOST"))
                }
                loadedCount++
                if (loadedCount == 2) onAllLoaded()
            }
            .addOnFailureListener { loadedCount++; if (loadedCount == 2) onAllLoaded() }

        db.collection("found_items").get()
            .addOnSuccessListener { snap ->
                for (doc in snap.documents) {
                    masterList.add(SearchResultItem(
                        id = doc.id,
                        itemName = doc.getString("itemName") ?: "",
                        category = doc.getString("category") ?: "",
                        location = doc.getString("location") ?: "",
                        photoUrl = doc.getString("photoUrl") ?: "",
                        type = "FOUND"))
                }
                loadedCount++
                if (loadedCount == 2) onAllLoaded()
            }
            .addOnFailureListener { loadedCount++; if (loadedCount == 2) onAllLoaded() }
    }

    private fun onAllLoaded() {
        progress.visibility = View.GONE
        applyFilters()
    }

    private fun applyFilters() {
        val keyword      = etSearch.text.toString().trim().lowercase()
        val selectedType = spinnerType.selectedItem?.toString() ?: "All Types"
        val selectedCat  = spinnerCat.selectedItem?.toString() ?: "All Categories"

        filteredList.clear()
        for (item in masterList) {
            val keywordMatch = keyword.isEmpty()
                    || item.itemName.lowercase().contains(keyword)
                    || item.location.lowercase().contains(keyword)
            val typeMatch = selectedType == "All Types" || item.type == selectedType.uppercase()
            val catMatch  = selectedCat == "All Categories" || item.category == selectedCat
            if (keywordMatch && typeMatch && catMatch) filteredList.add(item)
        }

        adapter.notifyDataSetChanged()
        tvCount.text = "${filteredList.size} result(s) found"
        tvEmpty.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
    }
}

class SearchAdapter(private val items: List<SearchResultItem>) :
    RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto   : ImageView = view.findViewById(R.id.ivItemPhoto)
        val tvName    : TextView  = view.findViewById(R.id.tvItemName)
        val tvCategory: TextView  = view.findViewById(R.id.tvCategory)
        val tvLocation: TextView  = view.findViewById(R.id.tvLocation)
        val tvType    : TextView  = view.findViewById(R.id.tvType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text     = item.itemName
        holder.tvCategory.text = item.category
        holder.tvLocation.text = item.location
        holder.tvType.text     = item.type

        val color = if (item.type == "LOST") 0xFFF44336.toInt() else 0xFF4CAF50.toInt()
        holder.tvType.setBackgroundColor(color)

        if (item.photoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.photoUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.ivPhoto)
        } else {
            holder.ivPhoto.setImageDrawable(null)
            holder.ivPhoto.setBackgroundColor(0xFFE0E0E0.toInt())
        }
    }

    override fun getItemCount() = items.size
}