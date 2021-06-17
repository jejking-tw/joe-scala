package com.tw.energy.repository

import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.file.{Files, Path, Paths}
import java.util.Comparator

class FileMeterReadingRepositoryTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  var tempDir: Path = null

  override def beforeAll(): Unit = {
    import java.nio.file.Files
    tempDir = Files.createTempDirectory("meter-reading-repository")
  }

  override def afterAll(): Unit = {
    Files.walk(tempDir)
      .sorted(Comparator.reverseOrder[Path]())
      .map(_.toFile)
      .forEach(file => file.delete())
  }

  "repository" should "return None if no file for the meter id can be located" in {
    val fileBasedRepository = new FileMeterReadingRepository(tempDir)

    val ret = fileBasedRepository.getReadings("meter-1")

    ret shouldBe Option.empty
  }

  it should "return an empty sequence if the file exists but is empty" in {
    val fileBasedRepository = new FileMeterReadingRepository(tempDir)
    Files.createFile(tempDir.resolve("meter-2"))

    val ret = fileBasedRepository.getReadings("meter-2")

    ret shouldBe Option(Seq.empty)
  }
}