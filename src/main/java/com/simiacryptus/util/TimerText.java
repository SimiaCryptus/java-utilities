/*
 * Copyright (c) 2019 by Andrew Charneski.
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

package com.simiacryptus.util;

import com.simiacryptus.ref.wrappers.RefSystem;

import javax.annotation.Nonnull;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TimerText {
  final long start = RefSystem.currentTimeMillis();
  @Nonnull
  private final SimpleDateFormat formatter;

  public TimerText() {
    formatter = new SimpleDateFormat("[HH:mm:ss]");
    formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  @Nonnull
  @Override
  public String toString() {
    return formatter.format(new Date(RefSystem.currentTimeMillis() - start));
  }
}
