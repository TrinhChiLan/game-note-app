package com.example.assignment3

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.assignment3.data.SettingsManager
import com.example.assignment3.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsManager = SettingsManager.getInstance(this)

        setupDrawer()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(left = systemBars.left, top = systemBars.top, right = systemBars.right, bottom = 0)
            insets
        }
    }

    private fun setupDrawer() {
        val navView: NavigationView = binding.navView
        val toggleItem = navView.menu.findItem(R.id.nav_toggle_layout)
        val switchView = toggleItem.actionView as SwitchCompat

        // Initialize switch state
        switchView.isChecked = settingsManager.isTallLayout.value

        switchView.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setTallLayout(isChecked)
        }
        
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
//                R.id.nav_home -> {
//                    // Logic to return to home if needed
//                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    fun openDrawer() {
        binding.drawerLayout.openDrawer(GravityCompat.START)
    }
}
