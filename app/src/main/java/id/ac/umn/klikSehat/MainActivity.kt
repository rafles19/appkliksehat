package id.ac.umn.klikSehat
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    private lateinit var buttonLogin: Button
    private lateinit var buttonRegister: Button
    private lateinit var buttonAdmin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonLogin = findViewById(R.id.buttonLogin)
        buttonRegister = findViewById(R.id.buttonRegister)
        buttonAdmin = findViewById(R.id.AdminLogin)

        buttonLogin.setOnClickListener {
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
        }

        buttonRegister.setOnClickListener {
            val intent = Intent(this@MainActivity, RegisterActivity::class.java)
            startActivity(intent)
        }

        buttonAdmin.setOnClickListener {
            val intent = Intent(this@MainActivity, AdminActivity::class.java)
            startActivity(intent)
        }
    }
}
