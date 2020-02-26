package wtf.janvr.zrakandroid;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

public class TimePickerFragment extends DialogFragment
        implements TimePickerDialog.OnTimeSetListener {
    static final public int START_TIME_PICKER = 0;
    static final public int STOP_TIME_PICKER = 1;
    private int mTag;

    public TimePickerFragment(int tag) {
        mTag = tag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getActivity(), this, hour, minute,
                DateFormat.is24HourFormat(getActivity()));
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        // Do something with the time chosen by the user
        String time_s = getString(R.string.time_string, hourOfDay, minute);
        switch (mTag) {
            case START_TIME_PICKER:
                TextView tv_start = getActivity().findViewById(R.id.device_start_time_text);
                tv_start.setText(time_s);
                break;
            case STOP_TIME_PICKER:
                TextView tv_stop = getActivity().findViewById(R.id.device_stop_time_text);
                tv_stop.setText(time_s);
                break;
        }
        return;
    }
}

