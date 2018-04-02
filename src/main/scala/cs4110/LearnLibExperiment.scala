package cs4110

import java.io.PrintWriter
import java.nio.file.Paths
import java.util
import java.util.Random

import cs4110.EQOracleType._
import cs4110.MealyLearnerType._
import de.learnlib.algorithms.dhc.mealy.MealyDHCBuilder
import de.learnlib.algorithms.discriminationtree.mealy.DTLearnerMealyBuilder
import de.learnlib.algorithms.kv.mealy.KearnsVaziraniMealyBuilder
import de.learnlib.algorithms.lstar.mealy.ExtensibleLStarMealyBuilder
import de.learnlib.algorithms.rivestschapire.RivestSchapireMealyBuilder
import de.learnlib.algorithms.ttt.mealy.TTTLearnerMealyBuilder
import de.learnlib.api.algorithm.LearningAlgorithm.MealyLearner
import de.learnlib.api.statistic.StatisticSUL
import de.learnlib.drivers.reflect._
import de.learnlib.filter.cache.sul.{SULCache, SULCaches}
import de.learnlib.filter.statistic.Counter
import de.learnlib.filter.statistic.sul.ResetCounterSUL
import de.learnlib.oracle.equivalence.WMethodEQOracle.MealyWMethodEQOracle
import de.learnlib.oracle.equivalence.WpMethodEQOracle.MealyWpMethodEQOracle
import de.learnlib.oracle.equivalence.mealy.RandomWalkEQOracle
import de.learnlib.oracle.membership.SULOracle
import de.learnlib.util.Experiment
import de.learnlib.util.Experiment.MealyExperiment
import de.learnlib.util.statistics.SimpleProfiler
import net.automatalib.automata.transout.MealyMachine
import net.automatalib.serialization.dot.GraphDOT
import net.automatalib.words.Word

import scala.collection.JavaConverters._

/**
  * @param problemClass The <code>classOf</code> the problem class under learning.
  *                     The problem class must extend <code>MockSystem</code>.
  * @param learnerType  The learner type to use for the experiment.
  * @param eqOracleType The equivalence oracle type to use for the experiment.
  */
class LearnLibExperiment(val problemClass: Class[_ <: MockSystem],
                         val learnerType: MealyLearnerType,
                         val eqOracleType: EQOracleType) {
  // Behaviour of equivalence oracles
  private val RESET_PROBABILITY = 0.5
  private val MAX_STEPS = 10000
  private val RANDOM_SEED = 46346293
  private val W_WP_MAX_DEPTH = 2

  // Output directory of dot-files
  private val dotOutputDirectory = "output/dot/"

  private type Input = MethodInput
  private type Output = AbstractMethodOutput

  @throws[ReflectiveOperationException]
  def run(): Unit = {
    val beginTime = System.currentTimeMillis

    // 1. Instantiate the test driver that wraps the problemClass, the SUL (system under learning)
    val driver = new SimplePOJOTestDriver(problemClass.getConstructor())

    // 2. Register the inputs for the `calculateOutput` method as valid transitions (inputs)
    val inputMap = new util.HashMap[String, Input]()
    val methodInputs = buildInputs(driver, inputMap)

    // oracle for counting queries wraps SUL
    val statisticSul: ResetCounterSUL[Input, Output] = new ResetCounterSUL[Input, Output]("membership queries", driver)

    // use caching in order to avoid duplicate queries
    val effectiveSul: SULCache[Input, Output] = SULCaches.createCache(driver.getInputs, statisticSul)

    val sulOracle = new SULOracle[Input, Output](effectiveSul)

    // 3. Select the Mealy learner algorithm
    val mealyLearner = buildMealyLearner(driver, sulOracle, methodInputs)

    // 4. Select the equivalence oracle
    val eqOracle = buildEquivalenceOracle(driver, sulOracle, inputMap)

    // 5. Construct a learning experiment from the learning algorithm and the equivalence oracle.
    // The experiment will execute the main loop of active learning
    val experiment = new MealyExperiment[Input, Output](mealyLearner, eqOracle, driver.getInputs)

    // Turn on time profiling
    experiment.setProfile(true)

    // Enable logging of models
    experiment.setLogModels(true)

    // Run experiment
    experiment.run

    // 6. Get learned model
    val result = experiment.getFinalHypothesis

    // Print result
    printResult(driver, statisticSul, experiment, result, beginTime)

    // Reset profiler before next experiment
    SimpleProfiler.reset()
  }

  /**
    * @param driver   The wrapper of the SUL.
    * @param inputMap An empty Map from String (input) to MethodInput, that will be filled in this method.
    * @throws ReflectiveOperationException
    * @return An array of MethodInputs that can be used as transitions in the SUL.
    */
  @throws[ReflectiveOperationException]
  private def buildInputs(driver: SimplePOJOTestDriver, inputMap: util.HashMap[String, Input]) = {
    // create method inputs (possible transitions)
    val mCalculate = problemClass.getMethod("calculateOutputString", classOf[String])
    val inputsField = problemClass.getDeclaredField("inputs")
    inputsField.setAccessible(true)
    inputsField.get(problemClass.newInstance).asInstanceOf[Array[String]].map(s => {
      val input = driver.addInput(s, mCalculate, s)
      inputMap.put(s, input)
      input
    })
  }

  private def buildMealyLearner(driver: SimplePOJOTestDriver,
                                sulOracle: SULOracle[Input, Output],
                                methodInputs: Seq[Input]): MealyLearner[Input, Output] = {
    // create initial set of suffixes
    val suffixes = List(Word.fromSymbols(methodInputs: _*)).asJava
    learnerType match {
      case DT_LEARNER      =>
        new DTLearnerMealyBuilder[Input, Output]().withAlphabet(driver.getInputs).withOracle(sulOracle).create
      case KEARNS_VAZIRANI =>
        new KearnsVaziraniMealyBuilder[Input, Output]().withAlphabet(driver.getInputs).withOracle(sulOracle).create
      case MEALY_DHC       =>
        new MealyDHCBuilder[Input, Output]().withAlphabet(driver.getInputs).withOracle(sulOracle).create
      case L_STAR          =>
        new ExtensibleLStarMealyBuilder[Input, Output]().withAlphabet(driver.getInputs).withOracle(sulOracle)
          .withInitialSuffixes(suffixes).create
      case RIVEST_SCHAPIRE =>
        new RivestSchapireMealyBuilder[Input, Output]().withAlphabet(driver.getInputs).withOracle(sulOracle)
          .withInitialSuffixes(suffixes).create
      case _ | TTT         =>
        new TTTLearnerMealyBuilder[Input, Output]().withAlphabet(driver.getInputs).withOracle(sulOracle).create
    }
  }

  private def buildEquivalenceOracle(driver: SimplePOJOTestDriver,
                                     sulOracle: SULOracle[Input, Output],
                                     inputMap: util.HashMap[String, Input]) =
    eqOracleType match {
      case KLEE_METHOD     =>
        new KLEEEQOracle(inputMap, sulOracle)
      case W_METHOD        =>
        new MealyWMethodEQOracle[Input, Output](sulOracle, W_WP_MAX_DEPTH)
      case WP_METHOD       =>
        new MealyWpMethodEQOracle[Input, Output](sulOracle, W_WP_MAX_DEPTH)
      case _ | RANDOM_WALK =>
        new RandomWalkEQOracle[Input, Output](driver, RESET_PROBABILITY, MAX_STEPS, false, new Random(RANDOM_SEED))
    }

  @throws[ReflectiveOperationException]
  private def printResult(driver: SimplePOJOTestDriver,
                          statisticSul: StatisticSUL[Input, Output],
                          experiment: Experiment.MealyExperiment[Input, Output],
                          result: MealyMachine[_, Input, _, Output],
                          beginTime: Long): Unit = {
    System.out.println("--- " + problemClass.getSimpleName + ", " + learnerType + ", " + eqOracleType + " ---")

    // model statistics
    printCounter("Sigma", "#", driver.getInputs.size)
    printCounter("States", "#", result.size)

    // profiling
    val cumulatedField = classOf[SimpleProfiler].getDeclaredField("CUMULATED")
    cumulatedField.setAccessible(true)
    cumulatedField.get(null).asInstanceOf[java.util.Map[String, Counter]].values.forEach(this.printCounter)

    // learning statistics
    printCounter(experiment.getRounds)
    printCounter(statisticSul.getStatisticalData.asInstanceOf[Counter])
    printCounter("Total time", "ms", System.currentTimeMillis - beginTime)

    GraphDOT.write(result, driver.getInputs, new PrintWriter(Paths.get(dotOutputDirectory,
      s"${problemClass.getSimpleName}_${learnerType.name}_${eqOracleType.name}.dot").toString))
  }

  private def printCounter(data: Counter): Unit = printCounter(data.getName, data.getUnit, data.getCount)

  private def printCounter(name: String, unit: String, count: Long): Unit =
    printf("%-30s %8s: %s\n", name, "[" + unit + "]", count)

}
