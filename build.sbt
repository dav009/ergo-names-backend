name := "ergo-names-backend"

scalaVersion := "2.12.15"

libraryDependencies += "org.ergoplatform" %% "ergo-appkit" % "dp-sigma-401-signing-func-623ece4d-SNAPSHOT"
libraryDependencies += "com.dav009" %% "ergopilot" % "0.0.0+13-d7214d69+20220218-2155"
libraryDependencies += ("org.scorexfoundation" %% "sigma-state" % "4.0.5" ).classifier("tests") % "compile->compile;test->compile"

