name := "sets-migration-postgres"

version := "0.1"

scalaVersion := "2.13.7"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-ahc-ws-standalone" % "2.1.3",
  "com.typesafe.play" %% "play-json" % "2.9.2",
  "org.postgresql" % "postgresql" % "42.3.1"
)