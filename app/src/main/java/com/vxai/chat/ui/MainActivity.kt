package com.vxai.chat.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vxai.chat.util.Prefs
import com.vxai.chat.databinding.ActivityMainBinding
import com.vxai.chat.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: ChatViewModel by viewModels()
    private val adapter = ChatAdapter()
    private var pendingImage: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            pendingImage = uri
            binding.imagePreview.setImageURI(uri)
            binding.imagePreview.visibility = android.view.View.VISIBLE
        }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && pendingImage != null) {
            binding.imagePreview.setImageURI(pendingImage)
            binding.imagePreview.visibility = android.view.View.VISIBLE
        } else {
            pendingImage = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Prefs(this).applyTheme()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recycler.adapter = adapter
        binding.recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this).apply {
            stackFromEnd = true
        }

        binding.btnSend.setOnClickListener {
            val text = binding.input.text.toString()
            vm.send(text, pendingImage, contentResolver)
            binding.input.setText("")
            pendingImage = null
            binding.imagePreview.visibility = android.view.View.GONE
        }

        binding.btnAttach.setOnClickListener {
            pickImage.launch(ActivityResultContracts.PickVisualMedia.ImageOnly)
        }

        binding.btnCamera.setOnClickListener {
            val uri = createImageUri()
            if (uri != null) {
                pendingImage = uri
                takePhoto.launch(uri)
            }
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnClear.setOnClickListener { vm.clear() }

        lifecycleScope.launch {
            vm.messages.collect { adapter.submit(it) }
        }
        lifecycleScope.launch {
            vm.loading.collect { loading ->
                binding.progress.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
                binding.btnSend.isEnabled = !loading
            }
        }
        lifecycleScope.launch {
            vm.error.collect { e ->
                if (e != null) Toast.makeText(this@MainActivity, e, Toast.LENGTH_LONG).show()
            }
        }
        lifecycleScope.launch {
            vm.models.collect { list ->
                if (list.isNotEmpty() && vm.selectedModel.isBlank()) {
                    vm.setModel(list.first().id)
                }
            }
        }
    }

    private fun createImageUri(): Uri? {
        return try {
            val file = java.io.File(cacheDir, "photo_${System.currentTimeMillis()}.jpg")
            androidx.core.content.FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        } catch (e: Exception) {
            null
        }
    }
}
