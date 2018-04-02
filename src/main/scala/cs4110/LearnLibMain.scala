package cs4110

import LTLRERS2017.Problem1.Problem1
import LTLRERS2017.Problem2.Problem2
import LTLRERS2017.Problem3.Problem3
import LTLRERS2017.Problem4.Problem4
import LTLRERS2017.Problem5.Problem5
import LTLRERS2017.Problem6.Problem6
import LTLRERS2017.Problem7.Problem7
import LTLRERS2017.Problem8.Problem8
import LTLRERS2017.Problem9.Problem9
import ReachabilityRERS2017.Problem10.Problem10
import ReachabilityRERS2017.Problem11.Problem11
import ReachabilityRERS2017.Problem12.Problem12
import ReachabilityRERS2017.Problem13.Problem13
import ReachabilityRERS2017.Problem14.Problem14
import ReachabilityRERS2017.Problem15.Problem15
import ReachabilityRERS2017.Problem16.Problem16
import ReachabilityRERS2017.Problem17.Problem17
import ReachabilityRERS2017.Problem18.Problem18

object LearnLibMain extends App {
  List(
    classOf[Problem1], classOf[Problem2], classOf[Problem3],
    classOf[Problem4], classOf[Problem5], classOf[Problem6],
    classOf[Problem7], classOf[Problem8], classOf[Problem9],
    classOf[Problem10], classOf[Problem11], classOf[Problem12],
    classOf[Problem13], classOf[Problem14], classOf[Problem15],
    classOf[Problem16], classOf[Problem17], classOf[Problem18]
  ).foreach(problemClass =>
    MealyLearnerType.values.foreach(mealyLearnerType =>
      EQOracleType.values.foreach(eqOracleType =>
        new LearnLibExperiment(problemClass, mealyLearnerType, eqOracleType).run())))
}
