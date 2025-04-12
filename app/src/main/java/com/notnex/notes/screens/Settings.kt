@file:OptIn(ExperimentalMaterial3Api::class)
package com.notnex.notes.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.notnex.notes.R

@Composable
fun Settings(
    onLogInClick: () -> Unit,
    onBack: () -> Boolean
) {
    

    Scaffold(
        topBar = {
            TopAppBar(
            title = { Text(stringResource(id = R.string.settings)) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            item {
                Button( onClick = {onLogInClick()}
                ) {
                    //Text(stringResource(R.string.sign_in))
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.sign_in))
                    }
                }
            }
        }
    }
}
