lazy val akkaHttpVersion = "10.2.2"
lazy val akkaVersion     = "2.6.10"
lazy val circeVersion    = "0.13.0"
lazy val tapirVersion    = "0.17.1"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.michael.rtb",
      scalaVersion    := "2.13.3"
    )),
    name := "akka_bidding_agent",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "ch.qos.logback"    % "logback-classic"           % "1.2.3",
      "de.heikoseeberger" %% "akka-http-circe"          % "1.31.0",

      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser"  % circeVersion,

      "mysql"                    % "mysql-connector-java" % "8.0.17",
      "io.getquill"              %% "quill-jdbc-monix"    % "3.5.0",
      "com.softwaremill.macwire" %% "macros"  % "2.3.7"   % "provided",

      "com.softwaremill.sttp.tapir" %% "tapir-core"             % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"       % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-akka-http-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-server-tests"     % tapirVersion,

      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.0.8"         % Test,
      "org.specs2"        %% "specs2-core"              % "4.10.0"        % Test,
      "org.scalatestplus" %% "mockito-3-4"              % "3.2.2.0"       % Test,
      "org.scalamock"     %% "scalamock"                % "5.1.0"         % Test
    )
  )
