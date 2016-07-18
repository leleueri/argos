enablePlugins(UniversalPlugin)

name := "argos-agent"

version := "0.3-SNAPSHOT"

scalaVersion := "2.11.7"

libraryDependencies += "org.apache.cassandra" % "cassandra-all" % "3.0.6" % "provided"

libraryDependencies += "javax.mail" % "mail" % "1.4.7"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.7"

libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.4.7"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.4.7" % "test"

libraryDependencies +=  "org.scalatest" %% "scalatest" % "2.2.6" % "test"

assemblyMergeStrategy in assembly := {
  case PathList("application.conf") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

// we specify the name for our fat jar
assemblyJarName in assembly := s"argos-agent-assembly.jar"

// removes all jar mappings in universal and appends the fat jar
mappings in Universal := {
  // universalMappings: Seq[(File,String)]
  val universalMappings = (mappings in Universal).value
  val fatJar = (assembly in Compile).value
  // removing means filtering
  val filtered = universalMappings filter {
    case (file, name) =>  ! name.endsWith(".jar")
  }
  // add the fat jar
  filtered :+ (fatJar -> ("lib/" + fatJar.getName))
}

// the bash scripts classpath only needs the fat jar
scriptClasspath := Seq( (jarName in assembly).value )
