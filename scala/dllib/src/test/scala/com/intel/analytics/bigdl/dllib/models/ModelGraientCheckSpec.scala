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

package com.intel.analytics.bigdl.dllib.models

import com.intel.analytics.bigdl.dllib.nn.{ClassNLLCriterion, GradientChecker}
import com.intel.analytics.bigdl.dllib.tensor.Tensor
import com.intel.analytics.bigdl.dllib.utils.Log4Error
import com.intel.analytics.bigdl.dllib.utils.RandomGenerator._
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import java.security.SecureRandom

@com.intel.analytics.bigdl.tags.Parallel
class ModelGraientCheckSpec extends FlatSpec with BeforeAndAfter with Matchers {

  private val checkModel = true
  val random = new SecureRandom()
  random.setSeed(100)
  "GoogleNet_v1 model in batch mode" should "be good in gradient check for input" in {
    val seed = 100
    RNG.setSeed(seed)
    val start = System.nanoTime()
    val input = Tensor[Double](4, 3, 224, 224).apply1(e => random.nextDouble())
    val model = GoogleNet_v1_test(1000)
    model.zeroGradParameters()

    val checker = new GradientChecker(1e-4).setType(checkModel)
    checker.checkLayer(model, input, 1e-2) should be(true)
    val scalaTime = System.nanoTime() - start
    println("Test Scala time : " + scalaTime / 1e9 + " s")
  }

  "GoogleNet_v1 model in batch mode" should "be good in gradient check for weight" in {
    val seed = 100
    RNG.setSeed(seed)
    val start = System.nanoTime()
    val input = Tensor[Double](4, 3, 224, 224).apply1(e => random.nextDouble())
    val model = GoogleNet_v1_test(1000)
    model.zeroGradParameters()

    val checker = new GradientChecker(1e-4).setType(checkModel)
    checker.checkWeight(model, input, 1e-2) should be(true)
    val scalaTime = System.nanoTime() - start
    println("Test Scala time : " + scalaTime / 1e9 + " s")
  }

  "GoogleNet_v1 model" should "init right" in {
    val seed = 100
    RNG.setSeed(seed)
    random.setSeed(seed)
    val input = Tensor[Double](4, 3, 224, 224).apply1(e => random.nextDouble())
    val labels = Tensor[Double](4).apply1(e => random.nextInt(1000))
    val criterion = new ClassNLLCriterion[Double]()
    val model = GoogleNet_v1_test(1000)
    val output = model.forward(input)
    val loss = criterion.forward(output, labels)
    import scala.math.abs
    Log4Error.invalidOperationError(
      abs(loss - 6.905944392665487) < 0.01,
      s"Loss should be in range 6.905944392665487 +- 0.01, but got ${loss}")
  }

  "GoogleNet_v2 model in batch mode" should "be good in gradient check for input" in {
    val seed = 100
    RNG.setSeed(seed)
    val start = System.nanoTime()
    val input = Tensor[Double](4, 3, 224, 224).apply1(e => random.nextDouble())
    val model = GoogleNet_v2_test(1000)
    model.zeroGradParameters()

    val checker = new GradientChecker(1e-4).setType(checkModel)
    checker.checkLayer(model, input, 1e-2) should be(true)
    val scalaTime = System.nanoTime() - start
    println("Test Scala time : " + scalaTime / 1e9 + " s")
  }

  "GoogleNet_v2 model in batch mode" should "be good in gradient check for weight" in {
    val seed = 100
    RNG.setSeed(seed)
    val start = System.nanoTime()
    val input = Tensor[Double](4, 3, 224, 224).apply1(e => random.nextDouble())
    val model = GoogleNet_v2_test.applyNoBn(1000)
    model.zeroGradParameters()

    val checker = new GradientChecker(1e-4).setType(checkModel)
    checker.checkWeight(model, input, 1e-2) should be(true)
    val scalaTime = System.nanoTime() - start
    println("Test Scala time : " + scalaTime / 1e9 + " s")
  }

  "VggLike model in batch mode" should "be good in gradient check for input" in {
    val seed = 100
    RNG.setSeed(seed)
    val start = System.nanoTime()
    val input = Tensor[Double](8, 3, 32, 32).apply1(e => random.nextDouble())
    val model = VggLike_test(10)
    model.zeroGradParameters()

    val checker = new GradientChecker(1e-4).setType(checkModel)
    checker.checkLayer[Double](model, input, 1e-2) should be(true)
    val scalaTime = System.nanoTime() - start
    println("Test Scala time : " + scalaTime / 1e9 + " s")
  }

  "VggLike model in batch mode" should "be good in gradient check for weight" in {
    val seed = 100
    RNG.setSeed(seed)
    val start = System.nanoTime()
    val input = Tensor[Double](8, 3, 32, 32).apply1(e => random.nextDouble())
    val model = VggLike_test(10)
    model.zeroGradParameters()

    val checker = new GradientChecker(1e-4).setType(checkModel)
    checker.checkWeight[Double](model, input, 1e-2) should be(true)
    val scalaTime = System.nanoTime() - start
    println("Test Scala time : " + scalaTime / 1e9 + " s")
  }

  "LeNet model in batch mode" should "be good in gradient check for input" in {
    val seed = 100
    RNG.setSeed(seed)
    val start = System.nanoTime()
    val input = Tensor[Double](8, 1, 28, 28).apply1(e => random.nextDouble())
    val model = LeNet5_test(10)
    model.zeroGradParameters()

    val checker = new GradientChecker(1e-4).setType(checkModel)
    checker.checkLayer[Double](model, input, 1e-2) should be(true)
    val scalaTime = System.nanoTime() - start
    println("Test Scala time : " + scalaTime / 1e9 + " s")
  }

  "LeNet model in batch mode" should "be good in gradient check for weight" in {
    val seed = 100
    RNG.setSeed(seed)
    val start = System.nanoTime()
    val input = Tensor[Double](8, 1, 28, 28).apply1(e => random.nextDouble())
    val model = LeNet5_test(10)
    model.zeroGradParameters()

    val checker = new GradientChecker(1e-4).setType(checkModel)
    checker.checkWeight(model, input, 1e-2) should be(true)
    val scalaTime = System.nanoTime() - start
    println("Test Scala time : " + scalaTime / 1e9 + " s")
  }

  "CNN model in batch mode" should "be good in gradient check for input" in {
    val seed = 100
    RNG.setSeed(seed)
    val start = System.nanoTime()
    val input = Tensor[Double](8, 1, 28, 28).apply1(e => random.nextDouble())
    val model = SimpleCNN_test(10)
    model.zeroGradParameters()

    val checker = new GradientChecker(1e-4).setType(checkModel)
    checker.checkLayer[Double](model, input, 1e-2) should be(true)
    val scalaTime = System.nanoTime() - start
    println("Test Scala time : " + scalaTime / 1e9 + " s")
  }

  "CNN model in batch mode" should "be good in gradient check for weight" in {
    val seed = 100
    RNG.setSeed(seed)
    val start = System.nanoTime()
    val input = Tensor[Double](8, 1, 28, 28).apply1(e => random.nextDouble())
    val model = SimpleCNN_test(10)
    model.zeroGradParameters()

    val checker = new GradientChecker(1e-4).setType(checkModel)
    checker.checkWeight(model, input, 1e-2) should be(true)
    val scalaTime = System.nanoTime() - start
    println("Test Scala time : " + scalaTime / 1e9 + " s")
  }
}
