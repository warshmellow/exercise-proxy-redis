# exercise-proxy-redis
exercise for proxy for redis
# Proxy for Redis
For simplicity we only work with the Redis Commands `GET` and `SET`, which deals only with string keys and values. A further enhancement would allow arbitrary data structures as values and access of those values.
## HTTP Interface
```
GET /key/:id # gets contents of key :id
PUT /key/:id # sets contents of key :id with value the entire request body
```

## Cache
We use a simple local cache using google guava cache. It supports caching by string key, global expiry, and LRU eviction and fixed key size.

## Parallel Concurrent Processing
We use Scalatra with Future support. Futures are configured with a thread pool. We use the default ForkJoin Pool with default thread count. We enforce maximum limits using a java semaphore.

We also use a connection pool for redis.

## Config

## System tests
Scalatest

## Docker
scala container

## Omitted Requirements
# Redis Client Protocol
Would take too much time to look up how to configure non-HTTP Requests in Scalatra.

