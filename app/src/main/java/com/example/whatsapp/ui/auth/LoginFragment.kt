package com.example.whatsapp.ui.auth

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.whatsapp.R
import com.example.whatsapp.databinding.FragmentLoginBinding
import com.example.whatsapp.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var binding: FragmentLoginBinding
    private lateinit var auth: FirebaseAuth
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentLoginBinding.bind(view)

        auth = FirebaseAuth.getInstance()


        checkUserSession()

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.signInWithEmail(email, password)
            } else {
                Toast.makeText(requireContext(), "Lütfen e-posta ve şifre girin", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val user = state.data
                    if (user != null) {
                        saveUserToFirestore(user.uid, user.email!!)
                        checkForGroupInvitations(user.email!!)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message ?: "Giriş başarısız!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkUserSession() {
        val currentUser = auth.currentUser
        if (currentUser != null) {

            firestore.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name")
                        val surname = document.getString("surname")
                        val profileImageUrl = document.getString("profileImageUrl")


                        if (name.isNullOrEmpty() || surname.isNullOrEmpty() || profileImageUrl.isNullOrEmpty()) {
                            findNavController().navigate(R.id.action_loginFragment_to_profileSetupFragment2)
                        } else {

                            findNavController().navigate(R.id.action_loginFragment_to_groupListFragment)
                        }
                    } else {

                        findNavController().navigate(R.id.action_loginFragment_to_profileSetupFragment2)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("LoginFragment", "Kullanıcı bilgileri alınırken hata oluştu", exception)
                    Toast.makeText(
                        requireContext(),
                        "Kullanıcı bilgileri alınamadı!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }



    private fun checkForGroupInvitations(userEmail: String) {
        val userId = auth.currentUser?.uid ?: return


        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name")
                    val surname = document.getString("surname")
                    val profileImageUrl = document.getString("profileImageUrl")


                    if (name.isNullOrEmpty() || surname.isNullOrEmpty() || profileImageUrl.isNullOrEmpty()) {
                        findNavController().navigate(R.id.action_loginFragment_to_profileSetupFragment2)
                        return@addOnSuccessListener
                    }


                    firestore.collection("groupInvitations")
                        .document(userEmail)
                        .get()
                        .addOnSuccessListener { inviteDocument ->
                            if (inviteDocument.exists()) {
                                val groupId = inviteDocument.getString("groupId") ?: return@addOnSuccessListener
                                val groupName = inviteDocument.getString("groupName") ?: "Bilinmeyen Grup"

                                val activity = requireActivity()
                                if (activity is Activity && !activity.isFinishing) {
                                    AlertDialog.Builder(requireContext())
                                        .setTitle("Grup Daveti")
                                        .setMessage("$groupName adlı gruba katılmak istiyor musunuz?")
                                        .setPositiveButton("Kabul Et") { _, _ ->
                                            acceptInvitation(userEmail, groupId)
                                        }
                                        .setNegativeButton("Reddet") { _, _ ->
                                            declineInvitation(userEmail)
                                        }
                                        .show()
                                }
                            } else {

                                findNavController().navigate(R.id.action_loginFragment_to_groupListFragment)
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("LoginFragment", "Grup davetlerini kontrol ederken hata oluştu", exception)
                            Toast.makeText(requireContext(), "Davetler kontrol edilirken hata oluştu", Toast.LENGTH_SHORT).show()
                        }
                } else {

                    findNavController().navigate(R.id.action_loginFragment_to_profileSetupFragment2)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("LoginFragment", "Kullanıcı bilgileri alınırken hata oluştu", exception)
                Toast.makeText(requireContext(), "Kullanıcı bilgileri alınamadı!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun acceptInvitation(userEmail: String, groupId: String) {
        val auth = FirebaseAuth.getInstance()


        val currentUser = auth.currentUser

        if (currentUser != null && currentUser.email == userEmail) {

            val userId = currentUser.uid

            val groupRef = firestore.collection("groups").document(groupId)

            groupRef.update("members", FieldValue.arrayUnion(userId))
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Gruba başarıyla katıldınız!", Toast.LENGTH_SHORT).show()
                    deleteInvitation(userEmail)
                    findNavController().navigate(R.id.action_loginFragment_to_groupListFragment)
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Gruba katılırken hata oluştu!", Toast.LENGTH_SHORT).show()

                }
        } else {
            Toast.makeText(requireContext(), "Kullanıcı oturumu açık değil! Lütfen giriş yapın.", Toast.LENGTH_SHORT).show()
            Log.e("FirebaseAuth", "Kullanıcı oturumu açık değil: $userEmail")
        }
    }


    private fun declineInvitation(userEmail: String) {
        deleteInvitation(userEmail)
        Toast.makeText(requireContext(), "Davet reddedildi", Toast.LENGTH_SHORT).show()
    }

    private fun deleteInvitation(userEmail: String) {
        firestore.collection("groupInvitations").document(userEmail)
            .delete()
            .addOnSuccessListener {
                Log.d("LoginFragment", "Davet silindi")
            }
            .addOnFailureListener { exception ->
                Log.e("LoginFragment", "Davet silinirken hata oluştu", exception)
            }
    }

    private fun saveUserToFirestore(userId: String, email: String) {
        val userRef = firestore.collection("users").document(userId)

        userRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {

                val newUser = mapOf(
                    "uid" to userId,
                    "email" to email,
                    "companyId" to null,
                    "role" to "member"
                )
                userRef.set(newUser)
                    .addOnSuccessListener {
                        Log.d("Firestore", "Yeni kullanıcı kaydedildi: $email, UID: $userId")
                    }
                    .addOnFailureListener { exception ->
                        Log.e("Firestore", "Kullanıcı kaydedilirken hata oluştu", exception)
                    }
            } else {
                Log.d("Firestore", "Kullanıcı zaten kayıtlı: $email, UID: $userId")
            }
        }
    }




}
