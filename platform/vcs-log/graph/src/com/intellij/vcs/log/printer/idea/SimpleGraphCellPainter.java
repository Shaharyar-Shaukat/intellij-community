/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.vcs.log.printer.idea;

import com.intellij.openapi.util.Pair;
import com.intellij.ui.JBColor;
import com.intellij.vcs.log.graph.EdgePrintElement;
import com.intellij.vcs.log.graph.NodePrintElement;
import com.intellij.vcs.log.graph.PrintElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.Collection;

/**
 * @author erokhins
 */
public class SimpleGraphCellPainter implements GraphCellPainter {

  private static final Color MARK_COLOR = JBColor.BLACK;
  private static final int ROW_HEIGHT = 24;
  private static final double ARROW_ANGLE_COS2 = 0.7;
  private static final double ARROW_LENGTH = 0.3;
  private Graphics2D g2;
  @NotNull private final ColorGenerator myColorGenerator;

  public SimpleGraphCellPainter(@NotNull ColorGenerator colorGenerator) {
    myColorGenerator = colorGenerator;
  }

  protected int getRowHeight() {
    return ROW_HEIGHT;
  }

  private float[] getDashLength(int edgeLength) {
    int space = getRowHeight() / 4 - 1;
    int dash = getRowHeight() / 4 + 1;
    int count = edgeLength / (2 * (dash + space));
    assert count != 0;
    int dashApprox = (edgeLength / 2 - count * space) / count;

    return new float[]{2 * dashApprox, 2 * space};
  }

  @NotNull
  private BasicStroke getOrdinaryStroke() {
    return new BasicStroke(PrintParameters.getLineThickness(getRowHeight()), BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL);
  }

  @NotNull
  private BasicStroke getSelectedStroke() {
    return new BasicStroke(PrintParameters.getSelectedLineThickness(getRowHeight()), BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL);
  }

  @NotNull
  private Stroke getDashedStroke(float[] dash) {
    return new BasicStroke(PrintParameters.getLineThickness(getRowHeight()), BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, dash,
                           dash[0] / 2);
  }

  @NotNull
  private Stroke getSelectedDashedStroke(float[] dash) {
    return new BasicStroke(PrintParameters.getSelectedLineThickness(getRowHeight()), BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, dash,
                           dash[0] / 2);
  }

  private void paintUpLine(int from, int to, Color color, boolean hasArrow, boolean isUsual, boolean isSelected) {
    // paint vertical lines normal size
    // paint non-vertical lines twice the size to make them dock with each other well
    int nodeWidth = PrintParameters.getNodeWidth(getRowHeight());
    if (from == to) {
      int x = nodeWidth * from + nodeWidth / 2;
      int y1 = getRowHeight() / 2 - 1;
      int y2 = 0;
      paintLine(color, hasArrow, x, y1, x, y2, x, y2, isUsual, isSelected);
    }
    else {
      int x1 = nodeWidth * from + nodeWidth / 2;
      int y1 = getRowHeight() / 2;
      int x2 = nodeWidth * to + nodeWidth / 2;
      int y2 = -getRowHeight() / 2;
      paintLine(color, hasArrow, x1, y1, x2, y2, (x1 + x2) / 2, (y1 + y2) / 2, isUsual, isSelected);
    }
  }

  private void paintDownLine(int from, int to, Color color, boolean hasArrow, boolean isUsual, boolean isSelected) {
    int nodeWidth = PrintParameters.getNodeWidth(getRowHeight());
    if (from == to) {
      int y2 = getRowHeight() - 1;
      int y1 = getRowHeight() / 2;
      int x = nodeWidth * from + nodeWidth / 2;
      paintLine(color, hasArrow, x, y1, x, y2, x, y2, isUsual, isSelected);
    }
    else {
      int x1 = nodeWidth * from + nodeWidth / 2;
      int y1 = getRowHeight() / 2;
      int x2 = nodeWidth * to + nodeWidth / 2;
      int y2 = getRowHeight() + getRowHeight() / 2;
      paintLine(color, hasArrow, x1, y1, x2, y2, (x1 + x2) / 2, (y1 + y2) / 2, isUsual, isSelected);
    }
  }

  private void paintLine(Color color,
                         boolean hasArrow,
                         int x1,
                         int y1,
                         int x2,
                         int y2,
                         int startArrowX,
                         int startArrowY,
                         boolean isUsual,
                         boolean isSelected) {
    g2.setColor(color);

    int length = (x1 == x2) ? getRowHeight() : (int)Math.ceil(Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)));
    setStroke(isUsual || hasArrow, isSelected, length);

    g2.drawLine(x1, y1, x2, y2);
    if (hasArrow) {
      Pair<Integer, Integer> rotate1 =
        rotate(x1, y1, startArrowX, startArrowY, Math.sqrt(ARROW_ANGLE_COS2), Math.sqrt(1 - ARROW_ANGLE_COS2), ARROW_LENGTH * getRowHeight());
      Pair<Integer, Integer> rotate2 =
        rotate(x1, y1, startArrowX, startArrowY, Math.sqrt(ARROW_ANGLE_COS2), -Math.sqrt(1 - ARROW_ANGLE_COS2), ARROW_LENGTH * getRowHeight());
      g2.drawLine(startArrowX, startArrowY, rotate1.first, rotate1.second);
      g2.drawLine(startArrowX, startArrowY, rotate2.first, rotate2.second);
    }
  }

  @NotNull
  private static Pair<Integer, Integer> rotate(double x, double y, double centerX, double centerY, double cos, double sin, double arrowLength) {
    double translateX = (x - centerX);
    double translateY = (y - centerY);

    double d = Math.sqrt(translateX * translateX + translateY * translateY);
    double scaleX = arrowLength * translateX / d;
    double scaleY = arrowLength * translateY / d;

    double rotateX = scaleX * cos - scaleY * sin;
    double rotateY = scaleX * sin + scaleY * cos;

    return Pair.create((int)Math.round(rotateX + centerX), (int)Math.round(rotateY + centerY));
  }

  private void paintCircle(int position, Color color, boolean select) {
    int nodeWidth = PrintParameters.getNodeWidth(getRowHeight());
    int circleRadius = PrintParameters.getCircleRadius(getRowHeight());
    int selectedCircleRadius = PrintParameters.getSelectedCircleRadius(getRowHeight());

    int x0 = nodeWidth * position + nodeWidth / 2;
    int y0 = getRowHeight() / 2;
    int r = circleRadius;
    if (select) {
      r = selectedCircleRadius;
    }
    Ellipse2D.Double circle = new Ellipse2D.Double(x0 - r + 0.5, y0 - r + 0.5, 2 * r, 2 * r);
    g2.setColor(color);
    g2.fill(circle);
  }

  private void setStroke(boolean usual, boolean select, int edgeLength) {
    if (usual) {
      if (select) {
        g2.setStroke(getSelectedStroke());
      }
      else {
        g2.setStroke(getOrdinaryStroke());
      }
    }
    else {
      if (select) {
        g2.setStroke(getSelectedDashedStroke(getDashLength(edgeLength)));
      }
      else {
        g2.setStroke(getDashedStroke(getDashLength(edgeLength)));
      }
    }
  }

  @NotNull
  private Color getColor(@NotNull PrintElement printElement) {
    return myColorGenerator.getColor(printElement.getColorId());
  }

  private static boolean isUsual(PrintElement printElement) {
    if (!(printElement instanceof EdgePrintElement)) return true;
    EdgePrintElement.LineStyle lineStyle = ((EdgePrintElement)printElement).getLineStyle();
    return lineStyle == EdgePrintElement.LineStyle.SOLID;
  }

  @Override
  public void draw(@NotNull Graphics2D g2, @NotNull Collection<? extends PrintElement> printElements) {
    this.g2 = g2;
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    for (final PrintElement printElement : printElements) {
      if (printElement instanceof EdgePrintElement) {

        EdgePrintElement edgePrintElement = (EdgePrintElement)printElement;
        Color usualColor = getColor(edgePrintElement);

        if (printElement.isSelected()) {
          printEdge(MARK_COLOR, true, edgePrintElement);
          printEdge(usualColor, false, edgePrintElement);
        }
        else {
          printEdge(usualColor, false, edgePrintElement);
        }

      }

      if (printElement instanceof NodePrintElement) {
        int position = printElement.getPositionInCurrentRow();
        if (printElement.isSelected()) {
          paintCircle(position, MARK_COLOR, true);
          paintCircle(position, getColor(printElement), false);
        }
        else {
          paintCircle(position, getColor(printElement), false);
        }
      }
    }
  }

  private void printEdge(Color color, boolean isSelected, EdgePrintElement edgePrintElement) {
    int from = edgePrintElement.getPositionInCurrentRow();
    int to = edgePrintElement.getPositionInOtherRow();
    boolean isUsual = isUsual(edgePrintElement);

    if (edgePrintElement.getType() == EdgePrintElement.Type.DOWN) {
      paintDownLine(from, to, color, edgePrintElement.hasArrow(), isUsual, isSelected);
    }
    else {
      paintUpLine(from, to, color, edgePrintElement.hasArrow(), isUsual, isSelected);
    }
  }

  @Nullable
  @Override
  public PrintElement mouseOver(@NotNull Collection<? extends PrintElement> printElements, int x, int y) {
    int nodeWidth = PrintParameters.getNodeWidth(getRowHeight());
    for (PrintElement printElement : printElements) {
      if (printElement instanceof NodePrintElement) {
        int circleRadius = PrintParameters.getCircleRadius(getRowHeight());
        if (PositionUtil.overNode(printElement.getPositionInCurrentRow(), x, y, getRowHeight(), nodeWidth, circleRadius)) {
          return printElement;
        }
      }
    }

    for (PrintElement printElement : printElements) {
      if (printElement instanceof EdgePrintElement) {
        EdgePrintElement edgePrintElement = (EdgePrintElement)printElement;
        float lineThickness = PrintParameters.getLineThickness(getRowHeight());
        if (edgePrintElement.getType() == EdgePrintElement.Type.DOWN) {
          if (PositionUtil
            .overDownEdge(edgePrintElement.getPositionInCurrentRow(), edgePrintElement.getPositionInOtherRow(), x, y, getRowHeight(),
                          nodeWidth, lineThickness)) {
            return printElement;
          }
        }
        else {
          if (PositionUtil
            .overUpEdge(edgePrintElement.getPositionInOtherRow(), edgePrintElement.getPositionInCurrentRow(), x, y, getRowHeight(),
                        nodeWidth, lineThickness)) {
            return printElement;
          }
        }
      }
    }
    return null;
  }
}
