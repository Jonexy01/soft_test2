package com.example.softtest2.models

import com.netpluspay.contactless.sdk.card.CardReadResult

data class CardResult(
    val cardReadResult: CardReadResult,
    val cardScheme: String,
)