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
package com.intel.analytics.bigdl.friesian

import java.io.{File => JFile}
import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.Files

import com.intel.analytics.bigdl.dllib.nn.abstractnn.{AbstractModule, Activity}
import com.intel.analytics.bigdl.dllib.tensor.Tensor
import com.intel.analytics.bigdl.dllib.utils.{RandomGenerator, Table}
import com.intel.analytics.bigdl.dllib.common.zooUtils
import org.apache.logging.log4j.LogManager
// import com.intel.analytics.bigdl.dllib.models.common.ZooModel
import org.apache.commons.io.FileUtils
import org.scalactic.TolerantNumerics
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.collection.mutable.ArrayBuffer

abstract class ZooSpecHelper extends FlatSpec with Matchers with BeforeAndAfter {
  protected val logger = LogManager.getLogger(getClass)

  private val epsilon = 1e-4f

  implicit val doubleEq = TolerantNumerics.tolerantDoubleEquality(epsilon)

  implicit val floatEq = TolerantNumerics.tolerantFloatEquality(epsilon)

  private val tmpDirs : ArrayBuffer[JFile] = new ArrayBuffer[JFile]()

  def createTmpFile(permissions: String = "rw-------"): JFile = {
    val file = Files.createTempFile("UnitTest", "AnalyticsZooSpecBase",
      PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString(permissions)))
      .toFile
    logger.info(s"created file $file")
    file.deleteOnExit()
    file
  }

  def createTmpDir(permissions: String = "rwx------"): JFile = {
    val dir = zooUtils.createTmpDir("ZooUT", permissions).toFile()
    logger.info(s"created directory $dir")
    tmpDirs.append(dir)
    dir
  }

  protected def getFileFolder(path: String): String = {
    path.substring(0, path.lastIndexOf(JFile.separator))
  }

  protected def getFileName(path: String): String = {
    path.substring(path.lastIndexOf(JFile.separator) + 1)
  }

  def doAfter(): Unit = {}

  def doBefore(): Unit = {}

  before {
    doBefore()
  }

  after {
    doAfter()
    tmpDirs.foreach(dir => {
      if (dir.exists() && dir.isDirectory()) {
        FileUtils.deleteDirectory(dir)
        logger.info(s"deleted directory $dir")
      }
    })
  }

  def compareOutputAndGradInput(model1: AbstractModule[Tensor[Float], Tensor[Float], Float],
                                model2: AbstractModule[Tensor[Float], Tensor[Float], Float],
                                input: Tensor[Float],
                                precision: Double = 1e-5, compareBackward: Boolean = true): Unit = {
    // Set seed in case of random factors such as dropout
    val seed = System.currentTimeMillis()
    RandomGenerator.RNG.setSeed(seed)
    val output1 = model1.forward(input)
    RandomGenerator.RNG.setSeed(seed)
    val output2 = model2.forward(input)
    output2.size().sameElements(output1.size()) should be (true)
    output2.almostEqual(output1, precision) should be (true)
    if(compareBackward) {
      RandomGenerator.RNG.setSeed(seed)
      val gradInput1 = model1.backward(input, output1)
      RandomGenerator.RNG.setSeed(seed)
      val gradInput2 = model2.backward(input, output2)
      gradInput2.size().sameElements(gradInput1.size()) should be (true)
      gradInput2.almostEqual(gradInput1, precision) should be (true)
    }
  }

  def compareOutputAndGradInputTable2Tensor(model1: AbstractModule[Table, Tensor[Float], Float],
      model2: AbstractModule[Table, Tensor[Float], Float],
      input: Table,
      precision: Double = 1e-5): Unit = {
    // Set seed in case of random factors such as dropout
    val seed = System.currentTimeMillis()
    RandomGenerator.RNG.setSeed(seed)
    val output1 = model1.forward(input)
    RandomGenerator.RNG.setSeed(seed)
    val output2 = model2.forward(input)
    output2.size().sameElements(output1.size()) should be (true)
    output2.almostEqual(output1, precision) should be (true)
    RandomGenerator.RNG.setSeed(seed)
    val gradInput1 = model1.backward(input, output1)
    RandomGenerator.RNG.setSeed(seed)
    val gradInput2 = model2.backward(input, output2)
    gradInput1.length() == gradInput2.length() should be (true)
    var i = 1
    while (i < gradInput1.length() + 1) {
      val gtensor2 = gradInput2.get[Tensor[Float]](i).get
      val gtensor1 = gradInput1.get[Tensor[Float]](i).get
      gtensor2.size.sameElements(gtensor1.size) should be (true)
      gtensor2.almostEqual(gtensor1, precision) should be (true)
      i += 1
    }

  }

  def compareOutputAndGradInputSetWeights(
      model1: AbstractModule[Tensor[Float], Tensor[Float], Float],
      model2: AbstractModule[Tensor[Float], Tensor[Float], Float],
      input: Tensor[Float],
      precision: Double = 1e-5): Unit = {
    if (model1.getWeightsBias() != null) {
      model2.setWeightsBias(model1.getWeightsBias())
    }
    compareOutputAndGradInput(model1, model2, input, precision)
  }

/*
  def testZooModelLoadSave[Model](model: ZooModel[Tensor[Float], Tensor[Float], Float],
                                  input: Tensor[Float],
                                  loader: (String, String) => Model,
                                  precision: Double = 1e-5): Unit = {
    val serFile = createTmpFile()
    model.saveModel(serFile.getAbsolutePath, overWrite = true)
    val loadedModel = loader(serFile.getAbsolutePath, null)
      .asInstanceOf[ZooModel[Tensor[Float], Tensor[Float], Float]]
    TestUtils.conditionFailTest(loadedModel.modules.length == 1)
    compareOutputAndGradInput(model, loadedModel, input, precision)
  }

  def testZooModelLoadSave2[Model](model: ZooModel[Table, Tensor[Float], Float],
                                   input: Table,
                                   loader: (String, String) => Model,
                                   precision: Double = 1e-5): Unit = {
    val serFile = createTmpFile()
    model.saveModel(serFile.getAbsolutePath, overWrite = true)
    val loadedModel = loader(serFile.getAbsolutePath, null)
      .asInstanceOf[ZooModel[Table, Tensor[Float], Float]]
    TestUtils.conditionFailTest(loadedModel.modules.length == 1)
    compareOutputAndGradInputTable2Tensor(model, loadedModel, input, precision)
  }
*/
}

trait SerialSpecHelper extends ZooSpecHelper {
}

// Make methods in ZooSpecHelper static so that SerialTest classes can call directly
object ZooSpecHelper extends SerialSpecHelper {
}
