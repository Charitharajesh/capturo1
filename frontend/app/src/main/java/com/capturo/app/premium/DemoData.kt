package com.capturo.app.premium

/**
 * Self-contained demo content for the CAPTURO premium experience.
 * Images are streamed from Unsplash (needs internet, but NO backend server).
 */

data class PhotoPackage(
    val name: String,
    val price: String,
    val features: List<String>
)

data class Photographer(
    val id: String,
    val name: String,
    val specialties: String,
    val location: String,
    val rating: Double,
    val reviews: Int,
    val startingPrice: String,
    val experience: Int,
    val followers: String,
    val bookings: Int,
    val verified: Boolean,
    val available: Boolean,
    val coverUrl: String,
    val avatarUrl: String,
    val about: String,
    val portfolio: List<String>,
    val packages: List<PhotoPackage>,
    val lat: Double? = null,
    val lon: Double? = null
)

data class Post(
    val id: String,
    val photographer: String,
    val avatarUrl: String,
    val location: String,
    val category: String,
    val imageUrl: String,
    val caption: String,
    val likes: Int,
    val comments: Int
)

enum class BookingStatus { CONFIRMED, COMPLETED, CANCELLED }

data class Booking(
    val id: String,
    val photographer: String,
    val avatarUrl: String,
    val eventType: String,
    val date: String,
    val time: String,
    val status: BookingStatus,
    val price: String
)

data class Category(val emoji: String, val name: String, val imageUrl: String)

object DemoData {

    private const val Q = "?auto=format&fit=crop&w=900&q=70"

    private fun u(id: String) = "https://images.unsplash.com/photo-$id$Q"

    // Reusable pools of premium photography
    private val weddingPool = listOf(
        "1519741497674-611481863552", "1511285560929-80b456fea0bc",
        "1606216794074-735e91aa2c92", "1520854221256-17451cc331bf",
        "1606800052052-a08af7148866", "1465495976277-4387d4b0b4c6"
    )
    private val portraitPool = listOf(
        "1524504388940-b1c1722653e1", "1534528741775-53994a69daeb",
        "1507003211169-0a1dd7228f2d", "1438761681033-6461ffad8d80",
        "1544005313-94ddf0286df2", "1500648767791-00dcc994a43e"
    )
    private val eventPool = listOf(
        "1492684223066-81342ee5ff30", "1511578314322-379afb476865",
        "1530103862676-de8c9debad1d", "1464349095431-e9a21285b5f3",
        "1522673607200-164d1b6ce486", "1519671482749-fd09be7ccebf"
    )

    private fun mixPortfolio(): List<String> =
        (weddingPool + portraitPool + eventPool).shuffled().take(9).map { u(it) }

    private fun standardPackages(base: Int) = listOf(
        PhotoPackage("Basic", "₹${base}", listOf("4 Hours Coverage", "1 Photographer", "200 Edited Photos", "Online Gallery")),
        PhotoPackage("Premium", "₹${base * 2 + 5000}", listOf("8 Hours Coverage", "2 Photographers", "500 Edited Photos", "Cinematic Video", "Premium Album")),
        PhotoPackage("Luxury", "₹${base * 4 + 5000}", listOf("Full Day Coverage", "3 Photographers", "2 Videographers", "Drone Shots", "Premium Album", "Same-day Teaser"))
    )

    /**
     * Live list backing every premium screen. Starts with the bundled demo
     * photographers (so the app is never empty / works offline) and is replaced
     * by real backend creators once [hydrateFromCreators] succeeds at startup.
     */
    var photographers: List<Photographer> = fallbackPhotographers
        private set

    private val fallbackPhotographers: List<Photographer> get() = listOf(
        Photographer(
            "p1", "Arjun Visuals", "Wedding • Pre-Wedding • Events", "Madanapalle",
            4.9, 126, "₹15,000", 8, "24.5k", 340, true, true,
            u("1519741497674-611481863552"), u("1507003211169-0a1dd7228f2d"),
            "Award-winning wedding storyteller capturing candid emotions and timeless frames across South India. Every wedding is a film waiting to be told.",
            mixPortfolio(), standardPackages(15000)
        ),
        Photographer(
            "p2", "Pixel Stories", "Portrait • Events", "Tirupati",
            4.8, 98, "₹8,000", 5, "12.1k", 210, true, true,
            u("1524504388940-b1c1722653e1"), u("1500648767791-00dcc994a43e"),
            "Portrait and lifestyle specialist. Natural light, honest expressions, and a relaxed studio experience for every client.",
            mixPortfolio(), standardPackages(8000)
        ),
        Photographer(
            "p3", "Moments Studio", "Wedding • Birthday • Events", "Bengaluru",
            5.0, 214, "₹20,000", 11, "58.3k", 620, true, false,
            u("1606216794074-735e91aa2c92"), u("1534528741775-53994a69daeb"),
            "A full-service premium studio for weddings and celebrations. Cinematic teams, drone coverage, and luxury albums.",
            mixPortfolio(), standardPackages(20000)
        ),
        Photographer(
            "p4", "Frame & Light", "Fashion • Portrait", "Hyderabad",
            4.7, 87, "₹12,000", 6, "18.9k", 175, true, true,
            u("1544005313-94ddf0286df2"), u("1438761681033-6461ffad8d80"),
            "Editorial fashion and bold portraiture. Studio and outdoor shoots with a signature dramatic lighting style.",
            mixPortfolio(), standardPackages(12000)
        ),
        Photographer(
            "p5", "Golden Hour Films", "Pre-Wedding • Cinematic", "Chennai",
            4.9, 152, "₹18,000", 9, "41.2k", 410, true, true,
            u("1520854221256-17451cc331bf"), u("1507003211169-0a1dd7228f2d"),
            "We chase the light. Cinematic pre-wedding films and dreamy couple shoots at the most beautiful hours of the day.",
            mixPortfolio(), standardPackages(18000)
        ),
        Photographer(
            "p6", "Candid Canvas", "Maternity • Baby • Family", "Vijayawada",
            4.8, 73, "₹9,500", 4, "9.4k", 130, false, true,
            u("1522673607200-164d1b6ce486"), u("1544005313-94ddf0286df2"),
            "Gentle, warm and heartfelt. Specialising in maternity, newborn and family sessions that you'll treasure forever.",
            mixPortfolio(), standardPackages(9500)
        ),
        Photographer(
            "p7", "Studio Noir", "Corporate • Product", "Hyderabad",
            4.6, 64, "₹11,000", 7, "7.8k", 156, true, true,
            u("1492684223066-81342ee5ff30"), u("1500648767791-00dcc994a43e"),
            "Clean, sharp commercial photography. Product, corporate headshots and brand campaigns delivered with precision.",
            mixPortfolio(), standardPackages(11000)
        ),
        Photographer(
            "p8", "Everlight Photography", "Wedding • Events", "Bengaluru",
            4.9, 189, "₹22,000", 10, "63.0k", 540, true, false,
            u("1606800052052-a08af7148866"), u("1534528741775-53994a69daeb"),
            "Luxury wedding coverage with a documentary heart. Trusted by 500+ couples across India for their biggest day.",
            mixPortfolio(), standardPackages(22000)
        )
    )

    val categories: List<Category> = listOf(
        Category("📸", "Wedding", u("1519741497674-611481863552")),
        Category("🎂", "Birthday", u("1464349095431-e9a21285b5f3")),
        Category("💍", "Pre-Wedding", u("1520854221256-17451cc331bf")),
        Category("👤", "Portrait", u("1524504388940-b1c1722653e1")),
        Category("👶", "Baby Shoot", u("1522673607200-164d1b6ce486")),
        Category("🤰", "Maternity", u("1544005313-94ddf0286df2")),
        Category("🎉", "Events", u("1492684223066-81342ee5ff30")),
        Category("🏢", "Corporate", u("1507003211169-0a1dd7228f2d")),
        Category("📦", "Product", u("1500648767791-00dcc994a43e")),
        Category("🎓", "Graduation", u("1438761681033-6461ffad8d80"))
    )

    val feed: List<Post> = listOf(
        Post("f1", "Everlight Photography", u("1534528741775-53994a69daeb"), "Taj Falaknuma, Hyderabad", "Wedding",
            u("1519741497674-611481863552"), "Beautiful evening wedding celebration ✨ #capturomoments", 245, 18),
        Post("f2", "Golden Hour Films", u("1507003211169-0a1dd7228f2d"), "Marina Beach, Chennai", "Pre-Wedding",
            u("1520854221256-17451cc331bf"), "Chasing the golden hour with Anaya & Rohan 🌅", 512, 41),
        Post("f3", "Pixel Stories", u("1500648767791-00dcc994a43e"), "Studio, Tirupati", "Portrait",
            u("1524504388940-b1c1722653e1"), "Natural light portraits never go out of style.", 189, 12),
        Post("f4", "Moments Studio", u("1534528741775-53994a69daeb"), "Bengaluru", "Birthday",
            u("1464349095431-e9a21285b5f3"), "A first birthday to remember 🎂🎉", 301, 27),
        Post("f5", "Frame & Light", u("1438761681033-6461ffad8d80"), "Hyderabad", "Fashion",
            u("1544005313-94ddf0286df2"), "Editorial vibes for this month's cover shoot.", 428, 33),
        Post("f6", "Candid Canvas", u("1544005313-94ddf0286df2"), "Vijayawada", "Maternity",
            u("1522673607200-164d1b6ce486"), "Every glow tells a story 🤍", 156, 9)
    )

    val bookings: List<Booking> = listOf(
        Booking("CAP-2043", "Arjun Visuals", u("1507003211169-0a1dd7228f2d"), "Wedding Photography",
            "25 Sep 2026", "5:00 PM – 10:00 PM", BookingStatus.CONFIRMED, "₹35,500"),
        Booking("CAP-1987", "Golden Hour Films", u("1507003211169-0a1dd7228f2d"), "Pre-Wedding Shoot",
            "12 Oct 2026", "6:00 AM – 9:00 AM", BookingStatus.CONFIRMED, "₹18,000"),
        Booking("CAP-1820", "Pixel Stories", u("1500648767791-00dcc994a43e"), "Portrait Session",
            "02 Aug 2026", "11:00 AM – 1:00 PM", BookingStatus.COMPLETED, "₹8,500"),
        Booking("CAP-1755", "Moments Studio", u("1534528741775-53994a69daeb"), "Birthday Event",
            "18 Jul 2026", "4:00 PM – 8:00 PM", BookingStatus.COMPLETED, "₹22,000"),
        Booking("CAP-1699", "Studio Noir", u("1500648767791-00dcc994a43e"), "Product Shoot",
            "05 Jun 2026", "10:00 AM – 12:00 PM", BookingStatus.CANCELLED, "₹11,000")
    )

    val eventTypes = listOf(
        "Wedding", "Birthday", "Pre-Wedding", "Engagement",
        "Maternity", "Portrait", "Corporate", "Other"
    )

    val timeSlots = listOf(
        "09:00 AM – 12:00 PM" to "available",
        "12:00 PM – 03:00 PM" to "booked",
        "03:00 PM – 06:00 PM" to "available",
        "06:00 PM – 09:00 PM" to "few"
    )

    fun byId(id: String): Photographer =
        photographers.firstOrNull { it.id == id } ?: photographers.first()

    /**
     * Build a live [Photographer] card from a locally-registered photographer.
     * Sample images the user uploaded become the portfolio; the hourly rate
     * drives the standard package pricing.
     */
    fun buildLocalPhotographer(
        id: String,
        name: String,
        specialties: String,
        location: String,
        pricePerHour: Int,
        sampleImages: List<String>,
        lat: Double?,
        lon: Double?
    ): Photographer {
        val cover = sampleImages.firstOrNull() ?: u(weddingPool.random())
        val portfolio = if (sampleImages.isNotEmpty()) sampleImages else mixPortfolio()
        val hourly = if (pricePerHour > 0) pricePerHour else 8000
        return Photographer(
            id = id,
            name = name,
            specialties = specialties,
            location = location,
            rating = 5.0,
            reviews = 0,
            startingPrice = "₹" + "%,d".format(hourly) + "/hr",
            experience = 1,
            followers = "New",
            bookings = 0,
            verified = true,
            available = true,
            coverUrl = cover,
            avatarUrl = cover,
            about = "New on Capturo — $specialties. Book me for your next shoot!",
            portfolio = portfolio,
            packages = standardPackages(hourly),
            lat = lat,
            lon = lon
        )
    }

    /**
     * Prepend locally-registered photographers to the live list so they appear
     * to every customer immediately. De-dupes by id so re-registering updates
     * the existing card instead of adding a duplicate.
     */
    fun mergeLocal(locals: List<Photographer>) {
        if (locals.isEmpty()) return
        val localIds = locals.map { it.id }.toSet()
        photographers = locals + photographers.filterNot { localIds.contains(it.id) }
    }

    // Bangalore city centre — the reference point the demo map is drawn around.
    private const val CENTER_LAT = 12.9716
    private const val CENTER_LON = 77.5946

    /**
     * Distance (km) from the city centre. Uses the real backend coordinates
     * when available (haversine), otherwise a stable per-id demo value.
     */
    fun distanceKm(p: Photographer): Double {
        val lat = p.lat; val lon = p.lon
        if (lat != null && lon != null) return haversineKm(CENTER_LAT, CENTER_LON, lat, lon)
        val h = kotlin.math.abs(p.id.hashCode())
        return 0.8 + (h % 180) / 10.0
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2).let { it * it } +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2).let { it * it }
        return r * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    }

    /**
     * (x, y) in 0..1 used to place pins on the demo map. Projects the real
     * backend coordinates around the city centre when present, otherwise falls
     * back to a stable per-id position so pins never overlap the "you" dot.
     */
    fun mapPoint(p: Photographer): Pair<Float, Float> {
        val lat = p.lat; val lon = p.lon
        if (lat != null && lon != null) {
            val span = 0.22 // ~24 km window across the map
            val x = (0.5 + (lon - CENTER_LON) / span).coerceIn(0.08, 0.92)
            val y = (0.5 - (lat - CENTER_LAT) / span).coerceIn(0.10, 0.90)
            return x.toFloat() to y.toFloat()
        }
        val h = kotlin.math.abs(p.id.hashCode())
        val x = 0.12f + (h % 76) / 100f
        val y = 0.16f + ((h / 7) % 64) / 100f
        return x to y
    }

    /**
     * Replace the demo photographers with real backend creators. Keeps the
     * bundled demo list if the backend returned nothing (offline-safe).
     */
    fun hydrateFromCreators(creators: List<com.capturo.app.data.model.response.CreatorResponse>) {
        val mapped = creators.mapNotNull { runCatching { fromCreator(it) }.getOrNull() }
        if (mapped.isNotEmpty()) photographers = mapped
    }

    private fun fromCreator(c: com.capturo.app.data.model.response.CreatorResponse): Photographer {
        val pic = c.profilePicUrl?.takeIf { it.isNotBlank() } ?: u(portraitPool.random())
        val rate = c.hourlyRate.toInt()
        val priceText = "₹" + "%,d".format(rate) + "/hr"
        val available = c.availabilityStatus.equals("available", ignoreCase = true)
        return Photographer(
            id = c.id,
            name = c.fullName,
            specialties = "Photography • Videography",
            location = localityFor(c.latitude, c.longitude),
            rating = c.avgRating,
            reviews = c.totalReviews,
            startingPrice = priceText,
            experience = 0,
            followers = "",
            bookings = 0,
            verified = true,
            available = available,
            coverUrl = pic,
            avatarUrl = pic,
            about = "Professional creator on Capturo, available for bookings around the city.",
            portfolio = (weddingPool + eventPool).shuffled().take(6).map { u(it) },
            packages = standardPackages(if (rate > 0) rate else 8000),
            lat = c.latitude,
            lon = c.longitude
        )
    }

    /** Rough Bangalore locality label from coordinates for the card subtitle. */
    private fun localityFor(lat: Double?, lon: Double?): String {
        if (lat == null || lon == null) return "Bengaluru"
        return when {
            lat >= 13.00 -> "Malleshwaram, Bengaluru"
            lat <= 12.86 -> "Electronic City, Bengaluru"
            lon >= 77.70 -> "Whitefield, Bengaluru"
            lon >= 77.64 -> "Koramangala, Bengaluru"
            lon <= 77.57 -> "Jayanagar, Bengaluru"
            else -> "Bengaluru"
        }
    }

    fun search(query: String): List<Photographer> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return photographers
        return photographers.filter {
            it.name.lowercase().contains(q) ||
                it.specialties.lowercase().contains(q) ||
                it.location.lowercase().contains(q)
        }
    }
}
