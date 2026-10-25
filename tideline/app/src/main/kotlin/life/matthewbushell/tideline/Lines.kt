package life.matthewbushell.tideline

import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/*
 * The words inside each seal (spine:tideline 4.2, 4.4): JSON naming the line's
 * kind, then the message as caught: the app, the sender and the conversation
 * as shown, the text, the time on the phone, and the conversation key the
 * phone uses to find the reply button. Matthew's session opens a line and
 * reads this JSON.
 */
object Lines {
    fun message(catch: Catch, zone: ZoneId = ZoneId.systemDefault()): String = JSONObject()
        .put("kind", "message")
        .put("app", catch.app)
        .put("sender", catch.sender)
        .put("conversation", catch.conversation ?: JSONObject.NULL)
        .put("text", catch.text)
        .put("time", timeOnThePhone(catch.time, zone))
        .put("key", catch.key)
        .toString()

    fun timeOnThePhone(millis: Long, zone: ZoneId): String =
        OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis), zone).truncatedTo(ChronoUnit.SECONDS).toString()
}
