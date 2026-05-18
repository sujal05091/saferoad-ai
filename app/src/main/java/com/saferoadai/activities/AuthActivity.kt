package com.saferoadai.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.saferoadai.R

/**
 * Authentication activity with Login, Sign Up, Google Sign-In and Forgot Password
 */
class AuthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        
        auth = FirebaseAuth.getInstance()
        
        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        
        // Check if already signed in
        if (auth.currentUser != null) {
            navigateToMain()
            return
        }
        
        setupListeners()
    }

    private fun setupListeners() {
        val emailInput = findViewById<TextInputEditText>(R.id.emailInput)
        val passwordInput = findViewById<TextInputEditText>(R.id.passwordInput)
        val loginButton = findViewById<MaterialButton>(R.id.loginButton)
        val signupButton = findViewById<TextView>(R.id.signupButton)
        val googleSignInButton = findViewById<MaterialButton>(R.id.googleSignInButton)
        val forgotPasswordText = findViewById<TextView>(R.id.forgotPasswordText)
        
        loginButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            signIn(email, password)
        }
        
        signupButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password to sign up", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            signUp(email, password)
        }
        
        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }
        
        forgotPasswordText.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun signIn(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }
        
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    Toast.makeText(this, "Sign in failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun signUp(email: String, password: String) {
        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    Toast.makeText(this, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
    
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }
    
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Welcome, ${account.displayName}!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
    
    private fun showForgotPasswordDialog() {
        val input = TextInputEditText(this)
        input.hint = "Enter your email"
        input.setPadding(50, 40, 50, 40)
        
        AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("Enter your email address to receive a password reset link")
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val email = input.text.toString()
                if (email.isEmpty()) {
                    Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                } else {
                    resetPassword(email)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Password reset email sent to $email", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Failed to send reset email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
