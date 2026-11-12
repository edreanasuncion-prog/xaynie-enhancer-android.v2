# X Studio — API & Backend Setup Guide

This document inventories every backend service, Supabase RPC/table/bucket, and environment variable used by X Studio based on inspecting `source/backend/index.ts`, `source/src/lib.ts`, `source/src/ai.ts`, and `source/src/social/db.ts`.

## 1. Configuration Variable Names (No Secret Values Embedded)

| Variable Name | Location | Visibility | Purpose |
| :--- | :--- | :--- | :--- |
| `SUPABASE_URL` | `local.properties` / Codemagic `xstudio_config` | Public Client Config | Existing X Studio Supabase project URL. |
| `SUPABASE_ANON_KEY` | `local.properties` / Codemagic `xstudio_config` | Public Client Config | Supabase anonymous/publishable key. Protected by Postgres Row Level Security (RLS). |
| `XSTUDIO_API_BASE_URL` | `local.properties` / Codemagic `xstudio_config` | Public Client Config | Base URL of the deployed `source/backend/index.ts` server for `/api/config`, `/api/ask`, and `/api/cobalt-download`. |
| `GEMINI_API_KEY` | Server Environment Only (`source/backend/index.ts`) | **PRIVATE SERVER SECRET** | Used exclusively on the backend by `POST /api/ask`. Never placed in the Android APK. |
| `COBALT_API_URL` | Server Environment Only (`source/backend/index.ts`) | Server Config | Cobalt media API endpoint (defaults to `https://xaynie-cobalt-api.onrender.com/`). |
| `COBALT_API_KEY` | Server Environment Only (`source/backend/index.ts`) | **PRIVATE SERVER SECRET** | Optional Cobalt API key on the server. |

## 2. Reused Supabase Schema & Endpoints (`001_x_social.sql`)

- **Storage Buckets**:
  - `social-public`: Public avatars (`avatars/{user_id}/{ts}.{ext}`) and rank badges (`badges/{rank}-{ts}.{ext}`).
  - `social-media`: Private post media and 24-hour stories (accessed via signed URLs).
  - `social-dm`: Private direct message photo attachments (accessed via signed URLs).
- **Database Tables**:
  - `social_profiles`, `social_posts`, `social_likes`, `social_comment_likes`, `social_saved_posts`, `social_comments`, `social_follows`, `social_blocks`, `social_reports`, `social_messages`, `social_badges`, `social_config`, `social_notifications`.
- **Database RPC Functions**:
  - `social_my_account`, `social_ensure_profile`, `social_unread_counts`, `social_story_cleanup`, `social_feed`, `social_user_posts`, `social_saved_page`, `social_explore_media`, `social_explore_topics`, `social_search_users`, `social_record_view`, `social_create_post`, `social_post_get`, `social_comments_page`, `social_follow_requests`, `social_respond_follow`, `social_notifications_page`, `social_notifications_mark_read`, `social_conversations_list`, `social_conversation_get`, `social_messages_page`, `social_mark_read`, `social_delete_message`, `social_respond_request`, `social_clear_conversation`, `social_profile_get`, `social_follow_list`, `social_start_conversation`, `social_blocked_list`, `social_user_stories`, `social_story_tray`, `social_story_view`, `social_story_viewers`, `social_story_delete`, `social_story_create`, `social_moderate`, `social_admin_reports`, `social_admin_resolve_report`, `social_admin_users`, `social_set_rank`, `social_set_staff_role`, `social_grant_subscription`, `social_revoke_subscription`, `social_set_badge_image`, `social_set_config`, `social_transfer_ownership`, `social_audit_page`.
- **Backend HTTP Endpoints (`source/backend/index.ts`)**:
  - `GET /api/_healthcheck`
  - `GET /api/config`
  - `POST /api/ask` (models: `x-ai-3-1`, `x-ai-4-5`, `xaynie-ai-5-5-astral`)
  - `POST /api/cobalt-download`
