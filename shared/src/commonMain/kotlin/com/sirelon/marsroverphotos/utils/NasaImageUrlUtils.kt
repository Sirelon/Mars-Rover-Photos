package com.sirelon.marsroverphotos.utils

private val SIZE_TOKEN_REGEX = Regex("~(thumb|small|medium|large)", RegexOption.IGNORE_CASE)

fun nasaImageOrigUrl(href: String): String = SIZE_TOKEN_REGEX.replace(href, "~orig")

fun nasaImageSmallUrl(href: String): String = SIZE_TOKEN_REGEX.replace(href, "~small")
