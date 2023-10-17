val scala3Version = "3.3.1"

enablePlugins(ScalaNativePlugin, BindgenPlugin)

import scala.scalanative.build._

// defaults set with common options shown
nativeConfig ~= { c =>
  // c.withLTO(LTO.full)
  //   .withMode(Mode.releaseFull)
  //   .withGC(GC.immix)
  c.withLTO(LTO.none)
    .withMode(Mode.debug)
    .withGC(GC.none)
}

import bindgen.interface.Binding

bindgenBindings := Seq(
  Binding
    .builder(
      file("/opt/homebrew/opt/portaudio/include/portaudio.h"),
      "portaudio"
    )
    .withLinkName("portaudio")
    .addCImport("portaudio.h")
    .build,
  Binding
    .builder(
      file("/opt/homebrew/Cellar/glfw/3.3.8/include/glfw/glfw3.h"),
      "glfw"
    )
    .withLinkName("glfw")
    .addCImport("glfw3.h")
    .build
)

lazy val root = project
  .in(file("."))
  .settings(
    name := "pedalboard",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies += "org.typelevel" %%% "cats-effect" % "3.5.1",
    libraryDependencies += "co.fs2"        %%% "fs2-core"    % "3.9.2",
    libraryDependencies += "co.fs2"        %%% "fs2-io"      % "3.9.2",
    Compile / scalacOptions -= "-Xfatal-warnings"
  )
