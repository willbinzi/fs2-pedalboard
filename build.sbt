val scala3Version = "3.3.1"

enablePlugins(ScalaNativePlugin, BindgenPlugin)

import scala.scalanative.build._

// defaults set with common options shown
nativeConfig ~= { c =>
  c.withLTO(LTO.none) // thin
    .withMode(Mode.debug) // releaseFast
    .withGC(GC.immix) // commix
}

import bindgen.interface.Binding

bindgenBindings := Seq(
  Binding
    .builder(
      // Root / "opt" / "homebrew" / "opt" / "portaudio" / "include" / "portaudio.h",
      file("/opt/homebrew/opt/portaudio/include/portaudio.h"),
      "portaudio"
    )
    .withLinkName("portaudio")
    .addCImport("portaudio.h") /* 3 */
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
