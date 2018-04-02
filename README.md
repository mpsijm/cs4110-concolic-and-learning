# Concolic and Learning
This repository contains an experiment that is executed for the TU Delft Master course [CS4110 - Software Testing and Reverse Engineering](https://github.com/TUDelft-CS4110-2018/course-info-2018).

## How to run

- Run `mvn install` to install LearnLib and other dependencies.
- Run `mvn compile` to compile the source code.
- Run `mvn exec:java` to run the experiment.
- Run `mvn cobertura:cobertura` to run the experiment and generate a code coverage report.

## Location of the RERS problems

The [RERS problems](http://www.rers-challenge.org/) can best be put in the [`src/main/java`](src/main/java) directory. This repository contains [Problem 1](src/main/java/LTLRERS2017/Problem1) and [Problem10](src/main/java/ReachabilityRERS2017/Problem10) from the RERS challenge of 2017. Other problems have been left out to reduce the space of this repository (note that the [main method](src/main/scala/cs4110/LearnLibMain.scala) assumes the other problems of 2017 to be there, but it can easily be changed to just use problems 1 and 10).

In order for the experiment to run, the RERS problems need two tiny changes:
1. Add a package declaration on the top of the file, e.g. `package LTLRERS2017.Problem1;`
2. Let the problem class extend `cs4110.MockSystem`, which mocks `System.out.println` so that the output of the problem can be caught.

Note: we found that the bigger RERS problems cannot be handled by IntelliJ. This means that it's probably best to do these changes in a simple text editor like GEdit or Vim. Also, IntelliJ says that `LearnLibMain` has compile errors because it cannot find these bigger problem classes. However, running Maven will just work fine.

## Tweaking settings

- The `private val`s at the top of the [`LearnLibExperiment`](src/main/scala/cs4110/LearnLibExperiment.scala) class change the behaviour of the equivalence oracles.
  - Below that, the output directory for dot-files can be changed, used in `LearnLibExperiment#printResult`
- At the top of the [`KLEEEQOracle`](src/main/scala/cs4110/KLEEEQOracle.scala) class, the locations of the generated test cases by KLEE can be changed.

## Graph visualisation

Using [GraphViz](https://www.graphviz.org/), it is possible to generate graph files from the generated `*.dot` files. The [`compilegraph.sh`](compilegraph.sh) script can be used to do this. Example:
```bash
./compilegraph.sh output/dot/Problem1_L_STAR_WP_METHOD.dot
```
Executing this command will generate an equally named `*.pdf` file with the graph as generated by GraphViz.