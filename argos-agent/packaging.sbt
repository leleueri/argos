mappings in Universal <+= (packageBin in Compile) map { jar =>
  jar -> ("lib/" + jar.getName)
}

mappings in Universal += file("src/main/bin/argos-agent.sh") -> "argos-agent.sh"

mappings in Universal += file("src/main/bin/argos-env.sh") -> "conf/argos-env.sh"

mappings in Universal += file("src/main/resources/application.conf") -> "conf/application.conf"