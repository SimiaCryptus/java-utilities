/*
 * Copyright (c) 2018 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.lang;

/**
 * The type Not implemented exception.
 */
public class NotImplementedException extends RuntimeException {

  /**
   * Instantiates a new Not implemented exception.
   */
// Show a warning whenever this is referenced - We shoud implement it!
  @Deprecated
  public NotImplementedException() {
    super();
  }

  /**
   * Instantiates a new Not implemented exception.
   *
   * @param arg0 the arg 0
   */
// Show a warning whenever this is referenced - We shoud implement it!
  @Deprecated
  public NotImplementedException(final String arg0) {
    super(arg0);
  }

}