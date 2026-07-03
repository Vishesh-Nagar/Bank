-- sliding_window_rate_limit.lua
-- Implements the Sliding Window Log rate limiting algorithm using Redis Sorted Sets.
--
-- KEYS[1] : Rate limit key (e.g., rate_limit:ip:192.168.1.1)
-- ARGV[1] : Current timestamp in milliseconds
-- ARGV[2] : Window size in milliseconds
-- ARGV[3] : Maximum allowed requests in the window
--
-- Returns 1 if allowed, 0 if denied (limit exceeded).

local key = KEYS[1]
local now_ms = tonumber(ARGV[1])
local window_ms = tonumber(ARGV[2])
local max_requests = tonumber(ARGV[3])

local clear_before = now_ms - window_ms

-- 1. Remove all requests that fall outside the current sliding window
redis.call('ZREMRANGEBYSCORE', key, 0, clear_before)

-- 2. Count the remaining requests (which are perfectly within the sliding window)
local current_count = redis.call('ZCARD', key)

if current_count < max_requests then
    -- 3a. If within limit, record this new request's timestamp.
    -- We use the timestamp as both the score (for sorting/removing) and the member.
    redis.call('ZADD', key, now_ms, now_ms)
    
    -- 4. Set expiration on the entire set to equal the window size.
    -- This guarantees that inactive keys are automatically garbage collected,
    -- preventing memory leaks for transient IPs/users.
    redis.call('PEXPIRE', key, window_ms)
    
    return 1 -- Allowed
else
    -- 3b. Over limit
    return 0 -- Denied
end
