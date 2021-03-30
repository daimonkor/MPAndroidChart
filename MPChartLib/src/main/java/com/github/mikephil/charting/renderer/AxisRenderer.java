
package com.github.mikephil.charting.renderer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.Log;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.utils.MPPointD;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;

/**
 * Baseclass of all axis renderers.
 *
 * @author Philipp Jahoda
 */

interface ICheckContainsGridValue {
    boolean checkValue(float value);
}

public abstract class AxisRenderer extends Renderer {

    /** base axis this axis renderer works with */
    protected AxisBase mAxis;

    /** transformer to transform values to screen pixels and return */
    protected Transformer mTrans;

    /**
     * paint object for the grid lines
     */
    protected Paint mGridPaint;

    protected Paint mMajorGridPaint;

    /**
     * paint for the x-label values
     */
    protected Paint mAxisLabelPaint;

    /**
     * paint for the line surrounding the chart
     */
    protected Paint mAxisLinePaint;

    /**
     * paint used for the limit lines
     */
    protected Paint mLimitLinePaint;

    public AxisRenderer(ViewPortHandler viewPortHandler, Transformer trans, AxisBase axis) {
        super(viewPortHandler);

        this.mTrans = trans;
        this.mAxis = axis;

        if(mViewPortHandler != null) {

            mAxisLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            mGridPaint = new Paint();
            mGridPaint.setColor(Color.GRAY);
            mGridPaint.setStrokeWidth(1f);
            mGridPaint.setStyle(Style.STROKE);
            mGridPaint.setAlpha(90);

            mMajorGridPaint = new Paint();
            mMajorGridPaint.setColor(Color.GRAY);
            mMajorGridPaint.setStrokeWidth(1f);
            mMajorGridPaint.setStyle(Style.STROKE);
            mMajorGridPaint.setAlpha(90);

            mAxisLinePaint = new Paint();
            mAxisLinePaint.setColor(Color.BLACK);
            mAxisLinePaint.setStrokeWidth(1f);
            mAxisLinePaint.setStyle(Style.STROKE);

            mLimitLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mLimitLinePaint.setStyle(Paint.Style.STROKE);
        }
    }

    /**
     * Returns the Paint object used for drawing the axis (labels).
     *
     * @return
     */
    public Paint getPaintAxisLabels() {
        return mAxisLabelPaint;
    }

    /**
     * Returns the Paint object that is used for drawing the grid-lines of the
     * axis.
     *
     * @return
     */
    public Paint getPaintGrid() {
        return mGridPaint;
    }

    /**
     * Returns the Paint object that is used for drawing the axis-line that goes
     * alongside the axis.
     *
     * @return
     */
    public Paint getPaintAxisLine() {
        return mAxisLinePaint;
    }

    /**
     * Returns the Transformer object used for transforming the axis values.
     *
     * @return
     */
    public Transformer getTransformer() {
        return mTrans;
    }

    /**
     * Computes the axis values.
     *
     * @param min - the minimum value in the data object for this axis
     * @param max - the maximum value in the data object for this axis
     */
    public void computeAxis(float min, float max, boolean inverted) {

        // calculate the starting and entry point of the y-labels (depending on
        // zoom / contentrect bounds)
        if (mViewPortHandler != null && mViewPortHandler.contentWidth() > 10 && !mViewPortHandler.isFullyZoomedOutY()) {

            MPPointD p1 = mTrans.getValuesByTouchPoint(mViewPortHandler.contentLeft(), mViewPortHandler.contentTop());
            MPPointD p2 = mTrans.getValuesByTouchPoint(mViewPortHandler.contentLeft(), mViewPortHandler.contentBottom());

            if (!inverted) {

                min = (float) p2.y;
                max = (float) p1.y;
            } else {

                min = (float) p1.y;
                max = (float) p2.y;
            }

            MPPointD.recycleInstance(p1);
            MPPointD.recycleInstance(p2);
        }

        computeAxisValues(min, max);
    }

    /**
     * Sets up the axis values. Computes the desired number of labels between the two given extremes.
     *
     * @return
     */
    protected void computeAxisValues(float min, float max) {

        float yMin = min;
        float yMax = max;

        int labelCount = mAxis.getLabelCount();
        double range = Math.abs(yMax - yMin);

        if (labelCount == 0 || range <= 0 || Double.isInfinite(range)) {
            mAxis.mEntries = new float[]{};
            mAxis.mCenteredEntries = new float[]{};
            mAxis.mEntryCount = 0;
            mAxis.mMajorEntries = new float [] {};
            return;
        }

        // Find out how much spacing (in y value space) between axis values
        double rawInterval = range / labelCount;
        double interval = Utils.roundToNextSignificant(rawInterval);
        double majorInterval = interval / 2;
        // If granularity is enabled, then do not allow the interval to go below specified granularity.
        // This is used to avoid repeated values when rounding values for display.
        if (mAxis.isGranularityEnabled())
            interval = mAxis.getGranularity();

        // Normalize interval
        double intervalMagnitude = Utils.roundToNextSignificant(Math.pow(10, (int) Math.log10(interval)));
        int intervalSigDigit = (int) (interval / intervalMagnitude);
        if (intervalSigDigit > 5) {
            // Use one order of magnitude higher, to avoid intervals like 0.9 or 90
            // if it's 0.0 after floor(), we use the old value
            interval = Math.floor(10.0 * intervalMagnitude) == 0.0
                    ? interval
                    : Math.floor(10.0 * intervalMagnitude);

        }

        if(mAxis.isMajorGranularityEnabled()){
            majorInterval = mAxis.getMajorGranularity();
        }

        if(!mAxis.isMajorGranularityEnabled()){
            majorInterval = interval / 2;
        }

        int n = mAxis.isCenterAxisLabelsEnabled() ? 1 : 0;
        final double scale = Math.pow(10, 4);
        // force label count
        if (mAxis.isForceLabelsEnabled()) {
            interval = (float) range / (float) (labelCount - 1);
            mAxis.mEntryCount = labelCount;
            if (mAxis.mEntries.length < labelCount) {
                // Ensure stops contains at least numStops elements.
                mAxis.mEntries = new float[labelCount];
            }
            float v = min;
            for (int i = 0; i < labelCount; i++) {
                mAxis.mEntries[i] = v;
                v += interval;
            }
            if(mAxis.isDrawMajorGridLines()) {
                if (mAxis.mMajorEntries.length < labelCount - 1) {
                    mAxis.mMajorEntries = new float[labelCount - 1];
                }
                for (int i = 0; i < labelCount - 1; i++) {
                    mAxis.mMajorEntries[i] = (mAxis.mEntries[i + 1] - mAxis.mEntries[i]) / 2 + mAxis.mEntries[i];
                }
            }
            n = labelCount;
            // no forced count
        } else {
            double first = interval == 0.0 ? 0.0 : Math.ceil(yMin / interval) * interval;
            if(mAxis.isCenterAxisLabelsEnabled()) {
                first -= interval;
            }
            final double last = interval == 0.0 ? 0.0 : Utils.nextUp(Math.floor(yMax / interval) * interval);
            mAxis.mEntries = calculateEntries(first, last, interval, new ICheckContainsGridValue() {
                @Override
                public boolean checkValue(float value) {
                    return true;
                }
            });
            mAxis.mEntryCount = mAxis.mEntries.length;
            if(mAxis.isDrawMajorGridLines()) {
                final double finalFirst = first;
                final double finalInterval = interval;
                mAxis.mMajorEntries = calculateEntries(first - interval, last + interval, majorInterval, new ICheckContainsGridValue() {
                    @Override
                    public boolean checkValue(float value) {
                        //Log.e("DDDD", String.format("%s %s %s %s %s", (value % (last - finalFirst) / finalInterval),  temp % 1, value, last, finalFirst));
                        return Math.round((value % (last + finalInterval - (finalFirst - finalInterval)) / finalInterval) * scale) / scale % 1 != 0;
                    }
                });
            }
            n = mAxis.mEntryCount;
        }
        // set decimals
        if (interval < 1) {
            mAxis.mDecimals = (int) Math.ceil(-Math.log10(interval));
        } else {
            mAxis.mDecimals = 0;
        }
        if (mAxis.isCenterAxisLabelsEnabled()) {
            if (mAxis.mCenteredEntries.length < n) {
                mAxis.mCenteredEntries = new float[n];
            }
            float offset = (float)interval / 2f;
            for (int i = 0; i < n; i++) {
                mAxis.mCenteredEntries[i] = mAxis.mEntries[i] + offset;
            }
        }
    }

    public float [] calculateEntries(double first, double last, double interval, ICheckContainsGridValue checkValue){
        int n = 0;
        double f;
        int i;

        if (interval != 0.0 && last != first) {
            for (f = first; f <= last; f += interval) {
                ++n;
            }
        }
        else if (last == first) {
            n = 1;
        }

        float[] entries = new float[n];

        for (f = first, i = 0; i < n; f += interval, ++i) {

            if (f == 0.0) // Fix for negative zero case (Where value == -0.0, and 0.0 == -0.0)
                f = 0.0;

            if(checkValue.checkValue((float)f)){
                entries[i] = (float) f;
            }

        }

        return entries;
    }

    /**
     * Draws the axis labels to the screen.
     *
     * @param c
     */
    public abstract void renderAxisLabels(Canvas c);

    /**
     * Draws the grid lines belonging to the axis.
     *
     * @param c
     */
    public abstract void renderGridLines(Canvas c);


    /**
     * Draws the major grid lines belonging to the axis.
     *
     * @param c
     */
    public abstract void renderMajorGridLines(Canvas c);

    /**
     * Draws the line that goes alongside the axis.
     *
     * @param c
     */
    public abstract void renderAxisLine(Canvas c);

    protected abstract void setupGridPaint();

    protected abstract void setupMajorGridPaint();

    /**
     * Draws the LimitLines associated with this axis to the screen.
     *
     * @param c
     */
    public abstract void renderLimitLines(Canvas c);

    public Paint getMajorGridPaint() {
        return mMajorGridPaint;
    }
}
