package com.jonicodes.notetaker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jonicodes.notetaker.domain.usecase.CheckAiAvailabilityUseCase
import com.jonicodes.notetaker.presentation.navigation.AppNavigation
import com.jonicodes.notetaker.presentation.theme.NotetakerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var checkAiAvailabilityUseCase: CheckAiAvailabilityUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotetakerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    var isChecking by remember { mutableStateOf(true) }
                    var isAiAvailable by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        isAiAvailable = checkAiAvailabilityUseCase()
                        isChecking = false
                    }

                    if (isChecking) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        }
                    } else {
                        AppNavigation(isAiAvailable = isAiAvailable)
                    }
                }
            }
        }
    }
}
