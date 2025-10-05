package com.example.spectacle.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.example.spectacle.MainActivity
import com.example.spectacle.R
import com.example.spectacle.databinding.ActivityLoginBinding
import com.example.spectacle.utils.AuthRepository
import kotlinx.coroutines.launch
import net.openid.appauth.*

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authRepository: AuthRepository

    private val authResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.let { handleAuthResult(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository(this)
        binding.statusText.text = getString(R.string.not_logged_in)

        lifecycleScope.launch {
            // If token is valid, skip login
            if (authRepository.isTokenValid()) {
                navigateToHome()
            } else {
                setupLoginButton()
            }
        }
    }

    private fun setupLoginButton() {
        binding.loginButton.setOnClickListener {
            binding.statusText.text = getString(R.string.loading_config)

            val discoveryUri = getString(R.string.keycloak_discovery_url).toUri()
            val clientId = getString(R.string.oidc_client_id)
            val redirectUri = getString(R.string.oidc_redirect_uri).toUri()
            val scopes = getString(R.string.oidc_scopes)

            AuthorizationServiceConfiguration.fetchFromUrl(discoveryUri) { serviceConfig, ex ->
                if (serviceConfig != null) {
                    val authRequest = AuthorizationRequest.Builder(
                        serviceConfig,
                        clientId,
                        ResponseTypeValues.CODE,
                        redirectUri
                    ).setScope(scopes).build()

                    val authServiceIntent = authRepository.provideAuthService()
                        .getAuthorizationRequestIntent(authRequest)

                    authResultLauncher.launch(authServiceIntent)
                } else {
                    binding.statusText.text = getString(R.string.failed_config, ex?.message ?: "")
                }
            }
        }
    }

    private fun handleAuthResult(data: Intent) {
        val resp = AuthorizationResponse.fromIntent(data)
        val ex = AuthorizationException.fromIntent(data)

        if (resp != null) {
            val tokenRequest = resp.createTokenExchangeRequest()
            authRepository.provideAuthService().performTokenRequest(tokenRequest) { response, tokenEx ->
                if (response != null) {
                    val accessToken = response.accessToken
                    val refreshToken = response.refreshToken
                    val expiresIn = response.accessTokenExpirationTime?.let { it - System.currentTimeMillis() } ?: 3600_000L

                    lifecycleScope.launch {
                        // Save tokens securely
                        authRepository.saveTokens(accessToken ?: "", refreshToken, expiresIn / 1000)
                        runOnUiThread {
                            binding.statusText.text = getString(R.string.login_success_message)
                            navigateToHome()
                        }
                    }
                } else {
                    runOnUiThread {
                        binding.statusText.text = getString(R.string.login_failed_message)
                    }
                }
            }
        } else {
            binding.statusText.text = getString(R.string.login_failed_message)
        }
    }

    private fun navigateToHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        authRepository.dispose()
    }
}
