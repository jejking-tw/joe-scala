package com.tw.energy.repository

import cats.effect.{IO, Resource}

import java.nio.file.{Files, Path}
import java.util.Comparator

object FileRepositoryTestUtil {

  def withTempDirectory[F](testCode: Resource[IO, Path] => F): F = {

    val tempDirResource = Resource.make(
      IO.blocking(Files.createTempDirectory("meter-reading-repository"))
    )(
      directory => {
        IO.blocking {
          Files.walk(directory)
            .sorted(Comparator.reverseOrder[Path]())
            .map(_.toFile)
            .forEach(file => file.delete())
        }
      })

    testCode(tempDirResource)
  }
}
