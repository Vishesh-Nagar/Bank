-- transfer_limit.lua
--
-- Atomically checks the daily transfer limit and increments if within bounds.
-- All reads and writes happen in a single server-side round-trip, eliminating
-- the check-then-act race condition that exists in pure Java get/set patterns.
--
-- KEYS[1]  : Redis key, e.g. "transfer_limit:{accountId}:{date}"
-- ARGV[1]  : The transfer amount as a string (e.g. "250.00")
-- ARGV[2]  : The daily limit as a string (e.g. "5000.00")
-- ARGV[3]  : TTL in seconds (e.g. "172800" for 2 days)
--
-- Returns:
--   0  → transfer allowed; limit has been updated
--   1  → transfer denied; daily limit would be exceeded
--   current_total (as string) in both cases via a second element (not used here;
--   we keep return simple: 0=ok, 1=denied)

local current = redis.call('GET', KEYS[1])
local currentAmount = tonumber(current) or 0
local transferAmount = tonumber(ARGV[1])
local dailyLimit    = tonumber(ARGV[2])
local ttl           = tonumber(ARGV[3])

local newTotal = currentAmount + transferAmount

if newTotal > dailyLimit then
    return 1  -- denied
end

-- Increment atomically
redis.call('SET', KEYS[1], tostring(newTotal), 'EX', ttl)
return 0  -- allowed
