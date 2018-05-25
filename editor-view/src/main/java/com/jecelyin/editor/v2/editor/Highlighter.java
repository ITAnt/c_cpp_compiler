package com.jecelyin.editor.v2.editor;

import android.core.text.SpannableStringBuilder;
import android.graphics.Color;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import com.duy.ide.editor.theme.model.EditorTheme;
import com.duy.ide.editor.theme.model.SyntaxStyle;
import com.jecelyin.common.utils.DLog;
import com.jecelyin.editor.v2.highlight.Buffer;
import com.jecelyin.editor.v2.highlight.HighlightInfo;

import org.gjt.sp.jedit.awt.Font;
import org.gjt.sp.jedit.syntax.DefaultTokenHandler;
import org.gjt.sp.jedit.syntax.Token;

import java.util.ArrayList;
import java.util.HashMap;

public class Highlighter {
    public Highlighter() {

    }

    public void highlight(Buffer buffer, EditorTheme editorTheme,
                          HashMap<Integer, ArrayList<? extends CharacterStyle>> colorsMap,
                          Spannable spannable,
                          int startLine, int endLine) {
        if (!buffer.isCanHighlight())
            return;
        SyntaxStyle[] syntaxStyles = editorTheme.getSyntaxStyles();

        DefaultTokenHandler tokenHandler;
        ArrayList<HighlightInfo> mergerArray;

        for (int i = startLine; i <= endLine; i++) {
            tokenHandler = new DefaultTokenHandler();
            buffer.markTokens(i, tokenHandler);
            Token token = tokenHandler.getTokens();

            mergerArray = new ArrayList<>();
            collectToken(syntaxStyles, buffer, i, token, mergerArray);
            addTokenSpans(spannable, i, mergerArray, colorsMap);
        }
    }

    private void addTokenSpans(Spannable spannable, int line, ArrayList<HighlightInfo> mergerArray,
                               HashMap<Integer, ArrayList<? extends CharacterStyle>> colorsMap) {
        CharacterStyle fcs;

        ArrayList<? extends CharacterStyle> oldSpans = colorsMap.remove(line);
        if (oldSpans != null) {
            for (CharacterStyle span : oldSpans) {
                spannable.removeSpan(span);
            }
        }

        int length = spannable.length();

        ArrayList<CharacterStyle> spans = new ArrayList<>(mergerArray.size());
        for (HighlightInfo info : mergerArray) {
            if (info.endOffset > length) {
                DLog.e("assert hi.endOffset %d > maxLength %d", info.endOffset, length);
                info.endOffset = length;
            }
            if (info.startOffset >= info.endOffset) {
                DLog.e("hi.startOffset %d >= hi.endOffset %d", info.startOffset, info.endOffset);
                continue;
            }
            fcs = new ForegroundColorSpan(info.style.getForegroundColor());
            spannable.setSpan(fcs, info.startOffset, info.endOffset, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
            spans.add(fcs);

            if (info.style.getFont() != null) {
                if (info.style.getFont().getStyle() != Font.NORMAL) {
                    fcs = new StyleSpan(info.style.getFont().getStyle());
                    spannable.setSpan(fcs, info.startOffset, info.endOffset, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spans.add(fcs);
                }
            }
            if (info.style.getBackgroundColor() != Color.TRANSPARENT) {
                if (info.style.getFont().getStyle() != Font.NORMAL) {
                    fcs = new BackgroundColorSpan(info.style.getFont().getStyle());
                    spannable.setSpan(fcs, info.startOffset, info.endOffset, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spans.add(fcs);
                }
            }
        }
        colorsMap.put(line, spans);
    }

    private void collectToken(SyntaxStyle[] syntaxStyles, Buffer buffer, int lineNumber, Token token,
                              ArrayList<HighlightInfo> mergerArray) {

        int lineStartOffset = buffer.getLineManager().getLineStartOffset(lineNumber);

        HighlightInfo hi;
        while (token.id != Token.END) {
            int startIndex = lineStartOffset + token.offset;
            int endIndex = lineStartOffset + token.offset + token.length;
            SyntaxStyle style = syntaxStyles[token.id];
            token = token.next;

            if (style == null)
                continue;

            if (mergerArray.isEmpty()) {
                mergerArray.add(new HighlightInfo(startIndex, endIndex, style));
            } else {
                hi = mergerArray.get(mergerArray.size() - 1);
                if (hi.style.equals(style) && hi.endOffset == startIndex) {
                    hi.endOffset = endIndex;
                } else {
                    mergerArray.add(new HighlightInfo(startIndex, endIndex, style));
                }
            }
        }


    }

}
