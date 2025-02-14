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

package com.intel.analytics.bigdl.dllib.nn

import com.intel.analytics.bigdl.dllib.tensor.Tensor
import com.intel.analytics.bigdl.dllib.utils.{LayerException, TestUtils}
import com.intel.analytics.bigdl.dllib.utils.serializer.ModuleSerializationTest
import org.scalatest.FlatSpec

import java.security.SecureRandom

class InferReshapeSpec extends FlatSpec {
  "A InferReshape Module with infer" should "generate correct output and grad" in {
    val module = new InferReshape[Double](Array(3, -1), true)
    for (batchSize <- 1 to 4) {
      val input = Tensor[Double](batchSize, 1, 6)
      input.rand()
      val inputOrg = input.clone()
      val output = module.forward(input)
      val gradOutput = Tensor[Double](batchSize, 3, 2)
      gradOutput.rand()
      val gradOutputOrg = gradOutput.clone()
      val gradInput = module.backward(input, gradOutput)
      TestUtils.conditionFailTest(output.nDimension() == 3)
      TestUtils.conditionFailTest(output.size(1) == batchSize)
      TestUtils.conditionFailTest(output.size(2) == 3)
      TestUtils.conditionFailTest(output.size(3) == 2)
      TestUtils.conditionFailTest(gradInput.isSameSizeAs(input))
      for (i <- 1 to batchSize) {
        for (j <- 0 to 5) {
          TestUtils.conditionFailTest(
            input(Array(i, 1, j + 1)) == output(Array(i, j / 2 + 1, j % 2 + 1)))
          TestUtils.conditionFailTest(
            gradInput(Array(i, 1, j + 1)) == gradOutput(Array(i, j / 2 + 1, j % 2 + 1)))
        }
      }
      TestUtils.conditionFailTest(input == inputOrg)
      TestUtils.conditionFailTest(gradOutput == gradOutputOrg)
    }

    intercept[com.intel.analytics.bigdl.dllib.utils.UnKnownException] {
      module.forward(Tensor[Double](2, 2))
    }

    intercept[com.intel.analytics.bigdl.dllib.utils.UnKnownException] {
      module.forward(Tensor[Double](3, 2, 2))
    }
  }

  "A InferReshape Module default batch with infer" should "generate correct output and grad" in {
    val module = new InferReshape[Double](Array(-1, 2))
    val input = Tensor[Double](2, 3)
    input.rand()
    val inputOrg = input.clone()
    val output = module.forward(input)
    val gradOutput = Tensor[Double](3, 2)
    gradOutput.rand()
    val gradOutputOrg = gradOutput.clone()
    val gradInput = module.backward(input, gradOutput)
    TestUtils.conditionFailTest(output.nDimension() == 2)
    TestUtils.conditionFailTest(output.size(1) == 3)
    TestUtils.conditionFailTest(output.size(2) == 2)
    for (j <- 0 to 5) {
      TestUtils.conditionFailTest(
        input(Array(j / 3 + 1, j % 3 + 1)) == output(Array(j / 2 + 1, j % 2 + 1)))
      TestUtils.conditionFailTest(
        gradInput(Array(j / 3 + 1, j % 3 + 1)) == gradOutput(Array(j / 2 + 1, j % 2 + 1)))
    }
    TestUtils.conditionFailTest(input == inputOrg)
    TestUtils.conditionFailTest(gradOutput == gradOutputOrg)
  }

  "A InferReshape Module disable batch with infer" should "generate correct output and grad" in {
    val module = new InferReshape[Double](Array(3, -1))
    val input = Tensor[Double](1, 2, 3)
    input.rand()
    val inputOrg = input.clone()
    val output = module.forward(input)
    val gradOutput = Tensor[Double](3, 2)
    gradOutput.rand()
    val gradOutputOrg = gradOutput.clone()
    val gradInput = module.backward(input, gradOutput)
    TestUtils.conditionFailTest(output.nDimension() == 2)
    TestUtils.conditionFailTest(output.size(1) == 3)
    TestUtils.conditionFailTest(output.size(2) == 2)
    for (j <- 0 to 5) {
      TestUtils.conditionFailTest(
        input(Array(1, j / 3 + 1, j % 3 + 1)) == output(Array(j / 2 + 1, j % 2 + 1)))
      TestUtils.conditionFailTest(gradInput(
        Array(1, j / 3 + 1, j % 3 + 1)) == gradOutput(Array(j / 2 + 1, j % 2 + 1)))
    }
    TestUtils.conditionFailTest(input == inputOrg)
    TestUtils.conditionFailTest(gradOutput == gradOutputOrg)
  }

  "A InferReshape Module enable batch with infer" should "generate correct output and grad" in {
    val module = new InferReshape[Double](Array(-1, 2), true)
    for (batchSize <- 1 to 4) {
      val input = Tensor[Double](batchSize, 1, 6)
      input.rand()
      val inputOrg = input.clone()
      val output = module.forward(input)
      val gradOutput = Tensor[Double](batchSize, 3, 2)
      gradOutput.rand()
      val gradOutputOrg = gradOutput.clone()
      val gradInput = module.backward(input, gradOutput)
      TestUtils.conditionFailTest(output.nDimension() == 3)
      TestUtils.conditionFailTest(output.size(1) == batchSize)
      TestUtils.conditionFailTest(output.size(2) == 3)
      TestUtils.conditionFailTest(output.size(3) == 2)
      TestUtils.conditionFailTest(gradInput.isSameSizeAs(input))
      for (i <- 1 to batchSize) {
        for (j <- 0 to 5) {
          TestUtils.conditionFailTest(
            input(Array(i, 1, j + 1)) == output(Array(i, j / 2 + 1, j % 2 + 1)))
          TestUtils.conditionFailTest(
            gradInput(Array(i, 1, j + 1)) == gradOutput(Array(i, j / 2 + 1, j % 2 + 1)))
        }
      }
      TestUtils.conditionFailTest(input == inputOrg)
      TestUtils.conditionFailTest(gradOutput == gradOutputOrg)
    }

    intercept[com.intel.analytics.bigdl.dllib.utils.UnKnownException] {
      module.forward(Tensor[Double](3, 1))
    }
  }

  "InferReshape with 0 and -1" should "work well" in {
    val tensor = Tensor.randperm[Float](1024)
    tensor.resize(2, 16, 4, 8)
    val model = new InferReshape[Float](Array(0, 4, -1, 0))
    val expectedShape = Array(2, 4, 16, 8)
    val out = model.forward(tensor).size()
    (out zip expectedShape).foreach(x => TestUtils.conditionFailTest(x._1 == x._2))

    val tensor2 = Tensor.randperm[Float](1024)
    tensor2.resize(2, 16, 4, 8)
    val model2 = new InferReshape[Float](Array(-1, 4))
    val expectedShape2 = Array(256, 4)
    val out2 = model2.forward(tensor).size()
    (out2 zip expectedShape2).foreach(x => TestUtils.conditionFailTest(x._1 == x._2))

    val tensor3 = Tensor.randperm[Float](1024)
    tensor3.resize(256, 4)
    val model3 = new InferReshape[Float](Array(1, 4, -1, 8))
    val expectedShape3 = Array(1, 4, 32, 8)
    val out3 = model3.forward(tensor).size()
    (out3 zip expectedShape3).foreach(x => TestUtils.conditionFailTest(x._1 == x._2))


    val cls = Tensor.randperm[Float](18 * 55 * 37)
    cls.resize(1, 18, 55, 37)
    val o1 = new InferReshape[Float](Array(2, -1)).forward(cls)
    println("o1", o1.size().mkString(","))
    assertIntArrayEqual(o1.size(), Array[Int](2, 9 * 55 * 37))
    val o2 = new SoftMax[Float]().forward(o1)
    assertIntArrayEqual(o2.size(), Array[Int](2, 9 * 55 * 37))
    val o3 = new InferReshape[Float](Array(1, 2 * 9, -1, 37)).forward(o2)
    assertIntArrayEqual(o3.size(), Array(1, 18, 55, 37))
    val clsProc = new Sequential[Float]()
    clsProc.add(new InferReshape[Float](Array(2, -1)))
    clsProc.add(new SoftMax[Float]())
    clsProc.add(new InferReshape[Float](Array(1, 2 * 9, -1, 37)))
    val out4 = clsProc.forward(cls).asInstanceOf[Tensor[Float]].size()
    (out4 zip cls.size()).foreach(x => TestUtils.conditionFailTest(x._1 == x._2))
  }

  def assertIntArrayEqual(a1: Array[Int], a2: Array[Int]): Unit = {
    (a1 zip a2).foreach(x => TestUtils.conditionFailTest(x._1 == x._2))
  }
}

class InferReshapeSerialTest extends ModuleSerializationTest {
  override def test(): Unit = {
    val inferReshape = InferReshape[Float](Array(-1, 2, 0, 5)).setName("inferReshape")
    val input = Tensor[Float](2, 5, 2, 2).apply1(_ => new SecureRandom().nextFloat())
    runSerializationTest(inferReshape, input)
  }
}
