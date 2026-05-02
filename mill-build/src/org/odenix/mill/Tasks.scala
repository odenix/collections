package org.odenix.mill

import mill.api.Evaluator
import mill.api.daemon.SelectMode

object Tasks {
  def run(evaluator: Evaluator, tasks: String*): Unit = {
    val result = evaluator.evaluate(tasks, SelectMode.Multi).get
    result.values.get
  }
}
