import sbt._

object Dependencies {

  object Versions {
    val opinionatedZio      = "0.0.3" // Brings in a lot of ZIO things
    val zio                 = "2.0.15"
    val zioLogging          = "2.1.12"
    val zioTelemetry        = "3.0.0-RC10"
    val zioMetricsConnector = "2.0.8"
    val logback             = "1.4.7"
    val rmq                 = "5.18.0"
    val hop                 = "5.0.0"
    val zioHttp             = "3.0.0-RC2"
  }

  val common: Seq[ModuleID] = Seq(
    "com.alterationx10" %% "opinionated-zio"      % Versions.opinionatedZio,
    "com.alterationx10" %% "opinionated-zio-test" % Versions.opinionatedZio % Test,
    "dev.zio"           %% "zio-test-sbt"         % Versions.zio
  )

  val sms: Seq[ModuleID] = Seq(
    "dev.zio"       %% "zio-logging"            % Versions.zioLogging,
    "dev.zio"       %% "zio-logging-slf4j2"     % Versions.zioLogging,
    "dev.zio"       %% "zio-opentelemetry"      % Versions.zioTelemetry,
    "dev.zio"       %% "zio-metrics-connectors" % Versions.zioMetricsConnector,
    "dev.zio"       %% "zio-http"               % Versions.zioHttp,
    "ch.qos.logback" % "logback-classic"        % Versions.logback,
    "com.rabbitmq"   % "amqp-client"            % Versions.rmq,
    "com.rabbitmq"   % "http-client"            % Versions.hop
  ) ++ common

  val smsTests: Seq[ModuleID] = Seq(
    "com.dimafeng" %% "testcontainers-scala-rabbitmq" % "0.40.17"
  ) ++ common

}
