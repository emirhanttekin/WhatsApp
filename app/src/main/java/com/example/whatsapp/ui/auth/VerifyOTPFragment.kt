package com.example.whatsapp.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.whatsapp.R
import com.example.whatsapp.databinding.FragmentVerifyOTPBinding
import com.example.whatsapp.utils.Resource
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VerifyOTPFragment : Fragment(R.layout.fragment_verify_o_t_p) {

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var binding: FragmentVerifyOTPBinding
    private val args: VerifyOTPFragmentArgs by navArgs() // ✅ Args tanımlandı

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentVerifyOTPBinding.bind(view)

        binding.btnVerify.setOnClickListener {
            val otpCode = binding.etOtp.text.toString().trim()

            if (otpCode.length == 6) {
                binding.progressBar.visibility = View.VISIBLE
                viewModel.verifyEmailOtp(args.email, otpCode)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Lütfen geçerli bir 6 haneli kod girin!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        viewModel.verifyState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Success -> {
                    Toast.makeText(requireContext(), "Doğrulama başarılı!", Toast.LENGTH_SHORT)
                        .show()
                    findNavController().navigate(R.id.action_verifyOTPFragment2_to_profileSetupFragment2)
                }

                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        state.message ?: "Doğrulama başarısız!",
                        Toast.LENGTH_LONG
                    ).show()
                }

                else -> {
                    // ✅ Eğer başka bir durum olursa, else ekleyerek hatayı önlüyoruz.
                    binding.progressBar.visibility = View.GONE
                }
            }

        }
    }
}
