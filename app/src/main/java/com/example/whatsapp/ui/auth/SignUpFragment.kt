package com.example.whatsapp.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.whatsapp.R
import com.example.whatsapp.databinding.FragmentSignUpBinding
import com.example.whatsapp.utils.Resource
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpFragment : Fragment(R.layout.fragment_sign_up) {

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var binding: FragmentSignUpBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSignUpBinding.bind(view)

        binding.btnSignUp.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                binding.progressBar.visibility = View.VISIBLE
                viewModel.registerWithEmail(email, password)
            } else {
                Toast.makeText(requireContext(), "Lütfen e-posta ve şifre girin", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.otpState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnSignUp.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Kod gönderildi! E-postanızı kontrol edin.", Toast.LENGTH_LONG).show()
                    val action = SignUpFragmentDirections.actionSignUpFragmentToVerifyOTPFragment(binding.etEmail.text.toString())
                    findNavController().navigate(action)
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSignUp.isEnabled = true
                    Toast.makeText(requireContext(), state.message ?: "Kod gönderme başarısız!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
