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

import java.io.File

import com.intel.analytics.bigdl.dllib.tensor.Tensor
import com.intel.analytics.bigdl.dllib.utils.RandomGenerator._
import com.intel.analytics.bigdl.dllib.utils.serializer.{ModuleLoader, ModulePersister, ModuleSerializationTest}

import java.security.SecureRandom


class DetectionOutputFrcnnSerialTest extends ModuleSerializationTest {
  override def test(): Unit = {
    val module = DetectionOutputFrcnn().setName("DetectionOutputFrcnn")
    val name = module.getName
    val serFile = File.createTempFile(name, postFix)

    ModulePersister.saveToFile[Float](serFile.getAbsolutePath, null, module.evaluate(), true)
    RNG.setSeed(1000)
    val loadedModule = ModuleLoader.loadFromFile[Float](serFile.getAbsolutePath)


    if (serFile.exists) {
      serFile.delete
    }
    tested.add(module.getClass.getName)
  }
}
