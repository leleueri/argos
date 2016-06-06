mappings in Universal <+= (packageBin in Compile) map { jar =>
  jar -> ("lib/" + jar.getName)
}

mappings in Universal += file("src/main/bin/cats-agent.sh") -> "cats-agent.sh"

mappings in Universal += file("src/main/bin/cats-env.sh") -> "conf/cats-env.sh"

mappings in Universal += file("src/main/resources/application.conf") -> "conf/application.conf"