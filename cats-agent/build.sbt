name := "cats-agent"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.7"

// TODO add the scope provided when we test this feature with automatized tests
libraryDependencies += "org.apache.cassandra" % "cassandra-all" % "2.1.12"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.1"

libraryDependencies += "javax.mail" % "mail" % "1.4.7"

