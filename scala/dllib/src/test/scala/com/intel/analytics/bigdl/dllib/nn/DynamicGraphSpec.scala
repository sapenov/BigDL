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

import com.intel.analytics.bigdl._
import com.intel.analytics.bigdl.Module
import com.intel.analytics.bigdl.dllib.models.DynamicTestModels
import com.intel.analytics.bigdl.dllib.models.autoencoder.Autoencoder
import com.intel.analytics.bigdl.dllib.models.inception.Inception_v1_NoAuxClassifier
import com.intel.analytics.bigdl.dllib.models.lenet.LeNet5
import com.intel.analytics.bigdl.dllib.models.vgg.{VggForCifar10, Vgg_16, Vgg_19}
import com.intel.analytics.bigdl.dllib.nn.Graph.ModuleNode
import com.intel.analytics.bigdl.dllib.nn.abstractnn.EmptyGradInput
import com.intel.analytics.bigdl.dllib.nn.ops.Less
import com.intel.analytics.bigdl.dllib.nn.tf.{ControlNodes, Enter, Const}
import com.intel.analytics.bigdl.numeric.NumericFloat
import com.intel.analytics.bigdl.dllib.utils.RandomGenerator._
import com.intel.analytics.bigdl.dllib.tensor.Tensor
import com.intel.analytics.bigdl.dllib.utils._
import com.intel.analytics.bigdl.dllib.utils.serializer.ModuleSerializationTest
import com.intel.analytics.bigdl.dllib.utils._

import scala.reflect.ClassTag
import java.security.SecureRandom
import org.scalatest.{FlatSpec, Matchers}

class DynamicGraphSpec  extends FlatSpec with Matchers {
  "Dynamic Graph init" should "throw exceptions when there's cycle" in {
    val fc1 = Linear(4, 2).inputs()
    val relu1 = ReLU().inputs(fc1)
    relu1 -> fc1

    intercept[IllegalArgumentException] {
      Graph.dynamic(fc1, relu1)
    }
  }

  "Dynamic Graph init" should "be successful when inputs node are same with outputs node" in {
    val fc1 = Linear(4, 2).inputs()
    val graph = Graph.dynamic(fc1, fc1)

    val inputData = Tensor(4, 4)
    fc1.element.parameters()._1(1).zero() // bias is set to 0
    graph.forward(inputData) should be((inputData * fc1.element.parameters()._1(0).t()))
  }

  "Dynamic Graph init" should "throw exceptions when some inputs are ignored" in {
    val fc1 = Linear(4, 2).inputs()
    val fc2 = Linear(4, 2).inputs()
    val output = CAddTable().inputs(fc1, fc2)

    intercept[IllegalArgumentException] {
      Graph.dynamic(fc1, output)
    }
  }

  "Dynamic Graph init" should "be successful output are ignored" in {
    val fc1 = Linear(4, 2).inputs()
    val fc2 = Linear(4, 2).inputs()
    val cadd = CAddTable().inputs(fc1, fc2)
    val output1 = ReLU().inputs(cadd)
    val output2 = ReLU().inputs(cadd)

    val graph = Graph.dynamic(Array(fc1, fc2), Array(output1))
    fc1.element.getParameters()._1.apply1(_ => 1.0f)
    fc2.element.getParameters()._1.apply1(_ => 1.0f)
    val output = graph.forward(T(Tensor(T(0.1f, 0.2f, -0.3f, -0.4f)),
      Tensor(T(0.5f, 0.4f, -0.2f, -0.1f))))
    output should be(Tensor(T(2.2f, 2.2f)))
  }

  "Dynamic Graph init" should "throw exceptions when input a tensor while a table is required" in {
    val fc1 = Linear(4, 2).inputs()
    val fc2 = Linear(4, 2).inputs()
    val cadd = CAddTable().inputs(fc1, fc2)
    val output1 = ReLU().inputs(cadd)
    val output2 = ReLU().inputs(cadd)

    val graph = Graph.dynamic(Array(fc1, fc2), Array(output1, output2))
    intercept[com.intel.analytics.bigdl.dllib.utils.InvalidOperationException] {
      graph.forward(Tensor(T(0.1f, 0.2f, -0.3f, -0.4f)))
    }
  }

  "Dynamic Graph init" should "throw exceptions when inputs has pre-nodes" in {
    val fc1 = Linear(4, 2).inputs()
    val fc2 = Linear(4, 2).inputs()
    val tanh1 = Tanh().inputs(fc1)
    val tanh2 = Tanh().inputs(fc2)

    val cadd = CAddTable().inputs(tanh1, tanh2)
    val output1 = ReLU().inputs(cadd)
    val output2 = ReLU().inputs(cadd)

    intercept[IllegalArgumentException] {
      Graph.dynamic(Array(tanh1, tanh2), Array(output1, output2))
    }
  }

  "Dynamic Graph init" should "throw exceptions when inputs has nothing to do with the " +
    "graph but same number with the roots node in the graph" in {
    val fc1 = Linear(4, 2).inputs()
    val fc2 = Linear(4, 2).inputs()
    val fc3 = Linear(4, 2).inputs()
    val fc4 = Linear(4, 2).inputs()
    val tanh1 = Tanh().inputs(fc1)
    val tanh2 = Tanh().inputs(fc2)

    val cadd = CAddTable().inputs(tanh1, tanh2)
    val output1 = ReLU().inputs(cadd)
    val output2 = ReLU().inputs(cadd)

    intercept[IllegalArgumentException] {
      Graph.dynamic(Array(fc3, fc4), Array(output1, output2))
    }
  }

  "Dynamic Graph forward" should "be successful" in {
    val fc1 = Linear(4, 2).inputs()
    val fc2 = Linear(4, 2).inputs()
    val cadd = CAddTable().inputs(fc1, fc2)
    val output1 = ReLU().inputs(cadd)
    val output2 = Threshold(10.0).inputs(cadd)

    val graph = Graph.dynamic(Array(fc1, fc2), Array(output1, output2))
    fc1.element.getParameters()._1.apply1(_ => 1.0f)
    fc2.element.getParameters()._1.apply1(_ => 1.0f)
    val output = graph.forward(T(Tensor(T(0.1f, 0.2f, -0.3f, -0.4f)),
      Tensor(T(0.5f, 0.4f, -0.2f, -0.1f))))
    output should be(T(Tensor(T(2.2f, 2.2f)), Tensor(T(.0f, .0f))))
  }

  "Dynamic Graph forward" should "throw exceptions when input a table while " +
    "a tensor is required" in {
    val fc1 = Linear(4, 2).inputs()
    val output1 = ReLU().inputs(fc1)

    val graph = Graph.dynamic(Array(fc1), Array(output1))

    intercept[com.intel.analytics.bigdl.dllib.utils.UnKnownException] {
      graph.forward(T(Tensor(T(0.1f, 0.2f, -0.3f, -0.4f)),
        Tensor(T(0.5f, 0.4f, -0.2f, -0.1f))))
    }
  }

  "Dynamic Graph forward" should "be successful when first node accept multiple tensors input" in {
    val input1 = Input()
    val input2 = Input()
    val cadd = CAddTable().inputs(input1, input2)
    val graph = Graph.dynamic(Array(input1, input2), cadd)
    val output = graph.forward(T(Tensor(T(0.1f, 0.2f, -0.3f, -0.4f)),
      Tensor(T(0.5f, 0.4f, -0.2f, -0.1f))))
    output should be(Tensor(T(0.6f, 0.6f, -0.5f, -0.5f)))
  }

  "Dynamic Graph forward" should "be successful when exchange input order" in {
    val fc1 = Linear(4, 2).inputs()
    val fc2 = Linear(4, 2).inputs()
    val cadd = CAddTable().inputs(fc1, fc2)
    val output1 = ReLU().inputs(cadd)
    val output2 = Threshold(10.0).inputs(cadd)

    val graph = Graph.dynamic(Array(fc2, fc1), Array(output1, output2))
    fc1.element.getParameters()._1.apply1(_ => 1.0f)
    fc2.element.getParameters()._1.apply1(_ => 2.0f)
    val output = graph.forward(T(Tensor(T(0.1f, 0.2f, -0.3f, -0.4f)),
      Tensor(T(0.5f, 0.4f, -0.2f, -0.1f))))
    output should be(T(Tensor(T(2.8f, 2.8f)), Tensor(T(0.0f, 0.0f))))
  }

  "Dynamic Graph forward" should "be successful when paths has different length" in {
    val fc1 = Linear(4, 2).inputs()
    val thd1 = Threshold(-10.0).inputs(fc1)
    val thd2 = Threshold(-10.0).inputs(thd1)
    val thd3 = Threshold(-10.0).inputs(thd2)
    val thd4 = Threshold(-10.0).inputs(thd3)
    val fc2 = Linear(4, 2).inputs()
    val cadd = CAddTable().inputs(thd4, fc2)
    val output1 = ReLU().inputs(cadd)
    val output2 = Threshold(10.0).inputs(cadd)

    val graph = Graph.dynamic(Array(fc1, fc2), Array(output1, output2))
    fc1.element.getParameters()._1.apply1(_ => 1.0f)
    fc2.element.getParameters()._1.apply1(_ => 1.0f)
    val output = graph.forward(T(Tensor(T(0.1f, 0.2f, -0.3f, -0.4f)),
      Tensor(T(0.5f, 0.4f, -0.2f, -0.1f))))
    output should be(T(Tensor(T(2.2f, 2.2f)), Tensor(T(.0f, .0f))))
  }

  "Dynamic Graph forward" should "be successful when exchange output order" in {
    val fc1 = Linear(4, 2).inputs()
    val fc2 = Linear(4, 2).inputs()
    val cadd = CAddTable().inputs(fc1, fc2)
    val output1 = ReLU().inputs(cadd)
    val output2 = Threshold(10.0).inputs(cadd)

    val graph = Graph.dynamic(Array(fc1, fc2), Array(output2, output1))
    fc1.element.getParameters()._1.apply1(_ => 1.0f)
    fc2.element.getParameters()._1.apply1(_ => 2.0f)
    val output = graph.forward(T(Tensor(T(0.1f, 0.2f, -0.3f, -0.4f)),
      Tensor(T(0.5f, 0.4f, -0.2f, -0.1f))))
    output should be(T(Tensor(T(0.0f, 0.0f)), Tensor(T(3.8f, 3.8f))))
  }

  "Dynamic Graph forward" should "be correct when contains multi output node" in {
    val x = SplitTable(1).inputs()
    val y1 = Identity().inputs(x(1))
    val y2 = Identity().inputs(x(2))
    val z = CAddTable().inputs(y1, y2)

    val graph = Graph.dynamic(x, z)
    val output = graph.forward(Tensor(T(T(1, 2, 3), T(4, 2, 7))))
    output should be(Tensor(T(5, 4, 10)))
  }

  "Dynamic Graph forward" should "be correct when connect a table to a node" in {
    val x = SplitTable(1).inputs()
    val y = CAddTable().inputs(x)

    val graph = Graph.dynamic(x, y)
    val output = graph.forward(Tensor(T(T(1, 2, 3), T(4, 2, 7))))
    output should be(Tensor(T(5, 4, 10)))
  }

  "Dynamic Graph forward" should "be correct when contains multi output node with table output" in {
    val x = Identity().inputs()
    val y = SplitTable(1).inputs(x)

    val graph = Graph.dynamic(x, y)
    val output = graph.forward(Tensor(T(T(1, 2, 3), T(4, 2, 7))))
    output.toTable[Tensor[Float]](1) should be(Tensor(T(1, 2, 3)))
    output.toTable[Tensor[Float]](2) should be(Tensor(T(4, 2, 7)))
  }

  "Dynamic Graph forward" should "be correct when contains nested output" in {
    val x = Identity().inputs()
    val y1 = SplitTable(1).inputs(x)
    val y2 = Identity().inputs(y1(1))

    val graph = Graph.dynamic(x, Array(y1, y2))
    val output = graph.forward(Tensor(T(T(1, 2, 3), T(4, 2, 7))))
    val t1 = output.toTable[Table](1)
    t1[Tensor[Float]](1) should be(Tensor(T(1, 2, 3)))
    t1[Tensor[Float]](2) should be(Tensor(T(4, 2, 7)))
    output.toTable[Tensor[Float]](2) should be(Tensor(T(1, 2, 3)))
  }

  "Dynamic Graph backward" should "be successful" in {
    val fc1 = Linear(4, 2).inputs()
    val fc2 = Linear(4, 2).inputs()
    val cadd = CAddTable().inputs(fc1, fc2)
    val output1 = ReLU().inputs(cadd)
    val output2 = Threshold(10.0).inputs(cadd)

    val graph = Graph.dynamic(Array(fc1, fc2), Array(output1, output2))
    fc1.element.getParameters()._1.apply1(_ => 1.0f)
    fc2.element.getParameters()._1.apply1(_ => 2.0f)
    val output = graph.forward(T(Tensor(T(0.1f, 0.2f, -0.3f, -0.4f)),
      Tensor(T(0.5f, 0.4f, -0.2f, -0.1f))))
    val gradInput = graph.backward(T(Tensor(T(0.1f, 0.2f, -0.3f, -0.4f)),
      Tensor(T(0.5f, 0.4f, -0.2f, -0.1f))), T(Tensor(T(1.0f, 2.0f)), Tensor(T(3.0f, 4.0f))))
    gradInput should be(T(Tensor(T(3.0f, 3.0f, 3.0f, 3.0f)),
      Tensor(T(6.0f, 6.0f, 6.0f, 6.0f))))
    fc1.element.parameters()._2(0) should be(Tensor(T(T(0.1f, 0.2f, -0.3f, -0.4f),
      T(0.2f, 0.4f, -0.6f, -0.8f))))
    fc1.element.parameters()._2(1) should be(Tensor(T(1.0f, 2.0f)))
    fc2.element.parameters()._2(0) should be(Tensor(T(T(0.5f, 0.4f, -0.2f, -0.1f),
      T(1.0f, 0.8f, -0.4f, -0.2f))))
    fc2.element.parameters()._2(1) should be(Tensor(T(1.0f, 2.0f)))
  }

  "Dynamic Graph backward" should "be successful when first node accept multiple tensors input" in {
    val input1 = Input()
    val input2 = Input()
    val cadd = CAddTable().inputs(input1, input2)
    val graph = Graph.dynamic(Array(input1, input2), cadd)
    val output = graph.forward(T(Tensor(T(0.1f, 0.2f, -0.3f, -0.4f)),
      Tensor(T(0.5f, 0.4f, -0.2f, -0.1f))))
    output should be(Tensor(T(0.6f, 0.6f, -0.5f, -0.5f)))
    val gradient = graph.backward(T(Tensor(T(0.1f, 0.2f, -0.3f, -0.4f)),
      Tensor(T(0.5f, 0.4f, -0.2f, -0.1f))), Tensor(T(0.1f, 0.1f, 0.1f, 0.1f)))
    gradient should be(T(Tensor(T(0.1f, 0.1f, 0.1f, 0.1f)), Tensor(T(0.1f, 0.1f, 0.1f, 0.1f))))
  }

  "Dynamic Graph backward" should "be successful when paths have different length" in {
    val fc1 = Linear(4, 2).inputs()
    val thd1 = Threshold(-10.0).inputs(fc1)
    val thd2 = Threshold(-10.0).inputs(thd1)
    val thd3 = Threshold(-10.0).inputs(thd2)
    val thd4 = Threshold(-10.0).inputs(thd3)
    val fc2 = Linear(4, 2).inputs()
    val cadd = CAddTable().inputs(thd4, fc2)
    val output1 = ReLU().inputs(cadd)
    val output2 = Threshold(10.0).inputs(cadd)

    val graph = Graph.dynamic(Array(fc1, fc2), Array(output1, output2))
    fc1.element.getParameters()._1.apply1(_ => 1.0f)
    fc2.element.getParameters()._1.apply1(_ => 2.0f)
    val output = graph.forward(T(Tensor(T(0.1f, 0.2f, -0.3f, -0.4f)),
      Tensor(T(0.5f, 0.4f, -0.2f, -0.1f))))
    val gradInput = graph.backward(T(Tensor(T(0.1f, 0.2f, -0.3f, -0.4f)),
      Tensor(T(0.5f, 0.4f, -0.2f, -0.1f))), T(Tensor(T(1.0f, 2.0f)), Tensor(T(3.0f, 4.0f))))
    gradInput should be(T(Tensor(T(3.0f, 3.0f, 3.0f, 3.0f)),
      Tensor(T(6.0f, 6.0f, 6.0f, 6.0f))))
    fc1.element.parameters()._2(0) should be(Tensor(T(T(0.1f, 0.2f, -0.3f, -0.4f),
      T(0.2f, 0.4f, -0.6f, -0.8f))))
    fc1.element.parameters()._2(1) should be(Tensor(T(1.0f, 2.0f)))
    fc2.element.parameters()._2(0) should be(Tensor(T(T(0.5f, 0.4f, -0.2f, -0.1f),
      T(1.0f, 0.8f, -0.4f, -0.2f))))
    fc2.element.parameters()._2(1) should be(Tensor(T(1.0f, 2.0f)))
  }

  "Dynamic Graph backward" should "be successful when exchange input order" in {
    val fc1 = Linear(4, 2).inputs()
    val fc2 = Linear(4, 2).inputs()
    val cadd = CAddTable().inputs(fc1, fc2)
    val output1 = ReLU().inputs(cadd)
    val output2 = Threshold(10.0).inputs(cadd)

    val graph = Graph.dynamic(Array(fc2, fc1), Array(output1, output2))
    fc1.element.getParameters()._1.apply1(_ => 1.0f)
    fc2.element.getParameters()._1.apply1(_ => 2.0f)
    val output = graph.forward(T(Tensor(T(0.1f, 0.2f, -0.3f, -0.4f)),
      Tensor(T(0.5f, 0.4f, -0.2f, -0.1f))))
    val gradInput = graph.backward(T(Tensor(T(0.1f, 0.2f, -0.3f, -0.4f)),
      Tensor(T(0.5f, 0.4f, -0.2f, -0.1f))), T(Tensor(T(1.0f, 2.0f)), Tensor(T(3.0f, 4.0f))))
    gradInput should be(T(Tensor(T(6.0f, 6.0f, 6.0f, 6.0f)), Tensor(T(3.0f, 3.0f, 3.0f, 3.0f))))
    fc1.element.parameters()._2(0) should be(Tensor(T(T(0.5f, 0.4f, -0.2f, -0.1f),
      T(1.0f, 0.8f, -0.4f, -0.2f))))
    fc1.element.parameters()._2(1) should be(Tensor(T(1.0f, 2.0f)))
    fc2.element.parameters()._2(0) should be(Tensor(T(T(0.1f, 0.2f, -0.3f, -0.4f),
      T(0.2f, 0.4f, -0.6f, -0.8f))))
    fc2.element.parameters()._2(1) should be(Tensor(T(1.0f, 2.0f)))
  }

  "Dynamic Graph backward" should "be successful when exchange output order" in {
    val fc1 = Linear(4, 2).inputs()
    val fc2 = Linear(4, 2).inputs()
    val cadd = CAddTable().inputs(fc1, fc2)
    val output1 = ReLU().inputs(cadd)
    val output2 = Threshold(10.0).inputs(cadd)

    val graph = Graph.dynamic(Array(fc1, fc2), Array(output2, output1))
    fc1.element.getParameters()._1.apply1(_ => 1.0f)
    fc2.element.getParameters()._1.apply1(_ => 2.0f)
    val output = graph.forward(T(Tensor(T(0.1f, 0.2f, -0.3f, -0.4f)),
      Tensor(T(0.5f, 0.4f, -0.2f, -0.1f))))
    val gradInput = graph.backward(T(Tensor(T(0.1f, 0.2f, -0.3f, -0.4f)),
      Tensor(T(0.5f, 0.4f, -0.2f, -0.1f))), T(Tensor(T(1.0f, 2.0f)), Tensor(T(3.0f, 4.0f))))
    gradInput should be(T(Tensor(T(7.0f, 7.0f, 7.0f, 7.0f)), Tensor(T(14.0f, 14.0f, 14.0f, 14.0f))))
    fc1.element.parameters()._2(0) should be(Tensor(T(T(0.3f, 0.6f, -0.9f, -1.2f),
      T(0.4f, 0.8f, -1.2f, -1.6f))))
    fc1.element.parameters()._2(1) should be(Tensor(T(3.0f, 4.0f)))
    fc2.element.parameters()._2(0) should be(Tensor(T(T(1.5f, 1.2f, -0.6f, -0.3f),
      T(2.0f, 1.6f, -0.8f, -0.4f))))
    fc2.element.parameters()._2(1) should be(Tensor(T(3.0f, 4.0f)))
  }

  "Dynamic Graph backward" should "be correct when contains multi output node" in {
    val x = SplitTable(1).inputs()
    val y1 = Identity().inputs(x(1))
    val y2 = Identity().inputs(x(2))
    val z = CAddTable().inputs(y1, y2)

    val graph = Graph.dynamic(x, z)
    val output = graph.forward(Tensor(T(T(1, 2, 3), T(4, 2, 7))))
    val grads = graph.backward(Tensor(T(T(1, 2, 3), T(4, 2, 7))), Tensor(T(5, 4, 10)))
    grads should be(Tensor(T(T(5, 4, 10), T(5, 4, 10))))
  }

  "Dynamic Graph backward" should "be correct when contains multi output " +
    "node with table output" in {
    val x = Identity().inputs()
    val y = SplitTable(1).inputs(x)

    val graph = Graph.dynamic(x, y)
    val output = graph.forward(Tensor(T(T(1, 2, 3), T(4, 2, 7))))
    val grad = graph.backward(Tensor(T(T(1, 2, 3), T(4, 2, 7))),
      T(Tensor(T(3, 2, 1)), Tensor(T(5, 7, 9))))
    grad should be(Tensor(T(T(3, 2, 1), T(5, 7, 9))))
  }

  "Dynamic Graph backward" should "be correct when connect a table to a node" in {
    val x = SplitTable(1).inputs()
    val y = CAddTable().inputs(x)

    val graph = Graph.dynamic(x, y)
    val output = graph.forward(Tensor(T(T(1, 2, 3), T(4, 2, 7))))
    val grads = graph.backward(Tensor(T(T(1, 2, 3), T(4, 2, 7))), Tensor(T(5, 4, 10)))
    grads should be(Tensor(T(T(5, 4, 10), T(5, 4, 10))))
  }

  "Dynamic Graph backward" should "be correct when contains nested output" in {
    val x = Identity().inputs()
    val y1 = SplitTable(1).inputs(x)
    val y2 = Identity().inputs(y1(1))

    val graph = Graph.dynamic(x, Array(y1, y2))
    val output = graph.forward(Tensor(T(T(1, 2, 3), T(4, 2, 7))))
    val result = graph.backward(Tensor(T(T(1, 2, 3), T(4, 2, 7))),
      T(T(Tensor(T(2, 7, 8)), Tensor(T(1, 5, 3))), Tensor(T(5, 4, 10))))
    result should be(Tensor(T(T(7, 11, 18), T(1, 5, 3))))
  }

  "Dynamic Graph forward/backward" should "be successful when there's output " +
    "from internal node" in {
    val input1 = Input()
    val input2 = Input()
    val add = CAddTable().inputs(input1, input2)
    val add2 = AddConstant(2.0f).inputs(add)
    val relu = ReLU().inputs(add2)
    val graph = Graph[Float](Array(input1, input2), Array(add, relu))

    val input = T(Tensor(T(1.0f, 2.0f)), Tensor(T(-2.0f, -1.0f)))
    val output = graph.forward(input)
    val gradient = graph.backward(input, T(Tensor(T(1.0f, 2.0f)), Tensor(T(-2.0f, -1.0f))))
    val output1 = output.toTable[Tensor[Float]](1)
    val output2 = output.toTable[Tensor[Float]](2)

    output1 should be(Tensor[Float](T(-1.0f, 1.0f)))
    output2 should be(Tensor[Float](T(1.0f, 3.0f)))
    gradient should be(T(Tensor(T(-1.0f, 1.0f)), Tensor(T(-1.0f, 1.0f))))
  }

  "lenet" should "be same with sequential model" in {
    RandomGenerator.RNG.setSeed(1000)
    val seqModel = Sequential().add(Reshape(Array(1, 28, 28)))
      .add(SpatialConvolution(1, 6, 5, 5).setName("conv1_5x5"))
      .add(Tanh())
      .add(SpatialMaxPooling(2, 2, 2, 2))
      .add(Tanh())
      .add(SpatialConvolution(6, 12, 5, 5).setName("conv2_5x5"))
      .add(SpatialMaxPooling(2, 2, 2, 2))
      .add(Reshape(Array(12 * 4 * 4)))
      .add(Linear(12 * 4 * 4, 100).setName("fc1"))
      .add(Tanh())
      .add(Linear(100, 10).setName("fc2"))
      .add(LogSoftMax())

    RandomGenerator.RNG.setSeed(1000)
    val input = Reshape(Array(1, 28, 28)).inputs()
    val conv1 = SpatialConvolution(1, 6, 5, 5).inputs(input)
    val tanh1 = Tanh().inputs(conv1)
    val pool1 = SpatialMaxPooling(2, 2, 2, 2).inputs(tanh1)
    val tanh2 = Tanh().inputs(pool1)
    val conv2 = SpatialConvolution(6, 12, 5, 5).inputs(tanh2)
    val pool2 = SpatialMaxPooling(2, 2, 2, 2).inputs(conv2)
    val reshape = Reshape(Array(12 * 4 * 4)).inputs(pool2)
    val fc1 = Linear(12 * 4 * 4, 100).inputs(reshape)
    val tanh3 = Tanh().inputs(fc1)
    val fc2 = Linear(100, 10).inputs(tanh3)
    val output = LogSoftMax().inputs(fc2)

    val funcModel = Graph.dynamic(input, output)

    val inputData = Tensor(4, 28 * 28).rand()
    val outputData1 = seqModel.forward(inputData) // warm up
    var start = System.nanoTime()
    seqModel.forward(inputData)
    println(s"seq model forward time is ${(System.nanoTime() - start) / 1e6}ms")
    start = System.nanoTime()
    val outputData2 = funcModel.forward(inputData)
    println(s"funcModel model forward time is ${(System.nanoTime() - start) / 1e6}ms")

    outputData1 should be(outputData2)

    val gradient = Tensor(4, 10).rand()
    start = System.nanoTime()
    val gradientBP1 = seqModel.backward(inputData, gradient)
    println(s"seq model backward time is ${(System.nanoTime() - start) / 1e6}ms")
    start = System.nanoTime()
    val gradientBP2 = funcModel.backward(inputData, gradient)
    println(s"funcModel model backward time is ${(System.nanoTime() - start) / 1e6}ms")

    gradientBP1 should be(gradientBP2)
  }

  "ResNet-18 basic block shortcut type A" should "be correct" in {
    RandomGenerator.RNG.setSeed(1000)
    val seqModel = ModelUntils.ResNet.basicBlockSeq(16, 16, 1, "A")
    RandomGenerator.RNG.setSeed(1000)
    val input = Input()
    val output = ModelUntils.ResNet.basicBlockSeq(16, 16, 1, "A").inputs(input)
    val funcModel = Graph.dynamic(input, output)

    println(seqModel)
    val inputData = Tensor(4, 16, 32, 32).rand()
    var start = System.nanoTime()
    val output1 = seqModel.forward(inputData)
    println(s"seq model forward time is ${(System.nanoTime() - start) / 1e6}ms")

    start = System.nanoTime()
    val output2 = funcModel.forward(inputData)
    println(s"func model forward time is ${(System.nanoTime() - start) / 1e6}ms")

    output1 should be(output2)

    val gradients = Tensor(4, 16, 32, 32).rand()
    start = System.nanoTime()
    val gradients1 = seqModel.backward(inputData, gradients)
    println(s"seq model backward time is ${(System.nanoTime() - start) / 1e6}ms")
    start = System.nanoTime()
    val gradients2 = funcModel.backward(inputData, gradients)
    println(s"func model backward time is ${(System.nanoTime() - start) / 1e6}ms")

    gradients1 should be(gradients2)
    seqModel.getParameters()._2 should be(funcModel.getParameters()._2)
  }

  "ResNet-18 basic block shortcut type C" should "be correct" in {
    RandomGenerator.RNG.setSeed(1000)
    val seqModel = ModelUntils.ResNet.basicBlockSeq(16, 16, 1, "C")
    RandomGenerator.RNG.setSeed(1000)
    val input = Input()
    val output = ModelUntils.ResNet.basicBlockFunc(16, 16, 1, "C")(input)
    val funcModel = Graph.dynamic(input, output)

    println(seqModel)
    val inputData = Tensor(4, 16, 32, 32).rand()
    var start = System.nanoTime()
    val output1 = seqModel.forward(inputData)
    println(s"seq model forward time is ${(System.nanoTime() - start) / 1e6}ms")

    start = System.nanoTime()
    val output2 = funcModel.forward(inputData)
    println(s"func model forward time is ${(System.nanoTime() - start) / 1e6}ms")

    output1 should be(output2)

    val gradients = Tensor(4, 16, 32, 32).rand()
    start = System.nanoTime()
    val gradients1 = seqModel.backward(inputData, gradients)
    println(s"seq model backward time is ${(System.nanoTime() - start) / 1e6}ms")
    start = System.nanoTime()
    val gradients2 = funcModel.backward(inputData, gradients)
    println(s"func model backward time is ${(System.nanoTime() - start) / 1e6}ms")

    gradients1 should be(gradients2)

    seqModel.getParametersTable()[Table]("conv1")[Tensor[Float]]("gradWeight") should be(
      funcModel.getParametersTable()[Table]("conv1")[Tensor[Float]]("gradWeight")
    )

    seqModel.getParametersTable()[Table]("bn1")[Tensor[Float]]("gradWeight") should be(
      funcModel.getParametersTable()[Table]("bn1")[Tensor[Float]]("gradWeight")
    )

    seqModel.getParametersTable()[Table]("conv2")[Tensor[Float]]("gradWeight") should be(
      funcModel.getParametersTable()[Table]("conv2")[Tensor[Float]]("gradWeight")
    )

    seqModel.getParametersTable()[Table]("bn2")[Tensor[Float]]("gradWeight") should be(
      funcModel.getParametersTable()[Table]("bn2")[Tensor[Float]]("gradWeight")
    )
  }

  "InceptionV1 block" should "be correct" in {
    RandomGenerator.RNG.setSeed(1000)
    val seqModel = ModelUntils.Inception.inceptionLayerV1Seq(
      2, T(T(4), T(96, 128), T(16, 32), T(32)))

    RandomGenerator.RNG.setSeed(1000)
    val input = Input()
    val output = ModelUntils.Inception.inceptionLayerV1Func(
      2, T(T(4), T(96, 128), T(16, 32), T(32)))(input)
    val funcModel = Graph.dynamic(input, output)

    println(seqModel)
    val inputData = Tensor(1, 2, 4, 4).rand()
    var start = System.nanoTime()
    val output1 = seqModel.forward(inputData)
    println(s"seq model forward time is ${(System.nanoTime() - start) / 1e6}ms")

    start = System.nanoTime()
    val output2 = funcModel.forward(inputData)
    println(s"func model forward time is ${(System.nanoTime() - start) / 1e6}ms")

    output1 should be(output2)
    val gradient = Tensor(1, 256, 4, 4).rand()
    start = System.nanoTime()
    val gradient1 = seqModel.backward(inputData, gradient)
    println(s"seq model backward time is ${(System.nanoTime() - start) / 1e6}ms")

    start = System.nanoTime()
    val gradient2 = funcModel.backward(inputData, gradient)

    println(s"func model backward time is ${(System.nanoTime() - start) / 1e6}ms")

    gradient1 should be(gradient2)

    seqModel.getParametersTable()[Table]("conv1x1")[Tensor[Float]]("gradWeight") should be(
      funcModel.getParametersTable()[Table]("conv1x1")[Tensor[Float]]("gradWeight")
    )

    seqModel.getParametersTable()[Table]("conv3x3_1")[Tensor[Float]]("gradWeight") should be(
      funcModel.getParametersTable()[Table]("conv3x3_1")[Tensor[Float]]("gradWeight")
    )

    seqModel.getParametersTable()[Table]("conv3x3_2")[Tensor[Float]]("gradWeight") should be(
      funcModel.getParametersTable()[Table]("conv3x3_2")[Tensor[Float]]("gradWeight")
    )

    seqModel.getParametersTable()[Table]("conv5x5_1")[Tensor[Float]]("gradWeight") should be(
      funcModel.getParametersTable()[Table]("conv5x5_1")[Tensor[Float]]("gradWeight")
    )

    seqModel.getParametersTable()[Table]("conv5x5_2")[Tensor[Float]]("gradWeight") should be(
      funcModel.getParametersTable()[Table]("conv5x5_2")[Tensor[Float]]("gradWeight")
    )

    seqModel.getParametersTable()[Table]("pool_conv")[Tensor[Float]]("gradWeight") should be(
      funcModel.getParametersTable()[Table]("pool_conv")[Tensor[Float]]("gradWeight")
    )
  }

  "Autoencoder graph" should "be correct" in {
    new SecureRandom().setSeed(1)
    val batchSize = 4
    val input = Tensor[Float](batchSize, 28 * 28).apply1(e => new SecureRandom().nextFloat())
    val gradOutput = Tensor[Float](batchSize, 784).apply1(e => new SecureRandom().nextFloat())

    RNG.setSeed(1000)
    val model = Autoencoder(32)
    RNG.setSeed(1000)
    val graphModel = DynamicTestModels.Autoencoder.graph(32)

    val output1 = model.forward(input).toTensor[Float]
    val output2 = graphModel.forward(input).toTensor[Float]
    output1 should be(output2)

    val gradInput1 = model.backward(input, gradOutput)
    val gradInput2 = graphModel.backward(input, gradOutput)
    gradInput1 should be(gradInput2)
    gradInput1 should be(gradInput2)
  }

  "Lenet graph" should "be correct" in {
    new SecureRandom().setSeed(1)
    val batchSize = 4
    val input = Tensor[Float](batchSize, 28*28).apply1(e => new SecureRandom().nextFloat())
    val gradOutput = Tensor[Float](batchSize, 10).apply1(e => new SecureRandom().nextFloat())

    RNG.setSeed(1000)
    val model = LeNet5(10)
    RNG.setSeed(1000)
    val graphModel = LeNet5.graph(10)

    val output1 = model.forward(input).toTensor[Float]
    val output2 = graphModel.forward(input).toTensor[Float]
    output1 should be(output2)

    val gradInput1 = model.backward(input, gradOutput)
    val gradInput2 = graphModel.backward(input, gradOutput)
    gradInput1 should be(gradInput2)
    model.getParameters().equals(graphModel.getParameters()) should be(true)
  }

  "VggForCifar10 graph" should "be correct" in {
    new SecureRandom().setSeed(1)
    val batchSize = 4
    val input = Tensor[Float](batchSize, 3, 32, 32).apply1(e => new SecureRandom().nextFloat())
    val gradOutput = Tensor[Float](batchSize, 10).apply1(e => new SecureRandom().nextFloat())

    RNG.setSeed(1000)
    val model = VggForCifar10(10, false)
    RNG.setSeed(1000)
    val graphModel = DynamicTestModels.VggForCifar10.graph(10, false)

    val output1 = model.forward(input).toTensor[Float]
    val output2 = graphModel.forward(input).toTensor[Float]
    output1 should be(output2)

    val gradInput1 = model.backward(input, gradOutput)
    val gradInput2 = graphModel.backward(input, gradOutput)
    gradInput1 should be(gradInput2)
  }

  "Vgg_16 graph" should "be correct" in {
    new SecureRandom().setSeed(1)
    val batchSize = 1
    val input = Tensor[Float](batchSize, 3, 224, 224).apply1(e => new SecureRandom().nextFloat())
    val gradOutput = Tensor[Float](1000).apply1(e => new SecureRandom().nextFloat())

    RNG.setSeed(1000)
    val model = Vgg_16(1000, false)
    RNG.setSeed(1000)
    val graphModel = DynamicTestModels.Vgg_16.graph(1000, false)

    val output1 = model.forward(input).toTensor[Float]
    val output2 = graphModel.forward(input).toTensor[Float]
    output1 should be(output2)

    val gradInput1 = model.backward(input, gradOutput)
    val gradInput2 = graphModel.backward(input, gradOutput)
    gradInput1 should be(gradInput2)
  }

  "Vgg_19 graph" should "be correct" in {
    new SecureRandom().setSeed(1)
    val batchSize = 1
    val input = Tensor[Float](batchSize, 3, 224, 224).apply1(e => new SecureRandom().nextFloat())
    val gradOutput = Tensor[Float](1000).apply1(e => new SecureRandom().nextFloat())

    RNG.setSeed(1000)
    val model = Vgg_19(1000, false)
    RNG.setSeed(1000)
    val graphModel = DynamicTestModels.Vgg_19.graph(1000, false)

    val output1 = model.forward(input).toTensor[Float]
    val output2 = graphModel.forward(input).toTensor[Float]
    output1 should be(output2)

    val gradInput1 = model.backward(input, gradOutput)
    val gradInput2 = graphModel.backward(input, gradOutput)
    gradInput1 should be(gradInput2)
  }

  "Dynamic Graph backward sequential with propagateBack false in the " +
    "first" should "work properly" in {
    RandomGenerator.RNG.setSeed(1000)
    val input = Reshape(Array(1, 28, 28)).setName("reshape").inputs()
    val conv1 = SpatialConvolution(1, 6, 5, 5).setName("conv1").inputs(input)
    val tanh1 = Tanh().inputs(conv1)
    val pool1 = SpatialMaxPooling(2, 2, 2, 2).inputs(tanh1)
    val tanh2 = Tanh().inputs(pool1)
    val conv2 = SpatialConvolution(6, 12, 5, 5).inputs(tanh2)
    val pool2 = SpatialMaxPooling(2, 2, 2, 2).inputs(conv2)
    val reshape = Reshape(Array(12 * 4 * 4)).inputs(pool2)
    val fc1 = Linear(12 * 4 * 4, 100).inputs(reshape)
    val tanh3 = Tanh().inputs(fc1)
    val fc2 = Linear(100, 10).inputs(tanh3)
    val output = LogSoftMax().inputs(fc2)

    RandomGenerator.RNG.setSeed(1000)
    val input2 = Reshape(Array(1, 28, 28)).inputs()
    val conv1_2 = SpatialConvolution(1, 6, 5, 5).setName("conv1").inputs(input2)
    val tanh1_2 = Tanh().inputs(conv1_2)
    val pool1_2 = SpatialMaxPooling(2, 2, 2, 2).inputs(tanh1_2)
    val tanh2_2 = Tanh().inputs(pool1_2)
    val conv2_2 = SpatialConvolution(6, 12, 5, 5).inputs(tanh2_2)
    val pool2_2 = SpatialMaxPooling(2, 2, 2, 2).inputs(conv2_2)
    val reshape_2 = Reshape(Array(12 * 4 * 4)).inputs(pool2_2)
    val fc1_2 = Linear(12 * 4 * 4, 100).inputs(reshape_2)
    val tanh3_2 = Tanh().inputs(fc1_2)
    val fc2_2 = Linear(100, 10).inputs(tanh3_2)
    val output_2 = LogSoftMax().inputs(fc2_2)

    val funcModelNoBack = Graph.dynamic(input, output)
    val funcModelOriginal = Graph.dynamic(input2, output_2)

    funcModelNoBack.stopGradient(Array("reshape"))

    val inputData = Tensor(4, 28 * 28).rand()
    val outputData1 = funcModelOriginal.forward(inputData) // warm up
    var start = System.nanoTime()
    funcModelOriginal.forward(inputData)
    println(s"seq model forward time is ${ (System.nanoTime() - start) / 1e6 }ms")
    start = System.nanoTime()
    val outputData2 = funcModelNoBack.forward(inputData)
    println(s"funcModel model forward time is ${ (System.nanoTime() - start) / 1e6 }ms")

    outputData1 should be(outputData2)

    val gradient = Tensor(4, 10).rand()
    start = System.nanoTime()
    val gradientBPOriginal = funcModelOriginal.backward(inputData, gradient)
    println(s"seq model backward time is ${ (System.nanoTime() - start) / 1e6 }ms")
    start = System.nanoTime()
    val gradientBPNoBack = funcModelNoBack.backward(inputData, gradient)
    println(s"funcModel model backward time is ${ (System.nanoTime() - start) / 1e6 }ms")

    gradientBPNoBack.isInstanceOf[EmptyGradInput] should be(true)
    val namedModule1 = funcModelOriginal.getParametersTable()
    val namedModule2 = funcModelNoBack.getParametersTable()
    namedModule1("conv1").asInstanceOf[Table] should
      equal(namedModule2("conv1").asInstanceOf[Table])
    funcModelOriginal.getParameters()._2 should be(funcModelNoBack.getParameters()._2)
  }

  "Dynamic Graph backward propagateBack false in the middle" should "work properly " +
    "in sequential lenet" in {
    RandomGenerator.RNG.setSeed(1000)
    val input = Reshape(Array(1, 28, 28)).setName("r1").inputs()
    val conv1 = SpatialConvolution(1, 6, 5, 5).setName("conv1").inputs(input)
    val tanh1 = Tanh().setName("tanh1").inputs(conv1)
    val pool1 = SpatialMaxPooling(2, 2, 2, 2).setName("pool1").inputs(tanh1)
    val tanh2 = Tanh().setName("tanh2").inputs(pool1)
    val conv2 = SpatialConvolution(6, 12, 5, 5).setName("conv2").inputs(tanh2)
    val pool2 = SpatialMaxPooling(2, 2, 2, 2).inputs(conv2)
    val reshape = Reshape(Array(12 * 4 * 4)).inputs(pool2)
    val fc1 = Linear(12 * 4 * 4, 100).inputs(reshape)
    val tanh3 = Tanh().inputs(fc1)
    val fc2 = Linear(100, 10).inputs(tanh3)
    val output = LogSoftMax().inputs(fc2)

    RandomGenerator.RNG.setSeed(1000)
    val input2 = Reshape(Array(1, 28, 28)).setName("r1").inputs()
    val conv1_2 = SpatialConvolution(1, 6, 5, 5).setName("conv1").inputs(input2)
    val tanh1_2 = Tanh().setName("tanh1").inputs(conv1_2)
    val pool1_2 = SpatialMaxPooling(2, 2, 2, 2).setName("pool1").inputs(tanh1_2)
    val tanh2_2 = Tanh().setName("tanh2").inputs(pool1_2)
    val conv2_2 = SpatialConvolution(6, 12, 5, 5).inputs(tanh2_2)
    val pool2_2 = SpatialMaxPooling(2, 2, 2, 2).inputs(conv2_2)
    val reshape_2 = Reshape(Array(12 * 4 * 4)).inputs(pool2_2)
    val fc1_2 = Linear(12 * 4 * 4, 100).inputs(reshape_2)
    val tanh3_2 = Tanh().inputs(fc1_2)
    val fc2_2 = Linear(100, 10).inputs(tanh3_2)
    val output_2 = LogSoftMax().inputs(fc2_2)

    val funcModelNoBack = Graph.dynamic(input, output)
    funcModelNoBack.stopGradient(Array("pool1"))
    val funcModelOriginal = Graph.dynamic(input2, output_2)

    val inputData = Tensor(4, 28 * 28).rand()
    val outputData1 = funcModelOriginal.forward(inputData)
    val outputData2 = funcModelNoBack.forward(inputData)
    outputData1 should be(outputData2)

    val gradient = Tensor(4, 10).rand()
    val gradientBPOriginal = funcModelOriginal.backward(inputData, gradient)
    val gradientBPNoBack = funcModelNoBack.backward(inputData, gradient)

    gradientBPNoBack.isInstanceOf[EmptyGradInput] should be(true)
    val namedModule1 = Utils.getNamedModules(funcModelOriginal)
    val namedModule2 = Utils.getNamedModules(funcModelNoBack)
    namedModule2("r1").gradInput.toTensor.nElement() should be(0)
    namedModule2("conv1").gradInput.toTensor.nElement() should be(0)
    namedModule2("tanh1").gradInput.toTensor.nElement() should be(0)
    namedModule2("pool1").gradInput.toTensor.nElement() should be(0)

    namedModule2("conv2").asInstanceOf[SpatialConvolution[Float]].parameters()._2 should be(
      namedModule2("conv2").asInstanceOf[SpatialConvolution[Float]].parameters()._2)
  }

  "Dynamic Graph propagate false in subpath" should "work properly" in {
    RandomGenerator.RNG.setSeed(1000)
    val fc1 = Linear(4, 2).inputs()
    val fc2 = Linear(4, 2).inputs()
    val cadd = CAddTable().inputs(fc1, fc2)
    val output1 = ReLU().inputs(cadd)
    val output2 = Threshold(10.0).inputs(cadd)

    val graph = Graph.dynamic(Array(fc2, fc1), Array(output1, output2))
    RandomGenerator.RNG.setSeed(1000)
    val fc1_1 = Linear(4, 2).inputs()
    val fc2_1 = Linear(4, 2).inputs()
    val cadd_1 = CAddTable().inputs(fc1_1, fc2_1)
    val output1_1 = ReLU().setName("relu").inputs(cadd_1)
    val output2_1 = Threshold(10.0).inputs(cadd_1)

    val graphNoBack = Graph.dynamic(Array(fc2_1, fc1_1), Array(output1_1, output2_1))
    graphNoBack.stopGradient(Array("relu"))

    RandomGenerator.RNG.setSeed(1000)
    val fc1_2 = Linear(4, 2).inputs()
    val fc2_2 = Linear(4, 2).inputs()
    val cadd_2 = CAddTable().inputs(fc1_2, fc2_2)
    val output2_2 = Threshold(10.0).inputs(cadd_2)

    val graphNoBackExpect = Graph.dynamic(Array(fc2_2, fc1_2), Array(output2_2))

    fc1.element.getParameters()._1.apply1(_ => 1.0f)
    fc2.element.getParameters()._1.apply1(_ => 2.0f)
    fc1_1.element.getParameters()._1.apply1(_ => 1.0f)
    fc2_1.element.getParameters()._1.apply1(_ => 2.0f)
    fc1_2.element.getParameters()._1.apply1(_ => 1.0f)
    fc2_2.element.getParameters()._1.apply1(_ => 2.0f)

    val input = T(Tensor(T(0.1f, 0.2f, -0.3f, -0.4f)),
      Tensor(T(0.5f, 0.4f, -0.2f, -0.1f)))
    graph.forward(input) should be (graphNoBack.forward(input))


    val gradOutput = T(Tensor(T(1.0f, 2.0f)), Tensor(T(3.0f, 4.0f)))
    val gradInput = graph.backward(input, gradOutput)

    graph.backward(input, gradOutput)
    graphNoBack.backward(input, gradOutput)
    graphNoBackExpect.forward(input)
    graphNoBackExpect.backward(input, Tensor(T(3.0f, 4.0f)))
    output1_1.element.gradInput.toTensor.nElement() should be (0)
    cadd_2.element.gradInput should be (cadd_1.element.gradInput)
    fc1_2.element.gradInput should be (fc1_1.element.gradInput)
    fc2_2.element.gradInput should be (fc2_1.element.gradInput)
    output2.element.gradInput should be (output2_1.element.gradInput)
  }

  "Dynamic Graph propagate false in concat subpath" should "work properly" in {
    RandomGenerator.RNG.setSeed(1000)
    val fc1 = Linear(4, 2).inputs()
    val fc2 = Linear(4, 2).inputs()
    val cadd = CAddTable().inputs(fc1, fc2)
    val output1 = ReLU().inputs(cadd)
    val output2 = Threshold(10.0).inputs(cadd)

    val graph = Graph.dynamic(Array(fc2, fc1), Array(output1, output2))
    RandomGenerator.RNG.setSeed(1000)
    val fc1_1 = Linear(4, 2).inputs()
    val fc2_1 = Linear(4, 2).setName("fc2_1").inputs()
    val cadd_1 = CAddTable().inputs(fc1_1, fc2_1)
    val output1_1 = ReLU().inputs(cadd_1)
    val output2_1 = Threshold(10.0).inputs(cadd_1)

    val graphNoBack = Graph.dynamic(Array(fc2_1, fc1_1), Array(output1_1, output2_1))
    graphNoBack.stopGradient(Array("fc2_1"))

    fc1.element.getParameters()._1.apply1(_ => 1.0f)
    fc2.element.getParameters()._1.apply1(_ => 2.0f)
    fc1_1.element.getParameters()._1.apply1(_ => 1.0f)
    fc2_1.element.getParameters()._1.apply1(_ => 2.0f)

    val input = T(Tensor(T(0.1f, 0.2f, -0.3f, -0.4f)),
      Tensor(T(0.5f, 0.4f, -0.2f, -0.1f)))
    graph.forward(input) should be (graphNoBack.forward(input))


    val gradOutput = T(Tensor(T(1.0f, 2.0f)), Tensor(T(3.0f, 4.0f)))

    graph.backward(input, gradOutput)
    graphNoBack.backward(input, gradOutput)
    fc2_1.element.gradInput.toTensor.nElement() should be (0)
    output2.element.gradInput should be (output2_1.element.gradInput)
    fc1_1.element.gradInput should be (fc1.element.gradInput)
    fc1_1.element.parameters()._2 should be (fc1.element.parameters()._2)
  }

  "Dynamic Graph propagate false in concat subpath with longer edge" should "work properly" in {
    RandomGenerator.RNG.setSeed(1000)
    val fc1 = Linear(4, 2).inputs()
    val fc2 = Linear(4, 2).inputs()
    val cadd = CAddTable().inputs(fc1, fc2)
    val output1 = ReLU().inputs(cadd)
    val output2 = Threshold(10.0).inputs(cadd)

    val graph = Graph.dynamic(Array(fc2, fc1), Array(output1, output2))
    RandomGenerator.RNG.setSeed(1000)
    val reshape = Reshape(Array(4)).inputs()
    val fc1_1 = Linear(4, 2).inputs()
    val fc2_1 = Linear(4, 2).setName("fc2_1").inputs(reshape)
    val cadd_1 = CAddTable().inputs(fc1_1, fc2_1)
    val output1_1 = ReLU().inputs(cadd_1)
    val output2_1 = Threshold(10.0).inputs(cadd_1)

    val graphNoBack = Graph.dynamic(Array(reshape, fc1_1), Array(output1_1, output2_1))
    graphNoBack.stopGradient(Array("fc2_1"))
    fc1.element.getParameters()._1.apply1(_ => 1.0f)
    fc2.element.getParameters()._1.apply1(_ => 2.0f)
    fc1_1.element.getParameters()._1.apply1(_ => 1.0f)
    fc2_1.element.getParameters()._1.apply1(_ => 2.0f)

    val input = T(Tensor(T(0.1f, 0.2f, -0.3f, -0.4f)),
      Tensor(T(0.5f, 0.4f, -0.2f, -0.1f)))
    graph.forward(input) should be (graphNoBack.forward(input))


    val gradOutput = T(Tensor(T(1.0f, 2.0f)), Tensor(T(3.0f, 4.0f)))

    graph.backward(input, gradOutput)
    graphNoBack.backward(input, gradOutput)
    fc2_1.element.gradInput.toTensor.nElement() should be (0)
    output2.element.gradInput should be (output2_1.element.gradInput)
    fc1_1.element.gradInput should be (fc1.element.gradInput)
    fc1_1.element.parameters()._2 should be (fc1.element.parameters()._2)
    reshape.element.gradInput.toTensor.nElement() should be (0)
  }

  "Dynamic Graph propagate false reset to true" should "work properly" in {
    RandomGenerator.RNG.setSeed(1000)
    val fc1 = Linear(4, 2).inputs()
    val fc2 = Linear(4, 2).inputs()
    val cadd = CAddTable().inputs(fc1, fc2)
    val output1 = ReLU().inputs(cadd)
    val output2 = Threshold(10.0).inputs(cadd)

    val graph = Graph.dynamic(Array(fc2, fc1), Array(output1, output2))
    RandomGenerator.RNG.setSeed(1000)
    val fc1_1 = Linear(4, 2).inputs()
    val fc2_1 = Linear(4, 2).setName("fc2_1").inputs()
    val cadd_1 = CAddTable().inputs(fc1_1, fc2_1)
    val output1_1 = ReLU().inputs(cadd_1)
    val output2_1 = Threshold(10.0).inputs(cadd_1)

    val graphNoBack = Graph.dynamic(Array(fc2_1, fc1_1), Array(output1_1, output2_1))
    graphNoBack.stopGradient(Array("fc2_1"))

    fc1.element.getParameters()._1.apply1(_ => 1.0f)
    fc2.element.getParameters()._1.apply1(_ => 2.0f)
    fc1_1.element.getParameters()._1.apply1(_ => 1.0f)
    fc2_1.element.getParameters()._1.apply1(_ => 2.0f)

    val input = T(Tensor(T(0.1f, 0.2f, -0.3f, -0.4f)),
      Tensor(T(0.5f, 0.4f, -0.2f, -0.1f)))
    graph.forward(input) should be (graphNoBack.forward(input))


    val gradOutput = T(Tensor(T(1.0f, 2.0f)), Tensor(T(3.0f, 4.0f)))

    graph.backward(input, gradOutput)
    graphNoBack.backward(input, gradOutput)
    fc2_1.element.gradInput.toTensor.nElement() should be (0)
    output2.element.gradInput should be (output2_1.element.gradInput)
    fc1_1.element.gradInput should be (fc1.element.gradInput)
    fc1_1.element.parameters()._2 should be (fc1.element.parameters()._2)

    // reset propagateBack
    graphNoBack.reset()
    graphNoBack.buildBackwardGraph()
    graphNoBack.zeroGradParameters()
    graphNoBack.forward(input) should be (graph.forward(input))
    graphNoBack.backward(input, gradOutput)

    graphNoBack.parameters()._1 should be (graph.parameters()._1)

    graphNoBack.parameters()._2 should be (graph.parameters()._2)
  }

  "Dynamic Graph backpropagation" should "ignore nodes on non output path" in {
    val node1 = Identity[Float]().setName("node1").inputs()
    val node2 = Identity[Float]().setName("node2").inputs(node1)
    val node3 = Identity[Float]().setName("node3").inputs(node2)
    val node4 = Identity[Float]().setName("node4").inputs(node2)

    val model1 = Graph[Float](node1, node3)
    model1.forward(Tensor[Float](T(1.0f, 2.0f))) should be(Tensor[Float](T(1.0f, 2.0f)))
    model1.backward(Tensor[Float](T(1.0f, 2.0f)), Tensor[Float](T(3.0f, 4.0f))) should be(
      Tensor[Float](T(3.0f, 4.0f)))

    val model2 = Graph[Float](node1, Array(node3, node4))
    model2.forward(Tensor[Float](T(1.0f, 2.0f))) should be(T(Tensor[Float](T(1.0f, 2.0f)),
      Tensor[Float](T(1.0f, 2.0f))))
    model2.backward(Tensor[Float](T(1.0f, 2.0f)), T(Tensor[Float](T(3.0f, 4.0f)),
      Tensor[Float](T(7.0f, 10.0f)))) should be(
      Tensor[Float](T(10.0f, 14.0f)))
  }

  "markdown test" should "work" in {
    val reshape = Reshape(Array(4)).inputs()
    val fc1 = Linear(4, 2).setName("fc1").inputs()
    val fc2 = Linear(4, 2).setName("fc2").inputs(reshape)
    val cadd_1 = CAddTable().setName("cadd").inputs(fc1, fc2)
    val output1_1 = ReLU().inputs(cadd_1)
    val output2_1 = Threshold(10.0).inputs(cadd_1)

    val model = Graph.dynamic(Array(reshape, fc1), Array(output1_1, output2_1))

    val input = T(Tensor(T(0.1f, 0.2f, -0.3f, -0.4f)),
      Tensor(T(0.5f, 0.4f, -0.2f, -0.1f)))
    val gradOutput = T(Tensor(T(1.0f, 2.0f)), Tensor(T(3.0f, 4.0f)))

    fc1.element.getParameters()._1.apply1(_ => 1.0f)
    fc2.element.getParameters()._1.apply1(_ => 2.0f)
    model.zeroGradParameters()
    println("output1: \n", model.forward(input))
    model.backward(input, gradOutput)
    println("fc2 weight \n", fc2.element.parameters()._1(0))


    fc1.element.getParameters()._1.apply1(_ => 1.0f)
    fc2.element.getParameters()._1.apply1(_ => 2.0f)
    model.zeroGradParameters()
    model.freeze("fc2")
    println("output2: \n", model.forward(input))
    model.backward(input, gradOutput)
    println("fc2 weight \n", fc2.element.parameters()._1(0))

    fc1.element.getParameters()._1.apply1(_ => 1.0f)
    fc2.element.getParameters()._1.apply1(_ => 2.0f)
    model.zeroGradParameters()
    model.unFreeze()
    println("output3: \n", model.forward(input))
    model.backward(input, gradOutput)
    println("fc2 weight \n", fc2.element.parameters()._1(0))

    fc1.element.getParameters()._1.apply1(_ => 1.0f)
    fc2.element.getParameters()._1.apply1(_ => 2.0f)
    model.stopGradient(Array("cadd"))
    model.zeroGradParameters()
    println("output4: \n", model.forward(input))
    model.backward(input, gradOutput)
    println("fc1 weight \n", fc1.element.parameters()._1(0))
    println("fc2 weight \n", fc2.element.parameters()._1(0))
  }

  "Dynamic Graph setFreeze" should "work properly" in {
    RandomGenerator.RNG.setSeed(1000)
    val fc1 = Linear(4, 2).inputs()
    val fc2 = Linear(4, 2).inputs()
    val cadd = CAddTable().inputs(fc1, fc2)
    val output1 = ReLU().inputs(cadd)
    val output2 = Threshold(10.0).inputs(cadd)

    val graph = Graph.dynamic(Array(fc2, fc1), Array(output1, output2))
    RandomGenerator.RNG.setSeed(1000)
    val reshape = Reshape(Array(4)).inputs()
    val fc1_1 = Linear(4, 2).inputs()
    val fc2_1 = Linear(4, 2).setName("fc2_1").inputs(reshape)
    val cadd_1 = CAddTable().inputs(fc1_1, fc2_1)
    val output1_1 = ReLU().inputs(cadd_1)
    val output2_1 = Threshold(10.0).inputs(cadd_1)

    val graphNoBack = Graph.dynamic(Array(reshape, fc1_1), Array(output1_1, output2_1))
    graphNoBack.stopGradient(Array("fc2_1"))

    fc1.element.getParameters()._1.apply1(_ => 1.0f)
    fc2.element.getParameters()._1.apply1(_ => 2.0f)
    fc1_1.element.getParameters()._1.apply1(_ => 1.0f)
    fc2_1.element.getParameters()._1.apply1(_ => 2.0f)

    val input = T(Tensor(T(0.1f, 0.2f, -0.3f, -0.4f)),
      Tensor(T(0.5f, 0.4f, -0.2f, -0.1f)))
    graph.forward(input) should be (graphNoBack.forward(input))


    val gradOutput = T(Tensor(T(1.0f, 2.0f)), Tensor(T(3.0f, 4.0f)))

    graph.backward(input, gradOutput)
    graphNoBack.backward(input, gradOutput)
    fc2_1.element.gradInput.toTensor.nElement() should be (0)
    output2.element.gradInput should be (output2_1.element.gradInput)
    fc1_1.element.gradInput should be (fc1.element.gradInput)
    fc1_1.element.parameters()._2 should be (fc1.element.parameters()._2)
    reshape.element.gradInput.toTensor.nElement() should be (0)
  }

  "save graph to tensorboard log dir" should "work" in {
    System.setProperty("bigdl.localMode", "true")
    Engine.init
    val tmpFile = java.io.File.createTempFile("Dynamic Graph", "tensorboard")
    val absolutePath = tmpFile.getAbsolutePath
    tmpFile.delete()

    val model = DynamicTestModels.Inception_v1_NoAuxClassifier.graph(1000, true)
      .asInstanceOf[Graph[Float]]
    model.saveGraphTopology(absolutePath)
    System.clearProperty("bigdl.localMode")
  }

  "Dynamic Graph" should "support switch with two branch" in {
    val data = Input("data")
    val condition = Input("condition")
    val swtich = ControlNodes.switch(condition, data)
    val echo1 = Echo().inputs(swtich.trueEdge())
    val echo2 = Echo().inputs(swtich.falseEdge())

    val model = Graph.dynamic(Array(data, condition), Array(echo1), None, false)
    val result = model.forward(T(Tensor[Float](T(1)), Tensor[Boolean](T(true))))
    result.toTensor should be(Tensor[Float](T(1)))

    intercept[com.intel.analytics.bigdl.dllib.utils.UnKnownException] {
      model.forward(T(Tensor[Float](T(1)), Tensor[Boolean](T(false))))
    }
  }

  "Dynamic Graph" should "support switch with two branch with merge" in {
    val data = Input("data")
    val condition = Input("condition")
    val swtich = ControlNodes.switch(condition, data)
    val echo1 = Echo().inputs(swtich.trueEdge())
    val echo2 = Echo().inputs(swtich.falseEdge())
    val add1 = AddConstant(1).inputs(echo1)
    val add5 = AddConstant(5).inputs(echo2)
    val merge = ControlNodes.merge(add1, add5)
    val output = Identity().inputs(merge)

    val model = Graph.dynamic(Array(data, condition), Array(output), None, false)
    var result = model.forward(T(Tensor[Float](T(1)), Tensor[Boolean](T(true))))
    result.toTensor should be(Tensor[Float](T(2)))
    result = model.forward(T(Tensor[Float](T(1)), Tensor[Boolean](T(false))))
    result.toTensor should be(Tensor[Float](T(6)))
  }

  "Dynamic Graph backward with stopGradient" should "not remove stopGradient recursive" in {
    val data = Input()
    val d1 = Identity().inputs(data)
    val d2 = Identity().inputs(d1)
    val d3 = Identity().inputs(data)
    val d4 = Identity().setName("d4").inputs(d3)
    val d5 = Identity().inputs(d4)

    val model = Graph.dynamic(data, Array(d2, d5))
    val output = model.forward(Tensor[Float](T(1, 2, 3))).toTable
    output[Tensor[Float]](1) should be(Tensor[Float](T(1, 2, 3)))
    output[Tensor[Float]](2) should be(Tensor[Float](T(1, 2, 3)))

    model.stopGradient(Array("d4"))
    model.backward(Tensor[Float](T(1, 2, 3)), T(Tensor[Float](T(2, 7, 9)),
      Tensor[Float](T(1, 3, 5)))) should be(Tensor[Float](T(2, 7, 9)))
  }

  "Dynamic Graph forward" should "not execute unrelated node" in {
    val data = Identity().setName("input").inputs()
    var isExecuted = false
    val l1 = Identity().setName("l1").inputs(data)
    val l2 = Identity().setName("l2").inputs(l1)
    val l3 = Identity().setName("l3").inputs(l2)
    val l4 = Echo().setName("l4").setFeval((a, b) => isExecuted = true).inputs(l1)

    val model = Graph.dynamic(data, l3)
    model.forward(Tensor(T(1)))
    isExecuted should be(false)
  }

  "Dynamic Graph backward" should "not execute unrelated node" in {
    val data = Identity().setName("input").inputs()
    val const = Const(Tensor(T(1, 2))).setName("const").inputs()
    var isExecuted = false
    val l1 = Echo().setName("l1").setBeval((a, b, c) => isExecuted = true).inputs(const)
    val cadd = CAddTable().setName("cadd").inputs(data, l1)

    val model = Graph.dynamic(data, cadd)
    model.forward(Tensor(T(3, 5))) should be(Tensor(T(4, 7)))
    model.backward(Tensor(T(3, 5)), Tensor(T(1, 2))) should be(Tensor(T(1, 2)))
    isExecuted should be(false)
  }

  "Dynamic Graph backward" should "not execute unrelated node 2" in {
    val data = Identity().setName("input").inputs()
    val const = Const(Tensor(T(1, 2))).setName("const").inputs()
    var isExecuted1 = false
    val l1 = Echo().setName("l1").setBeval((a, b, c) => isExecuted1 = true).inputs(const)
    val cadd = CAddTable().setName("cadd").inputs(data, l1)
    val l2 = Identity().setName("l2").inputs(cadd)
    var isExecuted2 = false
    var isExecuted3 = false
    val echo = Echo().setName("echo")
      .setFeval((a, b) => isExecuted2 = true)
      .setBeval((a, b, c) => isExecuted3 = true).inputs(cadd)
    val l3 = Identity().setName("l3").inputs(echo)

    val model = Graph.dynamic(data, l2)
    model.forward(Tensor(T(3, 5))) should be(Tensor(T(4, 7)))
    model.backward(Tensor(T(3, 5)), Tensor(T(1, 2))) should be(Tensor(T(1, 2)))
    isExecuted1 should be(false)
    isExecuted2 should be(false)
    isExecuted3 should be(false)
  }

  "Dynamic Graph get name" should "be correct" in {
    val data = Identity().setName("input").inputs()
    val const = Const(Tensor(T(1, 2))).setName("const").inputs()
    var isExecuted1 = false
    val l1 = Echo().setName("l1").setBeval((a, b, c) => isExecuted1 = true).inputs(const)
    val cadd = CAddTable().setName("cadd").inputs(data, l1)
    val l2 = Identity().setName("l2").inputs(cadd)
    var isExecuted2 = false
    var isExecuted3 = false
    val echo = Echo().setName("echo")
      .setFeval((a, b) => isExecuted2 = true)
      .setBeval((a, b, c) => isExecuted3 = true).inputs(cadd)
    val l3 = Identity().setName("l3").inputs(echo)

    val model = Graph.dynamic(data, l2)
    model.node("l1") should be(l1)

    intercept[com.intel.analytics.bigdl.dllib.utils.UnKnownException] {
      model.node("ll1")
    }
  }

  "Dynamic Graph" should "support while loop" in {
    val input = Input("input")

    val conditionInput = Input("conditionInput")
    val const = new com.intel.analytics.bigdl.dllib.nn.tf.Const(Tensor(T(9))).inputs()
    val constEnter = new Enter("test_frame").inputs(const)
    val less = Less().inputs(constEnter, conditionInput)

    val updateInput = Input()
    val add = AddConstant(1).inputs(updateInput)
    val addEnter = new Enter("test_frame").inputs(add)
    val echo = Echo().inputs(addEnter)

    val exit = ControlNodes.whileLoop(
      (Seq(conditionInput), less),
      (Seq((updateInput, echo))),
      Seq(input),
      "while"
    )
    val model = Graph.dynamic(Array(input), Array(exit(0)), None, false)
    val result = model.forward(Tensor(T(1)))
    result.toTensor.valueAt(1) should be(10)
  }

  "Dynamic Graph" should "support while loop twice and const node should not be executed twice" in {
    val input = Input()

    val conditionInput = Input()
    val const = new com.intel.analytics.bigdl.dllib.nn.tf.Const(Tensor(T(9))).inputs()
    var count = 0
    def feval(module: Echo[Float], input: Tensor[Float]): Unit = {
      count += 1
    }
    val echo = Echo(feval).inputs(const)
    val less = Less().inputs(echo, conditionInput)

    val updateInput = Input()
    val add = AddConstant(1).inputs(updateInput)

    val exit = ControlNodes.whileLoop(
      (Seq(conditionInput), less),
      Seq((updateInput, add)),
      Seq(input)
    )
    val model = Graph.dynamic(Array(input), Array(exit(0)), None, false)
    model.forward(Tensor(T(1)))
    val result = model.forward(Tensor(T(1)))
    result.toTensor.valueAt(1) should be(10)
    count should be(1)
  }

  "Dynamic Graph" should "support loop twice and merge after const node should be triggered" in {
    val input = new com.intel.analytics.bigdl.dllib.nn.tf.Const(Tensor(T(1))).inputs()

    val conditionInput = Input()
    val const = new com.intel.analytics.bigdl.dllib.nn.tf.Const(Tensor(T(9))).inputs()
    var count = 0
    def feval(module: Echo[Float], input: Tensor[Float]): Unit = {
      count += 1
    }
    val echo = Echo(feval).inputs(const)
    val less = Less().inputs(echo, conditionInput)

    val updateInput = Input()
    val add = AddConstant(1).inputs(updateInput)

    val exit = ControlNodes.whileLoop(
      (Seq(conditionInput), less),
      Seq((updateInput, add)),
      Seq(input)
    )
    val model = Graph.dynamic(Array[ModuleNode[Float]](),
      Array(exit(0)), None, false)
    model.forward(null)
    val result = model.forward(null)
    result.toTensor.valueAt(1) should be(10)
    count should be(1)
  }

  "Dynamic Graph" should "support while loop with multiple loop vars" in {
    val input1 = Input("Input1")
    val input2 = Input("Input2")

    val conditionInput1 = Input("conditionInput1")
    val conditionInput2 = Input("conditionInput2")
    val const = new com.intel.analytics.bigdl.dllib.nn.tf.Const(Tensor(T(9))).
      setName("inc").inputs()
    val less = Less().setName("less").inputs(const, conditionInput1)

    val updateInput1 = Input("updateInput1")
    val add1 = AddConstant(1).setName("add1").inputs(updateInput1)
    val echo1 = Echo().setName("echo1").inputs(add1)

    val updateInput2 = Input("updateInput2")
    val add2 = AddConstant(5).setName("add5").inputs(updateInput2)
    val echo2 = Echo().setName("echo2").inputs(add2)

    val exit = ControlNodes.whileLoop(
      (Seq(conditionInput1, conditionInput2), less),
      (Seq((updateInput1, echo1), (updateInput2, echo2))),
      Seq(input1, input2),
      "while"
    )
    val model = Graph.dynamic(Array(input1, input2), exit.toArray, None, false)
    val result = model.forward(T(Tensor(T(1)), Tensor(T(2))))
    result.toTable.apply[Tensor[Float]](1).valueAt(1) should be(10)
    result.toTable.apply[Tensor[Float]](2).valueAt(1) should be(47)
  }

  "DynamicGraph" should "not contain duplicate modules" in {
    val n1 = Identity[Float]().inputs()
    val n2 = Identity[Float]().inputs()
    val duplicate = Identity[Float]()
    val n3 = duplicate.inputs(n1)
    val n4 = duplicate.inputs(n2)
    intercept[IllegalArgumentException] {
      val model = Graph.dynamic(Array(n1, n2), Array(n3, n4))
    }
  }
}

class DynamicGraphSerialTest extends ModuleSerializationTest {
  override def test(): Unit = {
    val linear = Linear[Float](2, 2)
    val linearNode = linear.inputs()
    val linearWeight = linear.weight
    val linearBias = linear.bias
    val variables = Some(Array(linearWeight), Array(linearBias))
    val graphWithVariable = Graph.dynamic[Float](Array(linearNode), Array(linearNode),
      variables, false).setName("graphWithVariable")
    val input = Tensor[Float](2).apply1(_ => new SecureRandom().nextFloat())
    runSerializationTest(graphWithVariable, input)
  }
}
