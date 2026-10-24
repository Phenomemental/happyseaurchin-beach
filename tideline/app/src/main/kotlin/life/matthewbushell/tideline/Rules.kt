package life.matthewbushell.tideline

/*
 * The rules the phone reads, from spine:tideline position 3. Until stage 6.5
 * reads them from the block they are copied here, line for line, and this
 * file changes whenever position 3 does.
 */
object Rules {
    /** 3.1 LISTEN TO: no other app's notifications are read at all. */
    val listenTo: Map<String, String> = mapOf(
        "com.whatsapp" to "WhatsApp",
        "com.whatsapp.w4b" to "WhatsApp Business",
        "com.facebook.orca" to "Messenger",
        "com.instagram.android" to "Instagram",
        "org.thoughtcrime.securesms" to "Signal",
    )

    /** 3.3 IGNORE SENDERS: none yet. */
    val ignoreSenders: Set<String> = emptySet()
}
