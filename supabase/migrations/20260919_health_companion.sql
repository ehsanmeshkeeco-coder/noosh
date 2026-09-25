-- ================================================================
-- Supabase Schema Migration: Complete Noosh Backend Schema
-- ================================================================

-- 1. Profiles Table (Synced from Clerk & Onboarding)
CREATE TABLE IF NOT EXISTS public.profiles (
    id TEXT PRIMARY KEY,
    clerk_user_id TEXT,
    name TEXT NOT NULL,
    email TEXT NOT NULL,
    profile_image_url TEXT,
    daily_water_goal_ml INTEGER NOT NULL DEFAULT 2000,
    weight_kg NUMERIC,
    wake_time TEXT,
    sleep_time TEXT,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_profiles_email ON public.profiles(email);
CREATE INDEX IF NOT EXISTS idx_profiles_clerk_id ON public.profiles(clerk_user_id);

-- 2. Water Intakes Table (Offline-First Remote Sync)
CREATE TABLE IF NOT EXISTS public.water_intakes (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    amount_ml INTEGER NOT NULL,
    consumed_at BIGINT NOT NULL,
    source TEXT NOT NULL DEFAULT 'app_quick',
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_water_intakes_user ON public.water_intakes(user_id);
CREATE INDEX IF NOT EXISTS idx_water_intakes_consumed ON public.water_intakes(consumed_at);

-- 3. Health Sync Events (Aggregated Metrics & Daily Summaries)
CREATE TABLE IF NOT EXISTS public.health_sync_events (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    daily_intake_ml INTEGER NOT NULL,
    daily_goal_ml INTEGER NOT NULL,
    streak_days INTEGER NOT NULL,
    timestamp BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_health_sync_user ON public.health_sync_events(user_id);

-- 4. User Devices for FCM Push Notifications
CREATE TABLE IF NOT EXISTS public.user_devices (
    user_id TEXT NOT NULL,
    fcm_token TEXT NOT NULL,
    device_name TEXT,
    platform TEXT DEFAULT 'android',
    app_version TEXT DEFAULT '1.0',
    is_active BOOLEAN DEFAULT TRUE,
    last_seen_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    PRIMARY KEY (user_id, fcm_token)
);

CREATE INDEX IF NOT EXISTS idx_user_devices_user ON public.user_devices(user_id);

-- 5. Health Companion Connections (User Consent & Privacy)
CREATE TABLE IF NOT EXISTS public.health_companion_connections (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    companion_user_id TEXT NOT NULL,
    companion_name TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'CONNECTED', -- CONNECTED, PAUSED, DISCONNECTED
    alert_policy TEXT NOT NULL DEFAULT 'ALL', -- ALL, MEDIUM_AND_HIGH, HIGH_ONLY
    last_active_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_companion_user ON public.health_companion_connections(user_id);
CREATE INDEX IF NOT EXISTS idx_companion_status ON public.health_companion_connections(status);

-- 6. Health Alert Events (Audit Trail & Dispatch Log)
CREATE TABLE IF NOT EXISTS public.health_alert_events (
    event_id TEXT PRIMARY KEY,
    event_type TEXT NOT NULL,
    user_id TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    date TEXT NOT NULL,
    current_water_ml INTEGER NOT NULL,
    daily_goal_ml INTEGER NOT NULL,
    goal_percentage INTEGER NOT NULL,
    last_water_intake_at BIGINT,
    missed_reminder_count INTEGER DEFAULT 0,
    streak INTEGER DEFAULT 1,
    severity TEXT NOT NULL DEFAULT 'LOW',
    delivery_status TEXT NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_health_events_user ON public.health_alert_events(user_id);
CREATE INDEX IF NOT EXISTS idx_health_events_date ON public.health_alert_events(date);
CREATE INDEX IF NOT EXISTS idx_health_events_status ON public.health_alert_events(delivery_status);

-- 7. Companion Rooms for Invite-Code Pairing
CREATE TABLE IF NOT EXISTS public.companion_rooms (
    room_code TEXT PRIMARY KEY,
    host_user_id TEXT NOT NULL,
    host_name TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'WAITING',
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_companion_rooms_host ON public.companion_rooms(host_user_id);
CREATE INDEX IF NOT EXISTS idx_companion_rooms_status ON public.companion_rooms(status);

-- Enable Row Level Security (RLS)
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.water_intakes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.health_sync_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.health_companion_connections ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.health_alert_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.companion_rooms ENABLE ROW LEVEL SECURITY;

-- RLS Policies: Allow authenticated & service_role access
CREATE POLICY "Public Profiles Access" ON public.profiles FOR ALL USING (true);
CREATE POLICY "Public Water Intakes Access" ON public.water_intakes FOR ALL USING (true);
CREATE POLICY "Public Health Sync Events Access" ON public.health_sync_events FOR ALL USING (true);
CREATE POLICY "Users manage own devices" ON public.user_devices FOR ALL USING (true);
CREATE POLICY "Users manage companion connections" ON public.health_companion_connections FOR ALL USING (true);
CREATE POLICY "Users access health alert events" ON public.health_alert_events FOR ALL USING (true);
CREATE POLICY "Users access companion rooms" ON public.companion_rooms FOR ALL USING (true);

-- 8. Storage bucket for user avatars
INSERT INTO storage.buckets (id, name, public) 
VALUES ('avatars', 'avatars', true)
ON CONFLICT (id) DO NOTHING;

CREATE POLICY "Public Avatar Access" ON storage.objects
    FOR SELECT USING (bucket_id = 'avatars');

CREATE POLICY "Avatar Upload Access" ON storage.objects
    FOR INSERT WITH CHECK (bucket_id = 'avatars');
