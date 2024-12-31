val scala3Version = "3.3.1"

enablePlugins(VcpkgNativePlugin, ScalaNativePlugin, BindgenPlugin)

vcpkgDependencies := VcpkgDependencies(
  "portaudio"
)


import scala.scalanative.build._

nativeConfig ~= {
  _.withLTO(LTO.full)
  .withMode(Mode.releaseFull)
  .withGC(GC.immix)
}

vcpkgNativeConfig ~= {
  _.withRenamedLibraries(Map("portaudio" -> "portaudio-2.0"))
}

import bindgen.interface.Binding

bindgenBindings := {
  val configurator = vcpkgConfigurator.value
  Seq(
    Binding(
        configurator.includes("portaudio") / "portaudio.h",
        "portaudio"
      )
      .withLinkName("portaudio")
  )
}

lazy val root = project
  .in(file("."))
  .settings(
    name := "pedalboard",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies += ("org.typelevel" %%% "cats-effect" % "3.7-0b545af").cross(CrossVersion.for3Use2_13),
    libraryDependencies += ("co.fs2"        %%% "fs2-core"    % "3.12-2d1232c-20241231T120325Z-SNAPSHOT").cross(CrossVersion.for3Use2_13),
    Compile / scalacOptions -= "-Xfatal-warnings"
  )
