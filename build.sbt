Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / scalaVersion := "3.3.0"
ThisBuild / organization := "works.scala"
ThisBuild / scalacOptions ++= Seq()
ThisBuild / testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

lazy val root = project
  .in(file("."))
  .aggregate(sms)

lazy val sms = project
  .in(file("sms"))
  .settings(
    name := "simple-messaging-service",
    libraryDependencies ++= Dependencies.sms,
    fork := true
  )

lazy val smsTest = project
  .in(file("sms-test"))
  .settings(
    name := "sms-tests",
    libraryDependencies ++= Dependencies.smsTests,
    fork := true
  )
  .dependsOn(sms)

lazy val docs = project
  .in(file(".site-docs"))
  .dependsOn(sms)
  .settings(
    mdocOut := file("./website/docs")
  )
  .enablePlugins(MdocPlugin)

addCommandAlias("fmt", "all root/scalafmtSbt root/scalafmtAll")
addCommandAlias("fmtCheck", "all root/scalafmtSbtCheck root/scalafmtCheckAll")
