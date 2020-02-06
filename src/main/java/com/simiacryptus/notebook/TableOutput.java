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

package com.simiacryptus.notebook;

import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.RefString;
import com.simiacryptus.util.data.DoubleStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class TableOutput {
  public static final Logger logger = LoggerFactory.getLogger(TableOutput.class);
  public final List<Map<CharSequence, Object>> rows = new ArrayList<>();
  public final Map<CharSequence, Class<?>> schema = new LinkedHashMap<>();

  @Nonnull
  public static TableOutput create(@Nonnull final Map<CharSequence, Object>... rows) {
    @Nonnull final TableOutput table = new TableOutput();
    Arrays.stream(rows).forEach(properties -> table.putRow(properties));
    return table;
  }

  @Nonnull
  public TableOutput calcNumberStats() {
    @Nonnull final TableOutput tableOutput = new TableOutput();
    schema.entrySet().stream().filter(x -> {
      boolean temp_04_0001 = Number.class.isAssignableFrom(x.getValue());
      RefUtil.freeRef(x);
      return temp_04_0001;
    }).map(col -> {
      final CharSequence key = col.getKey();
      RefUtil.freeRef(col);
      final DoubleStatistics stats = rows.stream().filter(x -> x.containsKey(key)).map(x -> (Number) x.get(key))
          .collect(DoubleStatistics.NUMBERS);
      @Nonnull final LinkedHashMap<CharSequence, Object> row = new LinkedHashMap<>();
      row.put("field", key);
      row.put("sum", stats.getSum());
      row.put("avg", stats.getAverage());
      row.put("stddev", stats.getStandardDeviation());
      row.put("nulls", rows.size() - stats.getCount());
      return row;
    }).sorted(Comparator.comparing(x -> x.get("field").toString())).forEach(row -> tableOutput.putRow(row));
    return tableOutput;
  }

  public void clear() {
    schema.clear();
    rows.clear();
  }

  public void putRow(@Nonnull final Map<? extends CharSequence, ? extends Object> properties) {
    for (@Nonnull final Entry<? extends CharSequence, ? extends Object> prop : properties.entrySet()) {
      final CharSequence propKey = prop.getKey();
      if (null != propKey) {
        Object value = prop.getValue();
        if (null != value) {
          final Class<?> cellType = value.getClass();
          Class<?> colType = schema.getOrDefault(propKey, cellType);
          if (!colType.isAssignableFrom(cellType)) {
            logger.warn(RefString.format("Schema mismatch for %s (%s != %s)", propKey, colType, cellType));
          }
          schema.putIfAbsent(propKey, cellType);
        }
      }
    }
    rows.add(new HashMap<>(properties));
  }

  public CharSequence toCSV(final boolean sortCols) {
    try (@Nonnull
         ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
      try (@Nonnull
           PrintStream printStream = new PrintStream(buffer)) {
        @Nonnull final Collection<CharSequence> keys = sortCols ? new TreeSet<CharSequence>(schema.keySet()) : schema.keySet();
        final String formatString = keys.stream().map(k -> {
          switch (schema.get(k).getSimpleName()) {
            case "String":
              return "%-" + rows.stream().mapToInt(x -> x.getOrDefault(k, "").toString().length()).max().getAsInt() + "s";
            case "Integer":
              return "%6d";
            case "Double":
              return "%.4f";
            default:
              return "%s";
          }
        }).collect(Collectors.joining(","));
        printStream.println(keys.stream().collect(Collectors.joining(",")).trim());
        for (@Nonnull final Map<CharSequence, Object> row : rows) {
          printStream.println(RefString.format(formatString, keys.stream().map(k -> row.get(k)).toArray()));
        }
      }
      return buffer.toString();
    } catch (@Nonnull final IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String toHtmlTable() {
    return toHtmlTable(false);
  }

  public String toHtmlTable(final boolean sortCols) {
    try (@Nonnull
         ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
      try (@Nonnull
           PrintStream printStream = new PrintStream(buffer)) {
        @Nonnull final Collection<CharSequence> keys = sortCols ? new TreeSet<CharSequence>(schema.keySet()) : schema.keySet();
        final String formatString = keys.stream().map(k -> {
          switch (schema.get(k).getSimpleName()) {
            case "String":
              return "%-" + rows.stream().mapToInt(x -> x.getOrDefault(k, "").toString().length()).max().getAsInt() + "s";
            case "Integer":
              return "%6d";
            case "Double":
              return "%.4f";
            default:
              return "%s";
          }
        }).map(s -> "<td>" + s + "</td>").collect(Collectors.joining(""));
        printStream.print("<table border=1>");
        printStream.print("<tr>");
        printStream.println(keys.stream().map(s -> "<th>" + s + "</th>").collect(Collectors.joining("")).trim());
        printStream.print("</tr>");
        for (@Nonnull final Map<CharSequence, Object> row : rows) {
          printStream.print("<tr>");
          printStream.println(RefString.format(formatString, keys.stream().map(k -> row.get(k)).toArray()));
          printStream.print("</tr>");
        }
        printStream.print("</table>");
      }
      return buffer.toString();
    } catch (@Nonnull final IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String toMarkdownTable() {
    try (@Nonnull
         ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
      try (@Nonnull
           PrintStream printStream = new PrintStream(buffer)) {
        final String formatString = schema.entrySet().stream().map(e -> {
          try {
            switch (e.getValue().getSimpleName()) {
              case "String":
                CharSequence key = e.getKey();
                return "%-" + rows.stream()
                    .mapToInt(x -> {
                      Object val = x.getOrDefault(key, "");
                      return null == val ? 0 : val.toString().length();
                    }).max().getAsInt() + "s";
              case "Integer":
                return "%6d";
              case "Double":
                return "%.4f";
              default:
                return "%s";
            }
          } finally {
            if (null != e)
              RefUtil.freeRef(e);
          }
        }).collect(Collectors.joining(" | "));
        printStream.println(schema.entrySet().stream().map(x -> {
          CharSequence temp_04_0002 = x.getKey();
          RefUtil.freeRef(x);
          return temp_04_0002;
        }).collect(Collectors.joining(" | ")).trim());
        printStream.println(schema.entrySet().stream().map(x -> {
          CharSequence temp_04_0003 = x.getKey();
          RefUtil.freeRef(x);
          return temp_04_0003;
        }).map(x -> {
          @Nonnull final char[] t = new char[x.length()];
          Arrays.fill(t, '-');
          return new String(t);
        }).collect(Collectors.joining(" | ")).trim());
        for (@Nonnull final Map<CharSequence, Object> row : rows) {
          printStream.println(RefString.format(formatString, schema.entrySet().stream().map(e -> {
            Object temp_04_0004 = row.get(e.getKey());
            RefUtil.freeRef(e);
            return temp_04_0004;
          }).toArray()));
        }
      }
      return buffer.toString();
    } catch (@Nonnull final IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void writeProjectorData(@Nonnull final File path, final URL baseUrl) throws IOException {
    path.mkdirs();
    try (@Nonnull
         FileOutputStream file = new FileOutputStream(new File(path, "data.tsv"))) {
      try (@Nonnull
           PrintStream printStream = new PrintStream(file)) {
        printStream.println(toMarkdownTable());
      }
    }
    final List<Entry<CharSequence, Class<?>>> scalarCols = schema.entrySet().stream().filter(e -> {
      boolean temp_04_0005 = Number.class.isAssignableFrom(e.getValue());
      RefUtil.freeRef(e);
      return temp_04_0005;
    }).collect(Collectors.toList());
    try (@Nonnull
         FileOutputStream file = new FileOutputStream(new File(path, "tensors.tsv"))) {
      try (@Nonnull
           PrintStream printStream = new PrintStream(file)) {
        for (@Nonnull final Map<CharSequence, Object> row : rows) {
          printStream.println(scalarCols.stream().map(e -> {
            double temp_04_0006 = ((Number) row.getOrDefault(e.getKey(), 0)).doubleValue();
            RefUtil.freeRef(e);
            return temp_04_0006;
          }).map(x -> x.toString()).collect(Collectors.joining("\t")));
        }
      }
    }
    final List<Entry<CharSequence, Class<?>>> metadataCols = schema.entrySet().stream().filter(e -> {
      boolean temp_04_0007 = String.class.isAssignableFrom(e.getValue());
      RefUtil.freeRef(e);
      return temp_04_0007;
    }).collect(Collectors.toList());
    try (@Nonnull
         FileOutputStream file = new FileOutputStream(new File(path, "metadata.tsv"))) {
      try (@Nonnull
           PrintStream printStream = new PrintStream(file)) {
        if (1 < metadataCols.size()) {
          printStream.println(metadataCols.stream().map(e -> {
            CharSequence temp_04_0008 = e.getKey();
            RefUtil.freeRef(e);
            return temp_04_0008;
          }).collect(Collectors.joining("\t")));
        }
        for (@Nonnull final Map<CharSequence, Object> row : rows) {
          printStream.println(metadataCols.stream().map(e -> {
            CharSequence temp_04_0009 = (CharSequence) row.getOrDefault(e.getKey(), "");
            RefUtil.freeRef(e);
            return temp_04_0009;
          }).collect(Collectors.joining("\t")));
        }
      }
    }
    final List<Entry<CharSequence, Class<?>>> urlCols = schema.entrySet().stream().filter(e -> {
      boolean temp_04_0010 = URL.class.isAssignableFrom(e.getValue());
      RefUtil.freeRef(e);
      return temp_04_0010;
    }).collect(Collectors.toList());
    try (@Nonnull
         FileOutputStream file = new FileOutputStream(new File(path, "bookmarks.txt"))) {
      try (@Nonnull
           PrintStream printStream = new PrintStream(file)) {
        for (@Nonnull final Map<CharSequence, Object> row : rows) {
          printStream.println(urlCols.stream().map(e -> {
            String temp_04_0011 = row.get(e.getKey()).toString();
            RefUtil.freeRef(e);
            return temp_04_0011;
          }).collect(Collectors.joining("\t")));
        }
      }
    }
    try (@Nonnull
         FileOutputStream file = new FileOutputStream(new File(path, "config.json"))) {
      try (@Nonnull
           PrintStream printStream = new PrintStream(file)) {
        printStream.println("{\n" + "  \"embeddings\": [\n" + "    {\n" + "      \"tensorName\": \"" + path.getName()
            + "\",\n" + "      \"tensorShape\": [\n" + "        " + rows.size() + ",\n" + "        " + scalarCols.size()
            + "\n" + "      ],\n" + "      \"tensorPath\": \"" + new URL(baseUrl, "tensors.tsv")
            + (0 == metadataCols.size() ? "" : "\",\n      \"metadataPath\": \"" + new URL(baseUrl, "metadata.tsv"))
            + "\"\n" + "    }\n" + "  ]\n" + "}");
      }
    }
    if (0 < urlCols.size()) {
      try (@Nonnull
           FileOutputStream file = new FileOutputStream(new File(path, "config_withLinks.json"))) {
        try (@Nonnull
             PrintStream printStream = new PrintStream(file)) {
          printStream.println("{\n" + "  \"embeddings\": [\n" + "    {\n" + "      \"tensorName\": \"" + path.getName()
              + "\",\n" + "      \"tensorShape\": [\n" + "        " + rows.size() + ",\n" + "        "
              + scalarCols.size() + "\n" + "      ],\n" + "      \"tensorPath\": \"" + new URL(baseUrl, "tensors.tsv")
              + (0 == metadataCols.size() ? "" : "\",\n      \"metadataPath\": \"" + new URL(baseUrl, "metadata.tsv"))
              + "\",\n      \"bookmarksPath\": \"" + new URL(baseUrl, "bookmarks.txt") + "\"\n" + "    }\n" + "  ]\n"
              + "}");
        }
      }
    }
  }

}
