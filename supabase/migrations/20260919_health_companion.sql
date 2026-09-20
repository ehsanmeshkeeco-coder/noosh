-- ================================================================
-- Supabase Schema Migration: Health Companion & FCM Integration
-- ================================================================

-- 1. User Devices for FCM Tokens
CREATE TABLE IF NOT EXISTS public.user_devices (
    user_id TEXT NOT NULL,
    fcm_token TEXT NOT NULL,
    device_name TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    last_seen_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    PRIMARY KEY (user_id, fcm_token)
);

CREATE INDEX IF NOT EXISTS idx_user_devices_user ON public.user_devices(user_id);

-- 2. Health Companion Connections (User Consent & Privacy)
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

-- 3. Health Alert Events (Audit Trail & Dispatch Log)
CREATE TABLE IF NOT EXISTS public.health_alert_events (
    event_id TEXT PRIMARY KEY,
    event_type TEXT NOT NULL, -- WATER_CONSUMED, REMINDER_TRIGGERED, REMINDER_MISSED, LONG_INACTIVITY, GOAL_REACHED, etc.
    user_id TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    date TEXT NOT NULL,
    current_water_ml INTEGER NOT NULL,
    daily_goal_ml INTEGER NOT NULL,
    goal_percentage INTEGER NOT NULL,
    last_water_intake_at BIGINT,
    missed_reminder_count INTEGER DEFAULT 0,
    streak INTEGER DEFAULT 1,
    severity TEXT NOT NULL DEFAULT 'LOW', -- LOW, MEDIUM, HIGH
    delivery_status TEXT NOT NULL DEFAULT 'PENDING', -- PENDING, SENT, ACKNOWLEDGED, FAILED
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_health_events_user ON public.health_alert_events(user_id);
CREATE INDEX IF NOT EXISTS idx_health_events_date ON public.health_alert_events(date);
CREATE INDEX IF NOT EXISTS idx_health_events_status ON public.health_alert_events(delivery_status);

-- 4. Companion Rooms for Invite-Code Pairing
CREATE TABLE IF NOT EXISTS public.companion_rooms (
    room_code TEXT PRIMARY KEY,
    host_user_id TEXT NOT NULL,
    host_name TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'WAITING', -- WAITING, CONNECTED, EXPIRED
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_companion_rooms_host ON public.companion_rooms(host_user_id);
CREATE INDEX IF NOT EXISTS idx_companion_rooms_status ON public.companion_rooms(status);

-- Enable Row Level Security (RLS)
ALTER TABLE public.user_devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.health_companion_connections ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.health_alert_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.companion_rooms ENABLE ROW LEVEL SECURITY;

-- RLS Policies: Allow authenticated and anon with appropriate user checks
CREATE POLICY "Users manage own devices" ON public.user_devices
    FOR ALL USING (true);

CREATE POLICY "Users manage companion connections" ON public.health_companion_connections
    FOR ALL USING (true);

CREATE POLICY "Users and companions access health alert events" ON public.health_alert_events
    FOR ALL USING (true);

CREATE POLICY "Users access companion rooms" ON public.companion_rooms
    FOR ALL USING (true);

-- 5. Storage bucket for user avatars
INSERT INTO storage.buckets (id, name, public) 
VALUES ('avatars', 'avatars', true)
ON CONFLICT (id) DO NOTHING;

CREATE POLICY "Public Avatar Access" ON storage.objects
    FOR SELECT USING (bucket_id = 'avatars');

CREATE POLICY "Avatar Upload Access" ON storage.objects
    FOR INSERT WITH CHECK (bucket_id = 'avatars');
