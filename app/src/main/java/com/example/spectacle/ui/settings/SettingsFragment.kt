package com.example.spectacle.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.spectacle.R
import com.example.spectacle.databinding.FragmentSettingsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.InetAddress
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val PREFS_NAME = "local_display_prefs"
    private val KEY_LOCAL_IP = "key_local_ip"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        // Observe ViewModel text
        settingsViewModel.text.observe(viewLifecycleOwner) { value ->
            binding.textSettings.text = value
        }

        createNotificationChannel()
        loadLocalIP()
        setupModeToggle()
        setupButtons()

        return binding.root
    }

    private fun setupModeToggle() {
        binding.toggleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            when (checkedId) {
                binding.btnModeLocal.id -> {
                    binding.blockLocal.visibility = View.VISIBLE
                    binding.blockPublic.visibility = View.GONE
                }
                binding.btnModePublic.id -> {
                    binding.blockLocal.visibility = View.GONE
                    binding.blockPublic.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupButtons() {
        // Test Connection (local only)
        binding.btnTestConnection.setOnClickListener {
            val ip = binding.inputLocalIpv4.text.toString().trim()
            if (ip.isNotBlank()) {
                saveLocalIP(ip)
                testPing(ip)
            } else {
                Toast.makeText(requireContext(), "Please enter an IP address", Toast.LENGTH_SHORT).show()
            }
        }

        // Connect (public only)
        binding.btnConnect.setOnClickListener {
            val domain = binding.inputRemoteDomain.text.toString().trim()
            val username = binding.inputDefaultUsername.text.toString().trim()

            if (domain.isBlank()) {
                Toast.makeText(requireContext(), "Please enter a domain", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(requireContext(), "Connecting to $domain as $username...", Toast.LENGTH_SHORT).show()
        }
    }

    // Save IP to SharedPreferences
    private fun saveLocalIP(ip: String) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LOCAL_IP, ip).apply()
    }

    // Load IP from SharedPreferences
    private fun loadLocalIP() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedIP = prefs.getString(KEY_LOCAL_IP, "")
        binding.inputLocalIpv4.setText(savedIP)
    }

    private fun testPing(ip: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val address = InetAddress.getByName(ip)
                val reachable = address.isReachable(3000) // 3 seconds timeout

                CoroutineScope(Dispatchers.Main).launch {
                    if (reachable) {
                        Toast.makeText(requireContext(), "$ip is reachable!", Toast.LENGTH_SHORT).show()
                        sendConnectedNotification(ip)
                    } else {
                        Toast.makeText(requireContext(), "$ip is not reachable", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Local Display Notifications"
            val descriptionText = "Notifications for local display connection"
            val importance = android.app.NotificationManager.IMPORTANCE_DEFAULT
            val channel = android.app.NotificationChannel("local_display_channel", name, importance)
            channel.description = descriptionText
            val notificationManager: android.app.NotificationManager =
                requireContext().getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendConnectedNotification(ip: String) {
        val builder = NotificationCompat.Builder(requireContext(), "local_display_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // replace with your icon
            .setContentTitle("Local Display Connected")
            .setContentText("Local IP $ip is now reachable")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = NotificationManagerCompat.from(requireContext())
        notificationManager.notify(1001, builder.build())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
