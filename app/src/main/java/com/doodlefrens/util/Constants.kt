package com.doodlefren.util

import com.doodlefrens.BuildConfig

object Constants {

    const val USE_LOCALHOST = false

    const val HTTP_BASE_URL = BuildConfig.HTTP_BASE_URL
    const val HTTP_BASE_URL_LOCALHOST = BuildConfig.HTTP_BASE_URL_LOCALHOST

    const val MIN_USERNAME_LENGTH = 4
    const val MAX_USERNAME_LENGTH = 12

    const val MIN_ROOM_NAME_LENGTH = 4
    const val MAX_ROOM_NAME_LENGTH = 16

    const val SEARCH_DELAY = 500L
}