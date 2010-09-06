package appmonk.widget;

/*
 * Copyright (C) 2009, 2010 Sebastian Delmont <sd@notso.net>
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
 * See the License for the specific language governing permissions andlimitations under the License.
 * 
 */

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class MultiLineLayout extends ViewGroup {
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet s) {
            super(c, s);
        }

        public LayoutParams(int w, int h) {
            super(w, h);
        }
        
        public LayoutParams(ViewGroup.LayoutParams p) {
            super(p);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }
    }
    
    
    public MultiLineLayout(Context context) {
        super(context);
    }

    public MultiLineLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();
        final int paddingRight = getPaddingRight();
        final int paddingBottom = getPaddingBottom();
        
        int currentLineWidth = paddingLeft;
        int currentLineHeight = 0;
        int totalWidth = paddingLeft;
        int totalHeightMinusCurrentLine = paddingTop;
        
        final int count = getChildCount();
        for (int i = 0; i < count; ++i) {
            final View child = getChildAt(i);

            if (child == null) {
                continue;
            }

            if (child.getVisibility() == GONE) {
                continue;
            }

            final MultiLineLayout.LayoutParams lp = (MultiLineLayout.LayoutParams) child.getLayoutParams();

            if (lp.width == 0 || lp.width == LayoutParams.FILL_PARENT)
                lp.width = LayoutParams.WRAP_CONTENT;
            
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);

            int childrenWidth = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
            int childrenHeight = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;

            if (currentLineWidth + childrenWidth + paddingRight > widthSize) {
                totalWidth = Math.max(totalWidth, currentLineWidth + paddingRight);
                totalHeightMinusCurrentLine += currentLineHeight;
                currentLineHeight = childrenHeight;
                currentLineWidth = childrenWidth + paddingLeft;
            }
            else {
                currentLineHeight = Math.max(currentLineHeight, childrenHeight);
                currentLineWidth += childrenWidth;
            }
        }

        totalWidth = Math.max(totalWidth, currentLineWidth + paddingRight);
        
        setMeasuredDimension(totalWidth, totalHeightMinusCurrentLine + currentLineHeight + paddingBottom);
    }
    
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int widthSize = getMeasuredWidth();
        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();
        final int paddingRight = getPaddingRight();
        final int paddingBottom = getPaddingBottom();

        int currentLineLeft = paddingLeft;
        int currentLineHeight = 0;
        int currentLineTop = paddingTop;
        
        final int count = getChildCount();
        for (int i = 0; i < count; ++i) {
            final View child = getChildAt(i);

            if (child == null) {
                continue;
            }

            if (child.getVisibility() == GONE) {
                continue;
            }
            
            final MultiLineLayout.LayoutParams lp = (MultiLineLayout.LayoutParams) child.getLayoutParams();

            int childrenWidth = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
            int childrenHeight = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;

            if (currentLineLeft + childrenWidth + paddingRight > widthSize) {
                currentLineTop += currentLineHeight;
                currentLineHeight = childrenHeight;
                currentLineLeft = paddingLeft + childrenWidth;
            }
            else {
                currentLineHeight = Math.max(currentLineHeight, childrenHeight);
                currentLineLeft += childrenWidth;
            }

            int childLeft = currentLineLeft - childrenWidth + lp.leftMargin;
            int childTop = currentLineTop + lp.topMargin;
            int childRight = childLeft + childrenWidth - lp.rightMargin;
            int childBottom = childTop + childrenHeight - lp.bottomMargin;
            
            child.layout(childLeft, childTop, childRight, childBottom);
        }

    }
    
//    @Override
//    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
//        
//    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MultiLineLayout.LayoutParams(getContext(), attrs);
    }

    /**
     * Returns a set of layout parameters with a width of
     * {@link android.view.ViewGroup.LayoutParams#FILL_PARENT}
     * and a height of {@link android.view.ViewGroup.LayoutParams#WRAP_CONTENT}
     * when the layout's orientation is {@link #VERTICAL}. When the orientation is
     * {@link #HORIZONTAL}, the width is set to {@link LayoutParams#WRAP_CONTENT}
     * and the height to {@link LayoutParams#WRAP_CONTENT}.
     */
    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MultiLineLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new MultiLineLayout.LayoutParams(p);
    }


    // Override to allow type-checking of LayoutParams.
    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof MultiLineLayout.LayoutParams;
    }
}
