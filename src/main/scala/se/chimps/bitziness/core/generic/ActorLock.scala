package se.chimps.bitziness.core.generic

import java.util.concurrent.atomic.AtomicBoolean

import scala.collection.concurrent.TrieMap

/**
 *
 */
object ActorLock {
  private val locks:TrieMap[String, AtomicBoolean] = TrieMap()

  def apply(path:String):Option[AtomicBoolean] = {
    locks.putIfAbsent(path, new AtomicBoolean(true))
  }

  def get(path:String):AtomicBoolean = {
    locks(path)
  }
}
