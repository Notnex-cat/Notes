package com.notnex.notes.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.notnex.notes.R

@Composable
fun AuthScreen(
    onMainScreen: () -> Unit
) {

    val emailState = remember { mutableStateOf("damir180999@gmail.com") }
    val passwordState = remember { mutableStateOf("123456789") }
    val context = LocalContext.current
    val auth = remember { Firebase.auth }


    Scaffold {
        innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TextField(modifier = Modifier
                .border(width = 2.dp, color = Color.DarkGray, shape = RoundedCornerShape(20.dp)),
                label = {
                    Text(text = stringResource(R.string.login))
                },

                value = emailState.value, onValueChange = {
                emailState.value = it
            })

            Spacer(modifier = Modifier.height(10.dp))

            TextField(modifier = Modifier
                .border(width = 2.dp, color = Color.DarkGray, shape = RoundedCornerShape(20.dp)),
                label = {
                    Text(text = stringResource(R.string.password))
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                value = passwordState.value, onValueChange = {
                passwordState.value = it
            })

            Spacer(modifier = Modifier.height(10.dp))

            Button(onClick = {
                signIn(
                    context,
                    auth,
                    emailState.value,
                    passwordState.value,
                    onSignInSuccess = {
                        Toast.makeText(context, "success", Toast.LENGTH_SHORT ).show()
                        onMainScreen()
                    },
                    onSignInFailure = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_SHORT ).show()
                    })
            }
            ) {
                Text(text = stringResource(R.string.sign_in))
            }

            Button(onClick = {
                signUp(
                    context,
                    auth,
                    emailState.value,
                    passwordState.value,
                    onSignUpSuccess = {
                        Toast.makeText(context, "success", Toast.LENGTH_SHORT ).show()
                    },
                    onSignUpFailure = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_SHORT ).show()
                    }
                )
            }
            ) {
                Text(text = stringResource(R.string.sign_up))
            }

            Button(onClick = {
                signOut(auth)
            }
            ) {
                Text(text = stringResource(R.string.sign_out))
            }

            Button(onClick = {
                deleteAccount(context, auth, emailState.value, passwordState.value)
            }
            ) {
                Text(text = stringResource(R.string.delete_account))
            }
        }
    }
}


private fun signUp(
    context: Context,
    auth: FirebaseAuth,
    email: String,
    password: String,
    onSignUpSuccess: () -> Unit,
    onSignUpFailure: (String) -> Unit
){
    if(email.isBlank() || password.isBlank()){
        onSignUpFailure(context.getString(R.string.email_or_password_is_empty))
        return
    }
    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if(task.isSuccessful)
                onSignUpSuccess()
        }.addOnFailureListener{
            onSignUpFailure((it.localizedMessage ?: context.getString(R.string.sign_up_error)))}
}

private fun signIn(context: Context,
                   auth: FirebaseAuth,
                   email: String,
                   password: String,
                   onSignInSuccess: () -> Unit,
                   onSignInFailure: (String) -> Unit
){
    if(email.isBlank() || password.isBlank()){
        onSignInFailure(context.getString(R.string.email_or_password_is_empty))
        return
    }
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if(task.isSuccessful)
                onSignInSuccess()
        }.addOnFailureListener{
            onSignInFailure((it.localizedMessage ?: "Fail"))}
}

private fun signOut(auth: FirebaseAuth){
    auth.signOut()
}

private fun deleteAccount(context: Context, auth: FirebaseAuth, email: String, password: String){
    val credential = EmailAuthProvider.getCredential(email, password)
    auth.currentUser?.reauthenticate(credential)?.addOnCompleteListener {
        if (it.isSuccessful){
            auth.currentUser?.delete()!!.addOnCompleteListener {
                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
            }
        } else{
            Toast.makeText(context, "Not Deleted", Toast.LENGTH_SHORT).show()
        }
    }
}