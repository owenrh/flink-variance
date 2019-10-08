### Flink Variance
This project contains Flink job definitions that use synthetic data to create four different backpressure scenarios used in the [Stream Processing Backpressure Smackdown](http://owenrh.me.uk/blog/2019/09/30/).

### Running Flink
There are a number of ways to install and run Spark depending on your available hardware. I am just going to cover running it locally on a Mac laptop.

#### Install Flink
Download the version in question from the [Flink  website](https://flink.apache.org/downloads). I used v1.8.1.

#### (Optional) Install InfluxDB and Chronograf
These can be installed via Brew, and run up as normal Brew services, e.g. `brew services start influxdb/chronograf`.

Then create a database for the metrics, and name it _flink_ or something along those lines.

#### Configure Flink
When configuring Flink locally you have a couple of options:
1. Use a single worker.
1. Use multiple workers and therefore JVMs (more closely mimicking a distributed setup).

For the purposes of the smackdown, I found the results from both configurations to be the same.

The number of workers is configured in `FLINK_HOME/conf/slaves`. By default, it will run a single worker. If you want more then add repeated lines of localhost. For four workers use:
```
localhost
localhost
localhost
localhost
```

You may also need to increase the number of task slots available your workers, especially if using a single worker. Our scenarios will require a minimum of four slots, due to the four parallel dataflow tasks.

The worker (Task Manager) slots can be configured via the `FLINK_HOME/conf/flink-conf.yaml`. For example, to increase 
```
taskmanager.numberOfTaskSlots: 4
```

If using InfluxDB for metrics collection then the following lines need to be added to the `flink-conf.yaml`:
```
metrics.reporter.influxdb.class: org.apache.flink.metrics.influxdb.InfluxdbReporter
metrics.reporter.influxdb.host: localhost
metrics.reporter.influxdb.port: 8086
metrics.reporter.influxdb.db: flink
metrics.reporter.influxdb.retentionPolicy: one_hour
```

#### Run Flink
Finally, we run up the Flink cluster on our local machine:
```
start-cluster.sh
```

### Running scenarios
First build this project using `mvn clean package`. Then run the appropriate scenario using:
```
flink run target/flink-variance-1.0-SNAPSHOT.jar --properties <scenario name>.properties
```