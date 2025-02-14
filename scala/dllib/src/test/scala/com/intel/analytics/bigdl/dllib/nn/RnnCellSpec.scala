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
import com.intel.analytics.bigdl.dllib.utils.T
import com.intel.analytics.bigdl.dllib.utils.serializer.ModuleSerializationTest

import java.security.SecureRandom


class RnnCellSerialTest extends ModuleSerializationTest {
  override def test(): Unit = {
    val rnnCell = RnnCell[Float](6, 4, Sigmoid[Float]()).setName("rnnCell")
    val input1 = Tensor[Float](Array(1, 4)).apply1(_ => new SecureRandom().nextFloat())
    val input2 = Tensor[Float](Array(1, 4)).apply1(_ => new SecureRandom().nextFloat())
    val input = T()
    input(1.0f) = input1
    input(2.0f) = input2
    runSerializationTest(rnnCell, input)
  }
}
