name := "files-for-you"

version := "0.1"

ThisBuild / scalaVersion := "2.13.6"

val akkaHttp = "com.typesafe.akka" %% "akka-http" % "10.2.6"
val akkaStream = "com.typesafe.akka" %% "akka-stream" % "2.6.16"
val sttp3 = "com.softwaremill.sttp.client3" %% "akka-http-backend" % "3.3.13"
//val sttp = "com.softwaremill.sttp.client" %% "akka-http-backend" % "2.2.9"

lazy val server = project
  .settings(libraryDependencies ++= Seq(akkaHttp, akkaStream))

lazy val client = project
  .settings(libraryDependencies ++= Seq(akkaHttp,akkaStream, sttp3))
