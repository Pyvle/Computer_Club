package com.example.computerclub.app

object Routes {
    const val Splash = "splash"

    const val Clubs = "clubs"
    const val ClubDetails = "club_details/{clubId}"

    const val Booking = "booking"
    const val BookingSeats = "booking_seats"
    const val Shop = "shop"
    const val ShopSearch = "shop_search"
    const val Cart = "cart"
    const val History = "history"
    const val Profile = "profile"

    // auth (только телефон)
    const val LoginPhone = "login_phone?from={from}"
    const val LoginCode = "login_code?from={from}&phone={phone}&challengeId={challengeId}"

    const val Notifications = "notifications"
    const val ProfileDetails = "profile_details"
    const val About = "about"
    const val ReportProblem = "report_problem/{clubId}"
}