# exercise-proxy-redis
Exercise for proxy for redis
# Proxy for Redis
We build a proxy for redis using `Scala`, a web microframework called `Scalatra`, and `Docker`. We have tests in Scalatest and support Parallel Concurrency using Scala `Future`s, and a concurrent request cap using a Java `Semaphore`. We also support caching (Global expiry, LRU, fixed key size) using `Google Guava Cache`, a battle tested implementation backed by a Java `HashMap`.

For simplicity we only work with the Redis Commands `GET` and `SET`, which deals only with string keys and values. A further enhancement would allow arbitrary data structures as values and access of those values.

# Build and Run Tests
In this top-level directory, make sure you have docker installed and issue the following in bash:

```
make test
```

This will pull the docker container, compile and build the service and execute tests in the container and exit. You will see the `sbt` output if successful.

# HTTP Interface
```
GET /keys/:id # gets contents of key :id and returns 200 OK and JSON {"key":..., "value":...} or 404 Not Found
PUT /keys/:id # sets contents of key :id with value the string at key "value" in the JSON request body and returns 200 OK and JSON {"key":..., "value":...} or 400 Bad Request.

Error cases:
503 Service Unavailable if either the concurrency cap is reached or there is a system error with `set`.
```

# Config
The following settings are tunable in `./my-scalatra-web-app/src/main/resources/application.json`.

```json
{
  "redisHost": "localhost",
  "redisPort": 6379,
  "cacheExpiryTimeSeconds": 1000,
  "cacheCapacity": 1000,
  "httpPort": 80,
  "maximumConcurrentConnections": 10000
}
```

# Architecture
## Cache
We use a simple local cache using google guava cache. It supports caching by string keys and values, global expiry, and LRU eviction and fixed key size. It is backed by an in memory Java `HashMap` so will have the usual (amortized) `O(1)` `get` and `set`.

Its behavior is the usual LRU cache. In particular, if the key is missing when you call `get`, it will call its `load` function to try to get it from the Redis Cache. If successful, it stores the value and returns, or throws an exception. Further down the call stack we catch the exception in a Scala `Try` and deal with it.

## Parallel Concurrent Processing using Futures
We use Scalatra with `Future` support. Futures are configured with a thread pool. We use the `default ForkJoin Pool` with default thread count. We enforce maximum limits using a Java `Semaphore`. The `Semaphore` is checked at the top of each request. The request will try to acquire the semaphore, and if it can't immediately acquire it, it will exit with a 503. In particular, the request will not wait for a freed Semaphore. Each request will release the semaphore at its end.

For simplicity, the Redis Client we use is still a single connection, shared between all requests, as Redis itself is single-threaded. A future enhancement is the use of Redis connection pool.

## System tests
We use `ScalaTest`. Our tests, executed using `sbt test`, spin up a working service (server and all) and execute HTTP Requests. We mock out Redis, instead hitting a mock that supports the `GetAndSettable` trait. The real Redis client is wrapped in this trait.

## Docker
We use the latest `mozilla/sbt` Docker image to build and run tests. It contains `sbt`. While running the container, we mount the top-level directory and run `sbt test`. This downloads all the dependencies if missing and builds and runs tests.

## Startup and Controller
The 4 main pieces of the service are: a Redis Client, a Guava Cache, request Semaphore, and a controller called `MyScalatraServlet`. (Please excuse the boilerplate naming)

The entrypoint is `ScalatraBootstrap.init()`, found in `./my-scalatra-web-app/src/main/scala/ScalatraBootstrap.scala`. Here we read configs from `application.conf`, and instantiate each of the 4 pieces above.

The Redis Client is a single blocking connection shared among all requests, chosen for simplicity. It is wrapped in a trait `GetAndSettable`, so that it can easily be mocked for testing.

```scala
trait GetAndSettable {
  def get(key: String): Option[String]
  def set(key: String, value: String): Boolean
}

class RedisClientAsGetAndSettable(redisClient: RedisClient) extends GetAndSettable {
  override def get(key: String): Option[String] = redisClient.get(key)
  override def set(key: String, value: String): Boolean = redisClient.set(key, value)
}
```

We instantiate the global (shared among all requests) semaphore and the controller here:
```scala
val requestSemaphore = new Semaphore(maximumConcurrentConnections)
context.mount(new MyScalatraServlet(t, cache, requestSemaphore), "/*")
```

The Guava cache is instantiated with cache capacity, global expiry, and default LRU eviction using the following builder:
```scala
val t = new RedisClientAsGetAndSettable(new RedisClient(redisHost, redisPort))
val cache = CacheBuilder.newBuilder()
  .maximumSize(cacheCapacity)
  .expireAfterWrite(cacheExpiryTimeSeconds, TimeUnit.SECONDS)
  .build[String, String](
    new CacheLoader[String, String]() {
      def load(key: String): String = {
        t.get(key) match {
          case Some(value) => value
          case None => throw new Exception(s"Cannot find $key")
        }
      }
    })
```

Within the controller `MyScalatraServlet`, we have the usual `get` and `put` routes, wrapping the internal `get` and `set` in a Scala `Try`. Notice that even before anything, the request will try to acquire the semaphore. If it cannot immediately acquire it, it will exit with a 503; it will not wait. If it does get the semaphore, it will execute the request concurrently using a `Future`. Upon finish, it will release the semaphore.

## Omitted Requirements
### Redis Client Protocol
Would take too much time to look up how to configure non-HTTP Requests in Scalatra.

## Time Spent
About 10 hours over 5 days, at least a good third of it reading docs for Scalatra, Docker, Scala Redis Client, and Google Guava cache. 

