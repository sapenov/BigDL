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

import com.intel.analytics.bigdl.dllib.nn.SoftShrink
import com.intel.analytics.bigdl.dllib.tensor.Tensor

import java.security.SecureRandom

@com.intel.analytics.bigdl.tags.Serial
class SoftShrinkSpec extends TorchSpec {
    "A SoftShrink 3D input" should "generate correct output and grad" in {
    torchCheck()
    val layer = new SoftShrink[Double]()
    val input = Tensor[Double](2, 3, 4).apply1(_ => new SecureRandom().nextDouble())
    val gradOutput = Tensor[Double](2, 3, 4).apply1(_ => new SecureRandom().nextDouble())

    val start = System.nanoTime()
    val output = layer.forward(input)
    val gradInput = layer.backward(input, gradOutput)
    val end = System.nanoTime()
    val scalaTime = end - start

    val code = "module = nn.SoftShrink()\n" +
      "output = module:forward(input)\n" +
      "gradInput = module:backward(input,gradOutput)"

    val (luaTime, torchResult) = TH.run(code, Map("input" -> input, "gradOutput" -> gradOutput),
      Array("output", "gradInput"))
    val luaOutput = torchResult("output").asInstanceOf[Tensor[Double]]
    val luaGradInput = torchResult("gradInput").asInstanceOf[Tensor[Double]]

    output should be (luaOutput)
    gradInput should be (luaGradInput)

    println("Test case : SoftShrink, Torch : " + luaTime + " s, Scala : " + scalaTime / 1e9 + " s")
  }

  "A SoftShrink 4D input" should "generate correct output and grad" in {
    torchCheck()
    val layer = new SoftShrink[Double](2.0)
    val input = Tensor[Double](5, 4, 3, 2).apply1(_ => new SecureRandom().nextDouble())
    val gradOutput = Tensor[Double](5, 4, 3, 2).apply1(_ => new SecureRandom().nextDouble())

    val start = System.nanoTime()
    val output = layer.forward(input)
    val gradInput = layer.backward(input, gradOutput)
    val end = System.nanoTime()
    val scalaTime = end - start

    val code = "module = nn.SoftShrink(2.0)\n" +
      "output = module:forward(input)\n" +
      "gradInput = module:backward(input,gradOutput)"

    val (luaTime, torchResult) = TH.run(code, Map("input" -> input, "gradOutput" -> gradOutput),
      Array("output", "gradInput"))
    val luaOutput = torchResult("output").asInstanceOf[Tensor[Double]]
    val luaGradInput = torchResult("gradInput").asInstanceOf[Tensor[Double]]

    output should be (luaOutput)
    gradInput should be (luaGradInput)

    println("Test case : SoftShrink, Torch : " + luaTime + " s, Scala : " + scalaTime / 1e9 + " s")
  }

}
