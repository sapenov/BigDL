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

package com.intel.analytics.bigdl.keras.nn

import com.intel.analytics.bigdl.keras.KerasBaseSpec
import com.intel.analytics.bigdl.dllib.nn.abstractnn.AbstractModule
import com.intel.analytics.bigdl.dllib.nn.internal.{AveragePooling3D, Sequential => KSequential}
import com.intel.analytics.bigdl.dllib.tensor.Tensor
import com.intel.analytics.bigdl.dllib.utils.Shape
import com.intel.analytics.bigdl.dllib.utils.serializer.ModuleSerializationTest

import java.security.SecureRandom

class AveragePooling3DSpec extends KerasBaseSpec {

  "AveragePooling3D" should "be the same as Keras" in {
    val kerasCode =
      """
        |input_tensor = Input(shape=[3, 12, 12, 12])
        |input = np.random.random([2, 3, 12, 12, 12])
        |output_tensor = AveragePooling3D(dim_ordering="th")(input_tensor)
        |model = Model(input=input_tensor, output=output_tensor)
      """.stripMargin
    val seq = KSequential[Float]()
    val layer = AveragePooling3D[Float](inputShape = Shape(3, 12, 12, 12))
    seq.add(layer)
    seq.getOutputShape().toSingle().toArray should be (Array(-1, 3, 6, 6, 6))
    checkOutputAndGrad(seq.asInstanceOf[AbstractModule[Tensor[Float], Tensor[Float], Float]],
      kerasCode)
  }

}

class AveragePooling3DSerialTest extends ModuleSerializationTest {
  override def test(): Unit = {
    val layer = AveragePooling3D[Float](inputShape = Shape(3, 12, 12, 12))
    layer.build(Shape(2, 3, 12, 12, 12))
    val input = Tensor[Float](2, 3, 12, 12, 12).apply1(_ => new SecureRandom().nextFloat())
    runSerializationTest(layer, input)
  }
}
