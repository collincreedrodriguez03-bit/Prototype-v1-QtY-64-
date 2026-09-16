package com.example.ensemble

/**
 * Valid terminal signal outputs for Kalshi 15-minute BTC contracts.
 * QtY 64 is strictly an analytical signal engine; NO automated execution occurs.
 */
enum class SignalDecision {
  YES,
  NO,
  NO_TRADE
}
