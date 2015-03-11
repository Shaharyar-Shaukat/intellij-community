/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.dvcs.ui;

import com.intellij.dvcs.repo.Repository;
import com.intellij.dvcs.repo.VcsRepositoryManager;
import com.intellij.openapi.util.Condition;
import com.intellij.ui.JBColor;
import com.intellij.vcs.log.Hash;
import com.intellij.vcs.log.VcsCommitStyleFactory;
import com.intellij.vcs.log.VcsLogHighlighter;
import com.intellij.vcs.log.VcsShortCommitDetails;
import com.intellij.vcs.log.data.LoadingDetails;
import com.intellij.vcs.log.data.VcsLogDataHolder;
import com.intellij.vcs.log.data.VcsLogUiProperties;
import com.intellij.vcs.log.ui.VcsLogHighlighterFactory;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class CurrentBranchHighlighter implements VcsLogHighlighter {
  private static final JBColor CURRENT_BRANCH_BG = new JBColor(new Color(228, 250, 255), new Color(63, 71, 73));
  @NotNull private final VcsLogUiProperties myUiProperties;
  @NotNull private final VcsLogDataHolder myDataHolder;
  @NotNull private final VcsRepositoryManager myRepositoryManager;

  public CurrentBranchHighlighter(@NotNull VcsLogDataHolder logDataHolder, @NotNull VcsLogUiProperties uiProperties) {
    myDataHolder = logDataHolder;
    myUiProperties = uiProperties;
    myRepositoryManager = myDataHolder.getProject().getComponent(VcsRepositoryManager.class);
  }

  @NotNull
  @Override
  public VcsCommitStyle getStyle(int commitIndex, boolean isSelected) {
    if (isSelected || !myUiProperties.isHighlightCurrentBranch()) return VcsCommitStyle.DEFAULT;
    VcsShortCommitDetails details = myDataHolder.getMiniDetailsGetter().getCommitDataIfAvailable(commitIndex);
    if (details != null && !(details instanceof LoadingDetails)) {
      Repository repo = myRepositoryManager.getRepositoryForRoot(details.getRoot());
      if (repo != null) {
        String currentBranch = repo.getCurrentBranchName();
        if (currentBranch == null) {
          if (repo.getCurrentRevision() != null) currentBranch = "HEAD"; // does this work for hg?
        }
        if (currentBranch != null) {
          Condition<Hash> condition = myDataHolder.getContainingBranchesGetter().getContainedInBranchCondition(currentBranch, details.getRoot());
          if (condition.value(details.getId())) {
            return VcsCommitStyleFactory.background(CURRENT_BRANCH_BG);
          }
        }
      }
    }
    return VcsCommitStyle.DEFAULT;
  }

  public static class Factory implements VcsLogHighlighterFactory {
    @NotNull
    @Override
    public VcsLogHighlighter createHighlighter(@NotNull VcsLogDataHolder logDataHolder, @NotNull VcsLogUiProperties uiProperties) {
      return new CurrentBranchHighlighter(logDataHolder, uiProperties);
    }
  }
}
