name := "cats-agent"

version := "0.3-SNAPSHOT"

scalaVersion := "2.11.7"

libraryDependencies += "org.apache.cassandra" % "cassandra-all" % "3.0.6" //% "provided"

libraryDependencies += "javax.mail" % "mail" % "1.4.7"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.7"

libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.4.7"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.4.7" % "test"

libraryDependencies +=  "org.scalatest" %% "scalatest" % "2.2.6" % "test"

enablePlugins(UniversalPlugin)

