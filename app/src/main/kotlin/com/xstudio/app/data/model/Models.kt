package com.xstudio.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class UserRole(
    val wireName: String,
    val displayName: String,
    val priority: Int,
    val description: String
) {
    @SerialName("owner")
    OWNER("owner", "Owner", 100, "Founder & Designated Owner of X Studio"),

    @SerialName("developer")
    DEVELOPER("developer", "Developer", 90, "Core X Studio Engineering & Full Feature Access"),

    @SerialName("admin")
    ADMIN("admin", "Admin", 80, "Platform Moderator & Community Administrator"),

    @SerialName("beta_tester")
    BETA_TESTER("beta_tester", "Beta Tester", 60, "Early Access Testing & Experimental Tools"),

    @SerialName("verified")
    VERIFIED("verified", "Verified", 50, "Authentic Verified X Social Creator"),

    @SerialName("premium")
    PREMIUM("premium", "Premium", 40, "Full Ultra-HD 8K & High-FPS Enhancement Tier"),

    @SerialName("pro")
    PRO("pro", "Pro", 30, "Advanced Creative & Batch Processing Member"),

    @SerialName("free")
    FREE("free", "Free", 10, "Standard X Studio Community Member");

    val hasFullFeatureAccess: Boolean
        get() = this == OWNER || this == DEVELOPER || this == PREMIUM

    val canAccessModerationPanel: Boolean
        get() = this == OWNER || this == DEVELOPER || this == ADMIN

    fun canModerateTarget(targetRole: UserRole): Boolean {
        if (targetRole == OWNER) return false
        if (this == OWNER || this == DEVELOPER) return true
        if (this == ADMIN) return targetRole !in listOf(OWNER, DEVELOPER, ADMIN)
        return false
    }

    companion object {
        fun fromString(raw: String?): UserRole =
            entries.firstOrNull {
                it.wireName.equals(raw?.trim(), ignoreCase = true) ||
                    it.displayName.equals(raw?.trim(), ignoreCase = true)
            } ?: FREE
    }
}

@Serializable
data class SocialAuthor(
    val id: String = "",
    val username: String = "user",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("avatar_path") val avatarPath: String? = null,
    @SerialName("is_private") val isPrivate: Boolean = false,
    val rank: String = "free",
    @SerialName("rank_label") val rankLabel: String = "Free",
    @SerialName("badge_path") val badgePath: String? = null
) {
    val parsedRole: UserRole get() = UserRole.fromString(rank)
}

@Serializable
data class SocialProfileRecord(
    @SerialName("user_id") val userId: String = "",
    val username: String = "",
    @SerialName("display_name") val displayName: String = "",
    val bio: String = "",
    @SerialName("avatar_path") val avatarPath: String? = null,
    @SerialName("is_private") val isPrivate: Boolean = false,
    @SerialName("dm_policy") val dmPolicy: String = "everyone"
)

@Serializable
data class AccountLimits(
    @SerialName("max_media_per_post") val maxMediaPerPost: Int = 10
)

@Serializable
data class SocialAccount(
    @SerialName("user_id") val userId: String = "",
    val profile: SocialProfileRecord? = null,
    val author: SocialAuthor? = null,
    @SerialName("staff_role") val staffRole: String? = null,
    @SerialName("is_owner") val isOwner: Boolean = false,
    @SerialName("is_super") val isSuper: Boolean = false,
    val entitlement: String = "free",
    val beta: Boolean = false,
    val suspended: Boolean = false,
    val limits: AccountLimits = AccountLimits()
)

@Serializable
data class SocialMediaItem(
    val id: String = "",
    val kind: String = "image",
    val path: String = "",
    val thumb: String? = null,
    val w: Int? = null,
    val h: Int? = null
)

@Serializable
data class SocialPost(
    val id: String = "",
    val caption: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val removed: Boolean = false,
    val author: SocialAuthor = SocialAuthor(),
    val media: List<SocialMediaItem> = emptyList(),
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    val liked: Boolean = false,
    val saved: Boolean = false,
    val views: Int? = null
)

@Serializable
data class SocialComment(
    val id: String = "",
    @SerialName("post_id") val postId: String = "",
    @SerialName("parent_id") val parentId: String? = null,
    val body: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val author: SocialAuthor = SocialAuthor(),
    @SerialName("like_count") val likeCount: Int = 0,
    val liked: Boolean = false,
    @SerialName("reply_count") val replyCount: Int = 0
)

@Serializable
data class LastMessagePreview(
    val id: String = "",
    @SerialName("sender_id") val senderId: String = "",
    val body: String? = null,
    @SerialName("has_attachment") val hasAttachment: Boolean = false,
    val deleted: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class SocialConversation(
    val id: String = "",
    val status: String = "accepted",
    val other: SocialAuthor = SocialAuthor(),
    @SerialName("other_status") val otherStatus: String = "accepted",
    val blocked: Boolean = false,
    val last: LastMessagePreview? = null,
    val unread: Int = 0
)

@Serializable
data class DirectMessage(
    val id: String = "",
    @SerialName("sender_id") val senderId: String = "",
    val mine: Boolean = false,
    val body: String? = null,
    @SerialName("attachment_path") val attachmentPath: String? = null,
    val deleted: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class UserProfile(
    val id: String,
    val username: String = "",
    val email: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val role: String = "free",
    @SerialName("created_at") val createdAt: String = ""
) {
    val parsedRole: UserRole get() = UserRole.fromString(role)
}

enum class EnhancementQuality(val label: String, val targetShortSidePx: Int?) {
    ORIGINAL("Original", null),
    P720("720p", 720),
    P1080("1080p", 1080),
    K2("2K", 1440),
    K4("4K", 2160),
    K8("8K", 4320);

    val targetLongEdgePx: Int?
        get() = when (this) {
            ORIGINAL -> null
            P720 -> 1280
            P1080 -> 1920
            K2 -> 2560
            K4 -> 3840
            K8 -> 7680
        }
}

/**
 * Output frame-rate choices for the Video Enhancer.
 * [SOURCE] keeps the clip's own rate. The fixed rates can only LOWER the frame rate (frames are dropped,
 * never invented), so which of them are offered depends on the detected source rate; see VideoFpsPolicy.
 */
enum class VideoFpsOption(val fps: Int, val label: String) {
    SOURCE(0, "Source FPS"),
    FPS_30(30, "30 FPS"),
    FPS_60(60, "60 FPS"),
    FPS_90(90, "90 FPS"),
    FPS_120(120, "120 FPS")
}
