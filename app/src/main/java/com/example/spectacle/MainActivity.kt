package com.example.spectacle

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.spectacle.databinding.ActivityMainBinding
import net.openid.appauth.*
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.example.spectacle.storage.SecureStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var authService: AuthorizationService
    private var authState: AuthState? = null

    // ✅ New Activity Result API
    private val authResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (data != null) {
                handleAuthResult(data)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authService = AuthorizationService(this)
        val secureStorage = SecureStorage(this)

        lifecycleScope.launch {
            val accessToken = secureStorage.getAccessToken().first()
            val refreshToken = secureStorage.getRefreshToken().first()

            if (!accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty()) {
                // ✅ Already logged in
                binding.myTextView.text = getString(R.string.already_logged_in, accessToken)
                binding.loginButton.isEnabled = false // Disable login button
            } else {
                // ❌ Not logged in — allow login
                binding.myTextView.text = getString(R.string.not_logged_in)

                binding.loginButton.setOnClickListener {
                    binding.myTextView.text = getString(R.string.loading_config)

                    val discoveryUrl = getString(R.string.keycloak_discovery_url)
                    val clientId = getString(R.string.oidc_client_id)
                    val redirectUri = getString(R.string.oidc_redirect_uri).toUri()
                    val scopes = getString(R.string.oidc_scopes)

                    AuthorizationServiceConfiguration.fetchFromUrl(discoveryUrl.toUri()) { serviceConfig, ex ->
                        if (serviceConfig != null) {
                            val authRequest = AuthorizationRequest.Builder(
                                serviceConfig,
                                clientId,
                                ResponseTypeValues.CODE,
                                redirectUri
                            )
                                .setScope(scopes)
                                .build()

                            authResultLauncher.launch(authService.getAuthorizationRequestIntent(authRequest))
                        } else {
                            binding.myTextView.text = getString(R.string.failed_config, ex?.message ?: "")
                        }
                    }
                }
            }
        }
    }

    private fun handleAuthResult(data: Intent) {
        val resp = AuthorizationResponse.fromIntent(data)
        val ex = AuthorizationException.fromIntent(data)
        if (resp != null) {
            val tokenRequest = resp.createTokenExchangeRequest()
            authService.performTokenRequest(tokenRequest) { response, tokenEx ->
                if (response != null) {
                    authState = AuthState(resp, response, tokenEx)
                    val accessToken = response.accessToken
                    val refreshToken = response.refreshToken

                    lifecycleScope.launch {
                        SecureStorage(this@MainActivity).saveTokens(accessToken, refreshToken)
                    }

                    runOnUiThread {
                        binding.myTextView.text = getString(
                            R.string.login_success,
                            accessToken,
                            refreshToken
                        )
                        binding.loginButton.isEnabled = false
                    }
                } else {
                    runOnUiThread {
                        binding.myTextView.text = getString(
                            R.string.token_exchange_failed,
                            tokenEx?.message ?: ""
                        )
                    }
                }
            }
        } else {
            binding.myTextView.text = getString(R.string.auth_failed, ex?.message ?: "")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        authService.dispose()
    }
}
