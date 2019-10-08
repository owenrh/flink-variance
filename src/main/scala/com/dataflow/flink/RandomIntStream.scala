package com.dataflow.flink

import org.apache.flink.streaming.api.functions.source.SourceFunction

import scala.util.Random

class RandomIntStream(val streamInterval: Int, val numEventsInInterval: Int, val numMin: Int, val numRange: Int) extends SourceFunction[Int] {
  private var isRunning: Boolean = true

  override def run(ctx: SourceFunction.SourceContext[Int] ): Unit = {
    while (isRunning) {
      for(_ <- 1 to numEventsInInterval) {
        ctx.collect(numMin + Random.nextInt(numRange))
      }

      try {
        Thread.sleep(streamInterval)
      }
      catch {
        case _: InterruptedException =>
        // ignore
      }
    }
  }

  override def cancel(): Unit = {
    isRunning = false
  }
}

