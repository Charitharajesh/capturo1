package com.capturo.app.premium

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Lightweight SharedPreferences-backed persistence for the self-contained
 * CAPTURO premium demo: saved photographers, chat messages, created posts and
 * the customer/photographer mode flag. No backend involved.
 */
object PremiumStore {

    private const val PREFS = "capturo_premium"
    private const val KEY_SAVED = "saved_ids"
    private const val KEY_MODE_PHOTOGRAPHER = "mode_photographer"
    private const val KEY_CHAT_IDS = "chat_ids"
    private const val KEY_POSTS = "created_posts"
    private const val KEY_MY_PHOTOGRAPHER = "my_photographer"
    private const val KEY_ACCOUNTS = "accounts"
    private const val KEY_SESSION_EMAIL = "session_email"
    private const val KEY_PAYMENTS = "payments"
    private const val KEY_PAYMENTS_SEEDED = "payments_seeded"

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---------- Saved photographers ----------
    fun savedIds(ctx: Context): MutableSet<String> =
        prefs(ctx).getStringSet(KEY_SAVED, emptySet())!!.toMutableSet()

    fun isSaved(ctx: Context, id: String) = savedIds(ctx).contains(id)

    fun toggleSaved(ctx: Context, id: String): Boolean {
        val set = savedIds(ctx)
        val nowSaved = if (set.contains(id)) { set.remove(id); false } else { set.add(id); true }
        prefs(ctx).edit().putStringSet(KEY_SAVED, set).apply()
        return nowSaved
    }

    fun savedPhotographers(ctx: Context): List<Photographer> {
        val ids = savedIds(ctx)
        return DemoData.photographers.filter { ids.contains(it.id) }
    }

    // ---------- Mode ----------
    fun isPhotographerMode(ctx: Context) =
        prefs(ctx).getBoolean(KEY_MODE_PHOTOGRAPHER, false)

    fun setPhotographerMode(ctx: Context, on: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_MODE_PHOTOGRAPHER, on).apply()

    // ---------- Accounts (on-device local database) ----------
    data class Account(val name: String, val email: String)

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun accountsJson(ctx: Context): JSONArray {
        val raw = prefs(ctx).getString(KEY_ACCOUNTS, "[]") ?: "[]"
        return runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
    }

    private fun findAccount(ctx: Context, email: String): JSONObject? {
        val arr = accountsJson(ctx)
        val target = email.trim().lowercase()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.optString("email").lowercase() == target) return o
        }
        return null
    }

    /** Creates an account. Returns null on success, or an error message. */
    fun register(ctx: Context, name: String, email: String, password: String): String? {
        val cleanName = name.trim()
        val cleanEmail = email.trim().lowercase()
        if (cleanName.isEmpty()) return "Please enter your full name"
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches())
            return "Please enter a valid email address"
        if (password.length < 6) return "Password must be at least 6 characters"
        if (findAccount(ctx, cleanEmail) != null) return "An account with this email already exists"

        val arr = accountsJson(ctx)
        arr.put(
            JSONObject().put("name", cleanName).put("email", cleanEmail)
                .put("pass", sha256(password))
        )
        prefs(ctx).edit()
            .putString(KEY_ACCOUNTS, arr.toString())
            .putString(KEY_SESSION_EMAIL, cleanEmail)
            .apply()
        return null
    }

    /** Signs in an existing account. Returns null on success, or an error message. */
    fun login(ctx: Context, email: String, password: String): String? {
        val cleanEmail = email.trim().lowercase()
        val account = findAccount(ctx, cleanEmail)
            ?: return "No account found for this email. Please sign up first."
        if (account.optString("pass") != sha256(password))
            return "Incorrect password. Please try again."
        prefs(ctx).edit().putString(KEY_SESSION_EMAIL, cleanEmail).apply()
        return null
    }

    fun currentAccount(ctx: Context): Account? {
        val email = prefs(ctx).getString(KEY_SESSION_EMAIL, null) ?: return null
        val o = findAccount(ctx, email) ?: return null
        return Account(o.optString("name").ifBlank { "Capturo User" }, o.optString("email"))
    }

    fun isLoggedIn(ctx: Context) = currentAccount(ctx) != null

    fun logout(ctx: Context) =
        prefs(ctx).edit().remove(KEY_SESSION_EMAIL).apply()

    // ---------- Payment history ----------
    data class PaymentRecord(
        val id: String,
        val photographer: String,
        val event: String,
        val date: String,
        val method: String,
        val amount: Int,
        val ts: Long
    )

    private fun paymentFromJson(o: JSONObject) = PaymentRecord(
        id = o.optString("id"),
        photographer = o.optString("photographer"),
        event = o.optString("event"),
        date = o.optString("date"),
        method = o.optString("method"),
        amount = o.optInt("amount"),
        ts = o.optLong("ts")
    )

    private fun paymentToJson(p: PaymentRecord) = JSONObject()
        .put("id", p.id).put("photographer", p.photographer).put("event", p.event)
        .put("date", p.date).put("method", p.method).put("amount", p.amount).put("ts", p.ts)

    /** Returns payment history, newest first. Seeds a few demo entries on first use. */
    fun payments(ctx: Context): List<PaymentRecord> {
        if (!prefs(ctx).getBoolean(KEY_PAYMENTS_SEEDED, false)) {
            val now = System.currentTimeMillis()
            val day = 86_400_000L
            val seed = listOf(
                PaymentRecord("CAP-4821", "Ananya Rao", "Wedding Photography", "12 Aug 2025", "UPI", 45000, now - 6 * day),
                PaymentRecord("CAP-3910", "Vikram Shetty", "Pre-Wedding Shoot", "28 Jul 2025", "Google Pay", 18000, now - 20 * day),
                PaymentRecord("CAP-2765", "Meera Nair", "Birthday Event", "05 Jul 2025", "Credit / Debit Card", 9500, now - 40 * day)
            )
            val arr = JSONArray()
            seed.forEach { arr.put(paymentToJson(it)) }
            prefs(ctx).edit()
                .putString(KEY_PAYMENTS, arr.toString())
                .putBoolean(KEY_PAYMENTS_SEEDED, true)
                .apply()
        }
        val raw = prefs(ctx).getString(KEY_PAYMENTS, "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        return (0 until arr.length()).map { paymentFromJson(arr.getJSONObject(it)) }
            .sortedByDescending { it.ts }
    }

    fun addPayment(ctx: Context, record: PaymentRecord) {
        payments(ctx) // ensure seeded before we append
        val raw = prefs(ctx).getString(KEY_PAYMENTS, "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        arr.put(paymentToJson(record))
        prefs(ctx).edit().putString(KEY_PAYMENTS, arr.toString()).apply()
    }

    // ---------- Chat ----------
    data class ChatMessage(
        val fromMe: Boolean,
        val text: String,
        val ts: Long,
        val attachmentUri: String? = null,
        val attachmentName: String? = null
    )

    fun conversationIds(ctx: Context): List<String> {
        val raw = prefs(ctx).getString(KEY_CHAT_IDS, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { arr.getString(it) }
    }

    fun messages(ctx: Context, photographerId: String): MutableList<ChatMessage> {
        val raw = prefs(ctx).getString("chat_$photographerId", "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            ChatMessage(
                o.getBoolean("me"),
                o.getString("t"),
                o.getLong("ts"),
                o.optString("au", "").ifEmpty { null },
                o.optString("an", "").ifEmpty { null }
            )
        }.toMutableList()
    }

    fun addMessage(ctx: Context, photographerId: String, msg: ChatMessage) {
        val list = messages(ctx, photographerId)
        list.add(msg)
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject().put("me", it.fromMe).put("t", it.text).put("ts", it.ts)
                    .put("au", it.attachmentUri ?: "").put("an", it.attachmentName ?: "")
            )
        }
        val ids = conversationIds(ctx).toMutableList()
        if (!ids.contains(photographerId)) ids.add(0, photographerId)
        prefs(ctx).edit()
            .putString("chat_$photographerId", arr.toString())
            .putString(KEY_CHAT_IDS, JSONArray(ids).toString())
            .apply()
    }

    // ---------- Created posts ----------
    data class CreatedPost(val uri: String, val caption: String, val category: String, val ts: Long)

    fun createdPosts(ctx: Context): List<CreatedPost> {
        val raw = prefs(ctx).getString(KEY_POSTS, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            CreatedPost(o.getString("uri"), o.getString("caption"), o.getString("category"), o.getLong("ts"))
        }.sortedByDescending { it.ts }
    }

    fun addPost(ctx: Context, post: CreatedPost) {
        val list = createdPosts(ctx).toMutableList()
        list.add(post)
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject().put("uri", it.uri).put("caption", it.caption)
                    .put("category", it.category).put("ts", it.ts)
            )
        }
        prefs(ctx).edit().putString(KEY_POSTS, arr.toString()).apply()
    }

    // ---------- Registered photographer (the user's own live listing) ----------
    data class RegisteredPhotographer(
        val id: String,
        val name: String,
        val mobile: String,
        val email: String,
        val pricePerHour: Int,
        val eventTypes: List<String>,
        val location: String,
        val lat: Double?,
        val lon: Double?,
        val sampleImages: List<String>
    )

    fun myPhotographer(ctx: Context): RegisteredPhotographer? {
        val raw = prefs(ctx).getString(KEY_MY_PHOTOGRAPHER, null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            val events = o.optJSONArray("events") ?: JSONArray()
            val images = o.optJSONArray("images") ?: JSONArray()
            RegisteredPhotographer(
                id = o.getString("id"),
                name = o.getString("name"),
                mobile = o.optString("mobile"),
                email = o.optString("email"),
                pricePerHour = o.optInt("price"),
                eventTypes = (0 until events.length()).map { events.getString(it) },
                location = o.optString("location"),
                lat = if (o.has("lat") && !o.isNull("lat")) o.getDouble("lat") else null,
                lon = if (o.has("lon") && !o.isNull("lon")) o.getDouble("lon") else null,
                sampleImages = (0 until images.length()).map { images.getString(it) }
            )
        }.getOrNull()
    }

    fun saveMyPhotographer(ctx: Context, p: RegisteredPhotographer) {
        val o = JSONObject()
            .put("id", p.id).put("name", p.name).put("mobile", p.mobile)
            .put("email", p.email).put("price", p.pricePerHour).put("location", p.location)
            .put("lat", p.lat ?: JSONObject.NULL).put("lon", p.lon ?: JSONObject.NULL)
            .put("events", JSONArray(p.eventTypes))
            .put("images", JSONArray(p.sampleImages))
        prefs(ctx).edit().putString(KEY_MY_PHOTOGRAPHER, o.toString()).apply()
    }

    /** Convert the saved registration into a live [Photographer] card. */
    fun RegisteredPhotographer.toPhotographer(): Photographer =
        DemoData.buildLocalPhotographer(
            id = id,
            name = name,
            specialties = eventTypes.joinToString(" • ").ifEmpty { "Photography • Videography" },
            location = location.ifBlank { "Bengaluru" },
            pricePerHour = pricePerHour,
            sampleImages = sampleImages,
            lat = lat,
            lon = lon
        )
}
