package com.dataflow.flink

import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala._
import org.slf4j.LoggerFactory

import scala.util.Random

object VarianceApp {

  @transient lazy private val log = LoggerFactory.getLogger(getClass.getName)

  def main(args: Array[String]) {

    // --- configuration

    val switches = ParameterTool.fromArgs(args)
    val propertiesFilePath = switches.get("properties")
    val config = ParameterTool.fromPropertiesFile(propertiesFilePath)

    val mode = config.get("mode")
    val interval = config.getInt("stream.interval")
    val numEventsInInterval = config.getInt("stream.numEventsInInterval")

    val parallelism = config.getInt("stream.parallelism")

    val numMin = config.getInt("nums.min")
    val numRange = config.getInt("nums.range")

    // --- stream processing

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val randomNums = env.addSource(new RandomIntStream(interval, numEventsInInterval, numMin, numRange))

    mode match {
      case "MINIMAL_VARIANCE" =>
        randomNums
          .map(num => {
            sleep(num)
            num
          })
          .setParallelism(parallelism)

      case "STRAGGLER" =>
        val waitRange = config.getInt("straggler.waitRange")
        val waitMillis = config.getInt("straggler.waitMillis")

        var toProcess = 0

        def resetToProcess(): Unit = {
          toProcess = Random.nextInt(waitRange)
        }

        def timeToWait(): Boolean = {
          toProcess -= 1
          (toProcess <= 0)
        }

        randomNums
          .map(new RichMapFunction[Int,Int] {
            def map(num: Int): Int = {
              val subTaskId = getRuntimeContext().getIndexOfThisSubtask()

              if (subTaskId == 0 && timeToWait()) {
                resetToProcess()
                sleep(waitMillis)
              }
              else {
                sleep(num)
              }
              num
            }
          })
          .setParallelism(parallelism)

      case _ =>
        log.error(s"Failed to recognise mode - $mode")
        throw new RuntimeException(s"Invalid mode $mode")
    }

    def sleep(delay: Int): Unit = {
      try {
        Thread.sleep(delay.toLong)
      }
      catch {
        case _ => // ignore
          log.warn("Sleep failed!!!!!")
      }
    }

    env.execute(s"Flink variance mode = $mode")
  }
}