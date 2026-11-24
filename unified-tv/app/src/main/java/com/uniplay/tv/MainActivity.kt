package com.uniplay.tv

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uniplay.tv.model.Channel
import com.uniplay.tv.source.CatalogSource
import com.uniplay.tv.source.DirectSource
import com.uniplay.tv.source.Source
import android.widget.Toast
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private val sources: List<Source> by lazy {
        listOf(DirectSource(), CatalogSource(this))  // + M3uSource(url) + CoolxSource(...)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var adapter: ChannelAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        val title = TextView(this).apply {
            text = "UniPlay TV — fuentes: ${sources.joinToString { it.displayName }}"
            setPadding(24, 24, 24, 24); textSize = 18f
        }
        adapter = ChannelAdapter { ch ->
            if (ch.url.isEmpty()) {
                Toast.makeText(this, "${ch.name}: stream pendiente (SLB)", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, PlayerActivity::class.java)
                    .putExtra("url", ch.url).putExtra("name", ch.name))
            }
        }
        val recycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        root.addView(title); root.addView(recycler)
        setContentView(ScrollView(this).apply { addView(root) })
        loadChannels(recycler, title)
    }

    private fun loadChannels(recycler: RecyclerView, title: TextView) {
        scope.launch {
            val all = mutableListOf<Channel>()
            for (s in sources) {
                try { all.addAll(s.channels()) }
                catch (e: Exception) {
                    title.text = "Error en ${s.displayName}: ${e.message}"
                }
            }
            all.sortWith(compareBy({ it.url.isEmpty() }, { it.number }))
            adapter.submit(all)
            val playable = all.count { it.url.isNotEmpty() }
            title.text = "UniPlay TV — ${all.size} canales ($playable listos para reproducir)"
            if (all.isEmpty()) title.text = "Sin canales — revisa fuentes"
        }
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}

class ChannelAdapter(private val onClick: (Channel) -> Unit) : RecyclerView.Adapter<VH>() {
    private val items = mutableListOf<Channel>()
    fun submit(list: List<Channel>) { items.clear(); items.addAll(list); notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: android.view.ViewGroup, vt: Int): VH {
        val tv = TextView(parent.context).apply {
            setPadding(32, 24, 32, 24); textSize = 16f
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return VH(tv)
    }
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val c = items[pos]
        (h.view as TextView).text = if (c.group.isEmpty()) c.name else "${c.group}  ·  ${c.name}"
        h.view.setOnClickListener { onClick(c) }
        h.view.onFocusChangeListener = View.OnFocusChangeListener { _, has ->
            h.view.setBackgroundColor(if (has) 0x33FFFFFF else 0x00000000)
        }
    }
}

class VH(val view: View) : RecyclerView.ViewHolder(view)
