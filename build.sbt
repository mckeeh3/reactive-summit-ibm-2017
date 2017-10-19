name := "wml-lightbend"

version := "1.0"

//scalaVersion := "2.11.11"
scalaVersion := "2.12.3"

// https://mvnrepository.com/artifact/io.spray/spray-json_2.11
//libraryDependencies += "io.spray" % "spray-json_2.11" % "1.3.1"

//libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.10"

//libraryDependencies += "com.typesafe.akka" % "akka-http-spray-json_2.12" % "10.0.10"
libraryDependencies += "io.spray" %%  "spray-json" % "1.3.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.0.10",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.10" % Test
)
