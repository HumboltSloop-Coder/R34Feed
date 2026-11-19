package com.example.r34viewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.r34viewer.presentation.navigation.AppNavGraph
import com.example.r34viewer.presentation.navigation.AppNavViewModel
import com.example.r34viewer.presentation.theme.R34ViewerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appNavViewModel: AppNavViewModel = hiltViewModel()
            val isDarkTheme by appNavViewModel.isDarkTheme.collectAsStateWithLifecycle()

            R34ViewerTheme(darkTheme = isDarkTheme) {
                AppNavGraph(appNavViewModel = appNavViewModel)
            }
        }
    }
}