name := "MatrixMultiply"

version := "1.6"

scalaVersion := "2.13.10"  // Chisel 3.5.x 兼容的 Scala 版本

libraryDependencies ++= Seq(
  "edu.berkeley.cs" %% "chisel3" % "3.5.0",
  "edu.berkeley.cs" %% "chiseltest" % "0.5.0" % "test"
)