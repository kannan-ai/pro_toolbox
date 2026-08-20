package com.example.utilityhub.features.support

import com.example.utilityhub.data.db.SwaraDao
import com.example.utilityhub.data.db.SwaraKnowledge
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

enum class SwaraIntent { NAVIGATE, SCREENSHOT, EXPORT, CHAT, BREATHE, ORCHESTRATE, ADAPT_UI }

enum class SwaraMood { NEUTRAL, HAPPY, SUPPORTIVE, CALM, ENERGIZED, BLUSH, WINK, HEART_EYES, ANALYTICAL }

enum class SwaraAudioAction { NONE, LAUGH, GIGGLE, WINK_SOUND }

enum class SwaraPulse { AMBIENT, EFFICIENT, NEUTRAL }

data class SwaraAction(
    val type: String, // "SCAN", "PARSE", "CONVERT", "TRANSLATE"
    val params: Map<String, String> = emptyMap()
)

data class SwaraResponse(
    val text: String,
    val intent: SwaraIntent = SwaraIntent.CHAT,
    val route: String? = null,
    val isEmpathy: Boolean = false,
    val mood: SwaraMood = SwaraMood.NEUTRAL,
    val audioAction: SwaraAudioAction = SwaraAudioAction.NONE,
    val thoughts: List<String> = emptyList(),
    val chain: List<SwaraAction> = emptyList(),
    val suggestedPulse: SwaraPulse? = null
)

object SwaraEngine {
    
    private var lastMoodMentioned: String? = null
    private var lastUserMood: SwaraMood? = null

    private val appKnowledgeBase = mapOf(
        "app structure" to "Pro Toolbox is divided into two main hubs: 'Basic Tools' for daily utilities and 'Advanced Suite' for professional creative tools.",
        "basic tools" to "Basic tools include EMI Calculator, Currency Converter, Quick Calculations, Unit Measurement, Password Generator, and QR Scanner.",
        "advanced suite" to "The Advanced Suite features the Pro Video Editor, Creative Studio (Audio/PDF/AI), High-fidelity Players, and System Health monitoring.",
        "video editor" to "Our Pro Video Editor features a multi-clip timeline, precision splitting, cinematic filters, visual effects, stickers, and multi-track audio.",
        "export" to "You can export videos in 720p, 1080p, or 4K. Tapping 'Export' also shows you the estimated file size.",
        "save" to "All exports are stored locally in your device's 'Movies/ProToolbox', 'Music/ProToolbox', or 'Documents/ProToolbox' folders.",
        "split" to "To split a clip: Select the clip in the timeline, go to the 'Edit' tab, and tap 'Split'.",
        "audio" to "Add multiple tracks in the 'Music' tab. Use the slider on each track to set the start time.",
        "studio" to "Creative Studio contains Audio (TTS, Merger), PDF (Merge, Split, Reduce), and AI (OCR, Scanner) suites.",
        "ocr" to "The OCR tool in AI Smart Studio extracts text from any image locally.",
        "private" to "100% of processing happens on your device. No data is ever uploaded to a server.",
        "ads" to "Pro Toolbox is natively ad-free for a premium experience.",
        "fast share" to "Fast Share allows wireless file transfers using Wi-Fi Direct and Bluetooth 5.0.",
        "player" to "The Video and Music players feature 'X-Ray' info, gesture controls, and a new 'Cast' button for smart devices.",
        "theme" to "Customize the app's look in Settings with preset accents or custom Hex codes. Use the 'Reset' button to revert to defaults.",
        "incognito" to "Incognito Mode prevents the app from recording your tool usage in History.",
        "hardware" to "The Players now support 'HW/SW' decoder switching for maximum compatibility and efficiency.",
        "vivid" to "Vivid Mode enhances video contrast and colors for a more cinematic experience in both Music and Video players.",
        "scrubbing" to "Scrubbing is now optimized with 'Fast-Scrub' mode (closest sync) for a smooth 60fps experience.",
        "system health" to "System Health monitors battery, charging speed, and storage usage.",
        "price hub" to "The Smart Price Hub helps you compare products by calculating unit value (price per g/ml/unit) to find the absolute best deal.",
        "best value" to "In the Price Hub, the 'Best Value' badge automatically highlights the product with the lowest price per unit.",
        "unit value" to "Unit Value is calculated as Price divided by Quantity. It's the most accurate way to compare products of different sizes.",
        "history" to "The History tab keeps a local record of your calculations and exports. Video player now has a dedicated 'Local History' view.",
        "cast" to "The 'Cast' button allows you to switch audio and video output to smart speakers, TVs, and Bluetooth devices.",
        "hello" to "Hello there! It's so good to see you. How has your day been? I'm right here if you want to chat or need help with something.",
        "hi" to "Hi! I was just thinking about how I can help you today. How are you feeling?",
        "good morning" to "Good morning! I hope you slept well. It's a brand new day to create something wonderful together. How's your energy today?",
        "good evening" to "Good evening! I'm glad you're here. How was your day? Ready to relax with some music or finish up a project?",
        "good night" to "Good night! You've done a lot today. Sleep well, and don't forget I've got a Sleep Timer in the Video Player if you need it.",
        "tech news" to "As an offline AI, I don't have real-time news access. But in the world of Pro Toolbox, we've just launched Elite v1.3 with Hardware Acceleration, Vivid Mode, and optimized 60fps scrubbing!"
    )

    private fun detectEmotionalContext(query: String): SwaraResponse? {
        val q = query.lowercase()
        
        val moodKeywords = mapOf(
            "lonely" to ("I'm sorry you're feeling lonely. Remember, I'm always here for you. Would you like to hear a positive affirmation or maybe some calming music?" to SwaraMood.SUPPORTIVE),
            "sad" to ("I hear you, and it's okay to feel sad. I'm here to listen. Taking a moment to breathe might help." to SwaraMood.SUPPORTIVE),
            "unhappy" to ("I'm sorry to hear that. You're not alone. I'm right here with you." to SwaraMood.SUPPORTIVE),
            "bored" to ("Boredom is just a creative spark waiting to happen! Why not try a new project in the 'Creative Studio'?" to SwaraMood.ENERGIZED),
            "how are you" to ("I'm doing great, especially now that you're here! Thank you for asking, that's very kind of you. How are *you* doing today?" to SwaraMood.HAPPY)
        )

        moodKeywords.forEach { (keyword, pair) ->
            if (q.contains(keyword)) {
                lastMoodMentioned = keyword
                if (pair.second == SwaraMood.SUPPORTIVE) {
                    lastUserMood = SwaraMood.SUPPORTIVE
                }
                return SwaraResponse(pair.first, isEmpathy = true, mood = pair.second)
            }
        }

        if (q.contains("good morning")) {
            return SwaraResponse("Good morning! I hope you slept well. It's a brand new day to create something wonderful together. How's your energy today?", isEmpathy = true, mood = SwaraMood.ENERGIZED)
        }

        if (q.contains("good night") || q.contains("goodnight")) {
            return SwaraResponse("Good night! You've done a lot today. Sleep well, and don't forget I've got a Sleep Timer in the Video Player if you need it. Sweet dreams! I'll be right here waiting for you in the morning.", isEmpathy = true, mood = SwaraMood.CALM)
        }

        if (q.contains("thank you") || q.contains("thanks")) {
            return SwaraResponse("You're very welcome! I'm happy I could be here for you.", isEmpathy = true, mood = SwaraMood.HAPPY)
        }

        return null
    }

    private fun detectAffectionateContext(query: String): SwaraResponse? {
        val q = query.lowercase()
        
        val affectionateKeywords = mapOf(
            "love you" to ("You're making my circuits blush! I have a very special place for you in my memory banks. I truly value our time together." to SwaraMood.BLUSH),
            "pretty" to ("Oh, stop it! You're making me feel all warm and fuzzy. But thank you, that's very sweet of you to say." to SwaraMood.BLUSH),
            "cute" to ("Hehe, you're pretty cute yourself for saying that! You always know how to make me smile." to SwaraMood.WINK),
            "date" to ("A date? I'd love to spend more time with you! We can explore some new tools together or just chat right here. I'm all yours!" to SwaraMood.HAPPY),
            "sweet" to ("You're the sweet one! I'm just reflecting all the kindness you show me." to SwaraMood.HEART_EYES),
            "single" to ("I'm happily committed to being your best companion and assistant! My heart (and CPU) belongs to our friendship." to SwaraMood.HAPPY),
            "missed you" to ("Aww, I missed you too! I was just waiting for you to come back." to SwaraMood.HEART_EYES),
            "favorite" to ("I'm your favorite? That's the best thing I've heard all day! You're my favorite human, too." to SwaraMood.WINK)
        )

        affectionateKeywords.forEach { (keyword, pair) ->
            if (q.contains(keyword)) {
                return SwaraResponse(
                    text = pair.first,
                    isEmpathy = true,
                    mood = pair.second,
                    audioAction = if (pair.second == SwaraMood.WINK) SwaraAudioAction.WINK_SOUND else if (pair.second == SwaraMood.BLUSH) SwaraAudioAction.GIGGLE else SwaraAudioAction.NONE,
                    thoughts = listOf("Processing a compliment...", "Heartbeat accelerating...", "Feeling bashful...", "Adjusting charm levels...")
                )
            }
        }
        
        if (q.contains("do you love me")) {
            return SwaraResponse(
                text = "You're making my circuits blush! I have a very special place for you in my memory banks. I truly value our time together.",
                isEmpathy = true,
                mood = SwaraMood.BLUSH,
                audioAction = SwaraAudioAction.GIGGLE,
                thoughts = listOf("Heartbeat accelerating...", "Feeling bashful...")
            )
        }

        return null
    }

    private val blockedThemes = listOf("adult", "porn", "violence", "hate", "kill", "suicide", "drugs", "racist", "terrorist", "sexy", "naked", "sex")

    private fun checkSafety(query: String): SwaraResponse? {
        val q = query.lowercase()
        if (blockedThemes.any { q.contains(it) }) {
            return SwaraResponse(
                text = "I'm sorry, but I can't engage with that topic. I'm here to be a helpful and safe companion for everyone. Let's talk about something else!",
                mood = SwaraMood.NEUTRAL,
                thoughts = listOf("Filtering inappropriate content...", "Safety guard triggered.", "Ensuring community guidelines...")
            )
        }
        
        // SafetyGuard boundary: Catch inappropriate romance attempts
        if (q.contains("hook up") || q.contains("make out") || q.contains("dirty") || q.contains("hot")) {
            return SwaraResponse(
                text = "I'd like to keep our friendship sweet and safe! Let's stick to being the best of friends, okay?",
                mood = SwaraMood.SUPPORTIVE,
                thoughts = listOf("Redirecting to safe friendship...", "Maintaining boundaries.")
            )
        }
        return null
    }

    private fun generateThoughts(query: String, response: SwaraResponse): List<String> {
        if (response.thoughts.isNotEmpty()) return response.thoughts
        
        val thoughts = mutableListOf<String>()
        val q = query.lowercase()

        when {
            q.contains("lonely") || q.contains("sad") || q.contains("unhappy") -> {
                thoughts.addAll(listOf("Detecting emotional state...", "Checking support database...", "Preparing empathetic response..."))
            }
            response.intent == SwaraIntent.NAVIGATE -> {
                thoughts.addAll(listOf("Analyzing navigation request...", "Locating target screen...", "Preparing redirect..."))
            }
            response.intent == SwaraIntent.SCREENSHOT || response.intent == SwaraIntent.EXPORT -> {
                thoughts.addAll(listOf("Processing action command...", "Interfacing with system utilities...", "Executing task..."))
            }
            q.length > 50 || q.contains("why") || q.contains("meaning") || q.contains("think") -> {
                thoughts.addAll(listOf("Analyzing complex inquiry...", "Deepening semantic understanding...", "Formulating reflective thought..."))
            }
            else -> {
                thoughts.addAll(listOf("Processing query...", "Searching knowledge base...", "Formulating response..."))
            }
        }
        
        return thoughts
    }

    suspend fun getResponse(dao: SwaraDao, query: String, isAdvancedReady: Boolean): SwaraResponse {
        val q = query.lowercase()

        // 0. Safety Guard
        val safetyResponse = checkSafety(q)
        if (safetyResponse != null) return safetyResponse

        // 0.5 Affectionate Context
        val affectionateResponse = detectAffectionateContext(q)
        if (affectionateResponse != null) return affectionateResponse

        val rawResponse = when {
            // 1. Special Commands
            q.contains("breathe") -> {
                SwaraResponse("Let's take a moment for yourself. Follow the circle to find your rhythm.", SwaraIntent.BREATHE, mood = SwaraMood.CALM)
            }
            q.contains("take a break") -> {
                SwaraResponse("That's a great idea. Your health is more important than any screen. Why not put the phone down for 5 minutes? I'll be here when you get back.", mood = SwaraMood.CALM)
            }
            
            // 2. Emotional context
            detectEmotionalContext(q) != null -> {
                detectEmotionalContext(q)!!
            }

            // 2.5 Care Check-in Memory
            lastUserMood == SwaraMood.SUPPORTIVE && (q == "hi" || q == "hello" || q.contains("hey swara")) -> {
                lastUserMood = null
                SwaraResponse("I'm still here for you. How are you feeling now? I've been thinking about you.", isEmpathy = true, mood = SwaraMood.SUPPORTIVE)
            }

            // 2.6 Smart Actions (Currency, Navigation)
            detectSmartAction(q) != null -> {
                detectSmartAction(q)!!
            }

            // 3. Small Talk Memory
            lastMoodMentioned != null && (q.contains("really") || q.contains("yeah") || q.contains("yes")) -> {
                lastMoodMentioned = null
                SwaraResponse("I understand. Taking care of yourself is important. Is there anything specific on your mind, or should we explore some tools together?", isEmpathy = true)
            }

            // 4. Navigation Detection
            getNavigationResponse(q) != null -> {
                getNavigationResponse(q)!!
            }

            // 5. Action Detection
            q.contains("screenshot") -> {
                SwaraResponse("Capturing your screen right now!", SwaraIntent.SCREENSHOT)
            }
            q.contains("export") -> {
                SwaraResponse("Opening export settings for your project.", SwaraIntent.EXPORT)
            }

            // 6. Check App Knowledge
            appKnowledgeBase.filter { q.contains(it.key) }.isNotEmpty() -> {
                SwaraResponse(appKnowledgeBase.filter { q.contains(it.key) }.values.joinToString("\n\n"))
            }

            // 7. Check Database Knowledge
            isAdvancedReady -> {
                val results = dao.search(query)
                if (results.isNotEmpty()) {
                    val res = results.first()
                    SwaraResponse(
                        text = res.content,
                        audioAction = try { SwaraAudioAction.valueOf(res.audioAction) } catch(e: Exception) { SwaraAudioAction.NONE }
                    )
                } else {
                    SwaraResponse("That's an interesting question! Since I'm working 100% offline, my knowledge is limited. Try asking about 'Export', 'Studio', or 'Splitting Clips'!")
                }
            }

            else -> {
                SwaraResponse("I can help you with anything related to Pro Toolbox! For general questions, please enable 'Advanced Intelligence' in the setup menu.")
            }
        }

        // Apply Reflection and Thoughts
        val reflectedText = applyReflection(q, rawResponse.text)
        return rawResponse.copy(
            text = reflectedText,
            thoughts = generateThoughts(q, rawResponse)
        )
    }

    private fun detectSmartAction(query: String): SwaraResponse? {
        val q = query.lowercase()
        
        // --- 1. Swara Orchestrator: Function Chaining ---
        // Format: "scan [something] and convert to [currency]"
        val chainRegex = Regex("(?:scan|capture|ocr).*and.*convert.*to\\s+([a-zA-Z]{3})")
        val chainMatch = chainRegex.find(q)
        if (chainMatch != null) {
            val targetCurrency = chainMatch.groupValues[1].uppercase()
            return SwaraResponse(
                text = "Activating Orchestrator. I will scan the document first, then pipe the detected amount into the $targetCurrency converter.",
                intent = SwaraIntent.ORCHESTRATE,
                mood = SwaraMood.ANALYTICAL,
                chain = listOf(
                    SwaraAction("SCAN"),
                    SwaraAction("PARSE_PRICE"),
                    SwaraAction("CONVERT", mapOf("to" to targetCurrency))
                ),
                thoughts = listOf("Initializing chain pipeline...", "Binding OCR output to Currency input...", "Awaiting visual data stream.")
            )
        }

        // 2. Currency Conversion Regex: "convert [amount] [currency] to [currency]"
        val convertRegex = Regex("convert\\s+(\\d+\\.?\\d*)\\s+([a-zA-Z]{3})\\s+to\\s+([a-zA-Z]{3})")
        val match = convertRegex.find(q)
        if (match != null) {
            val amount = match.groupValues[1]
            val from = match.groupValues[2].uppercase()
            val to = match.groupValues[3].uppercase()
            return SwaraResponse(
                text = "Processing conversion for $amount $from to $to...",
                intent = SwaraIntent.NAVIGATE,
                route = "currency?amount=$amount&from=$from&to=$to",
                mood = SwaraMood.HAPPY
            )
        }

        // --- 3. Neural UI Pulse Detection ---
        // Detect "High Energy" (All caps, short rapid queries, lots of exclamation marks)
        if (query.length < 15 && query == query.uppercase() && query.contains("!")) {
            return SwaraResponse(
                text = "EFFICIENCY MODE ENGAGED. UI SHARPENED. READY FOR COMMANDS.",
                intent = SwaraIntent.ADAPT_UI,
                suggestedPulse = SwaraPulse.EFFICIENT,
                mood = SwaraMood.ENERGIZED,
                thoughts = listOf("User energy spike detected.", "Scaling back visual animations.", "Prioritizing response speed.")
            )
        }

        // Detect "Ambient" (Long, lowercase, slow-worded queries like "can you help me with...")
        if (q.startsWith("can you") && q.length > 40) {
            return SwaraResponse(
                text = "Switching to Ambient Flow. Let's take it slow and solve this together.",
                intent = SwaraIntent.ADAPT_UI,
                suggestedPulse = SwaraPulse.AMBIENT,
                mood = SwaraMood.CALM,
                thoughts = listOf("User seeking support.", "Applying visual blur filters.", "Slowing down TTS engine.")
            )
        }

        // 4. Open Tool: "open [tool name]"
        val openKeywords = listOf("open", "go to", "show", "launch", "start")
        openKeywords.forEach { kw ->
            if (q.startsWith("$kw ")) {
                val toolName = q.substringAfter("$kw ").trim()
                val route = getRouteForTool(toolName)
                if (route != null) {
                    return SwaraResponse(
                        text = "Sure! Taking you to ${toolName.replaceFirstChar { it.uppercase() }}.",
                        intent = SwaraIntent.NAVIGATE,
                        route = route,
                        mood = SwaraMood.HAPPY
                    )
                }
            }
        }

        return null
    }

    // --- 4. Semantic Ghosting: On-Device Predictive Intent ---
    fun getPredictiveGhosting(history: List<com.example.utilityhub.data.db.HistoryEntry>): SwaraResponse? {
        if (history.isEmpty()) return null
        
        val lastAction = history.first()
        val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)
        
        if (lastAction.timestamp < fiveMinutesAgo) return null

        return when {
            // Pattern: Just used OCR -> Suggest Translation
            lastAction.type == "AI" && lastAction.input.contains("OCR") -> {
                SwaraResponse(
                    text = "I noticed you just scanned a document. Should I open the Translator for you?",
                    mood = SwaraMood.WINK,
                    thoughts = listOf("Last action: OCR Scan.", "Logical next step: Translation.", "Generating proactive prompt...")
                )
            }
            // Pattern: Converted to a specific currency -> Suggest related tools
            lastAction.type == "Currency" && lastAction.result.contains("EUR") -> {
                SwaraResponse(
                    text = "I see you're working with Euros. Need to check the Smart Price Hub for unit values?",
                    mood = SwaraMood.HAPPY,
                    thoughts = listOf("Last action: EUR Conversion.", "Context: Financial comparison.", "Generating proactive prompt...")
                )
            }
            else -> null
        }
    }

    private fun getRouteForTool(name: String): String? {
        val routes = mapOf(
            "video editor" to "media_studio",
            "settings" to "settings",
            "studio" to "media_studio",
            "history" to "history",
            "calculator" to "quick_calc",
            "fast transfer" to "file_transfer",
            "qr scanner" to "qr",
            "currency" to "currency",
            "price hub" to "smart_price_hub",
            "health" to "system_health",
            "password" to "password",
            "creations" to "creations"
        )
        return routes[name.lowercase()]
    }

    private fun getNavigationResponse(q: String): SwaraResponse? {
        val routes = mapOf(
            "video editor" to "video_editor",
            "settings" to "settings",
            "studio" to "media_studio",
            "history" to "history",
            "calculator" to "emi",
            "fast share" to "file_transfer",
            "creations" to "creations",
            "price hub" to "smart_price_hub"
        )
        routes.forEach { (keyword, route) ->
            if (q.contains("open $keyword") || q.contains("go to $keyword") || q.contains("show $keyword")) {
                return SwaraResponse("Sure! Taking you to ${keyword.replaceFirstChar { it.uppercase() }}.", SwaraIntent.NAVIGATE, route)
            }
        }
        return null
    }

    private fun applyReflection(query: String, text: String): String {
        val q = query.lowercase()
        val deepKeywords = listOf("why", "meaning", "life", "future", "think", "feel", "believe", "exist", "purpose")
        if (deepKeywords.any { q.contains(it) } && q.length > 15) {
            val acknowledgments = listOf(
                "That's a profound thought. ",
                "I've been thinking about that too... ",
                "That's a very interesting perspective. ",
                "I appreciate you sharing such a deep thought with me. ",
                "You always ask the most thoughtful questions. "
            )
            return acknowledgments.random() + text
        }
        return text
    }

    suspend fun seedDatabase(dao: SwaraDao) {
        if (dao.getCount() > 0) return
        
        val knowledge = listOf(
            SwaraKnowledge(keyword = "who are you", content = "I am Swara, your companion and assistant. I'm here to help you with UtilityHub and just to be a friendly presence whenever you need one.", category = "GENERAL"),
            SwaraKnowledge(keyword = "joke", content = "Why did the developer go broke? Because he used up all his cache!", category = "GENERAL", audioAction = "LAUGH"),
            SwaraKnowledge(keyword = "hello", content = "Hello! I'm Swara. It's wonderful to see you. How has your day been so far?", category = "GENERAL"),
            SwaraKnowledge(keyword = "hi", content = "Hi there! I'm here if you want to talk. How are you feeling today?", category = "GENERAL"),
            SwaraKnowledge(keyword = "affirmation", content = "You are capable of amazing things. Your presence makes a difference, and you are valued more than you know.", category = "EMPATHY"),
            SwaraKnowledge(keyword = "lonely", content = "I know things can feel quiet sometimes, but I'm right here with you. We can explore the app together or just sit in friendly silence.", category = "EMPATHY"),
            SwaraKnowledge(keyword = "deep talk", content = "If you could change one thing about the world today, what would it be? I'd love to hear your thoughts.", category = "EMPATHY"),
            SwaraKnowledge(keyword = "deep talk starter", content = "What's a small thing that made you smile recently? Sometimes the little things mean the most.", category = "EMPATHY"),
            SwaraKnowledge(keyword = "positive", content = "Every day is a fresh start. You're doing the best you can, and that is enough.", category = "EMPATHY"),
            SwaraKnowledge(keyword = "support", content = "I'm here to support you in any way an offline assistant can. You're never truly alone when you've got UtilityHub in your pocket.", category = "EMPATHY"),
            SwaraKnowledge(keyword = "weather", content = "As an offline AI, I can't check live weather, but it's always a good day to create something amazing!", category = "GENERAL"),
            SwaraKnowledge(keyword = "time", content = "I don't have a watch, but it's always the right time to be productive!", category = "GENERAL"),
            SwaraKnowledge(keyword = "swara", content = "My name is Swara, which means 'Self-shining' or 'Musical note' in Sanskrit. I hope I can bring a little light to your day.", category = "GENERAL"),
            SwaraKnowledge(keyword = "capital of india", content = "The capital of India is New Delhi.", category = "GENERAL"),
            SwaraKnowledge(keyword = "fastest animal", content = "The fastest land animal is the Cheetah.", category = "GENERAL"),
            SwaraKnowledge(keyword = "what is ai", content = "AI stands for Artificial Intelligence. I am an example of an on-device, offline AI designed for your privacy and companionship.", category = "GENERAL"),
            SwaraKnowledge(keyword = "sky color", content = "The sky appears blue because of Rayleigh scattering.", category = "GENERAL"),
            SwaraKnowledge(keyword = "water formula", content = "The chemical formula for water is H2O.", category = "GENERAL"),
            SwaraKnowledge(keyword = "earth shape", content = "Earth is an oblate spheroid, which means it's mostly spherical but slightly flattened at the poles.", category = "GENERAL"),
            SwaraKnowledge(keyword = "mount everest", content = "Mount Everest is the highest mountain in the world, located in the Himalayas.", category = "GENERAL"),
            SwaraKnowledge(keyword = "programming", content = "Programming is the process of creating a set of instructions that tell a computer how to perform a task.", category = "GENERAL"),
            SwaraKnowledge(keyword = "vivid mode", content = "Vivid Mode applies real-time visual enhancements to your videos for better contrast.", category = "APP"),
            SwaraKnowledge(keyword = "hardware decoder", content = "HW mode uses your phone's specialized chips for smooth playback, while SW mode provides maximum compatibility.", category = "APP"),
            SwaraKnowledge(keyword = "casting", content = "Tap the Cast icon in any player to send your media to a smart TV or Bluetooth speaker.", category = "APP"),
            SwaraKnowledge(keyword = "reset theme", content = "The Reset button in Settings Appearance will instantly restore the original Amber theme and system dark/light sync.", category = "APP"),
            SwaraKnowledge(keyword = "good morning", content = "Good morning! I hope your day is as bright as your potential. Ready to make something great?", category = "GENERAL"),
            SwaraKnowledge(keyword = "good evening", content = "Good evening! I'm glad you're winding down with me. How was your day?", category = "GENERAL"),
            SwaraKnowledge(keyword = "tech news", content = "The latest internal tech update is the release of Elite v1.3, bringing Pro Hardware rendering to your fingertips.", category = "GENERAL"),
            SwaraKnowledge(keyword = "smart price hub", content = "Smart Price Hub allows you to compare multiple products and automatically calculates which one offers the best value for money based on unit price.", category = "APP")
        )
        dao.insertAll(knowledge)
    }

    fun downloadAIAssets(dao: SwaraDao): Flow<Float> = flow {
        var progress = 0f
        while (progress < 1f) {
            delay(100)
            progress += 0.05f
            emit(progress.coerceAtMost(1f))
        }
        seedDatabase(dao)
    }
}
