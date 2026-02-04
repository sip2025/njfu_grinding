package com.examapp.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;
import android.widget.HorizontalScrollView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.examapp.R;
import com.examapp.model.ExamHistoryEntry;
import java.util.ArrayList;
import java.util.List;

public class ScoreChartView extends View {

    private final List<Integer> scores = new ArrayList<>();
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path linePath = new Path();
    private final Path fillPath = new Path();
    private float density;
    private static final float POINT_SPACING_DP = 50f;
    private static final float LEFT_PADDING_DP = 40f;
    private static final float RIGHT_PADDING_DP = 20f;
    private static final float TOP_PADDING_DP = 20f;
    private static final float BOTTOM_PADDING_DP = 30f;

    public ScoreChartView(Context context) {
        super(context);
        init(context);
    }

    public ScoreChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ScoreChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ViewParent parent = getParent();
        if (parent instanceof View) {
            ((View) parent).getViewTreeObserver().addOnScrollChangedListener(scrollChangedListener);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        ViewParent parent = getParent();
        if (parent instanceof View) {
            ((View) parent).getViewTreeObserver().removeOnScrollChangedListener(scrollChangedListener);
        }
        super.onDetachedFromWindow();
    }

    private final android.view.ViewTreeObserver.OnScrollChangedListener scrollChangedListener = () -> invalidate();

    private void init(Context context) {
        density = context.getResources().getDisplayMetrics().density;

        linePaint.setColor(ContextCompat.getColor(context, R.color.primary));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2.5f * density);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint.setStyle(Paint.Style.FILL);

        pointPaint.setColor(ContextCompat.getColor(context, R.color.accent));
        pointPaint.setStyle(Paint.Style.FILL);

        gridPaint.setColor(ContextCompat.getColor(context, R.color.light_divider));
        gridPaint.setStrokeWidth(1f * density);

        textPaint.setColor(ContextCompat.getColor(context, R.color.gray));
        textPaint.setTextSize(11f * density);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        
        // Background paint for Y-axis area (to cover chart content when scrolling)
        axisBgPaint.setStyle(Paint.Style.FILL);
        
        // Border paint for Y-axis separator line
        axisBorderPaint.setColor(ContextCompat.getColor(context, R.color.divider));
        axisBorderPaint.setStrokeWidth(1f * density);
        axisBorderPaint.setStyle(Paint.Style.STROKE);
    }
    
    private int getParentScrollX() {
        ViewParent parent = getParent();
        if (parent instanceof HorizontalScrollView) {
            return ((HorizontalScrollView) parent).getScrollX();
        }
        return 0;
    }

    public void setEntries(List<ExamHistoryEntry> entries) {
        scores.clear();
        if (entries != null && !entries.isEmpty()) {
            // Data is already sorted from oldest to newest in HistoryActivity
            for (int i = 0; i < entries.size(); i++) {
                scores.add(entries.get(i).getScore());
            }
        }
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int minWidth = 0;
        if (scores.size() > 1) {
            float contentWidth = (scores.size() - 1) * POINT_SPACING_DP * density;
            float firstPointOffset = 20f * density;
            // Add padding and offset
            minWidth = (int) (contentWidth + firstPointOffset + (LEFT_PADDING_DP + RIGHT_PADDING_DP) * density);
        } else {
            minWidth = MeasureSpec.getSize(widthMeasureSpec);
        }

        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int finalWidth = Math.max(minWidth, parentWidth);
        setMeasuredDimension(finalWidth, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        linePath.reset();
        fillPath.reset();

        int width = getWidth();
        int height = getHeight();

        // Get scroll offset from parent HorizontalScrollView
        int scrollX = getParentScrollX();

        float leftPadding = LEFT_PADDING_DP * density;
        float rightPadding = RIGHT_PADDING_DP * density;
        float top = TOP_PADDING_DP * density;
        float bottom = height - BOTTOM_PADDING_DP * density;
        float chartHeight = bottom - top;

        if (scores.isEmpty()) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(getResources().getString(R.string.history_chart_empty), width / 2f, height / 2f, textPaint);
            return;
        }

        int minScore = 100;
        int maxScore = 0;
        for (int score : scores) {
            if (score < minScore) minScore = score;
            if (score > maxScore) maxScore = score;
        }

        int rangePadding = 5;
        float yMin = Math.max(0, minScore - rangePadding);
        float yMax = Math.min(100, maxScore + rangePadding);

        // Ensure minimum range of 20
        if (yMax - yMin < 20) {
            yMin = Math.max(0, yMax - 20);
            if (yMax - yMin < 20) {
                yMax = Math.min(100, yMin + 20);
            }
        }

        float yRange = yMax - yMin;

        float contentWidth = Math.max(0, (scores.size() - 1) * POINT_SPACING_DP * density);
        float firstPointOffset = 20f * density;
        float drawEnd = Math.max(width, contentWidth + leftPadding + firstPointOffset + rightPadding);

        int gridLines = 4;

        // Step 1: Draw grid lines across the whole chart
        for (int i = 0; i <= gridLines; i++) {
            float val = yMin + (yRange * i / gridLines);
            float y = bottom - ((val - yMin) / yRange) * chartHeight;
            canvas.drawLine(leftPadding, y, drawEnd, y, gridPaint);
        }

        // Step 2: Build and draw the chart content (fill, line, points)
        List<Float[]> points = new ArrayList<>();
        // Add extra padding for the first point so it's not covered by the axis
        for (int i = 0; i < scores.size(); i++) {
            float val = scores.get(i);
            float x = leftPadding + firstPointOffset + i * POINT_SPACING_DP * density;
            float y = bottom - ((val - yMin) / yRange) * chartHeight;
            points.add(new Float[]{x, y});
        }

        if (!points.isEmpty()) {
            Float[] first = points.get(0);
            linePath.moveTo(first[0], first[1]);

            fillPath.moveTo(first[0], bottom);
            fillPath.lineTo(first[0], first[1]);

            for (int i = 0; i < points.size() - 1; i++) {
                Float[] p1 = points.get(i);
                Float[] p2 = points.get(i + 1);

                // Cubic Bezier for smooth curves
                float midX = (p1[0] + p2[0]) / 2;
                linePath.cubicTo(midX, p1[1], midX, p2[1], p2[0], p2[1]);
                fillPath.cubicTo(midX, p1[1], midX, p2[1], p2[0], p2[1]);
            }

            Float[] last = points.get(points.size() - 1);
            fillPath.lineTo(last[0], bottom);
            fillPath.close();

            // Gradient fill
            if (fillPaint.getShader() == null) {
                int colorPrimary = ContextCompat.getColor(getContext(), R.color.primary);
                int startColor = (colorPrimary & 0x00FFFFFF) | 0x40000000; // 25% alpha
                int endColor = (colorPrimary & 0x00FFFFFF) | 0x05000000;   // ~3% alpha
                LinearGradient gradient = new LinearGradient(
                    0, top, 0, bottom,
                    startColor, endColor,
                    Shader.TileMode.CLAMP
                );
                fillPaint.setShader(gradient);
            }

            canvas.drawPath(fillPath, fillPaint);
            canvas.drawPath(linePath, linePaint);

            // Draw points and values
            textPaint.setTextAlign(Paint.Align.CENTER);
            for (int i = 0; i < points.size(); i++) {
                Float[] p = points.get(i);
                canvas.drawCircle(p[0], p[1], 4f * density, pointPaint);
                canvas.drawText(String.valueOf(scores.get(i)), p[0], p[1] - 8f * density, textPaint);
            }
        }

        // Step 3: Draw Y-axis background to cover chart content when scrolling
        // The axis stays fixed at the left edge of the visible viewport
        float axisAreaWidth = leftPadding;
        axisBgPaint.setColor(ContextCompat.getColor(getContext(), R.color.surface));
        canvas.drawRect(scrollX, 0, scrollX + axisAreaWidth, height, axisBgPaint);
        
        // Draw separator line between Y-axis and chart area
        canvas.drawLine(scrollX + axisAreaWidth, 0, scrollX + axisAreaWidth, height, axisBorderPaint);

        // Step 4: Draw Y-axis labels on top of the background (sticky position)
        textPaint.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= gridLines; i++) {
            float val = yMin + (yRange * i / gridLines);
            float y = bottom - ((val - yMin) / yRange) * chartHeight;
            // Draw label at fixed position relative to scroll
            canvas.drawText(String.valueOf((int) val), scrollX + axisAreaWidth - 6f * density, y + 4f * density, textPaint);
        }
    }
}
