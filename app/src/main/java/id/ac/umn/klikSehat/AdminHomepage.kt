package id.ac.umn.klikSehat
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminHomepage : AppCompatActivity() {

    private lateinit var btnDeleteQueue: Button
    private lateinit var etCalledNumber: EditText
    private lateinit var btnSetCalledNumber: Button
    private lateinit var btnScanQRCode: Button
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseDatabase: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_dasboard)

        btnDeleteQueue = findViewById(R.id.deleteAll)
        etCalledNumber = findViewById(R.id.etCalledNumber)
        btnSetCalledNumber = findViewById(R.id.btnSetCalledNumber)
        btnScanQRCode = findViewById(R.id.btnScanQRCode)
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()

        btnDeleteQueue.setOnClickListener {
            deleteQueueNumbers()
        }

        btnSetCalledNumber.setOnClickListener {
            val calledNumber = etCalledNumber.text.toString()
            setCalledNumber(calledNumber)
        }

        btnScanQRCode.setOnClickListener {
            // Memeriksa izin kamera
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startQRCodeScan()
            } else {
                // Jika izin kamera tidak diberikan, minta izin kamera kepada pengguna
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
            }
        }
    }

    private fun setCalledNumber(calledNumber: String) {
        val calledNumberRef = firebaseDatabase.reference.child("called_number")
        calledNumberRef.setValue(calledNumber)
            .addOnSuccessListener {
                Toast.makeText(this, "Nomor Antrian Dipanggil: $calledNumber", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memanggil nomor antrian: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteQueueNumbers() {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val queueNumbersRef = firebaseDatabase.reference.child("queue_numbers").child(date)

        queueNumbersRef.removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Nomor Antrian berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menghapus Nomor Antrian: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startQRCodeScan() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Arahkan kamera ke QR Code")
        integrator.setCameraId(0) // Gunakan kamera belakang
        integrator.setBeepEnabled(false)
        integrator.setOrientationLocked(false)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result: IntentResult? = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            // Mendapatkan data dari QR code
            val queueNumber = result.contents

            // Melakukan validasi nomor antrian di database
            validateQueueNumber(queueNumber)
        }
    }

    private fun validateQueueNumber(queueNumber: String) {
        val queueNumbersRef = firebaseDatabase.reference.child("queue_numbers")

        queueNumbersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var queueNumberValidated = false

                // Memeriksa apakah nomor antrian valid
                for (dateSnapshot in dataSnapshot.children) {
                    for (userSnapshot in dateSnapshot.children) {
                        val userQueueNumber = userSnapshot.getValue(Int::class.java)
                        if (userQueueNumber != null && userQueueNumber.toString() == queueNumber) {
                            // Mengubah status kehadiran menjadi true di database
                            userSnapshot.ref.child("status").setValue(true)
                            queueNumberValidated = true
                            break
                        }
                    }

                    if (queueNumberValidated) {
                        break
                    }
                }

                if (queueNumberValidated) {
                    Toast.makeText(this@AdminHomepage, "Validasi nomor antrian sukses", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AdminHomepage, "Nomor antrian tidak valid", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle the error if data retrieval is canceled
            }
        })
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 123
    }
}
