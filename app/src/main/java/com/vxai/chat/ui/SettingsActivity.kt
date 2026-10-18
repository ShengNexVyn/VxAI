package com.vxai.chat.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vxai.chat.R
import com.vxai.chat.databinding.ActivitySettingsBinding
import com.vxai.chat.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val vm: ChatViewModel by viewModels()

    private val themes = listOf("system", "light", "dark")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.editBaseUrl.setText(vm.prefs.baseUrl)
        binding.editApiKey.setText(vm.prefs.apiKey)
        binding.switchStream.isChecked = vm.prefs.stream

        // theme spinner
        val themeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themes)
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTheme.adapter = themeAdapter
        binding.spinnerTheme.setSelection(themes.indexOf(vm.prefs.theme).coerceAtLeast(0))

        binding.btnSave.setOnClickListener {
            vm.prefs.baseUrl = binding.editBaseUrl.text.toString()
            vm.prefs.apiKey = binding.editApiKey.text.toString()
            vm.prefs.stream = binding.switchStream.isChecked
            vm.prefs.theme = themes[binding.spinnerTheme.selectedItemPosition]
            val sel = binding.spinnerModel.selectedItem?.toString()
            if (!sel.isNullOrBlank()) vm.setModel(sel)
            vm.prefs.applyTheme()
            vm.repo.invalidate()
            vm.refreshModels()
            finish()
        }

        lifecycleScope.launch {
            vm.models.collect { list ->
                val adapter = ArrayAdapter(this@SettingsActivity, android.R.layout.simple_spinner_item, list.map { it.id })
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerModel.adapter = adapter
                val idx = list.indexOfFirst { it.id == vm.prefs.model }
                if (idx >= 0) binding.spinnerModel.setSelection(idx)
            }
        }
    }
}
