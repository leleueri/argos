name := "cats-agent"

version := "0.2-SNAPSHOT"

scalaVersion := "2.11.7"

libraryDependencies += "org.apache.cassandra" % "cassandra-all" % "3.0.6" //% "provided"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.7"

libraryDependencies += "javax.mail" % "mail" % "1.4.7"

enablePlugins(UniversalPlugin)

