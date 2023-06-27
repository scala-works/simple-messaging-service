Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / scalaVersion := "3.3.0"
ThisBuild / organization := "works.scala"
ThisBuild / scalacOptions ++= Seq()
ThisBuild / testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

lazy val root = project
  .in(file("."))
  .settings(
    name := "simple-messaging-service",
    libraryDependencies ++= Dependencies.server,
    fork := true
  )

addCommandAlias("fmt", "all root/scalafmtSbt root/scalafmtAll")
addCommandAlias("fmtCheck", "all root/scalafmtSbtCheck root/scalafmtCheckAll")
