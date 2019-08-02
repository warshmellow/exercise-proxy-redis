package com.example.app

import com.redis.RedisClient

class RedisClientAsGetAndSettable(redisClient: RedisClient) extends GetAndSettable {
  override def get(key: String): Option[String] = redisClient.get(key)
  override def set(key: String, value: String): Unit = redisClient.set(key, value)
}
