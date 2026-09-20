// Supabase Edge Function: health-companion-dispatch
// Handles health alert event routing to Health Companions via Firebase Cloud Messaging (FCM HTTP v1)
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.38.4";

interface HealthAlertPayload {
  event_id: string;
  event_type: string;
  user_id: string;
  timestamp: number;
  date: string;
  current_water_ml: number;
  daily_goal_ml: number;
  goal_percentage: number;
  last_water_intake_at?: number;
  missed_reminder_count?: number;
  streak?: number;
  severity: "LOW" | "MEDIUM" | "HIGH";
  delivery_status: string;
}

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    const fcmServerKey = Deno.env.get("FIREBASE_SERVER_KEY") ?? "";

    const supabase = createClient(supabaseUrl, supabaseServiceKey);

    const payload: HealthAlertPayload = await req.json();
    const { event_id, event_type, user_id, severity, current_water_ml, daily_goal_ml, goal_percentage } = payload;

    if (!event_id || !user_id || !event_type) {
      return new Response(JSON.stringify({ error: "Missing required event fields" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // 1. Fetch active companion connections with explicit user consent (Section 58)
    const { data: connections, error: connError } = await supabase
      .from("health_companion_connections")
      .select("*")
      .eq("user_id", user_id)
      .eq("status", "CONNECTED");

    if (connError) {
      console.error("Error fetching companion connections:", connError);
      return new Response(JSON.stringify({ error: connError.message }), {
        status: 500,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    if (!connections || connections.length === 0) {
      // No active companions; mark event as ACKNOWLEDGED locally
      await supabase
        .from("health_alert_events")
        .update({ delivery_status: "ACKNOWLEDGED" })
        .eq("event_id", event_id);

      return new Response(JSON.stringify({ message: "No active companions connected. Event logged." }), {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // 2. Filter companions based on Notification Policy (Section 57)
    const eligibleCompanions = connections.filter((conn) => {
      if (conn.alert_policy === "ALL") return true;
      if (conn.alert_policy === "MEDIUM_AND_HIGH") return severity === "MEDIUM" || severity === "HIGH";
      if (conn.alert_policy === "HIGH_ONLY") return severity === "HIGH";
      return true;
    });

    if (eligibleCompanions.length === 0) {
      await supabase
        .from("health_alert_events")
        .update({ delivery_status: "ACKNOWLEDGED" })
        .eq("event_id", event_id);

      return new Response(JSON.stringify({ message: "Filtered by companion alert policy" }), {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // 3. For each eligible companion, find active FCM tokens from user_devices
    const companionUserIds = eligibleCompanions.map((c) => c.companion_user_id);
    const { data: devices } = await supabase
      .from("user_devices")
      .select("fcm_token, user_id")
      .in("user_id", companionUserIds)
      .eq("is_active", true);

    const fcmTokens = devices?.map((d) => d.fcm_token) ?? [];

    if (fcmTokens.length === 0) {
      // Tokens not found or companion offline; event saved in DB for polling
      await supabase
        .from("health_alert_events")
        .update({ delivery_status: "SENT" })
        .eq("event_id", event_id);

      return new Response(JSON.stringify({ message: "No active FCM tokens found for companion devices" }), {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // 4. Dispatch FCM Notification
    const titleMap: Record<string, string> = {
      WATER_CONSUMED: "ثبت مصرف آب توسط کاربر",
      REMINDER_TRIGGERED: "ارسال یادآور به کاربر",
      REMINDER_MISSED: "⚠️ هشدار: یادآور آب بی‌پاسخ ماند",
      LONG_INACTIVITY: "🚨 هشدار بحرانی: عدم فعالیت طولانی",
      GOAL_REACHED: "🎉 تبریک: هدف مصرف آب روزانه محقق شد",
    };

    const notificationTitle = titleMap[event_type] || "گزارش وضعیت سلامت کاربر";
    const notificationBody = `مصرف امروز: ${current_water_ml} از ${daily_goal_ml} میلی‌لیتر (${goal_percentage}٪)`;

    let sentCount = 0;

    for (const token of fcmTokens) {
      try {
        if (fcmServerKey) {
          const fcmResponse = await fetch("https://fcm.googleapis.com/fcm/send", {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `key=${fcmServerKey}`,
            },
            body: JSON.stringify({
              to: token,
              notification: {
                title: notificationTitle,
                body: notificationBody,
                sound: "default",
              },
              data: {
                event_id,
                event_type,
                user_id,
                severity,
                current_water_ml: current_water_ml.toString(),
                daily_goal_ml: daily_goal_ml.toString(),
                goal_percentage: goal_percentage.toString(),
              },
            }),
          });

          if (fcmResponse.ok) {
            sentCount++;
          }
        }
      } catch (fcmErr) {
        console.error("FCM dispatch error:", fcmErr);
      }
    }

    // 5. Update Audit Trail status
    await supabase
      .from("health_alert_events")
      .update({
        delivery_status: sentCount > 0 ? "SENT" : "ACKNOWLEDGED",
      })
      .eq("event_id", event_id);

    return new Response(
      JSON.stringify({
        success: true,
        event_id,
        sent_count: sentCount,
        eligible_companions: eligibleCompanions.length,
      }),
      {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      }
    );
  } catch (err: any) {
    console.error("Edge function error:", err);
    return new Response(JSON.stringify({ error: err.message }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
