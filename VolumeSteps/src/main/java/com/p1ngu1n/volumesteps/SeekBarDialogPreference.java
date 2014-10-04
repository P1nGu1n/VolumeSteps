/*
 * Copyright (C) 2014  P1nGu1n
 *
 * This file is part of VolumeSteps+.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.p1ngu1n.volumesteps;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Preference showing a dialog containing a SeekBar.
 */
public class SeekBarDialogPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
    private TextView mValueTextView;
    private SeekBar mSeekBar;
    private int mDefaultValue;
    private int mValue;
    private int mMin = 0;
    private int mMax = 100;
    private String mFormat = "%1$s";

    public SeekBarDialogPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(context, attrs);
    }

    public SeekBarDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs);
    }

    /**
     * Setup this class
     * @param context The context to be used
     * @param attrs The attributes to be used
     */
    private void initialize(Context context, AttributeSet attrs) {
        setDialogLayoutResource(R.layout.seek_bar_dialog_preference);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SeekBarDialogPreference, 0, 0);
        mMin = a.getInt(R.styleable.SeekBarDialogPreference_min, mMin);
        mMax = a.getInt(R.styleable.SeekBarDialogPreference_max, mMax);
        String format = a.getString(R.styleable.SeekBarDialogPreference_summary_format);
        if (format != null) mFormat = format;
        a.recycle();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setNegativeButton(getContext().getResources().getString(R.string.default_reset), this);
    }

    @Override
    protected View onCreateDialogView() {
        View onCreateDialogView = super.onCreateDialogView();
        updateValue(getPersistedInt(mDefaultValue));

        mValueTextView = (TextView) onCreateDialogView.findViewById(R.id.value_selected);
        mValueTextView.setText(getSummary());

        mSeekBar = (SeekBar) onCreateDialogView.findViewById(R.id.preference_seekbar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setMax(mMax - mMin);
        mSeekBar.setProgress(translateValueToIndex(mValue));

        return onCreateDialogView;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (callChangeListener(mValue)) {
            setValue(mValue);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        if (which == DialogInterface.BUTTON_NEGATIVE) {
            updateValue(mDefaultValue);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            updateValue(translateIndexToValue(progress));
            mValueTextView.setText(getSummary());
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) { }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) { }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        mDefaultValue = a.getInt(index, 0);
        return mDefaultValue;
    }

    @Override
    public void setDefaultValue(Object newDefaultValue) {
        super.setDefaultValue(newDefaultValue);
        mDefaultValue = (Integer) newDefaultValue;
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(mValue) : (Integer) defaultValue);
    }

    /**
     * Creates a summary following the 'x steps' format.
     * @return The generated summary
     */
    @Override
    public CharSequence getSummary() {
        return String.format(mFormat, mValue);
    }

    /**
     * Updates the summary using the prefix, value and suffix.
     */
    private void updateSummary() {
        setSummary(getSummary());
    }

    /**
     * Translate the index from the SeekBar to the actual value.
     * @param index The index to translate
     * @return The value
     */
    private int translateIndexToValue(int index) {
        return mMin + index;
    }

    /**
     * Translate the actual value to the index for the SeekBar.
     * @param value The value to translate
     * @return The index
     */
    private int translateValueToIndex(int value) {
        return value - mMin;
    }

    /**
     * Get the value of the preference.
     * @return The current value
     */
    public int getValue() {
        return mValue;
    }

    /**
     * Set the value of the preference.
     * @param value The value to be set
     */
    public void setValue(int value) {
        updateValue(value);
        updateSummary();
        persistInt(value);
        notifyChanged();
    }

    /**
     * Updates the value internally.
     * @param value
     */
    private void updateValue(int value) {
        mValue = value;
    }

    /**
     * Set the minimum value of the SeekBar.
     * @param min The minimum value to set
     */
    public void setMin(int min) {
        mMin = min;
        mSeekBar.setProgress(getValue() - mMin);
        mSeekBar.setMax(mMax - mMin);
    }

    /**
     * Set the maximum value of the SeekBar.
     * @param max The maximum value to set
     */
    public void setMax(int max) {
        mMax = max;
        mSeekBar.setMax(mMax - mMin);
    }

    /**
     * Set the format for the summary
     * @param format The format to set
     */
    public void setSummaryFormat(String format) {
        mFormat = format;
        updateSummary();
    }
}
