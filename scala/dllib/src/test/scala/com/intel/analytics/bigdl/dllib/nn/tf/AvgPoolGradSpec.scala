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

package com.intel.analytics.bigdl.dllib.nn.tf

import com.intel.analytics.bigdl.dllib.nn.abstractnn.DataFormat
import com.intel.analytics.bigdl.dllib.tensor.Tensor
import com.intel.analytics.bigdl.dllib.utils.T
import com.intel.analytics.bigdl.dllib.utils.serializer.ModuleSerializationTest

import java.security.SecureRandom

class AvgPoolGradSerialTest extends ModuleSerializationTest {
  override def test(): Unit = {
    val avgPoolGrad = AvgPoolGrad[Float](4, 4, 1, 1, -1, -1, DataFormat.NHWC).
      setName("avgPoolGrad")
    val input1 = Tensor[Int](T(4, 32, 32, 3))
    val input2 = Tensor[Float](4, 32, 32, 3).apply1(_ => new SecureRandom().nextFloat())
    val input = T(input1, input2)
    runSerializationTest(avgPoolGrad, input)
  }
}
