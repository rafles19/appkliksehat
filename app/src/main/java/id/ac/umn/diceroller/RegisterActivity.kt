package id.ac.umn.diceroller

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import id.ac.umn.diceroller.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    lateinit var binding: ActivityRegisterBinding
    lateinit var auth: FirebaseAuth
    lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        binding.tvToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.btnRegister.setOnClickListener {
            val email = binding.edtEmailRegister.text.toString()
            val password = binding.edtPasswordRegister.text.toString()

            // Validasi email
            if (email.isEmpty()) {
                binding.edtEmailRegister.error = "Email Harus Diisi"
                binding.edtEmailRegister.requestFocus()
                return@setOnClickListener
            }

            // Validasi email tidak sesuai
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.edtEmailRegister.error = "Email Tidak Valid"
                binding.edtEmailRegister.requestFocus()
                return@setOnClickListener
            }

            // Validasi password
            if (password.isEmpty()) {
                binding.edtPasswordRegister.error = "Password Harus Diisi"
                binding.edtPasswordRegister.requestFocus()
                return@setOnClickListener
            }

            // Validasi panjang password
            if (password.length < 6) {
                binding.edtPasswordRegister.error = "Password Minimal 6 Karakter"
                binding.edtPasswordRegister.requestFocus()
                return@setOnClickListener
            }

            registerUser(email, password)
        }
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userId = user?.uid

                    // Simpan data pengguna ke Firebase Realtime Database
                    if (userId != null) {
                        val userRef = database.reference.child("users").child(userId)
                        userRef.child("email").setValue(email)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Register Berhasil", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, LoginActivity::class.java)
                                startActivity(intent)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Gagal menyimpan data: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Gagal register: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
