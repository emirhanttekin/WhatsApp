package com.example.whatsapp.ui.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.whatsapp.R
import com.example.whatsapp.databinding.FragmentVerifyOTPBinding
import com.example.whatsapp.utils.Resource
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VerifyOTPFragment : Fragment(R.layout.fragment_verify_o_t_p) {

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var binding: FragmentVerifyOTPBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentVerifyOTPBinding.bind(view)

        binding.btnVerify.setOnClickListener {
            viewModel.checkEmailVerification()
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnVerify.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnVerify.isEnabled = true
                    Toast.makeText(requireContext(), "E-posta doğrulandı, giriş yapabilirsiniz!", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_verifyOTPFragment2_to_homeFragment)
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnVerify.isEnabled = true
                    Toast.makeText(requireContext(), state.message ?: "Doğrulama başarısız", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
