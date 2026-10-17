-- Add job to queue unless it's already finished
if redis.call('EXISTS', KEYS[2]) == 1 then
	return 0
end
return redis.call('ZADD', KEYS[1], ARGV[1], ARGV[2])