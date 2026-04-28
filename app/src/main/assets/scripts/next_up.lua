-- Configuration
local SILENCE_THRESHOLD = -50 -- dB
local SILENCE_DURATION = 2.0  -- seconds
local FALLBACK_PERCENTAGE = 0.88
local MAX_CREDITS_LENGTH = 120 -- seconds
local MIN_ACTIVATION_PERCENTAGE = 0.70

local triggered = false
local silence_start_time = nil

-- Reset state on file load
mp.register_event("file-loaded", function()
    triggered = false
    silence_start_time = nil
    mp.set_property_native("user-data/mpvex/trigger_next_up", "no")
    
    -- Check if it's a video file (simple check)
    local vid = mp.get_property_native("vid")
    if vid and vid ~= "no" then
        -- Add audio filter for silence detection near the end
        -- Using astats filter which provides RMS_level in metadata
        mp.commandv("af", "add", "lavfi=[astats=metadata=1:reset=1]")
    end
end)

local function trigger_signal(reason)
    if triggered then return end
    triggered = true
    mp.set_property_native("user-data/mpvex/trigger_next_up", "yes")
    print("Next Up triggered by: " .. reason)
end

local function check_next_up()
    if triggered then return end

    local duration = mp.get_property_number("duration")
    local time_pos = mp.get_property_number("time-pos")
    
    if not duration or not time_pos or duration <= 0 then return end
    
    local progress = time_pos / duration
    
    -- Only activate after certain percentage to avoid false positives in the middle
    if progress < MIN_ACTIVATION_PERCENTAGE then return end

    -- 1. Duration-based fallback
    local credits_start_fallback = math.max(duration * FALLBACK_PERCENTAGE, duration - MAX_CREDITS_LENGTH)
    if time_pos >= credits_start_fallback then
        trigger_signal("fallback_reached")
        return
    end

    -- 2. Real-time silence detection
    local audio_level = mp.get_property_native("af-metadata/lavfi.astats.Overall.RMS_level")
    
    if audio_level and tonumber(audio_level) then
        local level = tonumber(audio_level)
        if level < SILENCE_THRESHOLD then
            if silence_start_time == nil then
                silence_start_time = mp.get_time()
            elseif mp.get_time() - silence_start_time >= SILENCE_DURATION then
                trigger_signal("silence_detected")
            end
        else
            silence_start_time = nil
        end
    else
        silence_start_time = nil
    end
end

-- Efficient polling (every 500ms)
mp.add_periodic_timer(0.5, check_next_up)
