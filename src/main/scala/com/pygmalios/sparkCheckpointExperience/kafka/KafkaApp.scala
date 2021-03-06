package com.pygmalios.sparkCheckpointExperience.kafka

import java.util.TimerTask
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

import com.pygmalios.sparkCheckpointExperience.kafka.embedded.{EmbeddedKafka, RunningEmbeddedKafka}
import jline.ConsoleReader

import scala.annotation.tailrec

/**
  * Embedded Apache Kafka.
  */
object KafkaApp extends App with EmbeddedKafka {
  private val counter = new AtomicInteger()
  private val negativeFlag = new AtomicBoolean()

  // Zookeeper port
  lazy val zookeeperPort = 6000

  // Kafka Server pokrt
  lazy val kafkaServerPort = 6001

  // How many seconds at minimum have to be left in the topic after log cleaning
  lazy val retentionSec = 15

  // How often to check the log for retention
  lazy val retentionCheckSec = 15

  // Length of a single segment in the topic in number of seconds
  lazy val segmentationSec = 1

  // Name of the topic
  lazy val topic = "Experience"

  // Start Zookeeper and Kafka, create topic
  withKafka { kafka =>
    // Publish a message every 1 second
    val timerTask = schedulePublishing(kafka, 1000)
    try {
      readFromConsole()
    }
    finally {
      timerTask.cancel()
    }
  }

  private def readFromConsole(): Unit = {
    val consoleReader = new ConsoleReader()

    @tailrec
    def loop(): Unit = {
      consoleReader.readVirtualKey() match {
        case '\n' =>
          negativeFlag.set(true)
          loop()

        case other =>
          log.debug(other.toString)
      }
    }

    loop()
  }

  private def schedulePublishing(kafka: RunningEmbeddedKafka, everyMs: Int): TimerTask = {
    val t = new java.util.Timer()
    val task = new java.util.TimerTask {
      def run(): Unit = {
        val key = counter.addAndGet(1)
        val c = if (negativeFlag.getAndSet(false)) -1 else 1
        kafka.publish(topic, (c * key).toString, key.toString)
      }
    }

    t.schedule(task, everyMs, everyMs)
    task
  }
}