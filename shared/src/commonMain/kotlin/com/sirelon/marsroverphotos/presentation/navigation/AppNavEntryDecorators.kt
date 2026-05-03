package com.sirelon.marsroverphotos.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey

@Composable
expect fun rememberAppNavEntryDecorators(): List<NavEntryDecorator<NavKey>>
