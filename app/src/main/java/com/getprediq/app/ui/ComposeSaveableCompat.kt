package com.getprediq.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable as runtimeRememberSaveable

@Composable
fun <T : Any> rememberSaveable(init: () -> T): T = runtimeRememberSaveable(init = init)
