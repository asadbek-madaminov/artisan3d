package com.dy.artisan3d

import androidx.compose.ui.window.ComposeUIViewController
import com.dy.artisan3d.di.initializeKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        initializeKoin()
    }
) {
    App()
}