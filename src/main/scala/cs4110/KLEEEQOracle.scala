package cs4110

import java.io.File
import java.nio.file.Files
import java.util
import java.util.Collections

import de.learnlib.api.oracle.EquivalenceOracle
import de.learnlib.api.query.DefaultQuery
import de.learnlib.oracle.membership.SULOracle
import net.automatalib.automata.transout.MealyMachine
import net.automatalib.words.Word

import scala.collection.JavaConverters._

class KLEEEQOracle[S, I, O](inputMap: util.HashMap[S, I], sul: SULOracle[I, O])
  extends EquivalenceOracle[MealyMachine[_, I, _, O], I, Word[O]] {

  private case class KLEETestCaseDirectory(dir: String, testCaseLength: Int)

  // Locations of the KLEE test case directories, accompanied with the (fixed) length of the generated test cases
  private val testCaseDirectories = List(
    KLEETestCaseDirectory("klee/klee-last/", 20),
    KLEETestCaseDirectory("klee/klee-last-2/", 25))

  override def findCounterExample(hypothesis: MealyMachine[_, I, _, O],
                         allowedInputs: util.Collection[_ <: I]): DefaultQuery[I, Word[O]] = {
    for (testCaseDirectory <- testCaseDirectories) {
      for (file <- getAllKTestFiles(testCaseDirectory.dir)) {
        val inputStr = readInputStringFromKTest(file, testCaseDirectory.testCaseLength)
        val input = Word.fromList(inputStr.map { s => inputMap.get(s.toString) }.takeWhile(_ != null).asJava)
        val hypOutput = hypothesis.computeOutput(input)
        val sulOutput = sul.answerQuery(input)
        if (!hypOutput.equals(sulOutput)) {
          return new DefaultQuery[I, Word[O]](Word.fromList(Collections.emptyList), input, sulOutput)
        }
      }
    }
    null
  }

  private def getAllKTestFiles(directory: String): Seq[File] = getAllKTestFiles(new File(directory))

  private def getAllKTestFiles(directory: File): Seq[File] =
    if (directory.exists && directory.isDirectory)
      directory.listFiles.filter(_.isFile).filter(_.getName.endsWith(".ktest")).toList
    else
      Nil

  private def readInputStringFromKTest(testFile: File, inputLength: Int) =
    new String(Files.readAllBytes(testFile.toPath).takeRight(4 * inputLength).grouped(4).map(_.head)
      .map(a => (a + 64).asInstanceOf[Byte]).toArray)
}
