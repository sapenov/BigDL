/*
 * Copyright 2016 The BigDL Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intel.analytics.bigdl.dllib.integration.torch

import com.intel.analytics.bigdl.dllib.nn.CrossEntropyCriterion
import com.intel.analytics.bigdl.dllib.tensor.Tensor
import com.intel.analytics.bigdl.dllib.utils.RandomGenerator._

import java.security.SecureRandom

@com.intel.analytics.bigdl.tags.Serial
class CrossEntropyCriterionSpec extends TorchSpec {
    "A CrossEntropyCriterion Module" should "generate correct output and grad" in {
    torchCheck()
    val seed = 100
    RNG.setSeed(seed)
    val module = new CrossEntropyCriterion[Double]()

    val input = Tensor[Double](3, 3).apply1(_ => new SecureRandom().nextDouble())
    val target = Tensor[Double](3)
    target(Array(1)) = 1
    target(Array(2)) = 2
    target(Array(3)) = 3

    val start = System.nanoTime()
    val output = module.forward(input, target)
    val gradInput = module.backward(input, target)
    val end = System.nanoTime()
    val scalaTime = end - start

    val code = "torch.manualSeed(" + seed + ")\n" +
      "module = nn.CrossEntropyCriterion()\n" +
      "output = module:forward(input, target)\n" +
      "gradInput = module:backward(input,target)\n"

    val (luaTime, torchResult) = TH.run(code, Map("input" -> input, "target" -> target),
      Array("output", "gradInput"))
    val luaOutput1 = torchResult("output").asInstanceOf[Double]
    val luaOutput2 = torchResult("gradInput").asInstanceOf[Tensor[Double]]

    luaOutput1 should be(output)
    luaOutput2 should be(gradInput)

    println("Test case : CrossEntropyCriterion, Torch : " + luaTime +
      " s, Scala : " + scalaTime / 1e9 + " s")

  }

  "A CrossEntropyCriterion(sizeAverage = false) Module" should
    "generate correct output and grad" in {
    torchCheck()
    val seed = 100
    RNG.setSeed(seed)
    val module = new CrossEntropyCriterion[Double](sizeAverage = false)

    val input = Tensor[Double](3, 3).apply1(_ => new SecureRandom().nextDouble())
    val target = Tensor[Double](3)
    target(Array(1)) = 1
    target(Array(2)) = 2
    target(Array(3)) = 3

    val start = System.nanoTime()
    val output = module.forward(input, target)
    val gradInput = module.backward(input, target)
    val end = System.nanoTime()
    val scalaTime = end - start

    val code = "torch.manualSeed(" + seed + ")\n" +
      "module = nn.CrossEntropyCriterion(nil, false)\n" +
      "output = module:forward(input, target)\n" +
      "gradInput = module:backward(input,target)\n"

    val (luaTime, torchResult) = TH.run(code, Map("input" -> input, "target" -> target),
      Array("output", "gradInput"))
    val luaOutput1 = torchResult("output").asInstanceOf[Double]
    val luaOutput2 = torchResult("gradInput").asInstanceOf[Tensor[Double]]

    luaOutput1 should be(output)
    luaOutput2 should be(gradInput)

    println("Test case : CrossEntropyCriterion, Torch : " + luaTime +
      " s, Scala : " + scalaTime / 1e9 + " s")

  }
}
