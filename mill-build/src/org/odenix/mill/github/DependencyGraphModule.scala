package org.odenix.mill.github

import mill.*

trait DependencyGraphModule extends Module {
  def githubDependencyGraph: Task[DependencyGraph]

  def submitDependencyGraph() = Task.Command {
    DependencyGraph.submit(Task.env, githubDependencyGraph())
    println("Submitted GitHub dependency graph snapshot")
  }
}
