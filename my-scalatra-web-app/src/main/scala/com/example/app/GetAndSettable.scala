package com.example.app

trait GetAndSettable {
  def get(key: String): Option[String]
  def set(key: String, value: String): Boolean
}
