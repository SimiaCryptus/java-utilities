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

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.wrappers.RefHashMap;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public @RefAware
class LocalAppSettings {

  private static final Logger logger = LoggerFactory.getLogger(LocalAppSettings.class);

  public final Map<String, String> properties = new HashMap<>();

  public LocalAppSettings(RefHashMap<String, String> properties) {
    this.properties.putAll(properties);
    if (null != properties)
      properties.freeRef();
  }

  public static RefHashMap<String, String> read() {
    return read(new File("."));

  }

  public static RefHashMap<String, String> read(File workingDir) {
    File parentFile = workingDir.getParentFile();
    File file = new File(workingDir, "app.json");
    if (file.exists()) {
      RefHashMap<String, String> settings = null;
      try {
        settings = JsonUtil.getMapper()
            .readValue(new String(FileUtils.readFileToByteArray(file), Charset.forName("UTF-8")), RefHashMap.class);
        settings.forEach((k, v) -> logger.info(String.format("Loaded %s = %s from %s", k, v, file)));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      if (parentFile != null && parentFile.exists()) {
        settings.putAll(read(parentFile));
      }
      return settings;
    } else if (parentFile != null && parentFile.exists()) {
      return read(parentFile);
    } else {
      return new RefHashMap<>();
    }

  }
}
