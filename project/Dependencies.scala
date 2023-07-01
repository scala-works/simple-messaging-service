import sbt._

object Dependencies {

  object Versions {
    val zio                 = "2.0.15"
    val zioConfig           = "3.0.7"
    val zioLogging          = "2.1.12"
    val zioMock             = "1.0.0-RC9"
    val zioTelemetry        = "3.0.0-RC10"
    val zioMetricsConnector = "2.0.8"
    val logback             = "1.4.7"
    val caliban             = "2.2.1"
    val rmq                 = "5.18.0"
    val hop                 = "5.0.0"
    val zioHttp             = "3.0.0-RC2"
  }

  val server: Seq[ModuleID] = Seq(
    "dev.zio"               %% "zio"                    % Versions.zio,
    "dev.zio"               %% "zio-streams"            % Versions.zio,
    "dev.zio"               %% "zio-test"               % Versions.zio,
    "dev.zio"               %% "zio-test-sbt"           % Versions.zio,
    "dev.zio"               %% "zio-test-magnolia"      % Versions.zio     % Test,
    "dev.zio"               %% "zio-mock"               % Versions.zioMock % Test,
    "dev.zio"               %% "zio-config"             % Versions.zioConfig,
    "dev.zio"               %% "zio-config-magnolia"    % Versions.zioConfig,
    "dev.zio"               %% "zio-config-typesafe"    % Versions.zioConfig,
    "dev.zio"               %% "zio-logging"            % Versions.zioLogging,
    "dev.zio"               %% "zio-logging-slf4j2"     % Versions.zioLogging,
    "dev.zio"               %% "zio-opentelemetry"      % Versions.zioTelemetry,
    "dev.zio"               %% "zio-metrics-connectors" % Versions.zioMetricsConnector,
    "dev.zio"               %% "zio-http"               % Versions.zioHttp,
    "ch.qos.logback"         % "logback-classic"        % Versions.logback,
    "com.github.ghostdogpr" %% "caliban"                % Versions.caliban,
    "com.github.ghostdogpr" %% "caliban-zio-http"       % Versions.caliban,
    "com.github.ghostdogpr" %% "caliban-tracing"        % Versions.caliban,
    "com.github.ghostdogpr" %% "caliban-tools"          % Versions.caliban,
    "com.github.ghostdogpr" %% "caliban-client"         % Versions.caliban,
    "com.rabbitmq"           % "amqp-client"            % Versions.rmq,
    "com.rabbitmq"           % "http-client"            % Versions.hop
  )

}
