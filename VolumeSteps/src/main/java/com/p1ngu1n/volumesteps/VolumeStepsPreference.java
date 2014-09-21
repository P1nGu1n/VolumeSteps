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

import java.util.Arrays;

/**
 * A preference for setting the maximum volume steps. You can choose from 5, 7, 15, 30, 45, 60, 75 and 90.
 */
public class VolumeStepsPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
    /** The volume steps to be used */
    private static final Integer[] volumeSteps = {5, 7, 15, 30, 45, 60, 75, 90};
    private int defaultValue;
    private int mValue;
    private TextView viewCurrentValue;

    public VolumeStepsPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(context, attrs);
    }

    public VolumeStepsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs);
    }

    /**
     * Setup this class
     * @param context The context to be used
     * @param attrs The attributes to be used
     */
    private void initialize(Context context, AttributeSet attrs) {
        setDialogLayoutResource(R.layout.seekbar_dialog_preference);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        defaultValue = a.getInt(index, 0);
        return defaultValue;
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
        String suffix = " steps";
        return mValue + suffix;
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
        mValue = value;
        setSummary(getSummary());
        persistInt(value);
        notifyChanged();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setNegativeButton(getContext().getResources().getString(R.string.default_reset), this);
    }

    @Override
    protected View onCreateDialogView() {
        View onCreateDialogView = super.onCreateDialogView();
        mValue = getPersistedInt(defaultValue);

        viewCurrentValue = (TextView) onCreateDialogView.findViewById(R.id.value_selected);
        viewCurrentValue.setText(getSummary());

        SeekBar seekBar = (SeekBar) onCreateDialogView.findViewById(R.id.preference_seekbar);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setMax(volumeSteps.length - 1);
        int index = Arrays.asList(volumeSteps).indexOf(mValue);
        seekBar.setProgress(index);

        return onCreateDialogView;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);

        if (which == DialogInterface.BUTTON_NEGATIVE) {
            mValue = defaultValue;
        }
        setValue(mValue);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            mValue = volumeSteps[progress];
            viewCurrentValue.setText(getSummary());
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) { }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) { }
}
