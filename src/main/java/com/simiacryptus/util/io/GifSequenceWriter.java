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

package com.simiacryptus.util.io;

//  Originally Created by Elliot Kroo on 2009-04-25.
//
// This work is licensed under the Creative Commons Attribution 3.0 Unported
// License. To view a copy of this license, visit
// http://creativecommons.org/licenses/by/3.0/ or send a letter to Creative
// Commons, 171 Second Street, Suite 300, San Francisco, California, 94105, USA.

import javax.annotation.Nonnull;
import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class GifSequenceWriter {

  protected ImageWriter gifWriter;
  protected ImageWriteParam imageWriteParam;
  protected IIOMetadata imageMetaData;

  public GifSequenceWriter(ImageOutputStream outputStream, int imageType, int timeBetweenFramesMS,
                           boolean loopContinuously) throws IOException {

    gifWriter = getWriter("gif");
    imageWriteParam = gifWriter.getDefaultWriteParam();
    ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(imageType);
    imageMetaData = gifWriter.getDefaultImageMetadata(imageTypeSpecifier, imageWriteParam);
    String metaFormatName = imageMetaData.getNativeMetadataFormatName();
    @Nonnull
    IIOMetadataNode root = (IIOMetadataNode) imageMetaData.getAsTree(metaFormatName);
    @Nonnull
    IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");
    graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
    graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
    graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE");
    graphicsControlExtensionNode.setAttribute("delayTime", Integer.toString(timeBetweenFramesMS / 10));
    graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");
    @Nonnull
    IIOMetadataNode commentsNode = getNode(root, "CommentExtensions");
    commentsNode.setAttribute("CommentExtension", "Created by MindsEye");
    @Nonnull
    IIOMetadataNode appEntensionsNode = getNode(root, "ApplicationExtensions");
    @Nonnull
    IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");
    child.setAttribute("applicationID", "NETSCAPE");
    child.setAttribute("authenticationCode", "2.0");

    int loop = loopContinuously ? 0 : 1;
    child.setUserObject(new byte[]{0x1, (byte) (loop & 0xFF), (byte) (loop >> 8 & 0xFF)});
    appEntensionsNode.appendChild(child);
    imageMetaData.setFromTree(metaFormatName, root);
    gifWriter.setOutput(outputStream);
    gifWriter.prepareWriteSequence(null);
  }

  public static void write(File gif, int timeBetweenFramesMS, boolean loopContinuously,
                           @Nonnull BufferedImage... images) throws IOException {
    @Nonnull
    ImageOutputStream output = new FileImageOutputStream(gif);
    write(output, timeBetweenFramesMS, loopContinuously, images);
  }

  public static void write(@Nonnull ImageOutputStream output, int timeBetweenFramesMS, boolean loopContinuously,
                           @Nonnull BufferedImage... images) throws IOException {
    try {
      @Nonnull
      GifSequenceWriter writer = new GifSequenceWriter(output, images[0].getType(), timeBetweenFramesMS,
          loopContinuously);
      for (@Nonnull
          BufferedImage image : images) {
        writer.writeToSequence(image);
      }
      writer.close();
    } finally {
      output.close();
    }
  }

  private static ImageWriter getWriter(@Nonnull String format) throws IIOException {
    Iterator<ImageWriter> iter = ImageIO.getImageWritersBySuffix(format);
    if (!iter.hasNext()) {
      throw new IIOException("No GIF Image Writers Exist");
    } else {
      return iter.next();
    }
  }

  @Nonnull
  private static IIOMetadataNode getNode(@Nonnull IIOMetadataNode rootNode, @Nonnull String nodeName) {
    int nNodes = rootNode.getLength();
    for (int i = 0; i < nNodes; i++) {
      if (rootNode.item(i).getNodeName().compareToIgnoreCase(nodeName) == 0) {
        @Nonnull
        IIOMetadataNode item = (IIOMetadataNode) rootNode.item(i);
        if (null == item)
          throw new IllegalStateException();
        return item;
      }
    }
    @Nonnull
    IIOMetadataNode node = new IIOMetadataNode(nodeName);
    rootNode.appendChild(node);
    return node;
  }

  public void writeToSequence(@Nonnull RenderedImage img) throws IOException {
    gifWriter.writeToSequence(new IIOImage(img, null, imageMetaData), imageWriteParam);
  }

  public void close() throws IOException {
    gifWriter.endWriteSequence();
  }
}