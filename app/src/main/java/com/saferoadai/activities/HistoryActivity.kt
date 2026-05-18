package com.saferoadai.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.saferoadai.R
import com.saferoadai.db.AppDatabase
import com.saferoadai.db.HazardEvent
import com.saferoadai.db.SignEvent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * History activity showing past rides, hazards, and signs detected
 */
class HistoryActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        
        database = AppDatabase.getDatabase(this)
        recyclerView = findViewById(R.id.historyRecyclerView)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter()
        recyclerView.adapter = adapter
        
        // Load history from database
        loadHistory()
    }
    
    private fun loadHistory() {
        lifecycleScope.launch {
            // Load all hazards and signs
            database.eventDao().getAllHazards().collectLatest { hazards ->
                val items = mutableListOf<HistoryItem>()
                
                // Add hazards
                hazards.forEach { hazard ->
                    items.add(
                        HistoryItem(
                            type = "HAZARD",
                            title = "⚠️ ${hazard.type}",
                            subtitle = "Severity: ${hazard.severity} | ${(hazard.confidence * 100).toInt()}% confidence",
                            timestamp = hazard.timestampFirstSeen,
                            location = "${hazard.latitude}, ${hazard.longitude}"
                        )
                    )
                }
                
                // Load signs
                database.eventDao().getAllSigns().collect { signs ->
                    signs.forEach { sign ->
                        items.add(
                            HistoryItem(
                                type = "SIGN",
                                title = "🚦 ${sign.type}",
                                subtitle = "Importance: ${sign.importance}/10 | ${(sign.confidence * 100).toInt()}% confidence",
                                timestamp = sign.timestamp,
                                location = "${sign.latitude}, ${sign.longitude}"
                            )
                        )
                    }
                    
                    // Sort by timestamp (most recent first)
                    items.sortByDescending { it.timestamp }
                    
                    adapter.updateItems(items)
                }
            }
        }
    }
    
    data class HistoryItem(
        val type: String,
        val title: String,
        val subtitle: String,
        val timestamp: Long,
        val location: String
    )
    
    inner class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
        
        private var items = listOf<HistoryItem>()
        
        fun updateItems(newItems: List<HistoryItem>) {
            items = newItems
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }
        
        override fun getItemCount() = items.size
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val titleText: TextView = itemView.findViewById(R.id.historyItemTitle)
            private val subtitleText: TextView = itemView.findViewById(R.id.historyItemSubtitle)
            private val timestampText: TextView = itemView.findViewById(R.id.historyItemTimestamp)
            private val locationText: TextView = itemView.findViewById(R.id.historyItemLocation)
            
            fun bind(item: HistoryItem) {
                titleText.text = item.title
                subtitleText.text = item.subtitle
                timestampText.text = formatTimestamp(item.timestamp)
                locationText.text = "📍 ${item.location}"
            }
            
            private fun formatTimestamp(timestamp: Long): String {
                val date = Date(timestamp)
                val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                return format.format(date)
            }
        }
    }
}
