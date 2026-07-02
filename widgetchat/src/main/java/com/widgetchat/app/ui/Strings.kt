package com.widgetchat.app.ui

/** Static UI strings. English is the source of truth; other locales fall back to English. */
object Strings {
    enum class Key {
        InputPlaceholder, Send, Attach, ClearHistory, ExportChat,
        AiDisclosure, LearnMore, GotIt, GoodResponse, BadResponse,
        ReportReason, ReportNote, Submit, Cancel, ChatUnavailable,
        PoweredBy, DataReuseNotice, Online, Working,
    }

    private val en = mapOf(
        Key.InputPlaceholder to "Type a message…", Key.Send to "Send", Key.Attach to "Attach",
        Key.ClearHistory to "Clear history", Key.ExportChat to "Export chat",
        Key.AiDisclosure to "You're chatting with an AI assistant.",
        Key.LearnMore to "Learn more", Key.GotIt to "Got it",
        Key.GoodResponse to "Good response", Key.BadResponse to "Bad response",
        Key.ReportReason to "What went wrong?", Key.ReportNote to "Add a note (optional)",
        Key.Submit to "Submit", Key.Cancel to "Cancel",
        Key.ChatUnavailable to "Chat is temporarily unavailable. Please try again later.",
        Key.PoweredBy to "Powered by Widget-Chat",
        Key.DataReuseNotice to "Conversations may be used to improve this service.",
        Key.Online to "Online", Key.Working to "Working on it…",
    )
    private val fr = mapOf(
        Key.InputPlaceholder to "Écrivez un message…", Key.Send to "Envoyer", Key.Attach to "Joindre",
        Key.ClearHistory to "Effacer l'historique", Key.ExportChat to "Exporter",
        Key.AiDisclosure to "Vous discutez avec un assistant IA.",
        Key.LearnMore to "En savoir plus", Key.GotIt to "Compris",
        Key.GoodResponse to "Bonne réponse", Key.BadResponse to "Mauvaise réponse",
        Key.ReportReason to "Qu'est-ce qui n'allait pas ?", Key.ReportNote to "Ajouter une note (facultatif)",
        Key.Submit to "Envoyer", Key.Cancel to "Annuler",
        Key.ChatUnavailable to "Le chat est temporairement indisponible.",
        Key.PoweredBy to "Propulsé par Widget-Chat",
        Key.DataReuseNotice to "Les conversations peuvent améliorer ce service.",
        Key.Online to "En ligne", Key.Working to "Un instant…",
    )
    private val es = mapOf(
        Key.InputPlaceholder to "Escribe un mensaje…", Key.Send to "Enviar", Key.Attach to "Adjuntar",
        Key.ClearHistory to "Borrar historial", Key.ExportChat to "Exportar",
        Key.AiDisclosure to "Estás chateando con un asistente de IA.",
        Key.LearnMore to "Más información", Key.GotIt to "Entendido",
        Key.GoodResponse to "Buena respuesta", Key.BadResponse to "Mala respuesta",
        Key.ReportReason to "¿Qué salió mal?", Key.ReportNote to "Añadir una nota (opcional)",
        Key.Submit to "Enviar", Key.Cancel to "Cancelar",
        Key.ChatUnavailable to "El chat no está disponible temporalmente.",
        Key.PoweredBy to "Con la tecnología de Widget-Chat",
        Key.DataReuseNotice to "Las conversaciones pueden mejorar este servicio.",
        Key.Online to "En línea", Key.Working to "Un momento…",
    )
    private val de = mapOf(
        Key.InputPlaceholder to "Nachricht schreiben…", Key.Send to "Senden", Key.Attach to "Anhängen",
        Key.ClearHistory to "Verlauf löschen", Key.ExportChat to "Exportieren",
        Key.AiDisclosure to "Du chattest mit einem KI-Assistenten.",
        Key.LearnMore to "Mehr erfahren", Key.GotIt to "Verstanden",
        Key.GoodResponse to "Gute Antwort", Key.BadResponse to "Schlechte Antwort",
        Key.ReportReason to "Was war falsch?", Key.ReportNote to "Notiz hinzufügen (optional)",
        Key.Submit to "Senden", Key.Cancel to "Abbrechen",
        Key.ChatUnavailable to "Der Chat ist vorübergehend nicht verfügbar.",
        Key.PoweredBy to "Bereitgestellt von Widget-Chat",
        Key.DataReuseNotice to "Unterhaltungen können diesen Dienst verbessern.",
        Key.Online to "Online", Key.Working to "Einen Moment…",
    )
    private val ar = mapOf(
        Key.InputPlaceholder to "اكتب رسالة…", Key.Send to "إرسال", Key.Attach to "إرفاق",
        Key.ClearHistory to "مسح السجل", Key.ExportChat to "تصدير",
        Key.AiDisclosure to "أنت تتحدث مع مساعد ذكاء اصطناعي.",
        Key.LearnMore to "اعرف المزيد", Key.GotIt to "حسناً",
        Key.GoodResponse to "رد جيد", Key.BadResponse to "رد سيئ",
        Key.ReportReason to "ما الخطأ؟", Key.ReportNote to "أضف ملاحظة (اختياري)",
        Key.Submit to "إرسال", Key.Cancel to "إلغاء",
        Key.ChatUnavailable to "المحادثة غير متاحة مؤقتاً.",
        Key.PoweredBy to "مدعوم من Widget-Chat",
        Key.DataReuseNotice to "قد تُستخدم المحادثات لتحسين هذه الخدمة.",
        Key.Online to "متصل", Key.Working to "لحظة…",
    )

    private val catalogs = mapOf("en" to en, "fr" to fr, "es" to es, "de" to de, "ar" to ar)
    private val rtl = setOf("ar", "he", "fa", "ur")

    fun get(key: Key, locale: String): String {
        val lang = locale.take(2)
        return catalogs[lang]?.get(key) ?: en[key] ?: key.name
    }

    fun isRtl(locale: String) = locale.take(2) in rtl
}
