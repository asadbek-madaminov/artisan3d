package com.dy.artisan3d

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform