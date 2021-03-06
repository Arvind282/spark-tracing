/* Copyright 2017 IBM Corp.
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

package org.apache.spark.instrument

import scala.collection.mutable.ListBuffer

object Util {
  def psplit(s: String): Seq[String] = {
    var lvl = 0
    val cur = new StringBuilder
    val ret = new ListBuffer[String]
    s.foreach { c =>
      if (c == '(') lvl += 1
      else if ( c == ')') lvl -= 1
      if (lvl < 0) throw new RuntimeException("Mismatched parentheses: " + s)
      if (c == ',' && lvl == 0) {
        ret += cur.toString
        cur.clear
      }
      else cur += c
    }
    if (lvl > 0) throw new RuntimeException("Mismatched parentheses: " + s)
    ret += cur.toString
    ret
  }
  def tokenize(str: String, re: String): Seq[String] = {
    val idx = re.r.findAllMatchIn(str).flatMap(m => Seq(m.start, m.start + m.matched.length)).toSeq
    if (idx.isEmpty) Seq(str)
    else {
      val substrs = (0, idx.head) +: (1 until idx.size).map(i => (idx(i - 1), idx(i))) :+ (idx.last, str.length)
      substrs.map(pair => str.substring(pair._1, pair._2))
    }
  }
}
