package id.ac.umn.diceroller
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import id.ac.umn.diceroller.databinding.ActivityHomepageBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder

class HompageActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var binding: ActivityHomepageBinding
    private lateinit var database: DatabaseReference
    private lateinit var txtQueueNumber: TextView
    private lateinit var liveQueue: TextView
    private lateinit var qrCodeImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepage)
        binding = ActivityHomepageBinding.inflate(layoutInflater)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()
        txtQueueNumber = findViewById(R.id.tvQueueNumber)
        qrCodeImageView = findViewById(R.id.qrCodeImageView)

        liveQueue = findViewById(R.id.liveQueue)

        val buttonGenerateNumber = findViewById<View>(R.id.buttonGenerateNumber)
        buttonGenerateNumber.setOnClickListener {
            generateQueueNumber()
        }
        findViewById<Button>(R.id.buttonLogOut).setOnClickListener {
            logoutUser()
        }

        displayQueueNumber()
        setupCalledNumberListener()
    }

    private fun logoutUser() {
        // Melakukan logout pengguna dari Firebase
        firebaseAuth.signOut()

        // Mengarahkan pengguna ke LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun getCalledNumberRef(): DatabaseReference {
        val firebaseDatabase = FirebaseDatabase.getInstance()
        return firebaseDatabase.reference.child("called_number")
    }

    private fun generateQueueNumber() {
        val currentUser = firebaseAuth.currentUser
        val userId = currentUser?.uid

        if (userId != null) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val queueNumberRef = firebaseDatabase.reference
                .child("queue_numbers")
                .child(date)
                .child(userId)

            // Check if the user has already generated a queue number for the current date
            queueNumberRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // User has already generated a queue number for today
                        // You can handle this case, such as showing an error message
                    } else {
                        // Generate a new queue number for the user
                        val queueNumbersRef = firebaseDatabase.reference
                            .child("queue_numbers")
                            .child(date)

                        // Generate a unique queue number
                        queueNumbersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val existingQueueNumbers = snapshot.children.mapNotNull { it.getValue(Int::class.java) }
                                val maxQueueNumber = existingQueueNumbers.maxOrNull() ?: 0
                                val newQueueNumber = maxQueueNumber + 1

                                // Save the new queue number to Firebase
                                queueNumberRef.setValue(newQueueNumber)
                                    .addOnSuccessListener {
                                        // Generate QR code for the new queue number
                                        generateQRCode(newQueueNumber.toString())

                                        // Success, queue number saved to Firebase
                                        // You can perform additional actions here, such as showing a success message
                                    }
                                    .addOnFailureListener { e ->
                                        // Failed to save queue number to Firebase
                                        // You can handle the failure here, such as showing an error message
                                    }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                // Handle the error if data retrieval is canceled
                            }
                        })
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle the error if data retrieval is canceled
                }
            })
        }
    }

    private fun displayQueueNumber() {
        val currentUser = firebaseAuth.currentUser
        val userId = currentUser?.uid

        if (userId != null) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val queueNumberRef = firebaseDatabase.reference
                .child("queue_numbers")
                .child(date)
                .child(userId)

            queueNumberRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val queueNumber = dataSnapshot.getValue(Int::class.java)

                    if (queueNumber != null) {
                        txtQueueNumber.text = "Nomor Antrian Anda: $queueNumber"

                        // Generate QR code for the queue number
                        generateQRCode(queueNumber.toString())
                    } else {
                        txtQueueNumber.text = "Anda belum memiliki nomor antrian"
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle the error if data retrieval is canceled
                }
            })
        }
    }

    private fun setupCalledNumberListener() {
        val calledNumberRef = getCalledNumberRef()

        calledNumberRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val calledNumber = dataSnapshot.getValue(String::class.java)

                if (calledNumber != null) {
                    liveQueue.text = "Nomor Antrian Dipanggil: $calledNumber"
                } else {
                    liveQueue.text = "Tidak ada nomor antrian yang dipanggil"
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle the error if data retrieval is canceled
            }
        })
    }

    private fun generateQRCode(queueNumber: String) {
        val multiFormatWriter = MultiFormatWriter()
        try {
            val bitMatrix = multiFormatWriter.encode(queueNumber, BarcodeFormat.QR_CODE, 300, 300)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.createBitmap(bitMatrix)
            qrCodeImageView.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }
}
