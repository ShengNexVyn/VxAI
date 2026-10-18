package com.vxai.chat.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vxai.chat.R
import com.vxai.chat.data.model.ChatMessage
import com.vxai.chat.data.model.displayText
import com.vxai.chat.data.model.hasImage
import com.vxai.chat.databinding.ItemMessageBinding
import io.noties.markwon.Markwon

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.VH>() {

    private val items = mutableListOf<ChatMessage>()
    private var markwon: Markwon? = null

    fun submit(list: List<ChatMessage>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) =
        if (items[position].role == "user") 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        if (markwon == null) markwon = Markwon.create(parent.context)
        return VH(b, markwon!!)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val msg = items[position]
        val text = msg.displayText()
        if (msg.role == "assistant") {
            holder.markwon.setMarkdown(holder.b.textMessage, text)
        } else {
            holder.b.textMessage.text = text
        }
        if (msg.hasImage()) {
            holder.b.imagePreview.visibility = android.view.View.VISIBLE
            holder.b.imagePreview.setImageResource(android.R.drawable.ic_menu_gallery)
        } else {
            holder.b.imagePreview.visibility = android.view.View.GONE
        }
        val isUser = msg.role == "user"
        holder.b.root.gravity = if (isUser) android.view.Gravity.END else android.view.Gravity.START
        holder.b.bubble.setBackgroundResource(
            if (isUser) R.drawable.bubble_user else R.drawable.bubble_ai
        )
    }

    class VH(val b: ItemMessageBinding, val markwon: Markwon) : RecyclerView.ViewHolder(b.root)
}
